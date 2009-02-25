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

import org.apache.servicemix.nmr.api.WireRegistry;
import junit.framework.TestCase;

/**
 * Test cases for {@link ServiceMix}
 */
public class ServiceMixTest extends TestCase {
    
    public void testInit() {
        ServiceMix servicemix = new ServiceMix();
        servicemix.init();
        assertNotNull(servicemix.getEndpointRegistry());
        assertNotNull(servicemix.getFlowRegistry());
        assertNotNull(servicemix.getListenerRegistry());
        assertNotNull(servicemix.getWireRegistry());
    }
    
    public void testSetWireRegistry() {
        ServiceMix servicemix = new ServiceMix();
        WireRegistry registry = new WireRegistryImpl();
        servicemix.setWireRegistry(registry);
        servicemix.init();
        assertSame("Should use the registry that was set instead of creating a new now",
                   registry, servicemix.getWireRegistry());
    }
}
