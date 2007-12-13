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

import java.util.List;

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Reference;
import org.apache.servicemix.nmr.api.ServiceMixException;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class EndpointRegistryImplTest {

    private EndpointRegistryImpl registry;

    @Before
    public void setUp() {
        registry = new EndpointRegistryImpl();
    }

    @Test
    public void testUnregister() {
        Endpoint endpoint = new DummyEndpoint();
        registry.register(endpoint, ServiceHelper.createMap(Endpoint.NAME, "id"));
        Reference ref = registry.lookup(ServiceHelper.createMap(Endpoint.NAME, "id"));
        assertNotNull(ref);
        assertTrue(ref instanceof ReferenceImpl);
        List<InternalEndpoint> endpoints = ((ReferenceImpl) ref).getEndpoints();
        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        registry.unregister(endpoint, null);
        try {
            registry.lookup(ServiceHelper.createMap(Endpoint.NAME, "id"));
        } catch (ServiceMixException e) {
            // ok
        }
    }

    protected static class DummyEndpoint implements Endpoint {
        public void setChannel(Channel channel) {
        }
        public void process(Exchange exchange) {
        }
    }
}
