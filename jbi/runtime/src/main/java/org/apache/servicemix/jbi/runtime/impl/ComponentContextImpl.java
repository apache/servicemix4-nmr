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
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.jbi.JBIException;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Port;

import org.w3c.dom.Document;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.runtime.ComponentRegistry;
import org.apache.servicemix.jbi.runtime.ComponentWrapper;
import org.apache.servicemix.jbi.runtime.impl.utils.DOMUtil;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import com.ibm.wsdl.Constants;

/**
 * The ComponentContext implementation
 */
public class ComponentContextImpl extends AbstractComponentContext {

    public static final int DEFAULT_QUEUE_CAPACITY = 1024;

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
        this.componentEndpoint = new EndpointImpl(properties);
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
            ServiceEndpoint se = new ServiceEndpointImpl(serviceName, endpointName);
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(Endpoint.NAME, serviceName.toString() + ":" + endpointName);
            props.put(Endpoint.SERVICE_NAME, serviceName.toString());
            props.put(Endpoint.ENDPOINT_NAME, endpointName);
            props.put(INTERNAL_ENDPOINT, Boolean.TRUE.toString());
            Document doc = component.getComponent().getServiceDescription(se);
            if (doc != null) {
                QName[] interfaceNames = getInterfaces(doc, se);
                if (interfaceNames != null) {
                    StringBuilder sb = new StringBuilder();
                    for (QName itf : interfaceNames) {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(itf.toString());
                    }
                    props.put(Endpoint.INTERFACE_NAME, sb.toString());
                }
                String data = DOMUtil.asXML(doc);
                String url = componentRegistry.getDocumentRepository().register(data.getBytes());
                props.put(Endpoint.WSDL_URL, url);
            }
            EndpointImpl endpoint = new EndpointImpl(props);
            endpoint.setQueue(queue);
            componentRegistry.getNmr().getEndpointRegistry().register(endpoint,  props);
            return endpoint;
        } catch (TransformerException e) {
            throw new JBIException(e);
        }
    }

    public synchronized void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
        List<Endpoint> eps = doQueryEndpoints(ServiceHelper.createMap(Endpoint.SERVICE_NAME,
                                                                      endpoint.getServiceName().toString(),
                                                                      Endpoint.ENDPOINT_NAME,
                                                                      endpoint.getEndpointName()),
                                              false);
        if (eps != null && eps.size() == 1) {
            Endpoint ep = eps.get(0);
            componentRegistry.getNmr().getEndpointRegistry().unregister(ep, null);
        }
    }

    public void registerExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        try {
            QName serviceName = externalEndpoint.getServiceName();
            String endpointName = externalEndpoint.getEndpointName();
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(Endpoint.NAME, serviceName.toString() + ":" + endpointName);
            props.put(Endpoint.SERVICE_NAME, serviceName.toString());
            props.put(Endpoint.ENDPOINT_NAME, endpointName);
            props.put(Endpoint.UNTARGETABLE, Boolean.TRUE.toString());
            props.put(EXTERNAL_ENDPOINT, Boolean.TRUE.toString());
            props.put(ServiceEndpoint.class.getName(), externalEndpoint);
            QName[] interfaceNames = externalEndpoint.getInterfaces();
            if (interfaceNames != null) {
                StringBuilder sb = new StringBuilder();
                for (QName itf : interfaceNames) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(itf.toString());
                }
                props.put(Endpoint.INTERFACE_NAME, sb.toString());
            }
            Document doc = component.getComponent().getServiceDescription(externalEndpoint);
            if (doc != null) {
                String data = DOMUtil.asXML(doc);
                String url = componentRegistry.getDocumentRepository().register(data.getBytes());
                props.put(Endpoint.WSDL_URL, url);
            }
            EndpointImpl endpoint = new EndpointImpl(props);
            endpoint.setQueue(queue);
            componentRegistry.getNmr().getEndpointRegistry().register(endpoint,  props);
        } catch (TransformerException e) {
            throw new JBIException(e);
        }
    }

    public void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        List<Endpoint> eps = doQueryEndpoints(ServiceHelper.createMap(Endpoint.SERVICE_NAME,
                                                                      externalEndpoint.getServiceName().toString(),
                                                                      Endpoint.ENDPOINT_NAME,
                                                                      externalEndpoint.getEndpointName()),
                                                 true);
        if (eps != null && eps.size() == 1) {
            Endpoint ep = eps.get(0);
            componentRegistry.getNmr().getEndpointRegistry().unregister(ep, null);
        }
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

    public static final String WSDL1_NAMESPACE = "http://schemas.xmlsoap.org/wsdl/";

    protected QName[] getInterfaces(Document document, ServiceEndpoint serviceEndpoint) {
        try {
            if (document == null || document.getDocumentElement() == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Endpoint " + serviceEndpoint + " has no service description");
                }
                return null;
            }
            if (!WSDL1_NAMESPACE.equals(document.getDocumentElement().getNamespaceURI())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Endpoint " + serviceEndpoint + " has a non WSDL1 service description");
                }
                return null;
            }
            WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
            reader.setFeature(Constants.FEATURE_VERBOSE, false);
            Definition definition = reader.readWSDL(null, document);
            // Check if the wsdl is only a port type
            // In these cases, only the port type is used, as the service name and endpoint name
            // are provided on the jbi endpoint
            if (definition.getPortTypes().keySet().size() == 1
                    && definition.getServices().keySet().size() == 0) {
                PortType portType = (PortType) definition.getPortTypes().values().iterator().next();
                QName interfaceName = portType.getQName();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Endpoint " + serviceEndpoint + " implements interface " + interfaceName);
                }
                return new QName[] { interfaceName };
            } else {
                Service service = definition.getService(serviceEndpoint.getServiceName());
                if (service == null) {
                    LOG.info("Endpoint " + serviceEndpoint + " has a service description, but no matching service found in "
                                    + definition.getServices().keySet());
                    return null;
                }
                Port port = service.getPort(serviceEndpoint.getEndpointName());
                if (port == null) {
                    LOG.info("Endpoint " + serviceEndpoint + " has a service description, but no matching endpoint found in "
                                    + service.getPorts().keySet());
                    return null;
                }
                if (port.getBinding() == null) {
                    LOG.info("Endpoint " + serviceEndpoint + " has a service description, but no binding found");
                    return null;
                }
                if (port.getBinding().getPortType() == null) {
                    LOG.info("Endpoint " + serviceEndpoint + " has a service description, but no port type found");
                    return null;
                }
                QName interfaceName = port.getBinding().getPortType().getQName();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Endpoint " + serviceEndpoint + " implements interface " + interfaceName);
                }
                return new QName[] { interfaceName };
            }
        } catch (Throwable e) {
            LOG.warn("Error retrieving interfaces from service description: " + e.getMessage());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error retrieving interfaces from service description", e);
            }
            return null;
        }
        
    }

}
