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
package org.apache.servicemix.jbi.management;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.springframework.beans.factory.InitializingBean;

/**
 */
public class ManagedJbiRegistry implements InitializingBean {

    private static final transient Log LOG = LogFactory.getLog(ManagedJbiRegistry.class);

    private NamingStrategy namingStrategy;
    private ManagementAgent managementAgent;
    private Map<String, ManagedSharedLibrary> sharedLibraries;
    private Map<String, ManagedComponent> components;
    private Map<String, ManagedServiceAssembly> serviceAssemblies;

    public ManagedJbiRegistry() {
        this.sharedLibraries = new ConcurrentHashMap<String, ManagedSharedLibrary>();
        this.components = new ConcurrentHashMap<String, ManagedComponent>();
        this.serviceAssemblies = new ConcurrentHashMap<String, ManagedServiceAssembly>();
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public ManagementAgent getManagementAgent() {
        return managementAgent;
    }

    public void setManagementAgent(ManagementAgent managementAgent) {
        this.managementAgent = managementAgent;
    }

    public void registerSharedLibrary(SharedLibrary sharedLibrary, Map<String, ?> properties) {
        try {
            LOG.info("Registering SharedLibrary: " + sharedLibrary + " with properties " + properties);
            ManagedSharedLibrary sl = new ManagedSharedLibrary(sharedLibrary, properties);
            sharedLibraries.put(sharedLibrary.getName() + "-" + sharedLibrary.getVersion(), sl);
            managementAgent.register(sl, namingStrategy.getObjectName(sl));
        } catch (Exception e) {
            LOG.warn("Unable to register managed SharedLibrary: " + e, e);
        }
    }

    public void unregisterSharedLibrary(SharedLibrary sharedLibrary, Map<String, ?> properties) {
        try {
            LOG.info("Unregistering SharedLibrary: " + sharedLibrary + " with properties " + properties);
            ManagedSharedLibrary sl = sharedLibraries.remove(sharedLibrary.getName() + "-" + sharedLibrary.getVersion());
            managementAgent.unregister(namingStrategy.getObjectName(sl));
        } catch (Exception e) {
            LOG.warn("Unable to unregister managed SharedLibrary: " + e, e);
        }
    }

    public void registerComponent(Component component, Map<String, ?> properties) {
        try {
            LOG.info("Registering Component: " + component + " with properties " + properties);
            ManagedComponent comp = new ManagedComponent(component, properties);
            components.put(component.getName(), comp);
            managementAgent.register(comp, namingStrategy.getObjectName(comp));
        } catch (Exception e) {
            LOG.warn("Unable to register managed Component: " + e, e);
        }
    }

    public void unregisterComponent(Component component, Map<String, ?> properties) {
        try {
            LOG.info("Unregistering Component: " + component + " with properties " + properties);
            ManagedComponent comp = components.remove(component.getName());
            managementAgent.unregister(namingStrategy.getObjectName(comp));
        } catch (Exception e) {
            LOG.warn("Unable to unregister managed Component: " + e, e);
        }
    }

    public void registerServiceAssembly(ServiceAssembly serviceAssembly, Map<String, ?> properties) {
        try {
            LOG.info("Registering ServiceAssembly: " + serviceAssembly + " with properties " + properties);
            ManagedServiceAssembly sa = new ManagedServiceAssembly(serviceAssembly, properties);
            serviceAssemblies.put(serviceAssembly.getName(), sa);
            managementAgent.register(sa, namingStrategy.getObjectName(sa));
        } catch (Exception e) {
            LOG.warn("Unable to register ServiceAssembly: " + e, e);
        }
    }

    public void unregisterServiceAssembly(ServiceAssembly serviceAssembly, Map<String, ?> properties) {
        try {
            LOG.info("Unregistering ServiceAssembly: " + serviceAssembly + " with properties " + properties);
            ManagedServiceAssembly sa = serviceAssemblies.remove(serviceAssembly.getName());
            managementAgent.unregister(namingStrategy.getObjectName(sa));
        } catch (Exception e) {
            LOG.warn("Unable to unregister managed ServiceAssembly: " + e, e);
        }
    }

    public void afterPropertiesSet() throws Exception {
        if (managementAgent == null) {
            throw new IllegalArgumentException("managementAgent must not be null");
        }
        if (namingStrategy == null) {
            throw new IllegalArgumentException("namingStrategy must not be null");
        }

    }

}
