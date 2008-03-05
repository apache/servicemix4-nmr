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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.runtime.ComponentRegistry;
import org.apache.servicemix.jbi.runtime.DocumentRepository;
import org.apache.servicemix.jbi.runtime.Environment;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.core.ServiceRegistryImpl;

/**
 * Registry of JBI components objects
 */
public class ComponentRegistryImpl extends ServiceRegistryImpl<Component>  implements ComponentRegistry {

    private static final Log LOGGER = LogFactory.getLog(ComponentRegistryImpl.class);

    private NMR nmr;
    private DocumentRepository documentRepository;
    private Map<String, ComponentContextImpl> contexts;
    private Environment environment;
    private ManagementContext managementContext;

    public ComponentRegistryImpl() {
        contexts = new ConcurrentHashMap<String, ComponentContextImpl>();
    }

    public NMR getNmr() {
        return nmr;
    }

    public void setNmr(NMR nmr) {
        this.nmr = nmr;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public ManagementContext getManagementContext() {
        return managementContext;
    }

    public void setManagementContext(ManagementContext managementContext) {
        this.managementContext = managementContext;
    }

    public DocumentRepository getDocumentRepository() {
        return documentRepository;
    }

    public void setDocumentRepository(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Register a service with the given metadata.
     *
     * @param component the component to register
     * @param properties the associated metadata
     */
    @Override
    protected void doRegister(Component component, Map<String, ?> properties) throws JBIException {
        LOGGER.info("JBI component registered with properties: " + properties);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
        try {
            String name = (String) properties.get(NAME);
            ComponentContextImpl context = new ComponentContextImpl(this, environment, managementContext, component, properties);
            component.getLifeCycle().init(context);
            if (name != null) {
                contexts.put(name, context);
            } else {
                LOGGER.warn("Component has no name!");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    /**
     * Unregister a previously registered component.
     *
     * @param component the component to unregister
     */
    @Override
    protected void doUnregister(Component component, Map<String, ?> properties)throws JBIException {
        LOGGER.info("JBI component unregistered with properties: " + properties);
        String name = properties != null ? (String) properties.get(NAME) : null;
        if (name != null) {
            ComponentContextImpl context = contexts.remove(name);
            context.getDeliveryChannel().close();
        }
    }

    public Component getComponent(String name) {
        return contexts.get(name).getComponent();
    }

}
