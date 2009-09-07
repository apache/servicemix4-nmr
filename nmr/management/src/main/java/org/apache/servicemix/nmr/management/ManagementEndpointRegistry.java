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
package org.apache.servicemix.nmr.management;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Role;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.event.ExchangeListener;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalExchange;
import org.fusesource.commons.management.ManagementStrategy;
import org.springframework.beans.factory.InitializingBean;

/**
 */
public class ManagementEndpointRegistry implements ExchangeListener, InitializingBean {

    private static final transient Log LOG = LogFactory.getLog(ManagementEndpointRegistry.class);

    private ManagementStrategy managementStrategy;
    private Map<String, ManagedEndpoint> endpoints;

    public ManagementEndpointRegistry() {
        endpoints = new ConcurrentHashMap<String, ManagedEndpoint>();
    }

    public ManagementStrategy getManagementAgent() {
        return managementStrategy;
    }

    public void setManagementStrategy(ManagementStrategy managementStrategy) {
        this.managementStrategy = managementStrategy;
    }

    public void register(InternalEndpoint endpoint, Map<String, ?> properties) {
        try {
            LOG.info("Registering endpoint: " + endpoint + " with properties " + properties);
            ManagedEndpoint ep = new ManagedEndpoint(endpoint, properties, managementStrategy);
            endpoints.put(endpoint.getId(), ep);
            managementStrategy.manageObject(ep);
        } catch (Exception e) {
            LOG.warn("Unable to register managed endpoint: " + e, e);
        }
    }

    public void unregister(InternalEndpoint endpoint, Map<String, ?> properties) {
        if (endpoint ==  null) {
            return;
        }
        try {
            LOG.info("Unregistering endpoint: " + endpoint + " with properties " + properties);
            ManagedEndpoint ep = endpoints.remove(endpoint.getId());
            managementStrategy.unmanageObject(ep);
        } catch (Exception e) {
            LOG.warn("Unable to unregister managed endpoint: " + e, e);
        }
    }

    public void exchangeSent(Exchange exchange) {
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Sending exchange: " + exchange);
            }
            if (exchange.getStatus() == Status.Active &&
                    exchange.getRole() == Role.Consumer &&
                    exchange.getOut(false) == null &&
                    exchange.getFault(false) == null &&
                    exchange instanceof InternalExchange) {
                String id = ((InternalExchange) exchange).getSource().getId();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Source endpoint: " + id + " (known endpoints: " + endpoints + ")");
                }
                ManagedEndpoint me = endpoints.get(id);
                if (me == null) {
                    LOG.warn("No managed endpoint registered with id: " + id);
                } else {
                    me.incrementOutbound();
                }
            }
        } catch (Throwable t) {
            LOG.warn("Caught exception while processing exchange: " + t, t);
        }
    }

    public void exchangeDelivered(Exchange exchange) {
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Receiving exchange: " + exchange);
            }
            if (exchange.getStatus() == Status.Active &&
                    exchange.getRole() == Role.Provider &&
                    exchange.getOut(false) == null &&
                    exchange.getFault(false) == null &&
                    exchange instanceof InternalExchange) {
                String id = ((InternalExchange) exchange).getDestination().getId();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Dest endpoint: " + id + " (known endpoints: " + endpoints + ")");
                }
                ManagedEndpoint me = endpoints.get(id);
                if (me == null) {
                    LOG.warn("No managed endpoint registered with id: " + id);
                } else {
                    me.incrementInbound();
                }
            }
        } catch (Throwable t) {
            LOG.warn("Caught exception while processing exchange: " + t, t);
        }
    }

    public void exchangeFailed(Exchange exchange) {
        ExchangeFailedEvent event = new ExchangeFailedEvent(exchange);
        try {
            managementStrategy.notify(event);
        } catch (Exception ex) {
            LOG.warn("ExchangeFailedEvent notification failed", ex);
        }
    }

    public void afterPropertiesSet() throws Exception {
        if (managementStrategy == null) {
            throw new IllegalArgumentException("managementStrategy must not be null");
        }
    }
}
