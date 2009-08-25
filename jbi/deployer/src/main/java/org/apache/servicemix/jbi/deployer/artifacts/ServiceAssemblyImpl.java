/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jbi.deployer.artifacts;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.jbi.JBIException;
import javax.jbi.management.LifeCycleMBean;

import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.descriptor.Connection;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceAssemblyDesc;
import org.apache.servicemix.jbi.deployer.events.LifeCycleEvent;
import org.apache.servicemix.nmr.api.Wire;
import org.apache.servicemix.nmr.core.util.MapToDictionary;
import org.apache.servicemix.nmr.management.Nameable;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.prefs.Preferences;

/**
 * ServiceAssembly object
 */
public class ServiceAssemblyImpl extends AbstractLifecycleJbiArtifact implements ServiceAssembly, Nameable {

    private enum Action {
        Init,
        Start,
        Stop,
        Shutdown;

        public Action reverse() {
            switch (this) {
                case Init:
                    return Shutdown;
                case Start:
                    return Stop;
                case Stop:
                    return Start;
                case Shutdown:
                    return Init;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private final Bundle bundle;

    private final ServiceAssemblyDesc serviceAssemblyDesc;

    private final List<ServiceUnitImpl> serviceUnits;

    private final AssemblyReferencesListener listener;

    // map of wires and the matching OSGi ServiceRegistration
    private Map<Wire, ServiceRegistration> wires = new HashMap<Wire, ServiceRegistration>();

    private int shutdownTimeout;

    public ServiceAssemblyImpl(Bundle bundle,
                               ServiceAssemblyDesc serviceAssemblyDesc,
                               List<ServiceUnitImpl> serviceUnits,
                               Preferences prefs,
                               AssemblyReferencesListener listener,
                               boolean autoStart) {
        this.bundle = bundle;
        this.serviceAssemblyDesc = serviceAssemblyDesc;
        this.serviceUnits = serviceUnits;
        this.prefs = prefs;
        this.listener = listener;
        this.runningState = loadState(autoStart ? State.Started : State.Shutdown);
        for (ServiceUnitImpl su : serviceUnits) {
            su.setServiceAssemblyImpl(this);
        }
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getName() {
        return serviceAssemblyDesc.getIdentification().getName();
    }

    public String getDescription() {
        return serviceAssemblyDesc.getIdentification().getDescription();
    }

    public String getDescriptor() {
        URL url = bundle.getResource(DescriptorFactory.DESCRIPTOR_FILE);
        return DescriptorFactory.getDescriptorAsText(url);
    }

    public ServiceUnit[] getServiceUnits() {
        return serviceUnits.toArray(new ServiceUnit[serviceUnits.size()]);
    }

    public List<ServiceUnitImpl> getServiceUnitsList() {
        return serviceUnits;
    }
    
    public void setShutdownTimeout(int shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    public synchronized void init() throws JBIException {
        checkComponentsStarted();
        listener.setAssembly(this);
        try {
            if (runningState == State.Started) {
                transition(Action.Init, State.Stopped);
                transition(Action.Start, State.Started);
            } else if (runningState == State.Stopped) {
                transition(Action.Init, State.Stopped);
            } else if (runningState == State.Shutdown) {
                transition(Action.Init, State.Stopped);
                transition(Action.Shutdown, State.Shutdown);
                state = State.Shutdown;
            }
        } finally {
            listener.setAssembly(null);
        }
    }

    public void start() throws JBIException {
        start(true);
    }

    public synchronized void start(boolean persist) throws JBIException {
        checkComponentsStarted();
        listener.setAssembly(this);
        try {
            if (state == State.Started) {
                return;
            }
            if (state == State.Shutdown) {
                transition(Action.Init, State.Stopped);
            }
            fireEvent(LifeCycleEvent.LifeCycleEventType.Starting);
            startConnections();
            transition(Action.Start, State.Started);
            if (persist) {
                saveState();
            }
            fireEvent(LifeCycleEvent.LifeCycleEventType.Started);
        } finally {
            listener.setAssembly(null);
        }
    }

    public void stop() throws JBIException {
        stop(true);
    }

    public synchronized void stop(boolean persist) throws JBIException {
        listener.setAssembly(this);
        try {
            if (state == State.Stopped) {
                return;
            }
            fireEvent(LifeCycleEvent.LifeCycleEventType.Stopping);
            if (state == State.Shutdown) {
                transition(Action.Init, State.Stopped);
            }
            if (state == State.Started) {
                transition(Action.Stop,  State.Stopped);
            }
            stopConnections();
            if (persist) {
                saveState();
            }
            fireEvent(LifeCycleEvent.LifeCycleEventType.Stopped);
        } finally {
            listener.setAssembly(null);
        }
    }

    public void shutDown() throws JBIException {
        shutDown(true, false);
    }

    public void forceShutDown() throws JBIException {
        shutDown(true, true);
    }

    public synchronized void shutDown(boolean persist, boolean force) throws JBIException {
        listener.setAssembly(this);
        final Semaphore semaphore = force && shutdownTimeout > 0 ? startShutdownMonitorThread() : null;
        try {
            if (state == State.Shutdown) {
                return;
            }
            if (state == State.Started) {
                transition(Action.Stop, State.Stopped);
            }
            fireEvent(LifeCycleEvent.LifeCycleEventType.ShuttingDown);
            if (!force) {
                for (; ;) {
                    try {
                        listener.waitFor(this);
                        break;
                    } catch (InterruptedException e) {
                    }
                }
            }
            transition(Action.Shutdown, State.Shutdown);
            if (persist) {
                saveState();
            }
            fireEvent(LifeCycleEvent.LifeCycleEventType.ShutDown);
        } finally {
            listener.setAssembly(null);
            listener.forget(this);
            
            //notify the shutdown monitor thread that things ended correctly
            if (semaphore != null) {
                semaphore.release();
            }
        }
    }

    protected void checkComponentsStarted() throws JBIException {
        Set<String> names = new HashSet<String>();
        for (ServiceUnitImpl su : serviceUnits) {
            if (su.getComponent() == null) {
                throw new JBIException("SU has not been correctly deployed: " + su.getName());
            }
            if (!LifeCycleMBean.STARTED.equals(su.getComponent().getCurrentState())) {
                names.add(su.getComponentName());
            }
        }
        if (!names.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String name : names) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(name);
            }
            throw new JBIException("Components are not started: " + sb.toString());
        }
    }

    protected void transition(Action action, State to) throws JBIException {
        LOGGER.info("Changing SA state to " + to);
        State from = state;
        List<ServiceUnitImpl> success = new ArrayList<ServiceUnitImpl>();
        for (ServiceUnitImpl su : serviceUnits) {
            try {
                changeState(su, action);
                success.add(su);
            } catch (JBIException e) {
                if (from != State.Unknown) {
                    for (ServiceUnitImpl su2 : success) {
                        try {
                            changeState(su2, action.reverse());
                        } catch (JBIException e2) {
                            // Ignore
                        }
                    }
                }
                throw e;
            }
        }
        state = to;
    }

    protected void changeState(ServiceUnitImpl su, Action action) throws JBIException {
        switch (action) {
            case Init:
                su.init();
                break;
            case Start:
                su.start();
                break;
            case Stop:
                su.stop();
                break;
            case Shutdown:
                su.shutdown();
                break;
        }
    }
    
    private void startConnections() {
        if (serviceAssemblyDesc.getConnections() != null && serviceAssemblyDesc.getConnections().getConnections() != null) {
            for (Connection connection : serviceAssemblyDesc.getConnections().getConnections()) {
                Wire wire = connection.getWire();
                wires.put(wire, registerWire(wire));
            }
        }
    }
    
    private void stopConnections() {
        for (Wire wire : wires.keySet()) {
            wires.get(wire).unregister();
        }
    }
    
    protected ServiceRegistration registerWire(Wire wire) {
        return bundle.getBundleContext().registerService(Wire.class.getName(), 
                                                         wire, new MapToDictionary(wire.getFrom()));
    }
    
    /*
     * Start the shutdown monitor thread and return a semaphore to notify the thread of a clean shutdown
     */
    private Semaphore startShutdownMonitorThread() {
        final Semaphore semaphore = new Semaphore(0);
        Thread thread = new Thread(getName()  + " - Shutdown Monitor Thread") {
            @Override
            public void run() {
                try {
                    LOGGER.debug("Waiting for " + shutdownTimeout + " milliseconds to a clean shutdown of SA " + ServiceAssemblyImpl.this.getName());
                    if (!semaphore.tryAcquire(shutdownTimeout, TimeUnit.MILLISECONDS)) {
                        LOGGER.warn("Unable to do a clean shutdown of SA " + ServiceAssemblyImpl.this.getName() + ", canceling all sync exchanges");
                        listener.cancelPendingSyncExchanges(ServiceAssemblyImpl.this);                        
                    }
                } catch (InterruptedException e) {
                    //let's assume things went OK if we got interrupted
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
        return semaphore;
    }
    
    public String toString() {
        return getName();
    }

    public String getParent() {
        return null;
    }

    public String getType() {
        return "ServiceAssembly";
    }
    
    public String getSubType() {
        return null;
    }
    
    public String getVersion() {
        return null;
    }

    public Class getPrimaryInterface() {
        return ServiceAssembly.class;
    }
}
