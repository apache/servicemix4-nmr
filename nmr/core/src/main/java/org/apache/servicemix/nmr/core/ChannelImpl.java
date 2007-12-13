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

import org.apache.servicemix.nmr.api.*;
import org.apache.servicemix.nmr.api.event.ExchangeListener;
import org.apache.servicemix.nmr.api.internal.InternalChannel;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalExchange;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The Channel implementation.
 * The channel uses an Executor (usually a thread pool)
 * to delegate  
 *
 * @version $Revision: $
 * @since 4.0
 */
public class ChannelImpl implements InternalChannel {

    private final InternalEndpoint endpoint;
    private final Executor executor;
    private final NMR nmr;

    public ChannelImpl(InternalEndpoint endpoint, Executor executor, NMR nmr) {
        this.endpoint = endpoint;
        this.executor = executor;
        this.nmr = nmr;
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
        try {
            dispatch(e);
            if (timeout > 0) {
                if (!lock.tryAcquire(timeout, TimeUnit.MILLISECONDS)) {
                    throw new TimeoutException();
                }
            } else {
                lock.acquire();
            }
        } catch (InterruptedException ex) {
            exchange.setError(ex);
            exchange.setStatus(Status.Error);
            return false;
        } catch (TimeoutException ex) {
            exchange.setError(ex);
            exchange.setStatus(Status.Error);
            return false;
        }
        return true;
    }

    /**
     * Closes the channel, freeing up any resources (like sockets, threads etc).
     * Channel that are injected onto Endpoints will be closed automatically by
     * the NMR.
     */
    public void close() {
        // TODO
    }

    /**
     * Deliver an exchange to the endpoint using this channel
     *
     * @param exchange the exchange to delivery
     */
    public void deliver(final InternalExchange exchange) {
        // Handle case where the exchange has been sent synchronously
        Semaphore lock = exchange.getRole() == Role.Provider ? exchange.getConsumerLock(false)
                                                             : exchange.getProviderLock(false);
        if (lock != null) {
            lock.release();
            return;
        }
        // Delegate processing to the executor
        this.executor.execute(new Runnable() {
            public void run() {
                process(exchange);
            }
        });
    }

    /**
     * Processes the exchange.  Delegate to the endpoint for actual processing.
     *
     * @param exchange the exchange to process
     */
    protected void process(InternalExchange exchange) {
        // Set destination endpoint
        if (exchange.getDestination() == null) {
            exchange.setDestination(endpoint);
        }
        // Call listeners
        for (ExchangeListener l : nmr.getListenerRegistry().getListeners(ExchangeListener.class)) {
            l.exchangeDelivered(exchange);
        }
        // Change role
        exchange.setRole(exchange.getRole() == Role.Provider ? Role.Consumer : Role.Provider);
        // Check if sendSync was used, in which case we need to unblock the other side
        // rather than delivering the exchange
        // TODO:
        // Process exchange
        endpoint.process(exchange);
    }

    /**
     * Dispatch the exchange to the NMR
     *
     * @param exchange the exchange to dispatch
     */
    protected void dispatch(InternalExchange exchange) {
        // Set source endpoint
        if (exchange.getSource() == null) {
            exchange.setSource(endpoint);
        }
        // Call listeners
        for (ExchangeListener l : nmr.getListenerRegistry().getListeners(ExchangeListener.class)) {
            l.exchangeSent(exchange);
        }
        // Dispatch in NMR
        nmr.getFlowRegistry().dispatch(exchange);
    }
}
