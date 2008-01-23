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
import org.apache.servicemix.jbi.runtime.ComponentRegistry;
import org.apache.servicemix.jbi.runtime.DocumentRepository;
import org.apache.servicemix.jbi.runtime.impl.utils.DOMUtil;
import org.apache.servicemix.jbi.runtime.impl.utils.URIResolver;
import org.apache.servicemix.jbi.runtime.impl.utils.WSAddressingConstants;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.NMR;

/**
 * The ComponentContext implementation
 */
public class ComponentContextImpl implements ComponentContext, MBeanNames {

    public static final String JBI_NAMESPACE = "http://java.sun.com/jbi/end-point-reference";
    public static final String JBI_PREFIX = "jbi:";
    public static final String JBI_ENDPOINT_REFERENCE = "end-point-reference";
    public static final String JBI_SERVICE_NAME = "service-name";
    public static final String JBI_ENDPOINT_NAME = "end-point-name";
    public static final String XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/";
    public static final int DEFAULT_QUEUE_CAPACITY = 100;

    private static final Log LOG = LogFactory.getLog(ComponentContextImpl.class);

    private NMR nmr;
    private ComponentRegistryImpl componentRegistry;
    private DocumentRepository documentRepository;
    private Component component;
    private Map<String,?> properties;
    private BlockingQueue<Exchange> queue;
    private DeliveryChannel dc;
    private List<EndpointImpl> endpoints;
    private EndpointImpl componentEndpoint;
    private String name;

    public ComponentContextImpl(ComponentRegistryImpl componentRegistry, Component component, Map<String,?> properties) {
        this.componentRegistry = componentRegistry;
        this.nmr = componentRegistry.getNmr();
        this.documentRepository = componentRegistry.getDocumentRepository();
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
        try {
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
                String data = DOMUtil.asXML(doc);
                String url = documentRepository.register(data.getBytes());
                props.put(Endpoint.WSDL_URL, url);
            }
            nmr.getEndpointRegistry().register(endpoint,  props);
            return new SimpleServiceEndpoint(props, endpoint);
        } catch (TransformerException e) {
            throw new JBIException(e);
        }
    }

    public synchronized void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
        EndpointImpl ep = (EndpointImpl) endpoint;
        nmr.getEndpointRegistry().unregister(ep, null);
        endpoints.remove(ep);
    }

    public void registerExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        // TODO
    }

    public void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        // TODO
    }

    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        for (Component component : componentRegistry.getServices()) {
            ServiceEndpoint se = component.resolveEndpointReference(epr);
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
        return null;
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
        return new ServiceEndpoint[0];  // TODO
    }

    public ServiceEndpoint[] getExternalEndpointsForService(QName serviceName) {
        return new ServiceEndpoint[0];  // TODO
    }

    public String getInstallRoot() {
        return null;  // TODO
    }

    public Logger getLogger(String suffix, String resourceBundleName) throws MissingResourceException, JBIException {
        try {
            String name = suffix != null ? this.name + suffix : this.name;
            return Logger.getLogger(name, resourceBundleName);
        } catch (IllegalArgumentException e) {
            throw new JBIException("A logger can not be created using resource bundle " + resourceBundleName);
        }
    }

    public MBeanNames getMBeanNames() {
        return this;
    }

    public MBeanServer getMBeanServer() {
        if (this.componentRegistry.getManagementContext() != null) {
            return this.componentRegistry.getManagementContext().getMbeanServer();
        }
        return null;
    }

    public InitialContext getNamingContext() {
        return this.componentRegistry.getNamingContext();
    }

    public Object getTransactionManager() {
        return this.componentRegistry.getTransactionManager();
    }

    public String getWorkspaceRoot() {
        return null;  // TODO
    }

    public ObjectName createCustomComponentMBeanName(String customName) {
        if (this.componentRegistry.getManagementContext() != null) {
            return componentRegistry.getManagementContext().createCustomComponentMBeanName(customName, this.name);
        }
        return null;
    }

    public String getJmxDomainName() {
        if (this.componentRegistry.getManagementContext() != null) {
            return componentRegistry.getManagementContext().getJmxDomainName();
        }
        return null;
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
            if (el.getNamespaceURI() == null || !el.getNamespaceURI().equals(JBI_NAMESPACE)) {
                continue;
            }
            if (el.getLocalName() == null || !el.getLocalName().equals(JBI_ENDPOINT_REFERENCE)) {
                continue;
            }
            String serviceName = el.getAttributeNS(el.getNamespaceURI(), JBI_SERVICE_NAME);
            // Now the DOM pain-in-the-you-know-what: we need to come up with QName for this;
            // fortunately, there is only one place where the xmlns:xxx attribute could be, on
            // the end-point-reference element!
            QName serviceQName = DOMUtil.createQName(el, serviceName);
            String endpointName = el.getAttributeNS(el.getNamespaceURI(), JBI_ENDPOINT_NAME);
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
            try {
                Document doc = DOMUtil.newDocument();
                DocumentFragment fragment = doc.createDocumentFragment();
                Element epr = doc.createElementNS(JBI_NAMESPACE, JBI_PREFIX + JBI_ENDPOINT_REFERENCE);
                epr.setAttributeNS(XMLNS_NAMESPACE, "xmlns:sns", endpoint.getServiceName().getNamespaceURI());
                epr.setAttributeNS(JBI_NAMESPACE, JBI_PREFIX + JBI_SERVICE_NAME, "sns:" + endpoint.getServiceName().getLocalPart());
                epr.setAttributeNS(JBI_NAMESPACE, JBI_PREFIX + JBI_ENDPOINT_NAME, endpoint.getEndpointName());
                fragment.appendChild(epr);
                return fragment;
            } catch (Exception e) {
                LOG.warn("Unable to create reference for ServiceEndpoint " + endpoint, e);
                return null;
            }
        }

        public String getEndpointName() {
            return (String) properties.get(Endpoint.ENDPOINT_NAME);
        }

        public QName[] getInterfaces() {
            QName itf = (QName) properties.get(Endpoint.INTERFACE_NAME);
            return itf != null ? new QName[] { itf } : new QName[0];
        }

        public QName getServiceName() {
            return (QName) properties.get(Endpoint.SERVICE_NAME);
        }
    }
}
