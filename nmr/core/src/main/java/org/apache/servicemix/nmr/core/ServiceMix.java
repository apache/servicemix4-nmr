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
package org.apache.servicemix.nmr.core;

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.WireRegistry;
import org.apache.servicemix.nmr.api.event.ListenerRegistry;
import org.apache.servicemix.nmr.api.internal.Flow;
import org.apache.servicemix.nmr.api.internal.FlowRegistry;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.executors.ExecutorFactory;
import org.apache.servicemix.executors.impl.ExecutorFactoryImpl;

/**
 * This class is the servicemix class implementing the NMR
 */
public class ServiceMix implements NMR {

    public static final String EXECUTOR_CLIENT = "nmr.client";

    private EndpointRegistry endpoints;
    private ListenerRegistry listeners;
    private FlowRegistry flows;
    private WireRegistry wires;
    private ExecutorFactory executorFactory;

    /**
     * Initialize ServiceMix
     */
    public void init() {
        if (executorFactory == null) {
            executorFactory = new ExecutorFactoryImpl();
        }
        if (endpoints == null) {
            EndpointRegistryImpl reg = new EndpointRegistryImpl(this);
            reg.setExecutorFactory(executorFactory);
            reg.init();
            endpoints = reg;
        }
        if (listeners == null) {
            listeners = new ListenerRegistryImpl();
        }
        if (flows == null) {
            FlowRegistryImpl reg = new FlowRegistryImpl();
            reg.setRegistry(endpoints);
            flows = reg;
            flows.register(new StraightThroughFlow(), ServiceHelper.createMap(Flow.ID, StraightThroughFlow.class.getName()));
        }
        if (wires == null) {
            wires = new WireRegistryImpl();
        }
    }

    /**
     * Access the endpoint registry.
     *
     * @return the endpoint registry
     */
    public EndpointRegistry getEndpointRegistry() {
        return endpoints;
    }

    /**
     * Set the endpoint registry
     *
     * @param endpoints the endpoint registry
     */
    public void setEndpointRegistry(EndpointRegistry endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Access the listener registry.
     *
     * @return the listener registry
     */
    public ListenerRegistry getListenerRegistry() {
        return listeners;
    }

    /**
     * Set the listener registry
     *
     * @param listeners the listener registry
     */
    public void setListenerRegistry(ListenerRegistry listeners) {
        this.listeners = listeners;
    }

    /**
     * Access the flow registry.
     *
     * @return the flow registry
     */
    public FlowRegistry getFlowRegistry() {
        return flows;
    }

    /**
     * Set the flow registry
     *
     * @param flows the flow registry
     */
    public void setFlowRegistry(FlowRegistry flows) {
        this.flows = flows;
    }


    /**
     * Access the executor factory
     *
     * @return the executor factory
     */
    public ExecutorFactory getExecutorFactory() {
        return executorFactory;
    }

    /**
     * Set the executor factory
     *
     * @param executorFactory the executor factory
     */
    public void setExecutorFactory(ExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
    }

    /**
     * Create a channel to interact with the NMR without exposing an endpoint.
     *
     * @return a channel
     */
    public Channel createChannel() {
        return new ClientChannel(this, executorFactory.createExecutor(EXECUTOR_CLIENT));
    }

    /**
     * {@inheritDoc}
     */
    public WireRegistry getWireRegistry() {
        return wires;
    }

    /**
     * Set the wire registry
     * 
     * @param wires the wire registry
     */
    public void setWireRegistry(WireRegistry wires) {
        this.wires = wires;
    }
}
