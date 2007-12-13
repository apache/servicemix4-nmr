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
package org.apache.servicemix.nmr.api;

import org.apache.servicemix.nmr.api.service.ServiceRegistry;

import org.w3c.dom.Document;

import java.util.Map;

/**
 * The Registry is used to register endpoints, unregister them, query endpoints
 * and create a Channel to interfact with them.
 *
 * @version $Revision: $
 * @since 4.0
 */
public interface EndpointRegistry extends ServiceRegistry<Endpoint> {

    /**
     * Register the given endpoint in the registry.
     * In an OSGi world, this would be performed automatically by a ServiceTracker.
     * Upon registration, a {@link Channel} will be injected onto the Endpoint using
     * the {@link Endpoint#setChannel(Channel)} method.
     *
     * @param endpoint the endpoint to register
     * @param properties the metadata associated with this endpoint
     */
    void register(Endpoint endpoint, Map<String, ?> properties);

    /**
     * Unregister a previously register enpoint.
     * In an OSGi world, this would be performed automatically by a ServiceTracker.
     *
     * @param endpoint the endpoint to unregister
     */
    void unregister(Endpoint endpoint, Map<String, ?> properties);

    /**
     * From a given amount of metadata which could include interface name, service name
     * policy data and so forth, choose an available endpoint reference to use
     * for invocations.
     *
     * This could return actual endpoints, or a dynamic proxy to a number of endpoints
     */
    Reference lookup(Map<String, ?> properties);

    /**
     * This methods creates a Reference from its xml representation.
     *
     * @see Reference#toXml()
     * @param xml the xml document describing this reference
     * @return a new Reference
     */
    Reference lookup(Document xml);

}

