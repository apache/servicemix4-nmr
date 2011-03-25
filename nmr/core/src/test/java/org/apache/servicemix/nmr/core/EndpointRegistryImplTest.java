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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import org.apache.servicemix.executors.ExecutorFactory;
import org.apache.servicemix.executors.impl.ExecutorConfig;
import org.apache.servicemix.executors.impl.ExecutorImpl;
import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.Reference;
import org.apache.servicemix.nmr.api.event.EndpointListener;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalReference;
import org.apache.servicemix.nmr.api.service.ServiceHelper;

public class EndpointRegistryImplTest extends TestCase {

    private NMR nmr;
    private EndpointRegistry registry;

    public void setUp() {
        ServiceMix smx = new ServiceMix();
        smx.init();
        nmr = smx;
        registry = smx.getEndpointRegistry();
        assertTrue(registry instanceof EndpointRegistryImpl);
        assertSame(nmr, ((EndpointRegistryImpl) registry).getNmr());
    }

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

    public void testRegisterUnregister() throws Exception {
        Endpoint endpoint = new DummyEndpoint();
        Reference ref = registry.lookup(ServiceHelper.createMap(Endpoint.NAME, "id"));
        assertNotNull(ref);
        assertTrue(ref instanceof InternalReference);
        InternalReference r = (InternalReference) ref;
        assertNotNull(r.choose(registry));
        assertFalse(r.choose(registry).iterator().hasNext());
        registry.register(endpoint, ServiceHelper.createMap(Endpoint.NAME, "id"));
        assertNotNull(r.choose(registry));
        assertTrue(r.choose(registry).iterator().hasNext());
        registry.unregister(endpoint, null);
        assertNotNull(r.choose(registry));
        assertFalse(r.choose(registry).iterator().hasNext());
    }

    public void testRegisterWithoutSyncChannel() throws Exception {
        DummyEndpoint endpoint = new DummyEndpoint();

        Map<String, Object> properties = ServiceHelper.createMap(Endpoint.NAME, "endpoint-id");

        registry.register(endpoint, properties);
        assertFalse("Injected channel should not have shouldRunSynchronously enabled",
                    endpoint.channel.isShouldRunSynchronously());
    }
    
    public void testRegisterWithSyncChannel() throws Exception {
        DummyEndpoint endpoint = new DummyEndpoint();

        Map<String, Object> properties = ServiceHelper.createMap(Endpoint.NAME, "endpoint-id");
        properties.put(Endpoint.CHANNEL_SYNC_DELIVERY, true);

        registry.register(endpoint, properties);
        assertTrue("Injected channel should have shouldRunSynchronously enabled",
                   endpoint.channel.isShouldRunSynchronously());
    }

    /*
     * Test to ensure that the CHANNEL_SYNC_DELIVERY flag can also be set with a String instead of Boolean value
     */
    public void testStringChannelSyncDeliveryProperty() throws Exception {
        DummyEndpoint endpoint = new DummyEndpoint();

        Map<String, Object> properties = ServiceHelper.createMap(Endpoint.NAME, "endpoint-id");
        properties.put(Endpoint.CHANNEL_SYNC_DELIVERY, "true");

        registry.register(endpoint, properties);
        assertTrue("Injected channel should have shouldRunSynchronously enabled",
                   endpoint.channel.isShouldRunSynchronously());
    }

    public void testLdapFilter() throws Exception {
        System.setProperty("org.osgi.vendor.framework", "org.apache.servicemix.nmr.core");

        Endpoint endpoint = new DummyEndpoint();
        Reference ref = registry.lookup("(NAME=id)");
        assertNotNull(ref);
        assertTrue(ref instanceof InternalReference);
        InternalReference r = (InternalReference) ref;
        assertNotNull(r.choose(registry));
        assertFalse(r.choose(registry).iterator().hasNext());
        registry.register(endpoint, ServiceHelper.createMap(Endpoint.NAME, "id"));
        assertNotNull(r.choose(registry));
        assertTrue(r.choose(registry).iterator().hasNext());
        registry.unregister(endpoint, null);
        assertNotNull(r.choose(registry));
        assertFalse(r.choose(registry).iterator().hasNext());
    }

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
    
    public void testEndpointWiringOnLookup() throws Exception {
        final Map<String, Object> from = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:wired-service",
                                                                 Endpoint.ENDPOINT_NAME, "endpoint");
        final Endpoint endpoint = createWiredEndpoint(from);
        
        // make sure that the query for the wire's from returns the target endpoint
        Reference ref = registry.lookup(from);
        assertNotNull(ref);
        assertTrue(ref instanceof InternalReference);
        InternalReference reference = (InternalReference) ref;
        Iterable<InternalEndpoint> endpoints = reference.choose(registry);
        assertNotNull(endpoints);
        assertTrue(endpoints.iterator().hasNext());
        assertEquals(endpoint, endpoints.iterator().next().getEndpoint());
    }

    public void testUntargetableEndpointLookup() throws Exception {
        registry.register(new DummyEndpoint(), ServiceHelper.createMap(Endpoint.NAME, "id", Endpoint.UNTARGETABLE, "true"));
        // make sure that the query for the wire's from returns the target endpoint
        Reference ref = registry.lookup(ServiceHelper.createMap());
        assertNotNull(ref);
        assertTrue(ref instanceof InternalReference);
        InternalReference reference = (InternalReference) ref;
        Iterable<InternalEndpoint> endpoints = reference.choose(registry);
        assertNotNull(endpoints);
        assertFalse(endpoints.iterator().hasNext());
    }

    /**
     * Test to ensure that the underlying executor can be configured by adding additional properties when registering
     * the endpoint
     */
    public void testConfigureChannelOnRegistration() throws Exception {
        DummyEndpoint endpoint = new DummyEndpoint();
        registry.register(endpoint, ServiceHelper.createMap(Endpoint.NAME, "some.endpoint.name",
                                                            ExecutorFactory.CORE_POOL_SIZE, "12"));


        // a bit of reflection magic to find the executor config
        Field field = ExecutorImpl.class.getDeclaredField("config");
        field.setAccessible(true);
        ExecutorConfig config = (ExecutorConfig) field.get(endpoint.channel.getExecutor());

        // ensure that we have the core pool size value we requested
        assertEquals(new Integer(12), config.getCorePoolSize());

        // let's make sure the endpoint is still available under the intended name
        InternalReference reference =
                (InternalReference) registry.lookup(ServiceHelper.createMap(Endpoint.NAME, "some.endpoint.name"));
        assertNotNull(reference.choose(registry));
        assertTrue(reference.choose(registry).iterator().hasNext());
    }

    private Endpoint createWiredEndpoint(Map<String, Object> from) {
        return createWiredEndpoint(from, ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:service",
                                                                 Endpoint.ENDPOINT_NAME, "endpoint"));
    }

    private Endpoint createWiredEndpoint(Map<String, Object> from, Map<String, Object> to) {
        final Endpoint endpoint = new DummyEndpoint();
        registry.register(endpoint, to);
        nmr.getWireRegistry().register(ServiceHelper.createWire(from, to));
        return endpoint;
    }

    protected static class DummyEndpoint implements Endpoint {
        private ChannelImpl channel;

        public void setChannel(Channel channel) {
            this.channel = (ChannelImpl) channel;
        }
        public void process(Exchange exchange) {
        }
    }
}
