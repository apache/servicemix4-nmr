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
package org.apache.servicemix.nmr.api.service;

import java.util.Map;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Wire;

import junit.framework.TestCase;

/**
 * Test cases for {@link ServiceHelper}
 */
public class ServiceHelperTest extends TestCase {
       
    public void testCreateWire() throws Exception {
        Map<String, Object> from = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:service",
                                                           Endpoint.ENDPOINT_NAME, "endpoint");
        Map<String, Object> to = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:wired-service",
                                                         Endpoint.ENDPOINT_NAME, "endpoint");
        Wire wire = ServiceHelper.createWire(from, to);
        assertEquals(from, wire.getFrom());
        assertEquals(to, wire.getTo());
    }

    public void testEqualsHandleNull() throws Exception {
        Map<String, Object> map = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:service",
                                                            Endpoint.ENDPOINT_NAME, "endpoint");
        assertFalse("Should always return false when either one is null", ServiceHelper.equals(null, map));
        assertFalse("Should always return false when either one is null", ServiceHelper.equals(map, null));
    }
    
    public void testEqualsSameSize() throws Exception {
        Map<String, Object> first = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:service",
                                                            Endpoint.ENDPOINT_NAME, "endpoint");
        Map<String, Object> second = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:service",
                                                             Endpoint.ENDPOINT_NAME, "endpoint",
                                                             Endpoint.INTERFACE_NAME, "test:interface");
        assertFalse("Maps with different sizes shouldn't be equal", ServiceHelper.equals(first, second));
        assertTrue("Maps with different sizes but the same values should match", ServiceHelper.matches(first, second));
    }
    
    public void testEqualsSameKeys() throws Exception {
        Map<String, Object> first = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:service",
                                                            Endpoint.ENDPOINT_NAME, "endpoint");
        Map<String, Object> second = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:wired-service",
                                                             Endpoint.INTERFACE_NAME, "test:interface");
        assertFalse("Maps with different values shouldn't be equal", ServiceHelper.equals(first, second));
        assertFalse("Maps with different values shouldn't match", ServiceHelper.matches(first, second));
    }

    public void testEqualsSameValues() throws Exception {
        Map<String, Object> first = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:service",
                                                            Endpoint.ENDPOINT_NAME, "endpoint");
        Map<String, Object> second = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:wired-service",
                                                             Endpoint.ENDPOINT_NAME, "endpoint");
        assertFalse("Maps with different values for the same key shouldn't match", ServiceHelper.equals(first, second));
    }
    
    public void testEquals() throws Exception {
        Map<String, Object> first = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:service",
                                                            Endpoint.ENDPOINT_NAME, "endpoint");
        Map<String, Object> second = ServiceHelper.createMap(Endpoint.SERVICE_NAME, "test:service",
                                                             Endpoint.ENDPOINT_NAME, "endpoint");
        assertTrue(ServiceHelper.equals(first, second));
    }
}
