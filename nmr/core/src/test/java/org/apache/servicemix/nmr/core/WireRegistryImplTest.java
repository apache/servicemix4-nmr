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
import java.util.Map;

import junit.framework.TestCase;

import org.apache.servicemix.nmr.api.Wire;
import org.apache.servicemix.nmr.api.WireRegistry;
import org.apache.servicemix.nmr.api.service.ServiceHelper;

/**
 * Test cases for {@link WireRegistryImpl}
 */
public class WireRegistryImplTest extends TestCase {
    
    public void testBasicRegistryBehavior() {
        Wire wire = new MockWire();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("wire-length", 12);
        properties.put("wire-type", "coton");
        
        WireRegistry registry = new WireRegistryImpl();
        registry.register(wire, properties);
        assertEquals(1, registry.getServices().size());
        assertEquals(properties, registry.getProperties(wire));
        
        assertSame(wire, registry.getWire(properties));
        
        registry.unregister(wire, properties);
        assertEquals(0, registry.getServices().size());
        assertEquals(null, registry.getProperties(wire));
    }
    
    public void testBasicRegistryBehaviorWithConvenienceMethods() {
        Map<String, Object> from = new HashMap<String, Object>();
        from.put("wire-length", 12);
        from.put("wire-type", "coton");
        Wire wire = ServiceHelper.createWire(from, ServiceHelper.createMap("wire-usage", "knitting"));
        
        WireRegistry registry = new WireRegistryImpl();
        registry.register(wire);
        assertEquals(1, registry.getServices().size());
        assertEquals(from, registry.getProperties(wire));
        
        assertSame(wire, registry.getWire(from));
        
        registry.unregister(wire);
        assertEquals(0, registry.getServices().size());
        assertEquals(null, registry.getProperties(wire));
    }
    
    private static final class MockWire implements Wire {

        public Map<String, ?> getFrom() {
            return null;
        }

        public Map<String, ?> getTo() {
            return null;
        }
    }
}
