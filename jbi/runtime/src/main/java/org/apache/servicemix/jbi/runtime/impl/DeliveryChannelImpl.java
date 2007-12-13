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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Reference;
import org.apache.servicemix.nmr.api.internal.InternalExchange;

/**
 * Implementation of the DeliveryChannel.
 *
 */
public class DeliveryChannelImpl implements DeliveryChannel {

    /** Mutable boolean indicating if the channe has been closed */
    private final AtomicBoolean closed;

    /** The Component Context **/
    private final ComponentContextImpl context;

    /** Holds exchanges to be polled by the component */
    private final BlockingQueue<Exchange> queue;

    /** The underlying Channel */
    private final Channel channel;

    /** The default QName for endpoints not having this property */
    private static final QName DEFAULT_SERVICE_NAME = new QName("urn:servicemix.apache.org", "jbi");

    public DeliveryChannelImpl(ComponentContextImpl context, Channel channel, BlockingQueue<Exchange> queue) {
        this.context = context;
        this.channel = channel;
        this.queue = queue;
        this.closed = new AtomicBoolean(false);
    }

    public void close() throws MessagingException {
        // TODO process everything
        channel.close();
        closed.set(true);
    }

    public MessageExchangeFactory createExchangeFactory() {
        return new MessageExchangeFactoryImpl(closed);
    }

    public MessageExchangeFactory createExchangeFactory(QName interfaceName) {
        MessageExchangeFactoryImpl factory = new MessageExchangeFactoryImpl(closed);
        factory.setInterfaceName(interfaceName);
        return factory;
    }

    public MessageExchangeFactory createExchangeFactoryForService(QName serviceName) {
        MessageExchangeFactoryImpl factory = new MessageExchangeFactoryImpl(closed);
        factory.setServiceName(serviceName);
        return factory;
    }

    public MessageExchangeFactory createExchangeFactory(ServiceEndpoint endpoint) {
        MessageExchangeFactoryImpl factory = new MessageExchangeFactoryImpl(closed);
        factory.setEndpoint(endpoint);
        return factory;
    }

    public MessageExchange accept() throws MessagingException {
        try {
            Exchange exchange = queue.take();
            if (exchange == null) {
                return null;
            }
            return getMessageExchange(exchange);
        } catch (InterruptedException e) {
            throw new MessagingException(e);
        }
    }

    public MessageExchange accept(long timeout) throws MessagingException {
        try {
            Exchange exchange = queue.poll(timeout, TimeUnit.MILLISECONDS);
            if (exchange == null) {
                return null;
            }
            return getMessageExchange(exchange);
        } catch (InterruptedException e) {
            throw new MessagingException(e);
        }
    }

    protected MessageExchange getMessageExchange(Exchange exchange) {
        MessageExchange me = exchange.getProperty(MessageExchange.class);
        if (me == null) {
            if (exchange.getPattern() == Pattern.InOnly) {
                me = new InOnlyImpl(exchange);
            } else if (exchange.getPattern() == Pattern.InOptionalOut) {
                me = new InOptionalOutImpl(exchange);
            } else if (exchange.getPattern() == Pattern.InOut) {
                me = new InOutImpl(exchange);
            } else if (exchange.getPattern() == Pattern.RobustInOnly) {
                me = new RobustInOnlyImpl(exchange);
            } else {
                throw new IllegalStateException("Unkown pattern: " + exchange.getPattern());
            }
            exchange.setProperty(MessageExchange.class, me);
        }
        // Translate the destination endpoint
        if (((InternalExchange) exchange).getDestination() != null && me.getEndpoint() == null) {
            Endpoint ep = ((InternalExchange) exchange).getDestination();
            Map<String, ?> props = context.getNmr().getEndpointRegistry().getProperties(ep);
            QName serviceName = (QName) props.get(Endpoint.SERVICE_NAME);
            if (serviceName == null) {
                serviceName = DEFAULT_SERVICE_NAME;
            }
            String endpointName = (String) props.get(Endpoint.ENDPOINT_NAME);
            if (endpointName == null) {
                endpointName = (String) props.get(Endpoint.NAME);
            }
            me.setEndpoint(new ServiceEndpointImpl(serviceName, endpointName));
        }
        return me;
    }

    protected Exchange getExchange(MessageExchange messageExchange) {
        // TODO
        return null;
    }

    public void send(MessageExchange exchange) throws MessagingException {
        assert exchange != null;
        createTarget(exchange);
        channel.send(((MessageExchangeImpl) exchange).getInternalExchange());
    }

    public boolean sendSync(MessageExchange exchange) throws MessagingException {
        assert exchange != null;
        createTarget(exchange);
        return channel.sendSync(((MessageExchangeImpl) exchange).getInternalExchange());
    }

    public boolean sendSync(MessageExchange exchange, long timeout) throws MessagingException {
        assert exchange != null;
        createTarget(exchange);
        return channel.sendSync(((MessageExchangeImpl) exchange).getInternalExchange(), timeout);
    }

    protected void createTarget(MessageExchange messageExchange) throws MessagingException {
        Exchange exchange = ((MessageExchangeImpl) messageExchange).getInternalExchange();
        if (exchange.getTarget() == null) {
            Map<String, Object> props = new HashMap<String, Object>();
            if (messageExchange.getEndpoint() != null) {
                // TODO: handle explicit addressing
            } else {
                QName serviceName = messageExchange.getService();
                if (serviceName != null) {
                    props.put(Endpoint.SERVICE_NAME, serviceName);
                } else {
                    QName interfaceName = messageExchange.getInterfaceName();
                    if (interfaceName != null) {
                        props.put(Endpoint.INTERFACE_NAME, interfaceName);
                    }
                }
            }
            if (props.isEmpty()) {
                throw new MessagingException("No endpoint, service or interface name specified for routing");
            }
            Reference target = context.getNmr().getEndpointRegistry().lookup(props);
            exchange.setTarget(target);
        }
    }
}
