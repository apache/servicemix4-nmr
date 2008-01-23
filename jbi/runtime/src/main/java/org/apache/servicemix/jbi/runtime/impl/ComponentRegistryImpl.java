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
package org.apache.servicemix.jbi.runtime.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.runtime.ComponentRegistry;
import org.apache.servicemix.jbi.runtime.DocumentRepository;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.ServiceMixException;
import org.apache.servicemix.nmr.core.ServiceRegistryImpl;

/**
 * Registry of JBI components objects
 */
public class ComponentRegistryImpl extends ServiceRegistryImpl<Component>  implements ComponentRegistry {

    private static final Log LOGGER = LogFactory.getLog(ComponentRegistryImpl.class);

    private NMR nmr;
    private DocumentRepository documentRepository;
    private Map<String, Component> components;
    private Object transactionManager;
    private List transactionManagers;
    private InitialContext namingContext;
    private ManagementContext managementContext;

    public ComponentRegistryImpl() {
        components = new ConcurrentHashMap<String, Component>();
    }

    public NMR getNmr() {
        return nmr;
    }

    public void setNmr(NMR nmr) {
        this.nmr = nmr;
    }

    public DocumentRepository getDocumentRepository() {
        return documentRepository;
    }

    public void setDocumentRepository(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public ManagementContext getManagementContext() {
        return managementContext;
    }

    public void setManagementContext(ManagementContext managementContext) {
        this.managementContext = managementContext;
    }

    public Object getTransactionManager() {
        if (transactionManager != null) {
            return transactionManager;
        }
        if (transactionManagers != null && !transactionManagers.isEmpty()) {
            return transactionManagers.get(0);
        }
        return null;
    }

    public void setTransactionManager(Object transactionManager) {
        this.transactionManager = transactionManager;
    }

    public List getTransactionManagers() {
        return transactionManagers;
    }

    public void setTransactionManagers(List transactionManagers) {
        this.transactionManagers = transactionManagers;
    }

    public InitialContext getNamingContext() {
        return namingContext;
    }

    public void setNamingContext(InitialContext namingContext) {
        this.namingContext = namingContext;
    }

    /**
     * Register a service with the given metadata.
     *
     * @param component the component to register
     * @param properties the associated metadata
     */
    public void register(Component component, Map<String, ?> properties) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
            if (components.containsValue(component)) {
                // Component is already registered
                return;
            }
            if (properties == null) {
                properties = new HashMap<String, Object>();
            }
            String name = (String) properties.get(NAME);
            ComponentContext context = new ComponentContextImpl(this, component, properties);
            component.getLifeCycle().init(context);
            if (name != null) {
                components.put(name, component);
            } else {
                LOGGER.warn("Component has no name!");
            }
        } catch (JBIException e) {
            throw new ServiceMixException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * Unregister a previously registered component.
     *
     * @param component the component to unregister
     */
    public void unregister(Component component, Map<String, ?> properties) {
        String name = properties != null ? (String) properties.get(NAME) : null;
        if (name != null) {
            components.remove(name);
        }
    }

    public Component getComponent(String name) {
        return components.get(name);
    }

}
