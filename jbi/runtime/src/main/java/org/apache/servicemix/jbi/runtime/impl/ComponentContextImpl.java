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

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

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
    private Component component;
    private Map<String,?> properties;
    private BlockingQueue<Exchange> queue;
    private DeliveryChannel dc;
    private List<EndpointImpl> endpoints;
    private EndpointImpl componentEndpoint;

    public ComponentContextImpl(NMR nmr, Component component, Map<String,?> properties) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        this.nmr = nmr;
        this.component = component;
        this.properties = properties;
        this.endpoints = new ArrayList<EndpointImpl>();
        this.queue = new ArrayBlockingQueue<Exchange>(DEFAULT_QUEUE_CAPACITY);
        this.componentEndpoint = new EndpointImpl();
        this.componentEndpoint.setQueue(queue);
        this.nmr.getEndpointRegistry().register(componentEndpoint, properties);
        this.dc = new DeliveryChannelImpl(this, componentEndpoint.getChannel(), queue);
    }

    public NMR getNmr() {
        return nmr;
    }

    public synchronized ServiceEndpoint activateEndpoint(QName serviceName, String endpointName) throws JBIException {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Endpoint.NAME, serviceName.toString() + ":" + endpointName);
        props.put(Endpoint.SERVICE_NAME, serviceName);
        props.put(Endpoint.ENDPOINT_NAME, endpointName);
        EndpointImpl endpoint = new EndpointImpl();
        endpoint.setQueue(queue);
        endpoint.setServiceName(serviceName);
        endpoint.setEndpointName(endpointName);
        nmr.getEndpointRegistry().register(endpoint,  props);
        return endpoint;
    }

    public synchronized void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
        EndpointImpl ep = (EndpointImpl) endpoint;
        nmr.getEndpointRegistry().register(ep, null);
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DeliveryChannel getDeliveryChannel() throws MessagingException {
        return dc;
    }

    public ServiceEndpoint getEndpoint(QName service, String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Document getEndpointDescriptor(ServiceEndpoint endpoint) throws JBIException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceEndpoint[] getEndpoints(QName interfaceName) {
        return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceEndpoint[] getEndpointsForService(QName serviceName) {
        return new ServiceEndpoint[0];  //To change body of implemented methods use File | Settings | File Templates.
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
}
