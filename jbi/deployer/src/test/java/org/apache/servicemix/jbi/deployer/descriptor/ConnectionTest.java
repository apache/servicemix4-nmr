/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jbi.deployer.descriptor;

import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Wire;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import junit.framework.TestCase;

/**
 * Test case for {@link Connection}
 */
public class ConnectionTest extends TestCase {
    
    private static final String CONSUMER_ENDPOINT = "consumer-endpoint";
    private static final QName CONSUMER_INTERFACE = new QName("urn:test:consumer", "consumer-inferface");
    private static final QName CONSUMER_SERVICE = new QName("urn:test:consumer", "consumer-service");
    private static final String PROVIDER_ENDPOINT = "provider-endpoint";
    private static final QName PROVIDER_SERVICE = new QName("urn:test:provider", "provider-service");
    
    public void testWireOnInterface() throws Exception {
        Connection connection = new Connection();
        Consumer consumer = new Consumer();
        consumer.setInterfaceName(CONSUMER_INTERFACE);
        connection.setConsumer(consumer);
        connection.setProvider(createProvider());
        
        Wire wire = connection.getWire();
        assertNotNull(wire);
        assertSame(wire, connection.getWire());
        Map<String, ?> from = ServiceHelper.createMap(Endpoint.INTERFACE_NAME, CONSUMER_INTERFACE.toString());
        assertTrue(ServiceHelper.equals(from, wire.getFrom()));
        assertWireToEnd(wire);
    }

    public void testWireOnServiceAndEndpoint() throws Exception {
        Connection connection = new Connection();
        Consumer consumer = new Consumer();
        consumer.setServiceName(CONSUMER_SERVICE);
        consumer.setEndpointName(CONSUMER_ENDPOINT);
        connection.setConsumer(consumer);
        connection.setProvider(createProvider());
        
        Wire wire = connection.getWire();
        assertNotNull(wire);
        assertSame(wire, connection.getWire());
        Map<String, ?> from = ServiceHelper.createMap(Endpoint.SERVICE_NAME, CONSUMER_SERVICE.toString(),
                                                      Endpoint.ENDPOINT_NAME, CONSUMER_ENDPOINT);
        assertTrue(ServiceHelper.equals(from, wire.getFrom()));
        assertWireToEnd(wire);
    }

    private void assertWireToEnd(Wire wire) {
        Map<String, ?> to = ServiceHelper.createMap(Endpoint.SERVICE_NAME, PROVIDER_SERVICE.toString(),
                                                    Endpoint.ENDPOINT_NAME, PROVIDER_ENDPOINT);
        assertTrue(ServiceHelper.equals(to, wire.getTo()));
        
    }

    private Provider createProvider() {
        Provider provider = new Provider();
        provider.setServiceName(PROVIDER_SERVICE);
        provider.setEndpointName(PROVIDER_ENDPOINT);
        return provider;
    }

}
