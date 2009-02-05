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

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.jbi.runtime.impl.utils.DOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A basic implementation of ServiceEndpoint
 */
public class ServiceEndpointImpl implements ServiceEndpoint {

    public static final String JBI_NAMESPACE = "http://java.sun.com/jbi/end-point-reference";
    public static final String JBI_PREFIX = "jbi:";
    public static final String JBI_ENDPOINT_REFERENCE = "end-point-reference";
    public static final String JBI_SERVICE_NAME = "service-name";
    public static final String JBI_ENDPOINT_NAME = "end-point-name";
    public static final String XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/";

    private static final Log LOG = LogFactory.getLog(ServiceEndpointImpl.class);

    private final Map<String, ?> properties;
    private final QName serviceName;
    private final String endpointName;
    private final QName[] interfaces;

    public ServiceEndpointImpl(Map<String, ?> properties) {
        this.properties = properties;
        this.serviceName = getQName(properties.get(Endpoint.SERVICE_NAME));
        this.endpointName = (String) properties.get(Endpoint.ENDPOINT_NAME);
        this.interfaces = getQNames(properties.get(Endpoint.INTERFACE_NAME));
    }

    public ServiceEndpointImpl(QName serviceName, String endpointName) {
        this.serviceName = serviceName;
        this.endpointName = endpointName;
        this.interfaces = null;
        this.properties = null;
    }

    public DocumentFragment getAsReference(QName operationName) {
        try {
            Document doc = DOMUtil.newDocument();
            DocumentFragment fragment = doc.createDocumentFragment();
            Element epr = doc.createElementNS(JBI_NAMESPACE, JBI_PREFIX + JBI_ENDPOINT_REFERENCE);
            epr.setAttributeNS(XMLNS_NAMESPACE, "xmlns:sns", getServiceName().getNamespaceURI());
            epr.setAttributeNS(JBI_NAMESPACE, JBI_PREFIX + JBI_SERVICE_NAME, "sns:" + getServiceName().getLocalPart());
            epr.setAttributeNS(JBI_NAMESPACE, JBI_PREFIX + JBI_ENDPOINT_NAME, getEndpointName());
            fragment.appendChild(epr);
            return fragment;
        } catch (Exception e) {
            LOG.warn("Unable to create reference for ServiceEndpoint " + this, e);
            return null;
        }
    }

    public QName getServiceName() {
        return serviceName;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public QName[] getInterfaces() {
        return interfaces;
    }

    public Map<String, ?> getProperties() {
        return properties;
    }

    private static QName getQName(Object o) {
        if (o instanceof QName) {
            return (QName) o;
        } else if (o instanceof String) {
            return QName.valueOf((String) o);
        } else {
            return null;
        }
    }

    private static QName[] getQNames(Object o) {
        if (o instanceof QName[]) {
            return (QName[]) o;
        } else if (o instanceof QName) {
            return new QName[] { (QName) o };
        } else if (o instanceof String) {
            String[] s = ((String) o).split(",");
            QName[] q = new QName[s.length];
            for (int i = 0; i < s.length; i++) {
                q[i] = QName.valueOf(s[i]);
            }
            return q;
        } else {
            return null;
        }
    }
}
