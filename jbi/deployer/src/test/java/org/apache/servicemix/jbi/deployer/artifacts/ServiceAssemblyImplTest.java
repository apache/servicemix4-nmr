/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jbi.deployer.artifacts;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.artifacts.AbstractLifecycleJbiArtifact.State;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceAssemblyDesc;
import org.apache.servicemix.jbi.deployer.impl.AssemblyReferencesListener;
import org.apache.servicemix.nmr.api.Wire;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.Preferences;

/**
 * Test cases for {@link ServiceAssemblyImpl}
 */
public class ServiceAssemblyImplTest {
    
    private Mockery mockery;

    @Before
    public void setup() {
        mockery = new Mockery();
    }
    
    @Test
    public void testWiringOnServiceAssemblyConnections() throws Exception {
        ServiceAssemblyDesc descriptor = DescriptorFactory.buildDescriptor(DescriptorFactory.class.getResource("serviceAssembly.xml")).getServiceAssembly();
        final Preferences prefs = mockery.mock(Preferences.class);
        mockery.checking(new Expectations() {{
            one(prefs).get("state", State.Shutdown.name());
                will(returnValue(State.Shutdown.name()));
            one(prefs).put("state", State.Started.name());
            one(prefs).flush();
            one(prefs).put("state", State.Stopped.name());
            one(prefs).flush();
        }});
        final List<Wire> wires = new LinkedList<Wire>();
        ServiceAssembly sa = new ServiceAssemblyImpl(null, descriptor, new ArrayList<ServiceUnitImpl>(), prefs, new AssemblyReferencesListener(), false) {
            @Override
            protected void registerWire(Wire wire, Map<String, ?> from) {
                wires.add(wire);
            }
            @Override
            protected void unregisterWire(Wire wire, Map<String, ?> from) {
                wires.remove(wire);
            }
        };
        sa.start();
        assertEquals(2, wires.size());
        
        sa.stop();
        assertEquals(0, wires.size());
    }
}
