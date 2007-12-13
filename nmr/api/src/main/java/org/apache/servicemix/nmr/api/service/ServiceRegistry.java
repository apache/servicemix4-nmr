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
package org.apache.servicemix.nmr.api.service;

import java.util.Map;
import java.util.Set;

/**
 * Templated registry to hold services and their associated metadata.
 * In an OSGi environment, services would be registered and unregistered
 * automatically using a service tracker.
 *
 * @version $Revision: $
 * @since 4.0
 */
public interface ServiceRegistry<T> {

    /**
     * Register a service with the given metadata.
     *
     * @param service the service to register
     * @param properties the associated metadata
     */
    void register(T service, Map<String, ?> properties);

    /**
     * Unregister a previously registered service.
     *
     * @param service the service to unregister
     */
    void unregister(T service, Map<String, ?> properties);

    /**
     * Get a set of registered services.
     *
     * @return the registered services
     */
    Set<T> getServices();

    /**
     * Retrieve the metadata associated to a registered service.
     *
     * @param service the service for which to retrieve metadata
     * @return the metadata associated with the service
     */
    Map<String, ?> getProperties(T service);

}
