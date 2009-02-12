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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.Reference;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.event.EndpointListener;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

public class EndpointRegistryImplTest {

    private NMR nmr;
    private EndpointRegistry registry;

    @Before
    public void setUp() {
        ServiceMix smx = new ServiceMix();
        smx.init();
        nmr = smx;
        registry = smx.getEndpointRegistry();
        assertTrue(registry instanceof EndpointRegistryImpl);
        assertSame(nmr, ((EndpointRegistryImpl) registry).getNmr());
    }

    @Test
    public void testRegistryConstructor() {
        EndpointRegistryImpl reg = new EndpointRegistryImpl();
        try {
            reg.init();
            fail();
        } catch (NullPointerException e) {
        }
        reg.setNmr(new ServiceMix());
        assertNotNull(reg.getNmr());
        reg.init();
    }

    @Test
    public void testRegisterUnregister() throws Exception {
        Endpoint endpoint = new DummyEndpoint();
        Reference ref = registry.lookup(ServiceHelper.createMap(Endpoint.NAME, "id"));
        assertNotNull(ref);
        assertTrue(ref instanceof DynamicReferenceImpl);
        DynamicReferenceImpl r = (DynamicReferenceImpl) ref;
        assertNotNull(r.choose());
        assertFalse(r.choose().iterator().hasNext());
        registry.register(endpoint, ServiceHelper.createMap(Endpoint.NAME, "id"));
        assertNotNull(r.choose());
        assertTrue(r.choose().iterator().hasNext());
        registry.unregister(endpoint, null);
        assertNotNull(r.choose());
        assertFalse(r.choose().iterator().hasNext());
    }

    @Test
    public void testLdapFilter() throws Exception {
        System.setProperty("org.osgi.vendor.framework", "org.apache.servicemix.nmr.core");

        Endpoint endpoint = new DummyEndpoint();
        Reference ref = registry.lookup("(NAME=id)");
        assertNotNull(ref);
        assertTrue(ref instanceof DynamicReferenceImpl);
        DynamicReferenceImpl r = (DynamicReferenceImpl) ref;
        assertNotNull(r.choose());
        assertFalse(r.choose().iterator().hasNext());
        registry.register(endpoint, ServiceHelper.createMap(Endpoint.NAME, "id"));
        assertNotNull(r.choose());
        assertTrue(r.choose().iterator().hasNext());
        registry.unregister(endpoint, null);
        assertNotNull(r.choose());
        assertFalse(r.choose().iterator().hasNext());
    }

    @Test
    public void testEndpointListener() throws Exception {
        final CountDownLatch regLatch = new CountDownLatch(1);
        final CountDownLatch unregLatch = new CountDownLatch(1);
        Endpoint endpoint = new DummyEndpoint();
        nmr.getListenerRegistry().register(new EndpointListener() {
            public void endpointRegistered(InternalEndpoint endpoint) {
                regLatch.countDown();
            }
            public void endpointUnregistered(InternalEndpoint endpoint) {
                unregLatch.countDown();
            }
        }, new HashMap<String,Object>());
        registry.register(endpoint, ServiceHelper.createMap(Endpoint.NAME, "id"));
        assertTrue(regLatch.await(1, TimeUnit.SECONDS));
        registry.unregister(endpoint, null);
        assertTrue(unregLatch.await(1, TimeUnit.SECONDS));
    }
    
    @Test
    public void testHandleWiring() throws Exception {
        final Map<String, Object> from = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:wired-service",
                                                                 Endpoint.ENDPOINT_NAME, "endpoint");
        final Map<String, Object> to = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:service",
                                                               Endpoint.ENDPOINT_NAME, "endpoint");
        createWiredEndpoint(from, to);
        assertEquals(to, ((EndpointRegistryImpl) registry).handleWiring(from));
        assertEquals(to, ((EndpointRegistryImpl) registry).handleWiring(to));
    }
        
    public void testEndpointWiringOnQuery() throws Exception {
        final Map<String, Object> from = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:wired-service",
                                                                 Endpoint.ENDPOINT_NAME, "endpoint");
        final Endpoint endpoint = createWiredEndpoint(from);
        
        // make sure that the query for the wire's from returns the target endpoint
        List<Endpoint> result = registry.query(from);
        assertEquals(1, result.size());
        assertEquals(endpoint, ((InternalEndpoint) result.get(0)).getEndpoint());
    }
    
    @Test
    public void testEndpointWiringOnLookup() throws Exception {
        final Map<String, Object> from = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:wired-service",
                                                                 Endpoint.ENDPOINT_NAME, "endpoint");
        final Endpoint endpoint = createWiredEndpoint(from);
        
        // make sure that the query for the wire's from returns the target endpoint
        Reference ref = registry.lookup(from);
        assertNotNull(ref);
        assertTrue(ref instanceof DynamicReferenceImpl);
        DynamicReferenceImpl reference = (DynamicReferenceImpl) ref;
        Iterable<InternalEndpoint> endpoints = reference.choose();
        assertNotNull(endpoints);
        assertTrue(endpoints.iterator().hasNext());
        assertEquals(endpoint, endpoints.iterator().next().getEndpoint());
    }

    private Endpoint createWiredEndpoint(Map<String, Object> from) {
        return createWiredEndpoint(from, ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:service",
                                                                 Endpoint.ENDPOINT_NAME, "endpoint"));
    }

    private Endpoint createWiredEndpoint(Map<String, Object> from, Map<String, Object> to) {
        final Endpoint endpoint = new DummyEndpoint();
        registry.register(endpoint, to);
        registry.register(ServiceHelper.createWire(from, to));
        return endpoint;
    }

    protected static class DummyEndpoint implements Endpoint {
        public void setChannel(Channel channel) {
        }
        public void process(Exchange exchange) {
        }
    }
}
