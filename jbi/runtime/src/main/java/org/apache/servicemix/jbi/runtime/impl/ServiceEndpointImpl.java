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

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.w3c.dom.DocumentFragment;

/**
 * A basic implementation of ServiceEndpoint
 */
public class ServiceEndpointImpl implements ServiceEndpoint {

    private final QName serviceName;
    private final String endpointName;

    public ServiceEndpointImpl(QName serviceName, String endpointName) {
        this.serviceName = serviceName;
        this.endpointName = endpointName;
    }

    public DocumentFragment getAsReference(QName operationName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getEndpointName() {
        return endpointName;
    }

    public QName[] getInterfaces() {
        return new QName[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public QName getServiceName() {
        return serviceName;
    }
}
