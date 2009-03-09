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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.Reference;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.core.util.MapToDictionary;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * A wrapper around an EndpointRegistry.
 * Instead of directly registering endpoints in the
 * delegate registry, this one will register the
 * endpoints in the OSGi registry.
 *
 * @see NMRWrapper
 *
 */
public class RegistryWrapper implements EndpointRegistry {

    private static final Log LOG = LogFactory.getLog(RegistryWrapper.class);

    private EndpointRegistry registry;
    private BundleContext bundleContext;
    private Map<Endpoint, ServiceRegistration> registrations = new ConcurrentHashMap<Endpoint, ServiceRegistration>();

    public RegistryWrapper(EndpointRegistry registry, BundleContext bundleContext) {
        this.registry = registry;
        this.bundleContext = bundleContext;
    }

    public void register(Endpoint endpoint, Map<String, ?> properties) {
        ServiceRegistration reg = bundleContext.registerService(
                                      Endpoint.class.getName(),
                                      endpoint,
                                      new MapToDictionary(properties));
        registrations.put(endpoint, reg);
    }

    public void unregister(Endpoint endpoint, Map<String, ?> properties) {
        ServiceRegistration reg = registrations.remove(endpoint);
        if (reg == null && endpoint instanceof InternalEndpoint) {
            reg = registrations.remove(((InternalEndpoint) endpoint).getEndpoint());
        }
        if (reg != null) {
            reg.unregister();
        } else {
            LOG.warn("Unregistration failed: the endpoint was not found in registry: " + endpoint + " (" + properties + ")");
            registry.unregister(endpoint, properties);
        }
    }

    public List<Endpoint> query(Map<String, ?> properties) {
        return registry.query(properties);
    }

    public Reference lookup(Map<String, ?> properties) {
        return registry.lookup(properties);
    }

    public Reference lookup(Document xml) {
        return registry.lookup(xml);
    }

    public Reference lookup(String filter) {
        return registry.lookup(filter);
    }

    public Map<String, ?> getProperties(Endpoint service) {
        return registry.getProperties(service);
    }

    public Set<Endpoint> getServices() {
        return registry.getServices();
    }    
}
