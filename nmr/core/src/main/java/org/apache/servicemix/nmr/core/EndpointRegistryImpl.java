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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.w3c.dom.Document;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.Reference;
import org.apache.servicemix.nmr.api.ServiceMixException;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.service.ServiceRegistry;

/**
 * @version $Revision: $
 * @since 4.0
 */
public class EndpointRegistryImpl implements EndpointRegistry {

    private NMR nmr;
    private Map<Endpoint, InternalEndpoint> endpoints = new ConcurrentHashMap<Endpoint, InternalEndpoint>();
    private Map<InternalEndpoint, Endpoint> wrappers = new ConcurrentHashMap<InternalEndpoint, Endpoint>();
    private ServiceRegistry<InternalEndpoint> registry = new ServiceRegistryImpl<InternalEndpoint>();

    public EndpointRegistryImpl() {
    }

    public EndpointRegistryImpl(NMR nmr) {
        this.nmr = nmr;
    }

    public NMR getNmr() {
        return this.nmr;
    }

    public void setNmr(NMR nmr) {
        this.nmr = nmr;
    }

    public void init() {
        // TODO: check nmr
    }

    /**
     * Register the given endpoint in the registry.
     * In an OSGi world, this would be performed automatically by a ServiceTracker.
     * Upon registration, a {@link org.apache.servicemix.nmr.api.Channel} will be injected onto the Endpoint using
     * the {@link org.apache.servicemix.nmr.api.Endpoint#setChannel(org.apache.servicemix.nmr.api.Channel)} method.
     *
     * @param endpoint   the endpoint to register
     * @param properties the metadata associated with this endpoint
     * @see org.apache.servicemix.nmr.api.Endpoint
     */
    public void register(Endpoint endpoint, Map<String, ?> properties) {
        Executor executor = Executors.newCachedThreadPool();
        InternalEndpointWrapper wrapper = new InternalEndpointWrapper(endpoint);
        ChannelImpl channel = new ChannelImpl(wrapper, executor, nmr);
        wrapper.setChannel(channel);
        endpoints.put(endpoint, wrapper);
        wrappers.put(wrapper, endpoint);
        registry.register(wrapper, properties);
    }

    /**
     * Unregister a previously register enpoint.
     * In an OSGi world, this would be performed automatically by a ServiceTracker.
     *
     * @param endpoint the endpoint to unregister
     */
    public void unregister(Endpoint endpoint, Map<String, ?> properties) {
        InternalEndpoint wrapper;
        if (endpoint instanceof InternalEndpoint) {
            wrapper = (InternalEndpoint) endpoint;
            endpoint = wrappers.remove(wrapper);
            endpoints.remove(endpoint);
        } else {
            wrapper = endpoints.remove(endpoint);
            wrappers.remove(wrapper);
        }
        registry.unregister(wrapper, properties);
    }

    /**
     * Get a set of registered services.
     *
     * @return the registered services
     */
    public Set<Endpoint> getServices() {
        return null;  // TODO
    }

    /**
     * Retrieve the metadata associated to a registered service.
     *
     * @param endpoint the service for which to retrieve metadata
     * @return the metadata associated with the=is endpoint
     */
    public Map<String, ?> getProperties(Endpoint endpoint) {
        InternalEndpoint wrapper;
        if (endpoint instanceof InternalEndpoint) {
            wrapper = (InternalEndpoint) endpoint;
        } else {
            wrapper = endpoints.get(endpoint);
        }
        return registry.getProperties(wrapper);
    }

    /**
     * From a given amount of metadata which could include interface name, service name
     * policy data and so forth, choose an available endpoint reference to use
     * for invocations.
     * <p/>
     * This could return actual endpoints, or a dynamic proxy to a number of endpoints
     */
    public Reference lookup(Map<String, ?> properties) {
        List<InternalEndpoint> endpoints = new ArrayList<InternalEndpoint>();
        for (InternalEndpoint e : registry.getServices()) {
            boolean match = true;
            for (String name : properties.keySet()) {
                if (!properties.get(name).equals(registry.getProperties(e).get(name))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                endpoints.add(e);
            }
        }
        if (endpoints.isEmpty()) {
            throw new ServiceMixException("No matching endpoints");
        }
        return new ReferenceImpl(endpoints);
    }

    /**
     * This methods creates a Reference from its xml representation.
     *
     * @param xml the xml document describing this reference
     * @return a new Reference
     * @see org.apache.servicemix.nmr.api.Reference#toXml()
     */
    public synchronized Reference lookup(Document xml) {
        // TODO: implement
        return null;
    }

}
