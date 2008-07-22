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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.management.MBeanNames;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.document.DocumentRepository;
import org.apache.servicemix.jbi.runtime.ComponentRegistry;
import org.apache.servicemix.jbi.runtime.Environment;
import org.apache.servicemix.jbi.runtime.ComponentWrapper;
import org.apache.servicemix.jbi.runtime.impl.utils.DOMUtil;
import org.apache.servicemix.jbi.runtime.impl.utils.URIResolver;
import org.apache.servicemix.jbi.runtime.impl.utils.WSAddressingConstants;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;

/**
 * The ComponentContext implementation
 */
public class ComponentContextImpl extends AbstractComponentContext {

    public static final int DEFAULT_QUEUE_CAPACITY = 100;

    private static final Log LOG = LogFactory.getLog(ComponentContextImpl.class);

    protected ComponentWrapper component;
    protected Map<String,?> properties;
    protected BlockingQueue<Exchange> queue;
    protected EndpointImpl componentEndpoint;
    protected String name;
    protected File workspaceRoot;
    protected File installRoot;

    public ComponentContextImpl(ComponentRegistryImpl componentRegistry,
                                ComponentWrapper component,
                                Map<String,?> properties) {
        super(componentRegistry);
        this.component = component;
        this.properties = properties;
        this.queue = new ArrayBlockingQueue<Exchange>(DEFAULT_QUEUE_CAPACITY);
        this.componentEndpoint = new EndpointImpl();
        this.componentEndpoint.setQueue(queue);
        this.componentRegistry.getNmr().getEndpointRegistry().register(componentEndpoint, properties);
        this.dc = new DeliveryChannelImpl(this, componentEndpoint.getChannel(), queue);
        this.name = (String) properties.get(ComponentRegistry.NAME);
        this.workspaceRoot = new File(System.getProperty("servicemix.base"), "data/jbi/" + name + "/workspace");
        this.workspaceRoot.mkdirs();
        this.installRoot = new File(System.getProperty("servicemix.base"), "data/jbi/" + name + "/install");
        this.installRoot.mkdirs();
    }

    public void destroy() {
        try {
            dc.close();
        } catch (MessagingException e) {
            LOG.warn("Error when closing the delivery channel", e);
        }
        componentRegistry.getNmr().getEndpointRegistry().unregister(componentEndpoint, properties);
    }

    public synchronized ServiceEndpoint activateEndpoint(QName serviceName, String endpointName) throws JBIException {
        try {
            EndpointImpl endpoint = new EndpointImpl();
            endpoint.setQueue(queue);
            endpoint.setServiceName(serviceName);
            endpoint.setEndpointName(endpointName);
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(Endpoint.NAME, serviceName.toString() + ":" + endpointName);
            props.put(Endpoint.SERVICE_NAME, serviceName);
            props.put(Endpoint.ENDPOINT_NAME, endpointName);
            Document doc = component.getComponent().getServiceDescription(endpoint);
            if (doc != null) {
                String data = DOMUtil.asXML(doc);
                String url = componentRegistry.getDocumentRepository().register(data.getBytes());
                props.put(Endpoint.WSDL_URL, url);
            }
            componentRegistry.getNmr().getEndpointRegistry().register(endpoint,  props);
            return new SimpleServiceEndpoint(props, endpoint);
        } catch (TransformerException e) {
            throw new JBIException(e);
        }
    }

    public synchronized void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
        EndpointImpl ep;
        if (endpoint instanceof EndpointImpl) {
            ep = (EndpointImpl) endpoint;
        } else if (endpoint instanceof SimpleServiceEndpoint) {
            ep = ((SimpleServiceEndpoint) endpoint).getEndpoint();
        } else {
            throw new IllegalArgumentException("Unrecognized endpoint");
        }
        componentRegistry.getNmr().getEndpointRegistry().unregister(ep, null);
    }

    public void registerExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        // TODO
    }

    public void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        // TODO
    }

    public String getComponentName() {
        return name;
    }

    public String getInstallRoot() {
        return installRoot.getAbsolutePath();
    }

    public String getWorkspaceRoot() {
        return workspaceRoot.getAbsolutePath();
    }

    public ComponentWrapper getComponent() {
        return component;
    }


}
