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
package org.apache.servicemix.nmr.spring;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.Reference;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.service.importer.OsgiSingleServiceProxyFactoryBean;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Sep 10, 2007
 * Time: 11:48:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class ReferenceFactory implements FactoryBean, InitializingBean, BundleContextAware {

    private BundleContext bundleContext;
    private NMR nmr;
    private String name;
    private QName itf;
    private QName service;
    private String endpoint;
    private Reference reference;

    public NMR getNmr() {
        return nmr;
    }

    public void setNmr(NMR nmr) {
        this.nmr = nmr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QName getInterface() {
        return itf;
    }

    public void setInterface(QName itf) {
        this.itf = itf;
    }

    public QName getService() {
        return service;
    }

    public void setService(QName service) {
        this.service = service;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public Object getObject() throws Exception {
        if (reference == null) {
            Map<String, Object> props = new HashMap<String, Object>();
            if (name != null) {
                props.put(Endpoint.NAME, name);
            }
            if (itf != null) {
                props.put(Endpoint.INTERFACE_NAME, itf);
            }
            if (service != null) {
                props.put(Endpoint.SERVICE_NAME, service);
                if (endpoint != null) {
                    props.put(Endpoint.ENDPOINT_NAME, endpoint);
                }
            }
            reference = nmr.getEndpointRegistry().lookup(props);
        }
        return reference;
    }

    public Class getObjectType() {
        return Reference.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        if (nmr == null) {
            if (bundleContext == null) {
                throw new IllegalArgumentException("nmr not set while bundleContext is null");
            }
            OsgiSingleServiceProxyFactoryBean factory = new OsgiSingleServiceProxyFactoryBean();
            factory.setInterface(new Class[] { NMR.class });
            factory.setBundleContext(getBundleContext());
            nmr = (NMR) factory.getObject();
        }
        if (name == null && itf == null && service == null) {
            throw new IllegalArgumentException("one of name, interface or service should be set");
        }
    }

}
