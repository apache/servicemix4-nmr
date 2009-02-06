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
package org.apache.servicemix.jbi.deployer.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.management.LifeCycleMBean;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.ObjectName;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.descriptor.ComponentDesc;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryList;
import org.apache.servicemix.jbi.runtime.ComponentWrapper;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 */
public class ComponentImpl implements Component, ComponentWrapper {

    private static final Log LOGGER = LogFactory.getLog(ComponentImpl.class);

    private static final String STATE = "state";

    protected enum State {
        Unknown,
        Initialized,
        Started,
        Stopped,
        Shutdown,
    }

    private ComponentDesc componentDesc;
    private javax.jbi.component.Component component;
    private State state = State.Unknown;
    private Preferences prefs;
    private State runningState;
    private Deployer deployer;
    private List<ServiceUnitImpl> serviceUnits;

    public ComponentImpl(ComponentDesc componentDesc,
                         javax.jbi.component.Component component,
                         Preferences prefs,
                         boolean autoStart,
                         Deployer deployer) {
        this.componentDesc = componentDesc;
        this.component = new ComponentWrapper(component);
        this.prefs = prefs;
        this.runningState = State.valueOf(this.prefs.get(STATE, (autoStart ? State.Started : State.Initialized).name()));
        this.deployer = deployer;
        this.serviceUnits = new ArrayList<ServiceUnitImpl>();
    }

    public void addServiceUnit(ServiceUnitImpl serviceUnit) {
        serviceUnits.add(serviceUnit);
    }

    public void removeServiceUnit(ServiceUnitImpl serviceUnit) {
        serviceUnits.remove(serviceUnit);
    }

    public ServiceUnit[] getServiceUnits() {
        return serviceUnits.toArray(new ServiceUnit[serviceUnits.size()]);
    }

    public String getName() {
        return componentDesc.getIdentification().getName();
    }

    public String getDescription() {
        return componentDesc.getIdentification().getDescription();
    }

    public ObjectName getExtensionMBeanName() throws JBIException {
        return component.getLifeCycle().getExtensionMBeanName();
    }

    public javax.jbi.component.Component getComponent() {
        return component;
    }

    public void start() throws JBIException {
        start(true);
    }

    public void start(boolean saveState) throws JBIException {
        LOGGER.info("Starting component " + getName());
        component.getLifeCycle().start();
        state = State.Started;
        if (saveState) {
            saveState();
        }
    }

    public void stop() throws JBIException {
        stop(true);
    }

    public Set<ServiceAssemblyImpl> getServiceAssemblies() {
        Set<ServiceAssemblyImpl> sas = new HashSet<ServiceAssemblyImpl>();
        for (ServiceUnitImpl su : serviceUnits) {
            sas.add(su.getServiceAssemblyImpl());
        }
        return sas;
    }
    
    public SharedLibraryList[] getSharedLibraries() {
        return componentDesc.getSharedLibraries();
    }

    public void stop(boolean saveState) throws JBIException {
        LOGGER.info("Stopping component " + getName());
        if (state == State.Started) {
            // Stop deployed SAs
            for (ServiceAssemblyImpl sa : getServiceAssemblies()) {
                if (sa.getState() == ServiceAssemblyImpl.State.Started) {
                    sa.stop(false);
                }
            }
            // Stop component
            component.getLifeCycle().stop();
            state = State.Stopped;
            if (saveState) {
                saveState();
            }
        }
    }

    public void shutDown() throws JBIException {
        shutDown(true, false);
    }

    public void forceShutDown() throws JBIException {
        shutDown(true, true);
    }

    public void shutDown(boolean saveState, boolean force) throws JBIException {
        LOGGER.info("Shutting down component " + getName());
        if (state == State.Started) {
            stop(saveState);
        }
        if (state == State.Stopped) {
            // Shutdown deployed SAs
            for (ServiceAssemblyImpl sa : getServiceAssemblies()) {
                if (sa.getState() == ServiceAssemblyImpl.State.Stopped
                        || sa.getState() == ServiceAssemblyImpl.State.Initialized) {
                    sa.shutDown(false, force);
                }
            }
            // Shutdown component
            component.getLifeCycle().shutDown();
            state = State.Shutdown;
            if (saveState) {
                saveState();
            }
        }
    }

    private void saveState() {
        this.prefs.put(STATE, state.name());
        try {
            this.prefs.flush();
        } catch (BackingStoreException e) {
            LOGGER.warn("Unable to persist state", e);
        }
    }

    public String getCurrentState() {
        switch (state) {
            case Started:
                return LifeCycleMBean.STARTED;
            case Stopped:
                return LifeCycleMBean.STOPPED;
            case Initialized:
            case Shutdown:
                return LifeCycleMBean.SHUTDOWN;
            default:
                return LifeCycleMBean.UNKNOWN;
        }
    }

    public State getState() {
        return state;
    }

    protected class ComponentWrapper implements javax.jbi.component.Component, ComponentLifeCycle {
        private javax.jbi.component.Component component;
        private ComponentLifeCycle lifeCycle;

        public ComponentWrapper(javax.jbi.component.Component component) {
            this.component = component;
        }

        public ComponentLifeCycle getLifeCycle() {
            if (lifeCycle == null) {
                lifeCycle = component.getLifeCycle();
            }
            return this;
        }

        public ServiceUnitManager getServiceUnitManager() {
            return component.getServiceUnitManager();
        }

        public Document getServiceDescription(ServiceEndpoint endpoint) {
            return component.getServiceDescription(endpoint);
        }

        public boolean isExchangeWithConsumerOkay(ServiceEndpoint endpoint, MessageExchange exchange) {
            return component.isExchangeWithConsumerOkay(endpoint, exchange);
        }

        public boolean isExchangeWithProviderOkay(ServiceEndpoint endpoint, MessageExchange exchange) {
            return component.isExchangeWithProviderOkay(endpoint, exchange);
        }

        public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
            return component.resolveEndpointReference(epr);
        }

        public ObjectName getExtensionMBeanName() {
            return lifeCycle.getExtensionMBeanName();
        }

        public void init(ComponentContext context) throws JBIException {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
                lifeCycle.init(context);
                state = State.Initialized;
                if (runningState == State.Started) {
                    start();
                    state = State.Started;
                } else if (runningState == State.Stopped) {
                    start();
                    state = State.Started;
                    stop();
                    state = State.Stopped;
                } else if (runningState == State.Shutdown) {
                    shutDown();
                    state = State.Shutdown;
                }
                deployer.checkPendingBundles();
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }

        public void shutDown() throws JBIException {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
                lifeCycle.shutDown();
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }

        public void start() throws JBIException {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
                lifeCycle.start();
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }

        public void stop() throws JBIException {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
                lifeCycle.stop();
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
    }

}
