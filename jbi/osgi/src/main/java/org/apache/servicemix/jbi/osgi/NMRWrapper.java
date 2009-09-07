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
package org.apache.servicemix.jbi.osgi;

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.WireRegistry;
import org.apache.servicemix.nmr.api.event.ListenerRegistry;
import org.apache.servicemix.nmr.api.internal.FlowRegistry;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;

/**
 * A NMR wrapper which purpose is to delegate to the OSGi registry
 * instead of registering the endpoints directly.
 * This way, the JBI layer is decoupled from OSGi, while still
 * having the endpoints registered in the OSGi registry.
 */
public class NMRWrapper implements NMR {

    private NMR nmr;
    private BundleContext bundleContext;
    private EndpointRegistry registry;

    public void setNmr(NMR nmr) {
        this.nmr = nmr;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() throws Exception {
        if (this.nmr == null) {
            throw new IllegalArgumentException("nmr must be set");
        }
        if (this.bundleContext == null) {
            throw new IllegalArgumentException("bundleContext must be set");
        }
        this.registry = new RegistryWrapper(nmr.getEndpointRegistry(), bundleContext);
    }

    public EndpointRegistry getEndpointRegistry() {
        return registry;
    }

    public ListenerRegistry getListenerRegistry() {
        return nmr.getListenerRegistry();
    }

    public FlowRegistry getFlowRegistry() {
        return nmr.getFlowRegistry();
    }
    
    public WireRegistry getWireRegistry() {
        return nmr.getWireRegistry();
    }

    public Channel createChannel() {
        return nmr.createChannel();
    }

    public String getId() {
        return nmr.getId();
    }
}
