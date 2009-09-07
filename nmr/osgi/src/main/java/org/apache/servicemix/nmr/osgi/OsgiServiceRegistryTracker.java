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

import org.apache.servicemix.nmr.api.service.ServiceRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;

/**
 * A simple spring factory bean that will create an OSGi service tracker and notify the configured registry
 * when services are registered / unregistered in the OSGi registry.  This avoid using spring proxies which are
 * not always needed especially in our case.
 */
public class OsgiServiceRegistryTracker<T> implements ServiceTrackerCustomizer {

    private BundleContext bundleContext;
    private ServiceRegistry<T> registry;
    private Class clazz;
    private ServiceTracker tracker;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public Class getInterface() {
        return clazz;
    }

    public void setInterface(Class clazz) {
        this.clazz = clazz;
    }

    public ServiceRegistry<T> getRegistry() {
        return registry;
    }

    public void setRegistry(ServiceRegistry<T> registry) {
        this.registry = registry;
    }

    public void init() throws Exception {
        tracker = new ServiceTracker(bundleContext, clazz.getName(), this);
        tracker.open();
    }

    public void destroy() throws Exception {
        tracker.close();
    }

    public Object addingService(ServiceReference reference) {
        T service = (T) bundleContext.getService(reference);
        Map properties = OsgiServiceReferenceUtils.getServicePropertiesSnapshotAsMap(reference);
        registry.register(service, properties);
        return service;
    }

    public void modifiedService(ServiceReference reference, Object service) {
    }

    public void removedService(ServiceReference reference, Object service) {
        Map properties = OsgiServiceReferenceUtils.getServicePropertiesSnapshotAsMap(reference);
        registry.unregister((T) service, properties);
    }
}
