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

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.RobustInOnly;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.core.ExchangeImpl;

/**
 * Resolver for URI patterns
 *
 * @version $Revision: 564607 $
 */
public class MessageExchangeFactoryImpl implements MessageExchangeFactory {

    private QName interfaceName;

    private QName serviceName;

    private QName operationName;

    private ServiceEndpoint endpoint;

    private AtomicBoolean closed;

    /**
     * Constructor for a factory
     *
     * @param closed indicates if the delivery channel has been closed
     */
    public MessageExchangeFactoryImpl(AtomicBoolean closed) {
        this.closed = closed;
    }

    protected void checkNotClosed() throws MessagingException {
        if (closed.get()) {
            throw new MessagingException("DeliveryChannel has been closed.");
        }
    }

    /**
     * Create an exchange from the specified pattern
     *
     * @param pattern the MEP URI
     * @return MessageExchange
     * @throws MessagingException
     */
    public MessageExchange createExchange(URI pattern) throws MessagingException {
        checkNotClosed();
        MessageExchange result = null;
        if (pattern != null) {
            Pattern p = Pattern.fromWsdlUri(pattern.toString());
            if (p == Pattern.InOnly) {
                result = createInOnlyExchange();
            } else if (p == Pattern.InOut) {
                result = createInOutExchange();
            } else if (p == Pattern.InOptionalOut) {
                result = createInOptionalOutExchange();
            } else if (p == Pattern.RobustInOnly) {
                result = createRobustInOnlyExchange();
            }
        }
        if (result == null) {
            throw new MessagingException("Do not understand pattern: " + pattern);
        }
        return result;
    }

    /**
     * create InOnly exchange
     *
     * @return InOnly exchange
     * @throws MessagingException
     */
    public InOnly createInOnlyExchange() throws MessagingException {
        checkNotClosed();
        InOnlyImpl result = new InOnlyImpl(new ExchangeImpl(Pattern.InOnly));
        setDefaults(result);
        return result;
    }

    /**
     * create RobustInOnly exchange
     *
     * @return RobsutInOnly exchange
     * @throws MessagingException
     */
    public RobustInOnly createRobustInOnlyExchange() throws MessagingException {
        checkNotClosed();
        RobustInOnlyImpl result = new RobustInOnlyImpl(new ExchangeImpl(Pattern.RobustInOnly));
        setDefaults(result);
        return result;
    }

    /**
     * create InOut Exchange
     *
     * @return InOut exchange
     * @throws MessagingException
     */
    public InOut createInOutExchange() throws MessagingException {
        checkNotClosed();
        InOutImpl result = new InOutImpl(new ExchangeImpl(Pattern.InOut));
        setDefaults(result);
        return result;
    }

    /**
     * create InOptionalOut exchange
     *
     * @return InOptionalOut exchange
     * @throws MessagingException
     */
    public InOptionalOut createInOptionalOutExchange() throws MessagingException {
        checkNotClosed();
        InOptionalOutImpl result = new InOptionalOutImpl(new ExchangeImpl(Pattern.InOptionalOut));
        setDefaults(result);
        return result;
    }

    /**
     * Create an exchange that points at an endpoint that conforms to the
     * declared capabilities, requirements, and policies of both the consumer
     * and the provider.
     *
     * @param svcName the service name
     * @param opName
     *            the WSDL name of the operation to be performed
     * @return a message exchange that is initialized with given interfaceName,
     *         operationName, and the endpoint decided upon by JBI.
     * @throws MessagingException
     */
    public MessageExchange createExchange(QName svcName, QName opName) throws MessagingException {
        // TODO: look for the operation in the wsdl and infer the MEP
        checkNotClosed();
        InOptionalOutImpl me = new InOptionalOutImpl(new ExchangeImpl(Pattern.InOptionalOut));
        setDefaults(me);
        me.setService(svcName);
        me.setOperation(opName);
        return me;
    }

    /**
     * @return endpoint
     */
    public ServiceEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * set endpoint
     *
     * @param endpoint The endpoint to set
     */
    public void setEndpoint(ServiceEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return interface name
     */
    public QName getInterfaceName() {
        return interfaceName;
    }

    /**
     * set interface name
     *
     * @param interfaceName The interfaceName to set
     */
    public void setInterfaceName(QName interfaceName) {
        this.interfaceName = interfaceName;
    }

    /**
     * @return service name
     */
    public QName getServiceName() {
        return serviceName;
    }

    /**
     * set service name
     *
     * @param serviceName The serviceName to set
     */
    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * @return Returns the operationName.
     */
    public QName getOperationName() {
        return operationName;
    }

    /**
     * @param operationName
     *            The operationName to set.
     */
    public void setOperationName(QName operationName) {
        this.operationName = operationName;
    }

    protected void setDefaults(MessageExchangeImpl exchange) {
        exchange.setOperation(getOperationName());
        if (endpoint != null) {
            exchange.setEndpoint(getEndpoint());
        } else {
            exchange.setService(serviceName);
            exchange.setInterfaceName(interfaceName);
        }
    }

}
