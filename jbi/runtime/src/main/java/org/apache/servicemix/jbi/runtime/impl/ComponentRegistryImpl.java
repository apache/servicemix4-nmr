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

import org.apache.servicemix.jbi.runtime.ComponentRegistry;
import org.apache.servicemix.jbi.runtime.DocumentRepository;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.ServiceMixException;
import org.apache.servicemix.nmr.core.ServiceRegistryImpl;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 4, 2007
 * Time: 10:30:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ComponentRegistryImpl extends ServiceRegistryImpl<Component>  implements ComponentRegistry {

    private NMR nmr;
    private DocumentRepository documentRepository;
    private Map<String, Component> components;

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

    /**
     * Register a service with the given metadata.
     *
     * @param component the component to register
     * @param properties the associated metadata
     */
    public void register(Component component, Map<String, ?> properties) {
        try {
            if (components.containsValue(component)) {
                // Component is already registered
                return;
            }
            if (properties == null) {
                properties = new HashMap<String, Object>();
            }
            String name = (String) properties.get(NAME);
            ComponentContext context = new ComponentContextImpl(nmr, documentRepository, component, properties);
            component.getLifeCycle().init(context);
            if (name != null) {
                components.put(name, component);
            } else {
                // TODO: log a warning
            }
        } catch (JBIException e) {
            throw new ServiceMixException(e);
        }
    }

    /**
     * Unregister a previously registered component.
     *
     * @param component the component to unregister
     */
    public void unregister(Component component, Map<String, ?> properties) {
        //try {
            String name = properties != null ? (String) properties.get(NAME) : null;
            if (name != null) {
                components.remove(name);
            }
        //} catch (JBIException e) {
        //    throw new ServiceMixException(e);
        //}
    }

    public Component getComponent(String name) {
        return components.get(name);
    }
}
