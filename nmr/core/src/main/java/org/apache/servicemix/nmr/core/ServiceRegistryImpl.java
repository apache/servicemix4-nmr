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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.nmr.api.ServiceMixException;
import org.apache.servicemix.nmr.api.service.ServiceRegistry;

/**
 * A very basic implementation of a ServiceRegistry that can be
 * inherited to add more specific methods if needed.
 */
public class ServiceRegistryImpl<T> implements ServiceRegistry<T> {

    private ConcurrentMap<T, Map<String, ?>> registry = new ConcurrentHashMap<T, Map<String, ?>>();

    private final Log logger = LogFactory.getLog(getClass());

    public void register(T service, Map<String, ?> properties) {
        assert service != null : "service should not be null";
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        if (registry.putIfAbsent(service, properties) == null) {
            try {
                doRegister(service, properties);
            } catch (Exception e) {
                logger.warn("Unable to register service " +
                        service + " with properties " + properties + ". Reason: " + e, e);
                registry.remove(service);
                throw new ServiceMixException("Unable to register service " +
                        service + " with properties " + properties + ". Reason: " + e, e);
            }
        }
    }

    /**
     * Placeholder to perform any registry specific operation
     * when a new service is registered.
     *
     * @param service
     * @param properties
     * @throws Exception
     */
    protected void doRegister(T service, Map<String, ?> properties) throws Exception {
    }

    public void unregister(T service, Map<String, ?> properties) {
        assert service != null : "service should not be null";
        if (service != null && registry.remove(service) != null) {
            try {
                doUnregister(service, properties);
            } catch (Exception e) {
                logger.warn("Unable to unregister service " +
                        service + " with properties " + properties + ". Reason: " + e, e);
                throw new ServiceMixException("Unable to unregister service " + 
                        service + " with properties " + properties + ". Reason: " + e, e);
            }
        }
    }

    /**
     * Placeholder to perform any registry specific operation
     * when a service is unregistered.
     *
     * @param service
     * @param properties
     * @throws Exception
     */
    protected void doUnregister(T service, Map<String, ?> properties) throws Exception {
    }

    public Set<T> getServices() {
        return registry.keySet();
    }

    public Map<String, ?> getProperties(T service) {
        return registry.get(service);
    }
}
