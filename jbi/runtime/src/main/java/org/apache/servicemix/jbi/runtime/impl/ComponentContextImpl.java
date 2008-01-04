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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.MBeanNames;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import org.apache.servicemix.jbi.runtime.ComponentRegistry;
import org.apache.servicemix.jbi.runtime.DocumentRepository;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.NMR;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 4, 2007
 * Time: 10:36:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class ComponentContextImpl implements ComponentContext {

    public int DEFAULT_QUEUE_CAPACITY = 100;

    private NMR nmr;
    private DocumentRepository documentRepository;
    private Component component;
    private Map<String,?> properties;
    private BlockingQueue<Exchange> queue;
    private DeliveryChannel dc;
    private List<EndpointImpl> endpoints;
    private EndpointImpl componentEndpoint;
    private String name;

    public ComponentContextImpl(NMR nmr, DocumentRepository documentRepository, Component component, Map<String,?> properties) {
        this.nmr = nmr;
        this.documentRepository = documentRepository;
        this.component = component;
        this.properties = properties;
        this.endpoints = new ArrayList<EndpointImpl>();
        this.queue = new ArrayBlockingQueue<Exchange>(DEFAULT_QUEUE_CAPACITY);
        this.componentEndpoint = new EndpointImpl();
        this.componentEndpoint.setQueue(queue);
        this.nmr.getEndpointRegistry().register(componentEndpoint, properties);
        this.dc = new DeliveryChannelImpl(this, componentEndpoint.getChannel(), queue);
        this.name = (String) properties.get(ComponentRegistry.NAME);
    }

    public NMR getNmr() {
        return nmr;
    }

    public synchronized ServiceEndpoint activateEndpoint(QName serviceName, String endpointName) throws JBIException {
        EndpointImpl endpoint = new EndpointImpl();
        endpoint.setQueue(queue);
        endpoint.setServiceName(serviceName);
        endpoint.setEndpointName(endpointName);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Endpoint.NAME, serviceName.toString() + ":" + endpointName);
        props.put(Endpoint.SERVICE_NAME, serviceName);
        props.put(Endpoint.ENDPOINT_NAME, endpointName);
        Document doc = component.getServiceDescription(endpoint);
        if (doc != null) {
            String data = XmlUtils.toString(doc);
            String url = documentRepository.register(data.getBytes());
            props.put(Endpoint.WSDL_URL, url);
        }
        nmr.getEndpointRegistry().register(endpoint,  props);
        return new SimpleServiceEndpoint(props, endpoint);
    }

    public synchronized void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
        EndpointImpl ep = (EndpointImpl) endpoint;
        nmr.getEndpointRegistry().unregister(ep, null);
        endpoints.remove(ep);
    }

    public void registerExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getComponentName() {
        return name;
    }

    public DeliveryChannel getDeliveryChannel() throws MessagingException {
        return dc;
    }

    public ServiceEndpoint getEndpoint(QName serviceName, String endpointName) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Endpoint.SERVICE_NAME, serviceName);
        props.put(Endpoint.ENDPOINT_NAME, endpointName);
        List<Endpoint> endpoints = nmr.getEndpointRegistry().query(props);
        if (endpoints.isEmpty()) {
            return null;
        }
        Map<String, ?> p = nmr.getEndpointRegistry().getProperties(endpoints.get(0));
        return new SimpleServiceEndpoint(p);
    }

    public Document getEndpointDescriptor(ServiceEndpoint endpoint) throws JBIException {
        if (endpoint instanceof SimpleServiceEndpoint) {
            Map<String, ?> props = ((SimpleServiceEndpoint) endpoint).getProperties();
            String url = (String) props.get(Endpoint.WSDL_URL);
            if (url != null) {
                InputStream is = null;
                try {
                    is = new URL(url).openStream();
                    return XmlUtils.parseDocument(is);
                } catch (Exception e) {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e2) {
                            // Ignore
                        }
                    }
                }
            }
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceEndpoint[] getEndpoints(QName interfaceName) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Endpoint.INTERFACE_NAME, interfaceName);
        return internalQueryEndpoints(props);
    }

    protected SimpleServiceEndpoint[] internalQueryEndpoints(Map<String, Object> props) {
        List<Endpoint> endpoints = nmr.getEndpointRegistry().query(props);
        List<ServiceEndpoint> ses = new ArrayList<ServiceEndpoint>();
        for (Endpoint endpoint : endpoints) {
            Map<String, ?> epProps = nmr.getEndpointRegistry().getProperties(endpoint);
            QName serviceName = (QName) epProps.get(Endpoint.SERVICE_NAME);
            String endpointName = (String) epProps.get(Endpoint.ENDPOINT_NAME);
            if (serviceName != null && endpointName != null) {
                ses.add(new SimpleServiceEndpoint(epProps));
            }
        }
        return ses.toArray(new SimpleServiceEndpoint[ses.size()]);
    }

    public ServiceEndpoint[] getEndpointsForService(QName serviceName) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Endpoint.SERVICE_NAME, serviceName);
        return internalQueryEndpoints(props);
    }

    public ServiceEndpoint[] getExternalEndpoints(QName interfaceName) {
        return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceEndpoint[] getExternalEndpointsForService(QName serviceName) {
        return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getInstallRoot() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Logger getLogger(String suffix, String resourceBundleName) throws MissingResourceException, JBIException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public MBeanNames getMBeanNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public MBeanServer getMBeanServer() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InitialContext getNamingContext() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getTransactionManager() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getWorkspaceRoot() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected static class SimpleServiceEndpoint implements ServiceEndpoint {

        private Map<String, ?> properties;
        private EndpointImpl endpoint;

        public SimpleServiceEndpoint(Map<String, ?> properties) {
            this.properties = properties;
        }

        public SimpleServiceEndpoint(Map<String, ?> properties, EndpointImpl endpoint) {
            this.properties = properties;
            this.endpoint = endpoint;
        }

        public Map<String, ?> getProperties() {
            return properties;
        }

        public EndpointImpl getEndpoint() {
            return endpoint;
        }

        public DocumentFragment getAsReference(QName operationName) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getEndpointName() {
            return (String) properties.get(Endpoint.ENDPOINT_NAME);
        }

        public QName[] getInterfaces() {
            return new QName[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        public QName getServiceName() {
            return (QName) properties.get(Endpoint.SERVICE_NAME);
        }
    }
}
