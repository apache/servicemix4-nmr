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

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.messaging.MessageExchange;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Role;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Message;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalExchange;
import org.apache.servicemix.nmr.api.event.EndpointListener;
import org.apache.servicemix.nmr.api.event.ExchangeListener;
import org.apache.servicemix.nmr.core.ServiceRegistryImpl;
import org.apache.servicemix.jbi.runtime.impl.MessageExchangeImpl;
import org.apache.servicemix.jbi.runtime.impl.ServiceEndpointImpl;
import org.apache.servicemix.jbi.runtime.impl.DeliveryChannelImpl;
import org.apache.servicemix.jbi.runtime.impl.AbstractComponentContext;
import org.apache.servicemix.jbi.cluster.requestor.JmsRequestor;
import org.apache.servicemix.jbi.cluster.requestor.Transacted;
import org.apache.servicemix.jbi.cluster.requestor.JmsRequestorListener;
import org.apache.servicemix.jbi.cluster.requestor.JmsRequestorPool;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

/**
 * Throttling
 * ==========
 *    As the processing of consumed JMS messages is performed asynchronously, in a case
 * where there are lots of requests pending in the queue, we need to limit the number
 * of exchanges sent into the NMR at a given time so that it does not end in out of
 * memory errors.  In order to do so, the <code>maxPendingExchanges</code> property can
 * be configured.  The cluster endpoint keeps track of the number of exchange that it has
 * send and which are not fully processed.  If the maximum number is reached, it will stop
 * the consumption of new requests until that number comes back below the threshold.
 * The default value is 4096.
 * 
 * Use of JMS selectors
 * ====================
 *    In order for this endpoint to be used to cluster several services, and in order
 * to use a minimum amount of JMS destinations, the cluster endpoint uses selector to
 * listen to exchanges it is interested in.
 *    Another requirement is that the response for a given JMS message is consumed only
 * by the cluster endpoint that originally sent the JMS request.
 *    Therefore, we use the <code>name</code> property of this endpoint (which has to be
 * unique in the cluster) as a JMS property on the messages.  The endpoint acting as a
 * JBI provider will create a JMS message and set this property.  It will only consume
 * messages that have the needed value for the cluster name, hence only consuming his
 * own responses.   On the JBI consumer side of the endpoint, the endpoint will consume
 * JMS messages of two kinds: new requests (containing an IN message and a JBI target
 * that is available on the container), and also replies targeted to this cluster endpoint.
 *     Also, the <code>maxPendingExchanges</code> on the JBI consumer side has an effect
 * here.  When the number of requests that have been sent into the NMR but not fully
 * processed yet reaches the above value, the JMS consumer will not consume messages
 * corresponding to new requests, but will only service replies.  This behavior will
 * remain until some pending exchanges are processed.
 *
 * Transactions
 * ============
 *     Four transactional modesl are define: None, ClientAck, Jms and Xa.  The first one
 * will use JMS session in auto acknowledge mode, which means there will be no redelivery
 * at all.  The <code>ClientAck</code> mode will use client acknowledgements on the JMS
 * sessions and will send the ack when the exchange comes back.  The <code>Jms</code> mode
 * uses JMS local transactions, and the <code>Xa</code> uses XA transactions.
 *     The behavior of the cluster endpoint when receiving back the exchange is also
 * controlled by the <code>rollbackOnErrors</code> flag.  If this flag is set and the
 * transaction model is not <code>None</code>, any exchange that comes back in an
 * <code>Error</code> status will lead to the transaction being rolled back (or the ack
 * not sent).  This also means that no error status can be conveyed back to the original
 * exchange.
 * TODO: this should be configured on a per endpoint basis
 *
 * Using multiple clustered endpoints:
 *            If/when this cluster endpoint is used to cluster multiple endpoints
 *            (i.e. an interceptor is used to redirect exchanges to this endpoint),
 *            and if the containers do not have the exact same set of endpoints deployed,
 *            a jms consumer may consume JMS messages targeted to an endpoint which happen
 *            to not be available in this container.  In such a case, the jms consumer will
 *            use a JMS selector to ensure it will only consume JMS messages it will be able
 *            to process.  This is of course a bit more CPU intensive on the broker.
 *            When this cluster endpoint is used in an explicit wiring between two endpoints,
 *            or if the container are supposed to have the exact same set of endpoints available,
 *            the <code>assumeSameContainers</code> flag can be turned on, which will disable the
 *            use of JMS selectors.
 *
 * Transactions support
 *            Transactions are supported at the JMS consumer level.  In JMS, such transactions
 *            will usually involve receiving a JMS message and sending a response back in the
 *            same transaction.  Transactions are supported if the JBI statuses are conveyed back
 *            but in such cases, the transaction will never be rolled back.  It thus means
 *            it will provide a guarantee if the container is shut down, but will not offer a
 *            redelivery mechanism in case something went wrong while processing the message.
 *            TODO: handle XA transactions
 *            TODO: transactions are not handled on the provider side
 *
 * Conveying JBI status back
 *            The boolean property <code>conveyJbiStatus</code> can be used to
 *            control whether the JBI statuses (DONE and ERROR) are always sent back
 *            across the JMS layer.  It changes the number of JMS messages sent
 *            for a given JBI exchange and also change the transactions semantics.
 *            When JBI statuses are always sent, an InOnly request will be translated
 *            into two JMS messages (one for the In message and another one for the
 *            DONE or ERROR status or three for an InOnly MEP.
 *            This means that the transaction (if any) will not be rolled back if the
 *            exchange comes back with an ERROR status.  On the opposite, if JBI statuses
 *            are not sent back, a single JMS message will be
 *            used: if the exchange comes back with an ERROR, the transaction on the JMS
 *            consumer side will be rolled back and the message redelivered; nothing will
 *            be reported back to the JMS producer side.
 *            Note that this behavior (not conveying JBI statuses and rolling back the
 *            transaction) is only available when using transactions as the message need
 *            to be redelivered by the JMS broker.
 *            Therefore the InOnly exchange DONE status will be sent immediately after the
 *            JMS message has been sent. For a RobustInOnly, the JMS consumer will sent back
 *            a fault or a DONE status, but an ERROR status will cause the transaction to be
 *            rolled back and the message redelivered.
 *            For an InOut MEP, either two or three messages will be used if JBI statuses
 *            are conveyed or not.
 *            TODO: what about InOptionalOut which allows a fault to be sent back from the
 *               JBI consumer to the provider after receiving the out message ?
 *            TODO: update doc with rollbackOnErrors
 *               this flag is only used on the JMS consumer side and sent in the JMS message
 *               for the other side to know how to handle the message
 *            TODO: the rollbackOnErrors flag should be configured on a per-endpoint basis
 *
 * TODO: add a cache level
 *            not caching the connection would only work when using non temporary
 *            queues for the reply destination
 * TODO: handle JMS exceptions => refresh connection
 *            note that refreshing the connection when using temporary queues
 *            would lead to loosing messages in the temp queue
 *
 * TODO: simplify the selectors when a single endpoint is clustered
 *
 */
public class ClusterEngine extends ServiceRegistryImpl<ClusterRegistration>
                             implements Endpoint, InitializingBean, DisposableBean, EndpointListener, ExchangeListener {

    /**
     * Default maximum number of pending exchanges
     */
    public static final int DEFAULT_MAX_PENDING_EXCHANGES = 4096;

    /**
     * Name of the JMS property holding the type of message sent
     */
    protected static final String JBI_MESSAGE = "JBIMessage";

    /**
     * The JMS message contains a JBI In message
     */
    protected static final int JBI_MESSAGE_IN = 0;

    /**
     * The JMS message contains a JBI Out message
     */
    protected static final int JBI_MESSAGE_OUT = 1;

    /**
     * The JMS message contains a JBI Fault message
     */
    protected static final int JBI_MESSAGE_FAULT = 2;

    /**
     * The JMS message contains a JBI DONE status
     */
    protected static final int JBI_MESSAGE_DONE = 3;

    /**
     * The JMS message contains a JBI ERROR status
     */
    protected static final int JBI_MESSAGE_ERROR = 4;

    /**
     * JMS property holding the Message Exchange Pattern
     */
    protected static final String JBI_MEP = "JBIMep";

    /**
     * JMS property holding the interface QName for the exchange
     */
    protected static final String JBI_INTERFACE = "JBIInterface";

    /**
     * JMS property holding the operation QName for the exchange
     */
    protected static final String JBI_OPERATION = "JBIOperation";

    /**
     * JMS property holding the service QName for the exchange
     */
    protected static final String JBI_SERVICE = "JBIService";

    /**
     * JMS property holding the endpoint for the exchange
     */
    protected static final String JBI_ENDPOINT = "JBIEndpoint";

    /**
     * JMS property holding the correlation id
     */
    protected static final String PROPERTY_CORR_ID = "ClusterCorrId";

    /**
     * JMS property holding the correlation id to be used for the reply.
     */
    protected static final String PROPERTY_SENDER_CORR_ID = "SenderClusterCorrId";

    /**
     * JMS property containing the rollbackOnErrors flag for this message.
     * This property is set on the IN message so that the cluster endpoint
     * consuming this message will handle it with the right behavior in case
     * the cluster endpoint that sent the message has a different configuration.
     */
    protected static final String PROPERTY_ROLLBACK_ON_ERRORS = "ClusterRollbackOnErrors";

    /**
     * JMS property holding the name of the cluster (used with selectors)
     */
    protected static final String PROPERTY_CLUSTER_NAME = "ClusterName";

    /**
     * JMS property holding the name of the cluster to use for the response
     */
    protected static final String PROPERTY_SENDER_CLUSTER_NAME = "SenderClusterName";

    protected static final Log logger = LogFactory.getLog(ClusterEngine.class);

    protected boolean rollbackOnErrors = true;
    protected String name;
    protected JmsRequestorPool pool;

    protected Channel channel;
    protected AtomicBoolean started = new AtomicBoolean();
    protected final Map<String, Exchange> exchanges = new ConcurrentHashMap<String, Exchange>();
    protected String selector;
    protected AtomicInteger pendingExchanges = new AtomicInteger();
    protected AtomicBoolean pauseConsumption = new AtomicBoolean(false);
    protected int maxPendingExchanges = DEFAULT_MAX_PENDING_EXCHANGES;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        try {
            start();
        } catch (Exception e) {
            throw new RuntimeException("Unable to start cluster endpoint", e);
        }
    }

    public JmsRequestorPool getPool() {
        return pool;
    }

    public void setPool(JmsRequestorPool pool) {
        this.pool = pool;
    }

    public boolean isRollbackOnErrors() {
        return rollbackOnErrors;
    }

    public void setRollbackOnErrors(boolean rollbackOnErrors) {
        this.rollbackOnErrors = rollbackOnErrors;
    }

    public String getName() {
        return name;
    }

    /**
     * A unique name for this cluster endpoint.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    public int getMaxPendingExchanges() {
        return maxPendingExchanges;
    }

    /**
     * Specifies the maximum number of pending exchanges on the JMS consumer side.
     * This allows to limit the number of JBI exchanges sent into the NMR at a given time.
     * If set to a huge number, the NMR may be flooded by exchanges and run out of memory.
     *
     * @param maxPendingExchanges
     */
    public void setMaxPendingExchanges(int maxPendingExchanges) {
        this.maxPendingExchanges = maxPendingExchanges;
    }

    public void afterPropertiesSet() throws Exception {
        if (pool == null) {
            throw new IllegalArgumentException("'pool' must be set");
        }
    }

    public void start() throws Exception {
        if (started.compareAndSet(false, true)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Starting cluster endpoint: " + name);
            }
            pool.setListener(new JmsRequestorListener() {
                public void onMessage(JmsRequestor requestor) throws Exception {
                    process(requestor);
                }
            });
            invalidateSelector();
            pool.start();
        }
    }

    public void destroy() throws Exception {
        if (started.compareAndSet(true, false)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Stopping cluster endpoint: " + name);
            }
            // TODO: We should first stop receiving new requests, then wait for all pending exchanges to be processed
//            maxPendingExchanges = 0;
//            if (pauseConsumption.compareAndSet(false, true)) {
//                invalidateSelector();
//            }
//            while (pendingExchanges.get() > 0) {
//                Thread.sleep(100);
//            }
            pool.stop();
        }
    }

    public void pause() {
        if (pauseConsumption.compareAndSet(false, true)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Pausing cluster endpoint: " + name);
            }
            invalidateSelector();
        }
    }

    public void resume() {
        if (pauseConsumption.compareAndSet(true, false)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Resuming cluster endpoint: " + name);
            }
            invalidateSelector();
        }
    }

    //-------------------------------------------------------------------------
    // Endpoint listener
    //-------------------------------------------------------------------------

    public void endpointRegistered(InternalEndpoint endpoint) {
        invalidateSelector();
    }

    public void endpointUnregistered(InternalEndpoint endpoint) {
        invalidateSelector();
    }

    //-------------------------------------------------------------------------
    // Exchange listener
    //-------------------------------------------------------------------------

    public void exchangeSent(Exchange exchange) {
        // Intercept exchanges
        if (exchange instanceof InternalExchange
                && exchange.getStatus() == Status.Active && exchange.getRole() == Role.Consumer
                && exchange.getOut(false) == null && exchange.getFault(false) == null) {
            // Filter JBI endpoints
            InternalEndpoint source = ((InternalExchange) exchange).getSource();
            for (ClusterRegistration reg : getServices()) {
                if (reg.match(source)) {
                    exchange.setTarget(getChannel().getNMR().getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, name)));
                    return;
                }
            }
        }
    }

    public void exchangeDelivered(Exchange exchange) {
    }

    public void exchangeFailed(Exchange exchange) {
    }

    public void invalidateSelector() {
        selector = null;
        if (pool != null) {
            String selector;
            // If we're pausing comsumption of new messages, use a selector that will
            // only select messages directed to this very container (i.e. not new
            // exchanges).
            if (pauseConsumption.get()) {
                selector = PROPERTY_CLUSTER_NAME + " = '" + name + "'";
            // Else we use the full selector that includes the JBI targets
            } else {
                selector = getSelector();
            }
            pool.setMessageSelector(selector);
        }
    }

    protected String getSelector() {
        if (selector == null) {
            Set<String> interfaces = new HashSet<String>();
            Set<String> services = new HashSet<String>();
            Set<String> endpoints = new HashSet<String>();

            for (ServiceEndpoint se : getAllEndpoints()) {
                // This endpoint is not a JBI endpoint, so we don't need to filter it out
                QName[] itfs = se.getInterfaces();
                if (itfs != null) {
                    for (QName itf : itfs) {
                        interfaces.add(itf.toString());
                    }
                }
                services.add(se.getServiceName().toString());
                endpoints.add("{" + se.getServiceName().toString() + "}" + se.getEndpointName());
            }
            StringBuilder selector = new StringBuilder();
            if (!endpoints.isEmpty()) {
                selector.append("(");
                selector.append(JBI_MESSAGE).append(" = ").append(JBI_MESSAGE_IN).append(" AND (");
                if (!interfaces.isEmpty()) {
                    selector.append(JBI_INTERFACE).append(" IN (");
                    boolean first = true;
                    for (String s : interfaces) {
                        if (!first) {
                            selector.append(", ");
                        } else {
                            first = false;
                        }
                        selector.append("'").append(s).append("'");
                    }
                    selector.append(")");
                }
                if (!interfaces.isEmpty()) {
                    selector.append(" OR ");
                }
                selector.append(JBI_SERVICE).append(" IN (");
                boolean first = true;
                for (String s : services) {
                    if (!first) {
                        selector.append(", ");
                    } else {
                        first = false;
                    }
                    selector.append("'").append(s).append("'");
                }
                selector.append(")");
                selector.append(" OR ");
                selector.append(JBI_ENDPOINT).append(" IN (");
                first = true;
                for (String s : endpoints) {
                    if (!first) {
                        selector.append(", ");
                    } else {
                        first = false;
                    }
                    selector.append("'").append(s).append("'");
                }
                selector.append(")");
                selector.append(")");
                selector.append(")");
                selector.append(" OR ");
            }
            selector.append(PROPERTY_CLUSTER_NAME).append(" = '").append(name).append("'");
            this.selector = selector.toString();
        }
        return this.selector;
    }

    //-------------------------------------------------------------------------
    // NMR helpers
    //-------------------------------------------------------------------------

    protected ServiceEndpoint getEndpoint(QName serviceName, String endpointName) {
        return new ServiceEndpointImpl(serviceName, endpointName);
    }

    protected List<ServiceEndpoint> getAllEndpoints() {
        List<ServiceEndpoint> endpoints = new ArrayList<ServiceEndpoint>();
        EndpointRegistry registry = getChannel().getNMR().getEndpointRegistry();
        for (Endpoint ep : registry.getServices()) {
            Map<String,?> props = registry.getProperties(ep);
            // Check if this endpoint is addressable in the JBI space
            if (props.get(Endpoint.SERVICE_NAME) != null && props.get(Endpoint.ENDPOINT_NAME) != null
                    && !Boolean.valueOf((String) props.get(Endpoint.UNTARGETABLE))) {
                endpoints.add(new ServiceEndpointImpl(props));
            }
        }
        return endpoints;
    }

    protected void done(Exchange exchange) {
        exchange.setStatus(Status.Done);
        send(exchange);
    }

    protected void fail(Exchange exchange, Exception e) {
        exchange.setStatus(Status.Error);
        exchange.setError(e);
        send(exchange);
    }

    protected void send(Exchange exchange) {
        decrementPendingExchangeIfNeeded(exchange);
        getChannel().send(exchange);
    }

    //-------------------------------------------------------------------------
    // JMS / JBI processing
    //-------------------------------------------------------------------------

    public void process(Exchange exchange) {
        try {
            String corrId = (String) exchange.getProperty(PROPERTY_CORR_ID + "." + name);
            if (corrId != null) {
                JmsRequestor item = pool.resume(corrId);
                synchronized (item) {
                    try {
                        processExchange(item, exchange);
                    } finally {
                        item.close();
                    }
                }
            } else {
                JmsRequestor item = pool.newRequestor();
                synchronized (item) {
                    try {
                        item.begin();
                        processExchange(item, exchange);
                    } finally {
                        item.close();
                    }
                }
            }
        } catch (Exception e) {
            // TODO what id the problem is a JMS exception or related
            fail(exchange,  e);
        }
    }

    /**
     * Process a JMS message
     *
     * @param requestor the item to use
     * @throws JMSException if an error occur
     */
    protected void process(JmsRequestor requestor) throws JMSException {
        javax.jms.Message message = requestor.getMessage();
        int type = message.getIntProperty(JBI_MESSAGE);
        switch (type) {
            case JBI_MESSAGE_DONE: {
                String corrId = message.getStringProperty(PROPERTY_CORR_ID);
                if (corrId == null) {
                    throw new IllegalStateException("Incoming JMS message has no correlationId");
                }
                Exchange exchange = exchanges.remove(corrId);
                if (exchange == null) {
                    throw new IllegalStateException("Exchange not found for id " + corrId);
                }
                done(exchange);
                break;
            }
            case JBI_MESSAGE_ERROR: {
                String corrId = message.getStringProperty(PROPERTY_CORR_ID);
                if (corrId == null) {
                    throw new IllegalStateException("Incoming JMS message has no correlationId");
                }
                Exchange exchange = exchanges.remove(corrId);
                if (exchange == null) {
                    throw new IllegalStateException("Exchange not found for id " + corrId);
                }
                fail(exchange, (Exception)((ObjectMessage) message).getObject());
                break;
            }
            case JBI_MESSAGE_IN: {
                String mep = message.getStringProperty(JBI_MEP);
                if (mep == null) {
                    throw new IllegalStateException("Exchange MEP not found for JMS message " + message.getJMSMessageID());
                }
                Exchange exchange = getChannel().createExchange(Pattern.fromWsdlUri(mep));
                exchange.setProperty(PROPERTY_ROLLBACK_ON_ERRORS + "." + name, message.getBooleanProperty(PROPERTY_ROLLBACK_ON_ERRORS));
                if (message.propertyExists(JBI_INTERFACE)) {
                    exchange.setProperty(MessageExchangeImpl.INTERFACE_NAME_PROP, QName.valueOf(message.getStringProperty(JBI_INTERFACE)));
                }
                if (message.propertyExists(JBI_OPERATION)) {
                    exchange.setOperation(QName.valueOf(message.getStringProperty(JBI_OPERATION)));
                }
                if (message.propertyExists(JBI_SERVICE)) {
                    exchange.setProperty(MessageExchangeImpl.SERVICE_NAME_PROP, QName.valueOf(message.getStringProperty(JBI_SERVICE)));
                }
                if (message.propertyExists(JBI_ENDPOINT)) {
                    QName q = QName.valueOf(message.getStringProperty(JBI_ENDPOINT));
                    String e = q.getLocalPart();
                    q = QName.valueOf(q.getNamespaceURI());
                    ServiceEndpoint se = getEndpoint(q, e);
                    // TODO: check that endpoint exists
                    exchange.setProperty(MessageExchangeImpl.SERVICE_ENDPOINT_PROP, se);
                }
                // Re-process JBI addressing
                DeliveryChannelImpl.createTarget(getChannel().getNMR(), exchange);
                // TODO: read exchange properties
                Message msg = (Message) ((ObjectMessage) message).getObject();
                exchange.setIn(msg);
                exchanges.put(exchange.getId(), exchange);
                if (pendingExchanges.incrementAndGet() >= maxPendingExchanges) {
                    if (pauseConsumption.compareAndSet(false, true)) {
                        invalidateSelector();
                    }
                }
                exchange.setProperty(PROPERTY_CORR_ID + "." + name, exchange.getId());
                requestor.suspend(exchange.getId());
                if (requestor.getTransaction() != null) {
                    exchange.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, requestor.getTransaction());
                }
                send(exchange);
                break;
            }
            case JBI_MESSAGE_OUT: {
                String corrId = message.getStringProperty(PROPERTY_CORR_ID);
                if (corrId == null) {
                    throw new IllegalStateException("Incoming JMS message has no correlationId");
                }
                Exchange exchange = exchanges.get(corrId);
                if (exchange == null) {
                    throw new IllegalStateException("Exchange not found for id " + corrId);
                }
                Message msg = (Message) ((ObjectMessage) message).getObject();
                exchange.setOut(msg);
                exchanges.put(exchange.getId(), exchange);
                exchange.setProperty(PROPERTY_CORR_ID + "." + name, exchange.getId());
                requestor.suspend(exchange.getId());
                if (requestor.getTransaction() != null) {
                    exchange.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, requestor.getTransaction());
                }
                send(exchange);
                break;
            }
            case JBI_MESSAGE_FAULT: {
                String corrId = message.getStringProperty(PROPERTY_CORR_ID);
                if (corrId == null) {
                    throw new IllegalStateException("Incoming JMS message has no correlationId");
                }
                Exchange exchange = exchanges.get(corrId);
                if (exchange == null) {
                    throw new IllegalStateException("Exchange not found for id " + corrId);
                }
                Message msg = (Message) ((ObjectMessage) message).getObject();
                exchange.setFault(msg);
                exchanges.put(exchange.getId(), exchange);
                exchange.setProperty(PROPERTY_CORR_ID + "." + name, exchange.getId());
                requestor.suspend(exchange.getId());
                if (requestor.getTransaction() != null) {
                    exchange.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, requestor.getTransaction());
                }
                send(exchange);
                break;
            }
            default: {
                throw new IllegalStateException("Received unknown message type: " + type);
            }
        }
    }

    /**
     * Process a JBI exchange
     *
     * @param requestor the item to use
     * @param exchange the exchange to process
     * @throws Exception if an error occur
     */
    protected void processExchange(JmsRequestor requestor, Exchange exchange)  throws Exception {
        synchronized (requestor) {
            decrementPendingExchangeIfNeeded(exchange);
            boolean rollbackOnErrors;
            if (exchange.getRole() == Role.Consumer) {
                rollbackOnErrors = Boolean.TRUE.equals(exchange.getProperty(PROPERTY_ROLLBACK_ON_ERRORS + "." + name));
            } else {
                rollbackOnErrors = this.rollbackOnErrors;
            }
            if (exchange.getStatus() == Status.Active) {
                Message msg = exchange.getFault(false);
                int type;
                if (msg != null) {
                    type = JBI_MESSAGE_FAULT;
                } else {
                    msg = exchange.getOut(false);
                    if (msg != null) {
                        type = JBI_MESSAGE_OUT;
                    } else {
                        msg = exchange.getIn(false);
                        if (msg != null) {
                            type = JBI_MESSAGE_IN;
                        } else {
                            throw new IllegalStateException("No normalized message on an active exchange: " + exchange);
                        }
                    }
                }
                javax.jms.Message message = requestor.getSession().createObjectMessage(msg);
                message.setIntProperty(JBI_MESSAGE, type);
                if (type == JBI_MESSAGE_IN) {
                    rollbackOnErrors = this.rollbackOnErrors;
                    exchange.setProperty(PROPERTY_ROLLBACK_ON_ERRORS + "." + name, rollbackOnErrors);
                    message.setStringProperty(JBI_MEP, exchange.getPattern().getWsdlUri());
                    if (exchange.getProperty(MessageExchangeImpl.INTERFACE_NAME_PROP) != null) {
                        message.setStringProperty(JBI_INTERFACE, exchange.getProperty(MessageExchangeImpl.INTERFACE_NAME_PROP).toString());
                    }
                    if (exchange.getOperation() != null) {
                        message.setStringProperty(JBI_OPERATION, exchange.getOperation().toString());
                    }
                    if (exchange.getProperty(MessageExchangeImpl.SERVICE_NAME_PROP) != null) {
                        message.setStringProperty(JBI_SERVICE, exchange.getProperty(MessageExchangeImpl.SERVICE_NAME_PROP).toString());
                    }
                    if (exchange.getProperty(MessageExchangeImpl.SERVICE_ENDPOINT_PROP) != null) {
                        ServiceEndpoint se = (ServiceEndpoint) exchange.getProperty(MessageExchangeImpl.SERVICE_ENDPOINT_PROP);
                        message.setStringProperty(JBI_ENDPOINT, "{" + se.getServiceName().toString() + "}" + se.getEndpointName());
                    }
                    // TODO: write exchange properties
                }
                message.setBooleanProperty(PROPERTY_ROLLBACK_ON_ERRORS, rollbackOnErrors);
                boolean expectResponse;
                if (!rollbackOnErrors) {
                    expectResponse = true;
                } else {
                    switch (exchange.getPattern()) {
                        case InOnly:
                            expectResponse = false;
                            break;
                        case RobustInOnly:
                            expectResponse = exchange.getRole() == Role.Provider;
                            break;
                        case InOut:
                            expectResponse = exchange.getRole() == Role.Provider;
                            break;
                        default:
                            // TODO:
                            expectResponse = true;
                            break;
                    }
                }
                if (expectResponse) {
                    exchanges.put(exchange.getId(), exchange);
                    message.setStringProperty(PROPERTY_SENDER_CLUSTER_NAME, name);
                    message.setStringProperty(PROPERTY_SENDER_CORR_ID, exchange.getId());
                    if (requestor.getMessage() != null) {
                        message.setStringProperty(ClusterEngine.PROPERTY_CLUSTER_NAME, requestor.getMessage().getStringProperty(ClusterEngine.PROPERTY_SENDER_CLUSTER_NAME));
                        message.setStringProperty(ClusterEngine.PROPERTY_CORR_ID, requestor.getMessage().getStringProperty(ClusterEngine.PROPERTY_SENDER_CORR_ID));
                    }
                    requestor.send(message);
                } else {
                    message.setStringProperty(PROPERTY_SENDER_CLUSTER_NAME, name);
                    message.setStringProperty(PROPERTY_SENDER_CORR_ID, null);
                    if (requestor.getMessage() != null) {
                        message.setStringProperty(ClusterEngine.PROPERTY_CLUSTER_NAME, requestor.getMessage().getStringProperty(ClusterEngine.PROPERTY_SENDER_CLUSTER_NAME));
                        message.setStringProperty(ClusterEngine.PROPERTY_CORR_ID, requestor.getMessage().getStringProperty(ClusterEngine.PROPERTY_SENDER_CORR_ID));
                    }
                    requestor.send(message);
                    // TODO: send done in the tx synchronization
                    done(exchange);
                }
            } else if (exchange.getStatus() == Status.Done) {
                boolean doSend;
                if (!rollbackOnErrors) {
                    doSend = true;
                } else {
                    switch (exchange.getPattern()) {
                        case InOnly:
                            // never send done for InOnly
                            doSend = false;
                            break;
                        case RobustInOnly:
                            // only send done when there is no fault
                            // which means the exchange has a consumer role
                            doSend = exchange.getRole() == Role.Consumer;
                            break;
                        case InOptionalOut:
                            // TODO
                            doSend = true;
                            break;
                        case InOut:
                            // in an InOut mep, the DONE status always come from the JBI consumer
                            doSend = false;
                            break;
                        default:
                            throw new IllegalStateException("Unsupported MEP: " + exchange.getPattern());
                    }
                }
                if (doSend) {
                    javax.jms.Message message = requestor.getSession().createMessage();
                    message.setIntProperty(JBI_MESSAGE, JBI_MESSAGE_DONE);
                    message.setStringProperty(PROPERTY_SENDER_CLUSTER_NAME, name);
                    message.setStringProperty(PROPERTY_SENDER_CORR_ID, null);
                    if (requestor.getMessage() != null) {
                        message.setStringProperty(ClusterEngine.PROPERTY_CLUSTER_NAME, requestor.getMessage().getStringProperty(ClusterEngine.PROPERTY_SENDER_CLUSTER_NAME));
                        message.setStringProperty(ClusterEngine.PROPERTY_CORR_ID, requestor.getMessage().getStringProperty(ClusterEngine.PROPERTY_SENDER_CORR_ID));
                    }
                    requestor.send(message);
                }
            } else if (exchange.getStatus() == Status.Error) {
                boolean doSend;
                if (!rollbackOnErrors) {
                    doSend = true;
                } else {
                    switch (exchange.getPattern()) {
                        case InOnly:
                            // never send errors for InOnly
                            doSend = false;
                            break;
                        case RobustInOnly:
                            // do not send exchange from the provider back to the consumer
                            doSend = pool.getTransacted() == Transacted.None || exchange.getRole() != Role.Consumer;
                            break;
                        case InOptionalOut:
                            // TODO
                            doSend = true;
                            break;
                        case InOut:
                            doSend = pool.getTransacted() == Transacted.None || exchange.getRole() != Role.Consumer;
                            break;
                        default:
                            throw new IllegalStateException("Unsupported MEP: " + exchange.getPattern());
                    }
                }
                if (doSend) {
                    javax.jms.Message message = requestor.getSession().createObjectMessage(exchange.getError());
                    message.setIntProperty(JBI_MESSAGE, JBI_MESSAGE_ERROR);
                    message.setStringProperty(PROPERTY_SENDER_CLUSTER_NAME, name);
                    message.setStringProperty(PROPERTY_SENDER_CORR_ID, null);
                    if (requestor.getMessage() != null) {
                        message.setStringProperty(ClusterEngine.PROPERTY_CLUSTER_NAME, requestor.getMessage().getStringProperty(ClusterEngine.PROPERTY_SENDER_CLUSTER_NAME));
                        message.setStringProperty(ClusterEngine.PROPERTY_CORR_ID, requestor.getMessage().getStringProperty(ClusterEngine.PROPERTY_SENDER_CORR_ID));
                    }
                    requestor.send(message);
                } else {
                    requestor.setRollbackOnly();
                }
            } else {
                throw new IllegalStateException("Unknown exchange status: " + exchange);
            }
        }
    }

    protected void decrementPendingExchangeIfNeeded(Exchange exchange) {
        if (exchange.getRole() == Role.Consumer && exchange.getStatus() != Status.Active) {
            if (pendingExchanges.decrementAndGet() < maxPendingExchanges) {
                if (pauseConsumption.compareAndSet(true, false)) {
                    invalidateSelector();
                }
            }
        }
    }

}
