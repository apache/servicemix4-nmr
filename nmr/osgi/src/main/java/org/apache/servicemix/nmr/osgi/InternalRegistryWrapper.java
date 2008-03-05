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
package org.apache.servicemix.nmr.osgi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.service.ServiceRegistry;
import org.apache.servicemix.nmr.core.ServiceRegistryImpl;
import org.apache.servicemix.nmr.core.util.MapToDictionary;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.context.BundleContextAware;

/**
 */
public class InternalRegistryWrapper extends ServiceRegistryImpl<InternalEndpoint>
                                     implements ServiceRegistry<InternalEndpoint>, BundleContextAware {

    private BundleContext bundleContext;
    private Map<Endpoint, ServiceRegistration> registrations = new ConcurrentHashMap();

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    protected void doRegister(InternalEndpoint endpoint, Map<String, ?> properties) {
        ServiceRegistration reg = bundleContext.registerService(
                                      InternalEndpoint.class.getName(),
                                      endpoint,
                                      new MapToDictionary(properties));
        registrations.put(endpoint, reg);
    }

    @Override
    protected void doUnregister(InternalEndpoint endpoint, Map<String, ?> properties) {
        ServiceRegistration reg = registrations.remove(endpoint);
        reg.unregister();
        super.unregister(endpoint, properties);
    }

}
