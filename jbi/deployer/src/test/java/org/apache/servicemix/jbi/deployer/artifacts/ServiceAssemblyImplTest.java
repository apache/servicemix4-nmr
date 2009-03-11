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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jbi.JBIException;
import javax.jbi.component.Component;

import junit.framework.TestCase;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.artifacts.AbstractLifecycleJbiArtifact.State;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceAssemblyDesc;
import org.apache.servicemix.nmr.api.Wire;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.prefs.Preferences;

/**
 * Test cases for {@link ServiceAssemblyImpl}
 */
public class ServiceAssemblyImplTest extends TestCase {

    public void testStartAssemblyWithStoppedComponents() throws Exception {
        ServiceAssemblyDesc descriptor = DescriptorFactory.buildDescriptor(DescriptorFactory.class.getResource("serviceAssembly.xml")).getServiceAssembly();
        final Preferences prefs = createMock(Preferences.class);
        expect(prefs.get("state", State.Shutdown.name())).andReturn(State.Shutdown.name()).anyTimes();
        replay(prefs);
        final Component component = createMock(Component.class);
        replay(component);

        ComponentImpl comp = new ComponentImpl(null, null, component, prefs, false, null);
        comp.state = State.Shutdown;
        ServiceUnitImpl su = new ServiceUnitImpl(descriptor.getServiceUnits()[0], null, comp);
        ServiceAssemblyImpl sa = new ServiceAssemblyImpl(null, descriptor, Collections.singletonList(su), prefs, new AssemblyReferencesListener(), false);
        sa.state = State.Shutdown;
        
        try {
            sa.start();
            fail("Exception should have been thrown");
        } catch (JBIException e) {
            // ok
        }
    }
    
    public void testWiringOnServiceAssemblyConnections() throws Exception {
        ServiceAssemblyDesc descriptor = DescriptorFactory.buildDescriptor(DescriptorFactory.class.getResource("serviceAssembly.xml")).getServiceAssembly();
        final Preferences prefs = createMock(Preferences.class);
        expect(prefs.get("state", State.Shutdown.name())).andReturn(State.Shutdown.name());
        prefs.put("state", State.Started.name());
        prefs.flush();
        prefs.put("state", State.Stopped.name());
        prefs.flush();
        replay(prefs);

        final List<ServiceRegistration> wires = new LinkedList<ServiceRegistration>();
        ServiceAssembly sa = new ServiceAssemblyImpl(null, descriptor, new ArrayList<ServiceUnitImpl>(), prefs, new AssemblyReferencesListener(), false) {
            @Override
            protected ServiceRegistration registerWire(Wire wire) {
                ServiceRegistration registration = createMock(ServiceRegistration.class);
                wires.add(registration);
                return registration;
            }
        };
        sa.start();
        assertEquals(2, wires.size());
        
        // ServiceRegistrations should be unregistered when the SA is stopped
        for (final ServiceRegistration registration : wires) {
            registration.unregister();
            replay(registration);
        }
        sa.stop();
    }
}
