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
package org.apache.servicemix.jbi.deployer.artifacts;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.runtime.impl.DeliveryChannelImpl;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Role;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.event.EndpointListener;
import org.apache.servicemix.nmr.api.event.ExchangeListener;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalExchange;

/**
 * This class will listen for endpoints activated and link them to service assemblies.
 * This only work if the endpoint is activated synchronously during a call to the SU
 * init() or start() method, but this should always be the case.
 * When an exchange is sent or received, the source / destination endpoint of the exchange
 * are retrieved, and their associated service assembly counter is incremented.
 * When a SA is cleanly shutdown, we wait for the reference count to be 0.
 */
public class AssemblyReferencesListener implements EndpointListener, ExchangeListener {

    private final ThreadLocal<ServiceAssembly> assembly = new ThreadLocal<ServiceAssembly>();
    private final ConcurrentMap<InternalEndpoint, ServiceAssembly> endpoints = new ConcurrentHashMap<InternalEndpoint, ServiceAssembly>();
    private final ConcurrentMap<ServiceAssembly, AtomicInteger> references = new ConcurrentHashMap<ServiceAssembly, AtomicInteger>();
    private final ConcurrentMap<ServiceAssembly, Object> locks = new ConcurrentHashMap<ServiceAssembly, Object>();
    private final ConcurrentMap<InternalExchange, ServiceAssembly> pending = new ConcurrentHashMap<InternalExchange, ServiceAssembly>();

    public void setAssembly(ServiceAssembly assembly) {
        this.assembly.set(assembly);
    }

    public void forget(ServiceAssembly assembly) {
        for (ConcurrentMap.Entry<InternalEndpoint, ServiceAssembly> entry : endpoints.entrySet()) {
            if (entry.getValue() == assembly) {
                endpoints.remove(entry.getKey());
            }
        }
        AtomicInteger count = references.remove(assembly);
        if (count != null) {
            count.set(0);
            Object lock = locks.remove(assembly);
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    public void endpointRegistered(InternalEndpoint endpoint) {
        ServiceAssembly assembly = this.assembly.get();
        if (assembly != null) {
            endpoints.put(endpoint, assembly);
            if (references.get(assembly) == null) {
                references.put(assembly, new AtomicInteger());
            }
            if (locks.get(assembly) == null) {
                locks.put(assembly, new Object());
            }
        }
    }

    public void endpointUnregistered(InternalEndpoint endpoint) {
        endpoints.remove(endpoint);
    }

    public void exchangeSent(Exchange exchange) {
        // Check if this is a new exchange
        if (exchange.getStatus() == Status.Active && exchange.getRole() == Role.Consumer &&
                exchange.getOut(false) == null && exchange.getFault(false) == null) {
            if (exchange instanceof InternalExchange) {
                // Increment reference to the source SA
                InternalExchange ie = (InternalExchange) exchange;
                reference(ie.getSource());
                if (isSync(exchange)) {
                    pending(ie);
                }
            }
        }
    }

    private boolean isSync(Exchange exchange) {
        return exchange.getProperty(DeliveryChannelImpl.SEND_SYNC) != null && exchange.getProperty(DeliveryChannelImpl.SEND_SYNC, Boolean.class).booleanValue();
    }

    private void pending(InternalExchange exchange) {
        ServiceAssembly assembly = endpoints.get(exchange.getSource());
        if (assembly != null) {
          pending.put(exchange, assembly);
        }
    }

    public void exchangeDelivered(Exchange exchange) {
        // Check if the exchange is finished
        if (exchange.getStatus() != Status.Active) {
            if (exchange instanceof InternalExchange) {
                InternalExchange ie = (InternalExchange) exchange;
                // Decrement references to source and destination SA
                unreference(ie.getSource());
                unreference(ie.getDestination());
                pending.remove(exchange);
            }
            // Check if this is a new exchange
        } else if (exchange.getStatus() == Status.Active && exchange.getRole() == Role.Provider &&
                exchange.getOut(false) == null && exchange.getFault(false) == null) {
            if (exchange instanceof InternalExchange) {
                // Increment reference to the destination SA
                InternalExchange ie = (InternalExchange) exchange;
                reference(ie.getDestination());
            }
        }
    }

    public void exchangeFailed(Exchange exchange) {
        if (exchange instanceof InternalExchange) {
            InternalExchange ie = (InternalExchange) exchange;
            // Decrement references to source and destination SA
            unreference(ie.getSource());
            unreference(ie.getDestination());
            pending.remove(exchange);
        }
    }

    public void waitFor(ServiceAssembly assembly) throws InterruptedException {
        if (assembly != null) {
            AtomicInteger count = references.get(assembly);
            if (count != null) {
                if (count.get() != 0) {
                    Object lock = locks.get(assembly);
                    synchronized (lock) {
                        while (count.get() != 0) {
                            lock.wait(Long.MAX_VALUE);
                        }
                    }
                }
            }
        }
    }
    
    public void cancelPendingSyncExchanges(ServiceAssembly assembly) {
        if (assembly != null) {
            for (Exchange exchange : getPending(assembly)) {
                exchange.cancel();
            }
        }
    }

    private void reference(InternalEndpoint endpoint) {
        if (endpoint != null) {
            reference(endpoints.get(endpoint));
        }
    }

    private void unreference(InternalEndpoint endpoint) {
        if (endpoint != null) {
            unreference(endpoints.get(endpoint));
        }
    }

    private void reference(ServiceAssembly assembly) {
        if (assembly != null) {
            AtomicInteger count = references.get(assembly);
            if (count != null) {
                count.incrementAndGet();
            }
        }
    }

    private void unreference(ServiceAssembly assembly) {
        if (assembly != null) {
            AtomicInteger count = references.get(assembly);
            if (count != null) {
                if (count.decrementAndGet() == 0) {
                    Object lock = locks.get(assembly);
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            }
        }
    }

    protected Set<InternalExchange> getPending(ServiceAssembly assembly) {
        Set<InternalExchange> result = new HashSet<InternalExchange>();
        for (InternalExchange exchange : pending.keySet()) {
            if (pending.get(exchange).equals(assembly)) {
                result.add(exchange);
            }
        }
        return result;
    }

}
