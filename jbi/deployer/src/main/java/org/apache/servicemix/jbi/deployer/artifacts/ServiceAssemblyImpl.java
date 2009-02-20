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
import java.util.List;
import java.util.Map;

import javax.jbi.JBIException;
import javax.xml.namespace.QName;

import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.descriptor.Connection;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.descriptor.Provider;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceAssemblyDesc;
import org.apache.servicemix.jbi.deployer.impl.AssemblyReferencesListener;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Wire;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.core.util.MapToDictionary;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.Preferences;

/**
 * ServiceAssembly object
 */
public class ServiceAssemblyImpl extends AbstractLifecycleJbiArtifact implements ServiceAssembly {

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
        this.runningState = State.valueOf(this.prefs.get(STATE, (autoStart ? State.Started : State.Shutdown).name()));
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

    public synchronized void init() throws JBIException {
        listener.setAssembly(this);
        try {
            if (runningState == State.Started) {
                transition(Action.Init, State.Stopped);
                transition(Action.Start, State.Started);
            } else if (runningState == State.Stopped) {
                transition(Action.Init, State.Stopped);
            } else if (runningState == State.Shutdown) {
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
        listener.setAssembly(this);
        try {
            if (state == State.Started) {
                return;
            }
            if (state == State.Shutdown) {
                transition(Action.Init, State.Stopped);
            }
            startConnections();
            transition(Action.Start, State.Started);
            if (persist) {
                saveState();
            }
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
        try {
            if (state == State.Shutdown) {
                return;
            }
            if (state == State.Started) {
                transition(Action.Stop, State.Stopped);
            }
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
        } finally {
            listener.setAssembly(null);
            listener.forget(this);
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
                registerWire(wire, wire.getFrom());
            }
        }
    }
    
    private void stopConnections() {
        if (serviceAssemblyDesc.getConnections() != null && serviceAssemblyDesc.getConnections().getConnections() != null) {
            for (Connection connection : serviceAssemblyDesc.getConnections().getConnections()) {
                Wire wire = connection.getWire();
                unregisterWire(wire, wire.getFrom());
            }
        }
    }
    
    protected void registerWire(Wire wire, Map<String, ?> from) {
        bundle.getBundleContext().registerService(Wire.class.getName(), 
                                                  wire, new MapToDictionary(from));
    }
    
    protected void unregisterWire(Wire wire, Map<String, ?> from) {
        // TODO
    }
}