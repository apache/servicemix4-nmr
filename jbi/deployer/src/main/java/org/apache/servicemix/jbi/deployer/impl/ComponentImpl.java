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

import javax.jbi.JBIException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.LifeCycleMBean;
import javax.management.ObjectName;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.descriptor.ComponentDesc;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 3, 2008
 * Time: 5:15:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ComponentImpl implements Component {

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

    public ComponentImpl(ComponentDesc componentDesc, javax.jbi.component.Component component) {
        this.componentDesc = componentDesc;
        this.component = component;
    }

    public String getName() {
        return componentDesc.getIdentification().getName();
    }

    public String getDescription() {
        return componentDesc.getIdentification().getDescription();
    }

    public ObjectName getExtensionMBeanName() throws JBIException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public javax.jbi.component.Component getComponent() {
        return component;
    }

    public void start() throws JBIException {
        component.getLifeCycle().start();
        state = State.Started;
    }

    public void stop() throws JBIException {
        if (state == State.Started) {
            component.getLifeCycle().stop();
            state = State.Stopped;
        }
    }

    public void shutDown() throws JBIException {
        if (state == State.Started) {
            stop();
        }
        if (state == State.Stopped) {
            component.getLifeCycle().shutDown();
            state = State.Shutdown;
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
            lifeCycle.init(context);
            state = State.Initialized;
        }

        public void shutDown() throws JBIException {
            lifeCycle.shutDown();
        }

        public void start() throws JBIException {
            lifeCycle.start();
        }

        public void stop() throws JBIException {
            lifeCycle.stop();
        }
    }

}
