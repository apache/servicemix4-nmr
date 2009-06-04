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

import java.util.Collections;

import javax.jbi.component.Component;

import junit.framework.TestCase;
import org.apache.servicemix.jbi.deployer.artifacts.AbstractLifecycleJbiArtifact.State;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceAssemblyDesc;
import org.apache.servicemix.jbi.runtime.impl.DeliveryChannelImpl;
import org.apache.servicemix.jbi.runtime.impl.EndpointImpl;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalExchange;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.core.ExchangeImpl;
import org.apache.servicemix.nmr.core.InternalEndpointWrapper;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import org.osgi.service.prefs.Preferences;

/**
 * Test cases for {@link AssemblyReferencesListener}
 */
public class AssemblyReferencesListenerTest extends TestCase {

    public void testKeepTrackOfSyncExchangesWhenDone() throws Exception {
        ServiceAssemblyImpl sa = createServiceAssembly();
        
        AssemblyReferencesListener listener = new AssemblyReferencesListener();
        listener.setAssembly(sa);
        
        InternalEndpoint endpoint = new InternalEndpointWrapper(new EndpointImpl(ServiceHelper.createMap(Endpoint.ENDPOINT_NAME, "endpoint")),
                                                                ServiceHelper.createMap(Endpoint.ENDPOINT_NAME, "internal-endpoint"));
        listener.endpointRegistered(endpoint);
        listener.setAssembly(null);
        
        InternalExchange exchange = new ExchangeImpl(Pattern.InOnly);
        exchange.setSource(endpoint);
        exchange.setProperty(DeliveryChannelImpl.SEND_SYNC, Boolean.TRUE);
        listener.exchangeSent(exchange);
        
        assertEquals(1, listener.getPending(sa).size());
        
        exchange.setStatus(Status.Done);
        listener.exchangeDelivered(exchange);
        
        assertEquals(0, listener.getPending(sa).size());
    }
    
    public void testKeepTrackOfSyncExchangesWhenFailed() throws Exception {
        ServiceAssemblyImpl sa = createServiceAssembly();
        
        AssemblyReferencesListener listener = new AssemblyReferencesListener();
        listener.setAssembly(sa);
        
        InternalEndpoint endpoint = new InternalEndpointWrapper(new EndpointImpl(ServiceHelper.createMap(Endpoint.ENDPOINT_NAME, "endpoint")),
                                                                ServiceHelper.createMap(Endpoint.ENDPOINT_NAME, "internal-endpoint"));
        listener.endpointRegistered(endpoint);
        listener.setAssembly(null);
        
        InternalExchange exchange = new ExchangeImpl(Pattern.InOnly);
        exchange.setSource(endpoint);
        exchange.setProperty(DeliveryChannelImpl.SEND_SYNC, Boolean.TRUE);
        listener.exchangeSent(exchange);
        
        assertEquals(1, listener.getPending(sa).size());
        
        exchange.setStatus(Status.Error);
        listener.exchangeFailed(exchange);
        
        assertEquals(0, listener.getPending(sa).size());
    }
    
    public void testCancelPendingExchanges() throws Exception {
        ServiceAssemblyImpl sa = createServiceAssembly();
        
        AssemblyReferencesListener listener = new AssemblyReferencesListener();
        listener.setAssembly(sa);
        
        InternalEndpoint endpoint = new InternalEndpointWrapper(new EndpointImpl(ServiceHelper.createMap(Endpoint.ENDPOINT_NAME, "endpoint")),
                                                                ServiceHelper.createMap(Endpoint.ENDPOINT_NAME, "internal-endpoint"));
        listener.endpointRegistered(endpoint);
        listener.setAssembly(null);
        
        InternalExchange exchange = new ExchangeImpl(Pattern.InOnly);
        exchange.setSource(endpoint);
        exchange.setProperty(DeliveryChannelImpl.SEND_SYNC, Boolean.TRUE);
        exchange.getConsumerLock(true);
        listener.exchangeSent(exchange);
        
        assertEquals(1, listener.getPending(sa).size());
        
        listener.cancelPendingSyncExchanges(sa);
        assertEquals(Status.Error, exchange.getStatus());
    }

    public void testNoExceptionsOnUnknownExchange() throws Exception {
        AssemblyReferencesListener listener = new AssemblyReferencesListener();
        InternalEndpoint endpoint = new InternalEndpointWrapper(new EndpointImpl(ServiceHelper.createMap(Endpoint.ENDPOINT_NAME, "endpoint")),
                                                                ServiceHelper.createMap(Endpoint.ENDPOINT_NAME, "internal-endpoint"));
        
        InternalExchange exchange = new ExchangeImpl(Pattern.InOnly);
        exchange.setSource(endpoint);
        exchange.setProperty(DeliveryChannelImpl.SEND_SYNC, Boolean.TRUE);
        exchange.getConsumerLock(true);
        
        // this should not throw an exception
        listener.exchangeSent(exchange);
    }

    private ServiceAssemblyImpl createServiceAssembly() {
        ServiceAssemblyDesc descriptor = DescriptorFactory.buildDescriptor(DescriptorFactory.class.getResource("serviceAssembly.xml")).getServiceAssembly();
        final Preferences prefs = createMock(Preferences.class);
        expect(prefs.get("state", State.Shutdown.name())).andReturn(State.Shutdown.name()).anyTimes();
        replay(prefs);
        final Component component = createMock(Component.class);
        replay(component);

        ComponentImpl comp = new ComponentImpl(null, null, component, prefs, false, null);
        ServiceUnitImpl su = new ServiceUnitImpl(descriptor.getServiceUnits()[0], null, comp);
        ServiceAssemblyImpl sa = new ServiceAssemblyImpl(null, descriptor, Collections.singletonList(su), prefs, new AssemblyReferencesListener(), false);
        return sa;
    }
}
