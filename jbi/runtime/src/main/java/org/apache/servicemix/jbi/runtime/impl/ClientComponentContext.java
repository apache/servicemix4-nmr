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

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.JBIException;
import javax.xml.namespace.QName;

import org.apache.servicemix.nmr.api.Channel;

public class ClientComponentContext extends AbstractComponentContext {

    public ClientComponentContext(ComponentRegistryImpl componentRegistry) {
        super(componentRegistry);
        this.dc = new ClientDeliveryChannel(this, componentRegistry.getNmr().createChannel());
    }

    public String getInstallRoot() {
        return null;
    }

    public String getWorkspaceRoot() {
        return null;
    }

    public ServiceEndpoint activateEndpoint(QName serviceName, String endpointName) throws JBIException {
        throw new UnsupportedOperationException();
    }

    public void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
        throw new UnsupportedOperationException();
    }

    public void registerExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        throw new UnsupportedOperationException();
    }

    public void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        throw new UnsupportedOperationException();
    }

    public String getComponentName() {
        return null;
    }

    protected static class ClientDeliveryChannel extends DeliveryChannelImpl {

        public ClientDeliveryChannel(AbstractComponentContext context, Channel channel) {
            super(context, channel, null);
        }

        public MessageExchange accept() throws MessagingException {
            throw new UnsupportedOperationException();
        }

        public MessageExchange accept(long timeout) throws MessagingException {
            throw new UnsupportedOperationException();
        }
    }

}
