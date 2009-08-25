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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

import javax.jbi.component.ComponentContext;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.JBIException;
import javax.jbi.management.MBeanNames;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.jbi.runtime.impl.utils.DOMUtil;
import org.apache.servicemix.jbi.runtime.impl.utils.WSAddressingConstants;
import org.apache.servicemix.jbi.runtime.impl.utils.URIResolver;
import org.apache.servicemix.jbi.runtime.ComponentWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.nmr.management.Nameable;

public abstract class AbstractComponentContext implements ComponentContext, MBeanNames {

    public static final String INTERNAL_ENDPOINT = "jbi.internal";
    public static final String EXTERNAL_ENDPOINT = "jbi.external";

    private static final Log LOG = LogFactory.getLog(AbstractComponentContext.class);

    protected DeliveryChannel dc;
    protected ComponentRegistryImpl componentRegistry;

    protected MBeanServer mbeanServer;
    protected InitialContext initialContext;
    protected Object transactionManager;

    public AbstractComponentContext(ComponentRegistryImpl componentRegistry) {
        this.componentRegistry = componentRegistry;
        if (componentRegistry.getEnvironment() != null) {
            MBeanServer mbs = componentRegistry.getEnvironment().getMBeanServer();
            if (componentRegistry.getManagementStrategy() != null) {
                this.mbeanServer = new MBeanServerWrapper(mbs, componentRegistry.getManagementStrategy());
            } else {
                this.mbeanServer = mbs;
            }

            this.mbeanServer = componentRegistry.getEnvironment().getMBeanServer();
            this.initialContext = componentRegistry.getEnvironment().getNamingContext();
            this.transactionManager = componentRegistry.getEnvironment().getTransactionManager();
        }
    }

    public NMR getNmr() {
        return componentRegistry.getNmr();
    }

    public ServiceEndpoint getEndpoint(QName serviceName, String endpointName) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Endpoint.SERVICE_NAME, serviceName.toString());
        props.put(Endpoint.ENDPOINT_NAME, endpointName);
        props.put(INTERNAL_ENDPOINT, Boolean.TRUE.toString());
        List<Endpoint> endpoints = getNmr().getEndpointRegistry().query(props);
        if (endpoints.isEmpty()) {
            return null;
        }
        Map<String, ?> p = getNmr().getEndpointRegistry().getProperties(endpoints.get(0));
        return new ServiceEndpointImpl(p);
    }

    public Document getEndpointDescriptor(ServiceEndpoint endpoint) throws JBIException {
        if (endpoint instanceof ServiceEndpointImpl) {
            Map<String, ?> props = ((ServiceEndpointImpl) endpoint).getProperties();
            if (props != null) {
                String url = (String) props.get(Endpoint.WSDL_URL);
                if (url != null) {
                    InputStream is = null;
                    try {
                        is = new URL(url).openStream();
                        return DOMUtil.parseDocument(is);
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
        }
        return null;
    }

    public ServiceEndpoint[] getEndpoints(QName interfaceName) {
        Map<String, Object> props = null;
        if (interfaceName != null) {
            props = new HashMap<String, Object>();
            props.put(Endpoint.INTERFACE_NAME, interfaceName.toString());
        }
        return queryInternalEndpoints(props);
    }

    protected ServiceEndpoint[] queryInternalEndpoints(Map<String, Object> props) {
        return doQueryServiceEndpoints(props, false);
    }

    protected ServiceEndpoint[] queryExternalEndpoints(Map<String, Object> props) {
        return doQueryServiceEndpoints(props, true);
    }

    protected ServiceEndpoint[] doQueryServiceEndpoints(Map<String, Object> props, boolean external) {
        List<Endpoint> endpoints = doQueryEndpoints(props, external);
        List<ServiceEndpoint> ses = new ArrayList<ServiceEndpoint>();
        for (Endpoint endpoint : endpoints) {
            ServiceEndpoint se = getServiceEndpoint(endpoint);
            if (se != null) {
                ses.add(se);
            }
        }
        return ses.toArray(new ServiceEndpoint[ses.size()]);
    }

    protected List<Endpoint> doQueryEndpoints(Map<String, Object> props, boolean external) {
        if (props == null) {
            props = new HashMap<String, Object>();
        }
        props.put(external ? EXTERNAL_ENDPOINT : INTERNAL_ENDPOINT, Boolean.TRUE.toString());
        List<Endpoint> endpoints = getNmr().getEndpointRegistry().query(props);
        return endpoints;
    }

    protected ServiceEndpoint getServiceEndpoint(Endpoint endpoint) {
        if (endpoint instanceof InternalEndpoint) {
            endpoint = ((InternalEndpoint) endpoint).getEndpoint();
        }
        if (endpoint instanceof ServiceEndpoint) {
            ServiceEndpoint se = (ServiceEndpoint) endpoint;
            if (se.getServiceName() != null && se.getEndpointName() != null) {
                return se;
            }
        } else {
            Map<String, ?> epProps = getNmr().getEndpointRegistry().getProperties(endpoint);
            QName serviceName = getServiceQNameFromProperties(epProps);
            String endpointName = epProps.get(Endpoint.ENDPOINT_NAME) != null ? (String) epProps.get(Endpoint.ENDPOINT_NAME) : null;
            if (serviceName != null && endpointName != null) {
                return new ServiceEndpointImpl(epProps);
            }
        }
        return null;
    }

    protected QName getServiceQNameFromProperties(Map<String, ?> epProps) {
        QName svcName = null;
        if (epProps != null && epProps.containsKey(Endpoint.SERVICE_NAME)) {
            Object prop = epProps.get(Endpoint.SERVICE_NAME);
            if (prop instanceof QName) {
                svcName = (QName)prop;
            } else if (prop instanceof String && prop.toString().trim().length()>0) {
                svcName = QName.valueOf((String)prop);
            }
        }
        
        return svcName;
    }
    
    public ServiceEndpoint[] getEndpointsForService(QName serviceName) {
        if (serviceName == null) {
            // invalid
            throw new IllegalArgumentException("This method needs a non-null serviceName parameter!");
        }
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Endpoint.SERVICE_NAME, serviceName.toString());
        return queryInternalEndpoints(props);
    }

    public ServiceEndpoint[] getExternalEndpoints(QName interfaceName) {
        Map<String, Object> props = null;
        if (interfaceName != null) {
            props = new HashMap<String, Object>();
            props.put(Endpoint.INTERFACE_NAME, interfaceName.toString());
        }
        return queryExternalEndpoints(props);
    }

    public ServiceEndpoint[] getExternalEndpointsForService(QName serviceName) {
        if (serviceName == null) {
            // invalid
            throw new IllegalArgumentException("This method needs a non-null serviceName parameter!");
        }
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Endpoint.SERVICE_NAME, serviceName.toString());
        return queryExternalEndpoints(props);
    }

    public DeliveryChannel getDeliveryChannel() throws MessagingException {
        return dc;
    }

    public Logger getLogger(String suffix, String resourceBundleName) throws MissingResourceException, JBIException {
        try {
            String name = suffix != null ? getComponentName() + suffix : getComponentName();
            return Logger.getLogger(name, resourceBundleName);
        } catch (IllegalArgumentException e) {
            throw new JBIException("A logger can not be created using resource bundle " + resourceBundleName);
        }
    }

    public MBeanNames getMBeanNames() {
        return this;
    }

    public MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    public InitialContext getNamingContext() {
        return initialContext;
    }

    public Object getTransactionManager() {
        return transactionManager;
    }

    public ObjectName createCustomComponentMBeanName(String customName) {
        ObjectName name = null;
        if (componentRegistry.getManagementStrategy() != null) {
            try {
                Nameable nameable = new Nameable() {
                    public String getName() {
                        return getComponentName();
                    }                    
                    public String getParent() {
                        return null;
                    }
                    public String getType() {
                        return null;
                    }
                    public String getSubType() {
                        return null;
                    }
                    public String getVersion() {
                        return null;
                    }
                    public Class getPrimaryInterface() {
                        return null;
                    }
                };
                name = componentRegistry.getManagementStrategy().getManagedObjectName(nameable, 
                                                                                      customName, 
                                                                                      ObjectName.class);
            } catch (Exception e) {
                // ignore
            }
        }
        return name;
    }

    public String getJmxDomainName() {
        String name = null;
        if (this.componentRegistry.getManagementStrategy() != null) {
            try {
                name = componentRegistry.getManagementStrategy().getManagedObjectName(null, null, String.class);
            } catch (Exception e) {
                // ignore
            }
        }
        return name;
    }

    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        for (ComponentWrapper component : componentRegistry.getServices()) {
            ServiceEndpoint se = component.getComponent().resolveEndpointReference(epr);
            if (se != null) {
                return se;
            }
        }
        ServiceEndpoint se = resolveInternalEPR(epr);
        if (se != null) {
            return se;
        }
        return resolveStandardEPR(epr);
    }


    /**
     * <p>
     * Resolve an internal JBI EPR conforming to the format defined in the JBI specification.
     * </p>
     *
     * <p>The EPR would look like:
     * <pre>
     * <jbi:end-point-reference xmlns:jbi="http://java.sun.com/xml/ns/jbi/end-point-reference"
     *      jbi:end-point-name="endpointName"
     *      jbi:service-name="foo:serviceName"
     *      xmlns:foo="urn:FooNamespace"/>
     * </pre>
     * </p>
     *
     * @author Maciej Szefler m s z e f l e r @ g m a i l . c o m
     * @param epr EPR fragment
     * @return internal service endpoint corresponding to the EPR, or <code>null</code>
     *         if the EPR is not an internal EPR or if the EPR cannot be resolved
     */
    public ServiceEndpoint resolveInternalEPR(DocumentFragment epr) {
        if (epr == null) {
            throw new NullPointerException("resolveInternalEPR(epr) called with null epr.");
        }
        NodeList nl = epr.getChildNodes();
        for (int i = 0; i < nl.getLength(); ++i) {
            Node n = nl.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element el = (Element) n;
            // Namespace should be "http://java.sun.com/jbi/end-point-reference"
            if (el.getNamespaceURI() == null || !el.getNamespaceURI().equals(ServiceEndpointImpl.JBI_NAMESPACE)) {
                continue;
            }
            if (el.getLocalName() == null || !el.getLocalName().equals(ServiceEndpointImpl.JBI_ENDPOINT_REFERENCE)) {
                continue;
            }
            String serviceName = el.getAttributeNS(el.getNamespaceURI(), ServiceEndpointImpl.JBI_SERVICE_NAME);
            // Now the DOM pain-in-the-you-know-what: we need to come up with QName for this;
            // fortunately, there is only one place where the xmlns:xxx attribute could be, on
            // the end-point-reference element!
            QName serviceQName = DOMUtil.createQName(el, serviceName);
            String endpointName = el.getAttributeNS(el.getNamespaceURI(), ServiceEndpointImpl.JBI_ENDPOINT_NAME);
            return getEndpoint(serviceQName, endpointName);
        }
        return null;
    }

    /**
     * Resolve a standard EPR understood by ServiceMix container.
     * Currently, the supported syntax is the WSA one, the address uri
     * being parsed with the following possiblities:
     *    jbi:endpoint:service-namespace/service-name/endpoint
     *    jbi:endpoint:service-namespace:service-name:endpoint
     *
     * The full EPR will look like:
     *   <epr xmlns:wsa="http://www.w3.org/2005/08/addressing">
     *     <wsa:Address>jbi:endpoint:http://foo.bar.com/service/endpoint</wsa:Address>
     *   </epr>
     *
     * BCs should also be able to resolve such EPR but using their own URI parsing,
     * for example:
     *   <epr xmlns:wsa="http://www.w3.org/2005/08/addressing">
     *     <wsa:Address>http://foo.bar.com/myService?http.soap=true</wsa:Address>
     *   </epr>
     *
     * or
     *   <epr xmlns:wsa="http://www.w3.org/2005/08/addressing">
     *     <wsa:Address>jms://activemq/queue/FOO.BAR?persistent=true</wsa:Address>
     *   </epr>
     *
     * Note that the separator should be same as the one used in the namespace
     * depending on the namespace:
     *     http://foo.bar.com  => '/'
     *     urn:foo:bar         => ':'
     *
     * The syntax is the same as the one that can be used to specifiy a target
     * for a JBI exchange with the restriction that it only allows the
     * endpoint subprotocol to be used.
     *
     * @param epr the xml fragment to resolve
     * @return the resolved endpoint or <code>null</code>
     */
    public ServiceEndpoint resolveStandardEPR(DocumentFragment epr) {
        try {
            NodeList children = epr.getChildNodes();
            for (int i = 0; i < children.getLength(); ++i) {
                Node n = children.item(i);
                if (n.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element elem = (Element) n;
                String[] namespaces = new String[] { WSAddressingConstants.WSA_NAMESPACE_200508,
                                                     WSAddressingConstants.WSA_NAMESPACE_200408,
                                                     WSAddressingConstants.WSA_NAMESPACE_200403,
                                                     WSAddressingConstants.WSA_NAMESPACE_200303 };
                NodeList nl = null;
                for (String ns : namespaces) {
                    NodeList tnl = elem.getElementsByTagNameNS(ns, WSAddressingConstants.EL_ADDRESS);
                    if (tnl.getLength() == 1) {
                        nl = tnl;
                        break;
                    }
                }
                if (nl != null) {
                    Element address = (Element) nl.item(0);
                    String uri = DOMUtil.getElementText(address);
                    if (uri != null) {
                        uri = uri.trim();
                        if (uri.startsWith("endpoint:")) {
                            uri = uri.substring("endpoint:".length());
                            String[] parts = URIResolver.split3(uri);
                            return getEndpoint(new QName(parts[0], parts[1]), parts[2]);
                        } else if (uri.startsWith("service:")) {
                            uri = uri.substring("service:".length());
                            String[] parts = URIResolver.split2(uri);
                            return getEndpoint(new QName(parts[0], parts[1]), parts[1]);
                        }
                    }
                    // TODO should we support interface: and operation: here?
                }
            }
        } catch (Exception e) {
            LOG.debug("Unable to resolve EPR: " + e);
        }
        return null;
    }

}
