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

import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Role;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.event.ExchangeListener;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalExchange;
import org.fusesource.commons.management.ManagementStrategy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ManagementEndpointRegistry implements ExchangeListener {

    private final Logger logger = LoggerFactory.getLogger(ManagementEndpointRegistry.class);

    private BundleContext bundleContext;
    private ManagementStrategy managementStrategy;
    private final Map<String, InternalEndpoint> internalEndpoints;
    private final Map<String, ManagedEndpoint> endpoints;
    private ServiceTracker managementStrategyTracker;
    private ServiceTracker endpointTracker;

    public ManagementEndpointRegistry() {
        endpoints = new ConcurrentHashMap<String, ManagedEndpoint>();
        internalEndpoints = new ConcurrentHashMap<String, InternalEndpoint>();
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() {
        if (bundleContext == null) {
            throw new IllegalArgumentException("bundleContext must be set");
        }
        managementStrategyTracker = new ServiceTracker(bundleContext, ManagementStrategy.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                ManagementStrategy newMs = (ManagementStrategy) super.addingService(reference);
                if (getService() == null) {
                    bindManagementStrategy(newMs);
                }
                return newMs;
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                ManagementStrategy newMs = (ManagementStrategy) getService();
                bindManagementStrategy(newMs);
                super.removedService(reference, service);
            }
        };
        managementStrategyTracker.open();
        endpointTracker = new ServiceTracker(bundleContext, InternalEndpoint.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                InternalEndpoint endpoint = (InternalEndpoint) super.addingService(reference);
                register(endpoint);
                return endpoint;
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                InternalEndpoint endpoint = (InternalEndpoint) service;
                unregister(endpoint);
                super.removedService(reference, service);
            }
        };
        endpointTracker.open();
    }

    public void destroy() {
        unregisterAll();
        managementStrategyTracker.close();
        endpointTracker.close();
    }

    public void bindManagementStrategy(ManagementStrategy ms) {
        logger.debug("Using new management strategy: {}", ms);
        unregisterAll();
        managementStrategy = ms;
        registerAll();
    }

    protected void registerAll() {
        if (managementStrategy != null) {
            for (String id : internalEndpoints.keySet()) {
                registerEndpoint(internalEndpoints.get(id));
            }
        }
    }

    protected void unregisterAll() {
        if (managementStrategy != null) {
            for (String id : internalEndpoints.keySet()) {
                unregisterEndpoint(internalEndpoints.get(id));
            }
        }
    }

    protected void registerEndpoint(InternalEndpoint iep) {
        if (managementStrategy != null) {
            try {
                logger.info("Registering endpoint: {} with properties {}", iep, iep.getMetaData());
                ManagedEndpoint ep = new ManagedEndpoint(iep, managementStrategy);
                endpoints.put(iep.getId(), ep);
                managementStrategy.manageObject(ep);
            } catch (Exception e) {
                logger.warn("Unable to register managed endpoint.", e);
            }
        }
    }

    private void unregisterEndpoint(InternalEndpoint iep) {
        if (managementStrategy != null) {
            try {
                logger.info("Unregistering endpoint: {} with properties {}", iep, iep.getMetaData());
                ManagedEndpoint ep = endpoints.remove(iep.getId());
                managementStrategy.unmanageObject(ep);
            } catch (Exception e) {
                logger.warn("Unable to unregister managed endpoint.", e);
            }
        }
    }

    public void register(InternalEndpoint endpoint) {
        internalEndpoints.put(endpoint.getId(), endpoint);
        registerEndpoint(endpoint);
    }

    public void unregister(InternalEndpoint endpoint) {
        internalEndpoints.remove(endpoint.getId());
        unregisterEndpoint(endpoint);
    }

    public void exchangeSent(Exchange exchange) {
        try {
            logger.trace("Sending exchange: {}", exchange);
            if (exchange.getStatus() == Status.Active &&
                    exchange.getRole() == Role.Consumer &&
                    exchange.getOut(false) == null &&
                    exchange.getFault(false) == null &&
                    exchange instanceof InternalExchange) {
                String id = ((InternalExchange) exchange).getSource().getId();
                logger.trace("Source endpoint: {} (known endpoints: {})", id, endpoints);
                ManagedEndpoint me = endpoints.get(id);
                if (me == null) {
                	logger.trace("No managed endpoint registered with id: {}", id);
                  
                } else {
                    me.incrementOutbound();
                }
            }
        } catch (Throwable t) {
            logger.warn("Caught exception while processing exchange.", t);
        }
    }

    public void exchangeDelivered(Exchange exchange) {
        try {
            logger.trace("Receiving exchange: {}", exchange);
            if (exchange.getStatus() == Status.Active &&
                    exchange.getRole() == Role.Provider &&
                    exchange.getOut(false) == null &&
                    exchange.getFault(false) == null &&
                    exchange instanceof InternalExchange) {
                String id = ((InternalExchange) exchange).getDestination().getId();
                logger.trace("Dest endpoint: {} (known endpoints: {})", id, endpoints);
                ManagedEndpoint me = endpoints.get(id);
                if (me == null) {
                    logger.warn("No managed endpoint registered with id: {}", id);
                } else {
                    me.incrementInbound();
                }
            }
        } catch (Throwable t) {
            logger.warn("Caught exception while processing exchange.", t);
        }
    }

    public void exchangeFailed(Exchange exchange) {
        ExchangeFailedEvent event = new ExchangeFailedEvent(exchange);
        try {
            managementStrategy.notify(event);
        } catch (Exception ex) {
            logger.warn("ExchangeFailedEvent notification failed", ex);
        }
    }

}
