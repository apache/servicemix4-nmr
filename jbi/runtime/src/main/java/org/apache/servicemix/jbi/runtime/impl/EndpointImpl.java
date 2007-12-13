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

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.w3c.dom.DocumentFragment;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import java.util.Queue;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 5, 2007
 * Time: 11:08:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class EndpointImpl implements Endpoint, ServiceEndpoint {

    private Channel channel;
    private Queue<Exchange> queue;
    private QName serviceName;
    private String endpointName;

    public EndpointImpl() {
    }

    public void process(Exchange exchange) {
        if (exchange.getProperty(ServiceEndpoint.class) == null) {
            exchange.setProperty(ServiceEndpoint.class, this);
        }
        queue.offer(exchange);
    }

    public DocumentFragment getAsReference(QName operationName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public QName[] getInterfaces() {
        return new QName[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Queue<Exchange> getQueue() {
        return queue;
    }

    public void setQueue(Queue<Exchange> queue) {
        this.queue = queue;
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
}
