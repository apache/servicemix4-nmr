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
import java.util.HashSet;
import java.util.Set;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.ObjectName;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.events.LifeCycleEvent;
import org.apache.servicemix.jbi.deployer.descriptor.ComponentDesc;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.runtime.ComponentWrapper;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.Preferences;

/**
 */
public class ComponentImpl extends AbstractLifecycleJbiArtifact implements Component, ComponentWrapper {

    private Bundle bundle;
    private ComponentDesc componentDesc;
    private javax.jbi.component.Component component;
    private Set<ServiceUnitImpl> serviceUnits;
    private SharedLibrary[] sharedLibraries;
    private boolean restoreState = true;
    private ClassLoader componentClassLoader;

    public ComponentImpl(Bundle bundle,
                         ComponentDesc componentDesc,
                         javax.jbi.component.Component component,
                         Preferences prefs,
                         boolean autoStart,
                         SharedLibrary[] sharedLibraries) {
        this.bundle = bundle;
        this.componentDesc = componentDesc;
        this.component = new ComponentWrapper(component);
        this.prefs = prefs;
        this.runningState = loadState(autoStart ? State.Started : State.Shutdown);
        this.serviceUnits = new HashSet<ServiceUnitImpl>();
        this.sharedLibraries = sharedLibraries;
        this.componentClassLoader = component.getClass().getClassLoader();
    }

    public ClassLoader getComponentClassLoader() {
        return componentClassLoader;
    }

    public void addServiceUnit(ServiceUnitImpl serviceUnit) {
        serviceUnits.add(serviceUnit);
    }

    public void removeServiceUnit(ServiceUnitImpl serviceUnit) {
        serviceUnits.remove(serviceUnit);
    }

    public Bundle getBundle() {
        return bundle;
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

    public String getDescriptor() {
        URL url = bundle.getResource(DescriptorFactory.DESCRIPTOR_FILE);
        return DescriptorFactory.getDescriptorAsText(url);
    }

    public String getType() {
        return componentDesc.getType();
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
        if (state != State.Started) {
            if (state == State.Shutdown) {
                component.getLifeCycle().init(null);
            }
            fireEvent(LifeCycleEvent.LifeCycleEventType.Starting);
            component.getLifeCycle().start();
            state = State.Started;
            if (saveState) {
                saveState();
            }
            fireEvent(LifeCycleEvent.LifeCycleEventType.Started);
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

    public SharedLibrary[] getSharedLibraries() {
        return sharedLibraries;
    }

    public void stop(boolean saveState) throws JBIException {
        LOGGER.info("Stopping component " + getName());
        if (state == State.Started) {
            fireEvent(LifeCycleEvent.LifeCycleEventType.Stopping);
            // Stop component
            component.getLifeCycle().stop();
            state = State.Stopped;
            if (saveState) {
                saveState();
            }
            fireEvent(LifeCycleEvent.LifeCycleEventType.Stopped);
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
            fireEvent(LifeCycleEvent.LifeCycleEventType.ShuttingDown, force);
            // Shutdown component
            component.getLifeCycle().shutDown();
            state = State.Shutdown;
            if (saveState) {
                saveState();
            }
            fireEvent(LifeCycleEvent.LifeCycleEventType.ShutDown);
        }
    }

    protected class ComponentWrapper implements javax.jbi.component.Component, ComponentLifeCycle {
        private javax.jbi.component.Component component;
        private ComponentLifeCycle lifeCycle;
        private ComponentContext context;

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
            ComponentContext contextToUse = context;
            if (this.context == null) {
                this.context = context;
            }
            if (contextToUse == null) {
                contextToUse = this.context;
            }
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getComponentClassLoader());
                if (restoreState) {
                    State stateToUse = context != null ? runningState : State.Shutdown;
                    switch (stateToUse) {
                        case Started:
                            fireEvent(LifeCycleEvent.LifeCycleEventType.Starting);
                            lifeCycle.init(contextToUse);
                            start();
                            state = State.Started;
                            fireEvent(LifeCycleEvent.LifeCycleEventType.Started);
                            break;
                        case Stopped:
                            fireEvent(LifeCycleEvent.LifeCycleEventType.Stopping);
                            lifeCycle.init(contextToUse);
                            start();
                            stop();
                            state = State.Stopped;
                            fireEvent(LifeCycleEvent.LifeCycleEventType.Stopped);
                            break;
                        case Shutdown:
                            state = State.Shutdown;
                            break;
                    }
                    restoreState = false;
                } else {
                    lifeCycle.init(contextToUse);
                }
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
