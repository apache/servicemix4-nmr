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
package org.apache.servicemix.nmr.core;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.nmr.api.AbortedException;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Role;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.event.ExchangeListener;
import org.apache.servicemix.nmr.api.internal.InternalChannel;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalExchange;
import org.apache.servicemix.executors.Executor;
import org.apache.servicemix.executors.ExecutorAwareRunnable;

/**
 * The {@link org.apache.servicemix.nmr.api.Channel} implementation.
 * The channel uses an Executor (usually a thread pool)
 * to delegate task executions to.
 *
 * @version $Revision: $
 * @since 4.0
 */
public class ChannelImpl implements InternalChannel {

    private static final Log LOG = LogFactory.getLog(NMR.class);

    private final InternalEndpoint endpoint;
    private final Executor executor;
    private final NMR nmr;
    private String name;
    private AtomicBoolean closed = new AtomicBoolean();
    private boolean shouldRunSynchronously;

    public ChannelImpl(InternalEndpoint endpoint, Executor executor, NMR nmr) {
        this.endpoint = endpoint;
        this.executor = executor;
        this.nmr = nmr;
        Map<String,?> props = nmr.getEndpointRegistry().getProperties(endpoint);
        if (props != null) {
            this.name = (String) props.get(Endpoint.NAME);
        }
        if (this.name == null) {
            this.name = toString();
        }
    }

    public boolean isShouldRunSynchronously() {
        return shouldRunSynchronously;
    }

    public void setShouldRunSynchronously(boolean shouldRunSynchronously) {
        this.shouldRunSynchronously = shouldRunSynchronously;
    }

    /**
     * Access to the bus
     *
     * @return the NMR
     */
    public NMR getNMR() {
        return nmr;
    }

    /**
     * Access to the endpoint
     *
     * @return the endpoint for which this channel has been created
     */
    public InternalEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Creates a new exchange.
     *
     * @param pattern specify the InOnly / InOut / RobustInOnly / RobustInOut
     * @return a new exchange of the given pattern
     */
    public Exchange createExchange(Pattern pattern) {
        return new ExchangeImpl(pattern);
    }

    /**
     * An asynchronous invocation of the service
     *
     * @param exchange the exchange to send
     */
    public void send(Exchange exchange) {
        InternalExchange e = (InternalExchange) exchange;
        dispatch(e);
    }

    /**
     * Synchronously send the exchange, blocking until the exchange is returned.
     *
     * @param exchange the exchange to send
     * @return <code>true</code> if the exchange has been processed succesfully
     */
    public boolean sendSync(Exchange exchange) {
        return sendSync(exchange, 0);
    }

    /**
     * Synchronously send the exchange
     *
     * @param exchange the exchange to send
     * @param timeout  time to wait in milliseconds
     * @return <code>true</code> if the exchange has been processed succesfully
     */
    public boolean sendSync(Exchange exchange, long timeout) {
        InternalExchange e = (InternalExchange) exchange;
        Semaphore lock = e.getRole() == Role.Consumer ? e.getConsumerLock(true)
                : e.getProviderLock(true);
        dispatch(e);
        Thread thread = Thread.currentThread();
        String original = thread.getName();
        try {
            if (timeout > 0) {
                if (!lock.tryAcquire(timeout, TimeUnit.MILLISECONDS)) {
                    throw new TimeoutException();
                }
            } else {
                thread.setName(original + " (waiting for exchange " + exchange.getId() + ")");
                lock.acquire();
            }
            e.setRole(e.getRole() == Role.Consumer ? Role.Provider : Role.Consumer);
        } catch (InterruptedException ex) {
            exchange.setError(ex);
            for (ExchangeListener l : nmr.getListenerRegistry().getListeners(ExchangeListener.class)) {
                l.exchangeFailed(exchange);
            }
            return false;
        } catch (TimeoutException ex) {
            exchange.setError(new AbortedException(ex));
            for (ExchangeListener l : nmr.getListenerRegistry().getListeners(ExchangeListener.class)) {
                l.exchangeFailed(exchange);
            }
            return false;
        } finally {
            thread.setName(original);
        }
        return true;
    }

    /**
     * Closes the channel, freeing up any resources (like sockets, threads etc).
     * Channel that are injected onto Endpoints will be closed automatically by
     * the NMR.
     */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Map<String,?> props = nmr.getEndpointRegistry().getProperties(endpoint);
            nmr.getEndpointRegistry().unregister(endpoint, props);
            executor.shutdown();
        }
    }

    /**
     * Deliver an exchange to the endpoint using this channel
     *
     * @param exchange the exchange to delivery
     */
    public void deliver(final InternalExchange exchange) {
        if (closed.get()) {
            throw new ChannelClosedException();
        }
        // Log the exchange
        if (LOG.isTraceEnabled()) {
            LOG.trace("Channel " + name + " delivering exchange: " + exchange.display(true));
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Channel " + name + " delivering exchange: " + exchange.display(false));
        }
        // Handle case where the exchange has been sent synchronously
        Semaphore lock = exchange.getRole() == Role.Provider ? exchange.getConsumerLock(false)
                : exchange.getProviderLock(false);
        if (lock != null) {
            // Call listeners
            for (ExchangeListener l : nmr.getListenerRegistry().getListeners(ExchangeListener.class)) {
                l.exchangeDelivered(exchange);
            }
            lock.release();
            return;
        }
        // Delegate processing to the executor
        try {
            this.executor.execute(new ExecutorAwareRunnable() {
                public void run() {
                    process(exchange);
                }
                public boolean shouldRunSynchronously() {
                    return shouldRunSynchronously;
                }
            });
        } catch (RejectedExecutionException e) {
            if (closed.get()) {
                throw new ChannelClosedException();
            } else {
                throw e;
            }
        }
    }

    /**
     * Processes the exchange.  Delegate to the endpoint for actual processing.
     *
     * @param exchange the exchange to process
     */
    protected void process(InternalExchange exchange) {
        try {
            // Check for aborted exchanges
            if (exchange.getError() instanceof AbortedException) {
                return;
            }
            // Set destination endpoint
            if (exchange.getDestination() == null) {
                exchange.setDestination(endpoint);
            }
            // Change role
            exchange.setRole(exchange.getRole() == Role.Provider ? Role.Consumer : Role.Provider);
            // Call listeners
            for (ExchangeListener l : nmr.getListenerRegistry().getListeners(ExchangeListener.class)) {
                l.exchangeDelivered(exchange);
            }
            // Check if sendSync was used, in which case we need to unblock the other side
            // rather than delivering the exchange
            // TODO:
            // Process exchange
            endpoint.process(exchange);
        } catch (RuntimeException e) {
            handleFailure(exchange, e, false);
        }
    }                             

    /**
     * Dispatch the exchange to the NMR
     *
     * @param exchange the exchange to dispatch
     */
    protected void dispatch(InternalExchange exchange) {
        if (closed.get()) {
            throw new ChannelClosedException();
        }
        // Log the exchange
        if (LOG.isTraceEnabled()) {
            LOG.trace("Channel " + name + " dispatching exchange: " + exchange.display(true));
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Channel " + name + " dispatching exchange: " + exchange.display(false));
        }
        // Set source endpoint
        if (exchange.getSource() == null) {
            exchange.setSource(endpoint);
        }
        // Call listeners
        for (ExchangeListener l : nmr.getListenerRegistry().getListeners(ExchangeListener.class)) {
            l.exchangeSent(exchange);
        }
        // Dispatch in NMR
        try {
            nmr.getFlowRegistry().dispatch(exchange);
        } catch (RuntimeException e) {
            handleFailure(exchange, e, true);
        }
    }

    protected void handleFailure(InternalExchange exchange, RuntimeException e, boolean dispatch) {
        LOG.warn("Error processing exchange " + exchange, e);
        if (dispatch) {
            exchange.setError(e);
            for (ExchangeListener l : nmr.getListenerRegistry().getListeners(ExchangeListener.class)) {
                l.exchangeFailed(exchange);
            }
            // Rethrow the exception so that sendSync are unblocked
            throw e;
        } else {
            // If the exchange is active, let's try to send an error on behalf of the endpoint
            if (exchange.getStatus() == Status.Active) {
                try {
                    exchange.setError(e);
                    send(exchange);
                } catch (RuntimeException e2) {
                    for (ExchangeListener l : nmr.getListenerRegistry().getListeners(ExchangeListener.class)) {
                        l.exchangeFailed(exchange);
                    }
                }
            } else {
                exchange.setError(e);
                Semaphore lock = exchange.getRole() == Role.Provider ? exchange.getConsumerLock(false)
                        : exchange.getProviderLock(false);
                if (lock != null) {
                    lock.release();
                }
                for (ExchangeListener l : nmr.getListenerRegistry().getListeners(ExchangeListener.class)) {
                    l.exchangeFailed(exchange);
                }
            }
        }
    }

}
