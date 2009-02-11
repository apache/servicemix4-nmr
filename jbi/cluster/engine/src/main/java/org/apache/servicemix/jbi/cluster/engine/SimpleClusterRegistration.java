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
package org.apache.servicemix.jbi.cluster.engine;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;

import javax.xml.namespace.QName;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.core.util.MapToDictionary;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Filter;

public class SimpleClusterRegistration implements ClusterRegistration {

    private static final String SMX_COMMON_ENDPOINT_CLASS_NAME = "org.apache.servicemix.common.Endpoint";

    private Object endpoint;
    private QName interfaceName;
    private QName serviceName;
    private String endpointName;
    private String name;
    private String filter;

    private Endpoint nmrEndpoint;

    private transient Filter osgiFilter;

    public Object getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Object endpoint) {
        this.endpoint = endpoint;
    }

    public QName getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(QName interfaceName) {
        this.interfaceName = interfaceName;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void init() throws Exception {
        if (endpoint instanceof Endpoint) {
            nmrEndpoint = (Endpoint) endpoint;
        }
        if (implement(endpoint.getClass(), SMX_COMMON_ENDPOINT_CLASS_NAME)) {
            serviceName = (QName) invoke(endpoint, "getService");
            endpointName = (String) invoke(endpoint,  "getEndpoint");
        }
        if (nmrEndpoint == null) {
            List<String> filters = new ArrayList<String>();
            if (filter != null) {
                filters.add(filter);
            }
            if (interfaceName != null) {
                filters.add(Endpoint.INTERFACE_NAME + "=" + interfaceName.toString());
            }
            if (serviceName != null) {
                filters.add(Endpoint.SERVICE_NAME + "=" + serviceName.toString());
            }
            if (endpointName != null) {
                filters.add(Endpoint.ENDPOINT_NAME + "=" + endpointName);
            }
            if (name != null) {
                filters.add(Endpoint.NAME + "=" + name);
            }
            if (filters.isEmpty()) {
                throw new IllegalArgumentException("one field to match on must be set");
            }
            String f;
            if (filters.size() == 1) {
                f = filters.get(0);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("(&");
                for (String sf : filters) {
                    if (sf.startsWith("(")) {
                        sb.append(sf);
                    } else {
                        sb.append("(").append(sf).append(")");
                    }
                }
                sb.append(")");
                f = sb.toString();
            }
            osgiFilter = FrameworkUtil.createFilter(f);
        }
    }

    public boolean match(InternalEndpoint source) {
        if (nmrEndpoint != null) {
            return source.getEndpoint() == nmrEndpoint;
        } else {
            return osgiFilter.match(new MapToDictionary(source.getMetaData()));
        }
    }

    private Object invoke(Object obj, String method) throws Exception {
        try {
            Method mth = obj.getClass().getMethod(method);
            return mth.invoke(obj);
        } catch (Throwable t) {
            throw new Exception("Could not invoke getter '" + method + "' on " + obj, t);
        }
    }

    private boolean implement(Class clz, String classOrInterface) {
        if (clz == null) {
            return false;
        }
        if (classOrInterface.equals(clz.getName())) {
            return true;
        }
        if (Object.class.equals(clz)) {
            return false;
        }
        for (Class itf : clz.getInterfaces()) {
            if (implement(itf, classOrInterface)) {
                return true;
            }
        }
        return implement(clz.getSuperclass(), classOrInterface);
    }

}
