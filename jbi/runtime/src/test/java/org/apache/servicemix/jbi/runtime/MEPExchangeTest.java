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
package org.apache.servicemix.jbi.runtime;

import java.util.HashMap;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.namespace.QName;

import junit.framework.TestCase;
import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.jbi.ExchangeTimeoutException;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.runtime.impl.ComponentRegistryImpl;
import org.apache.servicemix.nmr.core.ServiceMix;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.api.Endpoint;

public class MEPExchangeTest extends TestCase {

    public static final String PAYLOAD = "<payload/>";

    public static final String RESPONSE = "<response/>";

    private TestComponent provider;

    private TestComponent consumer;

    private ExchangeCompletedListener listener;

    public static class TestComponent extends ComponentSupport {
        public TestComponent(QName service, String endpoint) {
            super(service, endpoint);
        }

        public DeliveryChannel getChannel() throws MessagingException {
            return getContext().getDeliveryChannel();
        }
    }

    public void setUp() throws Exception {
        ServiceMix smx = new ServiceMix();
        smx.init();
        ComponentRegistryImpl reg = new ComponentRegistryImpl();
        reg.setNmr(smx);

        listener = new ExchangeCompletedListener();
        smx.getListenerRegistry().register(listener, new HashMap<String, Object>());
        // Create components
        provider = new TestComponent(new QName("provider"), "endpoint");
        consumer = new TestComponent(new QName("consumer"), "endpoint");
        // Register components
        reg.register(new SimpleComponentWrapper(provider), ServiceHelper.createMap(ComponentRegistry.NAME, "provider"));
        reg.register(new SimpleComponentWrapper(consumer), ServiceHelper.createMap(ComponentRegistry.NAME, "consumer"));
    }

    public void tearDown() throws Exception {
        if (listener != null) {
            listener.assertExchangeCompleted();
        }
    }

    public void testInOnly() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOnly mec = mef.createInOnlyExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        assertEquals(Role.CONSUMER, mec.getRole());
        try {
            mec.setMessage(null, "in");
            fail("Message is null");
        } catch (Exception e) {
            // ok
        }
        try {
            mec.setMessage(mec.createMessage(), "in");
            fail("Message already set");
        } catch (Exception e) {
            // ok
        }
        try {
            mec.setMessage(mec.createMessage(), "out");
            fail("Out not supported");
        } catch (Exception e) {
            // ok
        }
        try {
            mec.setMessage(mec.createFault(), "fault");
            fail("Fault not supported");
        } catch (Exception e) {
            // ok
        }
        consumer.getChannel().send(mec);
        // Provider side
        InOnly mep = (InOnly) provider.getChannel().accept(1000L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        assertEquals(Role.PROVIDER, mep.getRole());
        mep.setStatus(ExchangeStatus.DONE);
        provider.getChannel().send(mep);
        // Consumer side
        assertSame(mec, consumer.getChannel().accept(1000L));
        assertEquals(ExchangeStatus.DONE, mec.getStatus());
        assertEquals(Role.CONSUMER, mec.getRole());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOnlyWithError() throws Exception {
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOnly mec = mef.createInOnlyExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        assertEquals(Role.CONSUMER, mec.getRole());
        consumer.getChannel().send(mec);
        // Provider side
        InOnly mep = (InOnly) provider.getChannel().accept(1000L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        assertEquals(Role.PROVIDER, mep.getRole());
        mep.setError(new Exception());
        provider.getChannel().send(mep);
        // Consumer side
        assertSame(mec, consumer.getChannel().accept(1000L));
        assertEquals(ExchangeStatus.ERROR, mec.getStatus());
        assertEquals(Role.CONSUMER, mec.getRole());
        // Check we can not send the exchange anymore
        try {
            mec.setStatus(ExchangeStatus.DONE);
            consumer.getChannel().send(mec);
            fail("Exchange status is ERROR");
        } catch (Exception e) {
            // ok
        }
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOnlySync() throws Exception {
        // Create thread to answer
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Provider side
                    InOnly mep = (InOnly) provider.getChannel().accept(10000L);
                    assertNotNull(mep);
                    assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
                    assertEquals(Boolean.TRUE, mep.getProperty(JbiConstants.SEND_SYNC));
                    mep.setStatus(ExchangeStatus.DONE);
                    provider.getChannel().send(mep);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }).start();
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOnly mec = mef.createInOnlyExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        boolean result = consumer.getChannel().sendSync(mec, 10000L);
        assertTrue(result);
        assertEquals(ExchangeStatus.DONE, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOnlySyncWithTimeoutBeforeAccept() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOnly mec = mef.createInOnlyExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        boolean result = consumer.getChannel().sendSync(mec, 100L);
        assertFalse(result);
        assertEquals(ExchangeStatus.ERROR, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOnlySyncWithTimeoutAfterAccept() throws Exception {
        // Create thread to answer
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    // Provider side
                    InOnly mep = (InOnly) provider.getChannel().accept(10000L);
                    assertNotNull(mep);
                    assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
                    assertEquals(Boolean.TRUE, mep.getProperty(JbiConstants.SEND_SYNC));
                    Thread.sleep(100L);
                    mep.setStatus(ExchangeStatus.DONE);
                    provider.getChannel().send(mep);
                } catch (ExchangeTimeoutException e) {
                    // ok
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }
        });
        t.start();
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOnly mec = mef.createInOnlyExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        boolean result = consumer.getChannel().sendSync(mec, 50L);

        assertFalse(result);
        assertEquals(ExchangeStatus.ERROR, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        t.join();
    }

    public void testInOut() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOut mec = mef.createInOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOut mep = (InOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        m = mep.createMessage();
        m.setContent(new StringSource(RESPONSE));
        mep.setOutMessage(m);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        mec.setStatus(ExchangeStatus.DONE);
        consumer.getChannel().send(mec);
        // Provider site
        assertSame(mep, provider.getChannel().accept(100L));
        assertEquals(ExchangeStatus.DONE, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOutSync() throws Exception {
        // Create thread to answer
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Provider side
                    InOut mep = (InOut) provider.getChannel().accept(10000L);
                    assertNotNull(mep);
                    assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
                    NormalizedMessage m = mep.createMessage();
                    m.setContent(new StringSource(RESPONSE));
                    mep.setOutMessage(m);
                    provider.getChannel().send(mep);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }).start();
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOut mec = mef.createInOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().sendSync(mec, 10000L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        mec.setStatus(ExchangeStatus.DONE);
        consumer.getChannel().send(mec);
        // Provider site
        assertNotNull(provider.getChannel().accept(100L));
        assertEquals(ExchangeStatus.DONE, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOutSyncSync() throws Exception {
        // Create thread to answer
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    // Provider side
                    InOut mep = (InOut) provider.getChannel().accept(10000L);
                    assertNotNull(mep);
                    assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
                    NormalizedMessage m = mep.createMessage();
                    m.setContent(new StringSource(RESPONSE));
                    mep.setOutMessage(m);
                    provider.getChannel().sendSync(mep);
                    assertEquals(ExchangeStatus.DONE, mep.getStatus());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }
        });
        t.start();
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOut mec = mef.createInOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().sendSync(mec, 10000L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        mec.setStatus(ExchangeStatus.DONE);
        consumer.getChannel().send(mec);
        // Wait until other thread end
        t.join(100L);
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOutWithFault() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOut mec = mef.createInOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOut mep = (InOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        Fault f = mep.createFault();
        f.setContent(new StringSource(RESPONSE));
        mep.setFault(f);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        assertNotNull(mec.getFault());
        mec.setStatus(ExchangeStatus.DONE);
        consumer.getChannel().send(mec);
        // Provider site
        assertSame(mep, provider.getChannel().accept(100L));
        assertEquals(ExchangeStatus.DONE, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOutWithFaultAndError() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOut mec = mef.createInOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOut mep = (InOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        Fault f = mep.createFault();
        f.setContent(new StringSource(RESPONSE));
        mep.setFault(f);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        assertNotNull(mec.getFault());
        mec.setStatus(ExchangeStatus.ERROR);
        consumer.getChannel().send(mec);
        // Provider site
        assertSame(mep, provider.getChannel().accept(100L));
        assertEquals(ExchangeStatus.ERROR, mec.getStatus());
        try {
            consumer.getChannel().send(mec);
            fail("Exchange status is ERROR");
        } catch (Exception e) {
            // ok
        }
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOutWithError1() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOut mec = mef.createInOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOut mep = (InOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        m = mep.createMessage();
        m.setContent(new StringSource(RESPONSE));
        mep.setStatus(ExchangeStatus.ERROR);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ERROR, mec.getStatus());
        try {
            mec.setStatus(ExchangeStatus.DONE);
            consumer.getChannel().send(mec);
            fail("Exchange status is ERROR");
        } catch (Exception e) {
            // ok
        }
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOutWithError2() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOut mec = mef.createInOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOut mep = (InOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        m = mep.createMessage();
        m.setContent(new StringSource(RESPONSE));
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        mec.setStatus(ExchangeStatus.ERROR);
        consumer.getChannel().send(mec);
        // Provider site
        assertSame(mep, provider.getChannel().accept(100L));
        assertEquals(ExchangeStatus.ERROR, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOptOutWithRep() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOptionalOut mec = mef.createInOptionalOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOptionalOut mep = (InOptionalOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        m = mep.createMessage();
        m.setContent(new StringSource(RESPONSE));
        mep.setOutMessage(m);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOptionalOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        mec.setStatus(ExchangeStatus.DONE);
        consumer.getChannel().send(mec);
        // Provider site
        mep = (InOptionalOut) provider.getChannel().accept(100L);
        assertEquals(ExchangeStatus.DONE, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOptOutWithoutRep() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOptionalOut mec = mef.createInOptionalOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOptionalOut mep = (InOptionalOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        mep.setStatus(ExchangeStatus.DONE);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOptionalOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.DONE, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOptOutWithProviderFault() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOptionalOut mec = mef.createInOptionalOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOptionalOut mep = (InOptionalOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        mep.setFault(mep.createFault());
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOptionalOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        assertNotNull(mec.getFault());
        mec.setStatus(ExchangeStatus.DONE);
        consumer.getChannel().send(mec);
        // Provider site
        mep = (InOptionalOut) provider.getChannel().accept(100L);
        assertEquals(ExchangeStatus.DONE, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOptOutWithProviderError() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOptionalOut mec = mef.createInOptionalOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOptionalOut mep = (InOptionalOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        mep.setStatus(ExchangeStatus.ERROR);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOptionalOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ERROR, mec.getStatus());
        try {
            mec.setStatus(ExchangeStatus.DONE);
            consumer.getChannel().send(mec);
            fail("Exchange status is ERROR");
        } catch (Exception e) {
            // ok
        }
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOptOutWithRepAndConsumerFault() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOptionalOut mec = mef.createInOptionalOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOptionalOut mep = (InOptionalOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        m = mep.createMessage();
        m.setContent(new StringSource(RESPONSE));
        mep.setOutMessage(m);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOptionalOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        mec.setFault(mec.createFault());
        consumer.getChannel().send(mec);
        // Provider site
        mep = (InOptionalOut) provider.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        assertNotNull(mep.getFault());
        mep.setStatus(ExchangeStatus.DONE);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOptionalOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.DONE, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOptOutWithRepAndConsumerError() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOptionalOut mec = mef.createInOptionalOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOptionalOut mep = (InOptionalOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        m = mep.createMessage();
        m.setContent(new StringSource(RESPONSE));
        mep.setOutMessage(m);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOptionalOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        mec.setStatus(ExchangeStatus.ERROR);
        consumer.getChannel().send(mec);
        // Provider site
        mep = (InOptionalOut) provider.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ERROR, mep.getStatus());
        try {
            mep.setStatus(ExchangeStatus.DONE);
            provider.getChannel().send(mep);
            fail("Exchange status is ERROR");
        } catch (Exception e) {
            // ok
        }
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testInOptOutWithRepFaultAndError() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        InOptionalOut mec = mef.createInOptionalOutExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        InOptionalOut mep = (InOptionalOut) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        m = mep.createMessage();
        m.setContent(new StringSource(RESPONSE));
        mep.setOutMessage(m);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOptionalOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        mec.setFault(mec.createFault());
        consumer.getChannel().send(mec);
        // Provider site
        assertSame(mep, provider.getChannel().accept(100L));
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        assertNotNull(mep.getFault());
        mep.setStatus(ExchangeStatus.ERROR);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (InOptionalOut) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ERROR, mec.getStatus());
        try {
            mec.setStatus(ExchangeStatus.DONE);
            consumer.getChannel().send(mec);
            fail("Exchange status is ERROR");
        } catch (Exception e) {
            // ok
        }
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testRobustInOnly() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        RobustInOnly mec = mef.createRobustInOnlyExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        RobustInOnly mep = (RobustInOnly) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        mep.setStatus(ExchangeStatus.DONE);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (RobustInOnly) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.DONE, mec.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testRobustInOnlyWithFault() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        RobustInOnly mec = mef.createRobustInOnlyExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        RobustInOnly mep = (RobustInOnly) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        mep.setFault(mep.createFault());
        provider.getChannel().send(mep);
        // Consumer side
        mec = (RobustInOnly) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        assertNotNull(mec.getFault());
        mec.setStatus(ExchangeStatus.DONE);
        provider.getChannel().send(mec);
        // Provider site
        mep = (RobustInOnly) provider.getChannel().accept(100L);
        assertEquals(ExchangeStatus.DONE, mep.getStatus());
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testRobustInOnlyWithError() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        RobustInOnly mec = mef.createRobustInOnlyExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        RobustInOnly mep = (RobustInOnly) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        mep.setStatus(ExchangeStatus.ERROR);
        provider.getChannel().send(mep);
        // Consumer side
        mec = (RobustInOnly) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ERROR, mec.getStatus());
        try {
            mec.setStatus(ExchangeStatus.DONE);
            provider.getChannel().send(mec);
            fail("Exchange status is ERROR");
        } catch (Exception e) {
            // ok
        }
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

    public void testRobustInOnlyWithFaultAndError() throws Exception {
        // Send message exchange
        MessageExchangeFactory mef = consumer.getChannel().createExchangeFactoryForService(new QName("provider"));
        RobustInOnly mec = mef.createRobustInOnlyExchange();
        NormalizedMessage m = mec.createMessage();
        m.setContent(new StringSource(PAYLOAD));
        mec.setInMessage(m);
        consumer.getChannel().send(mec);
        // Provider side
        RobustInOnly mep = (RobustInOnly) provider.getChannel().accept(100L);
        assertNotNull(mep);
        assertEquals(ExchangeStatus.ACTIVE, mep.getStatus());
        mep.setFault(mep.createFault());
        provider.getChannel().send(mep);
        // Consumer side
        mec = (RobustInOnly) consumer.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ACTIVE, mec.getStatus());
        assertNotNull(mec.getFault());
        mec.setError(new Exception());
        provider.getChannel().send(mec);
        // Provider site
        mep = (RobustInOnly) provider.getChannel().accept(100L);
        assertEquals(ExchangeStatus.ERROR, mep.getStatus());
        try {
            mep.setStatus(ExchangeStatus.DONE);
            provider.getChannel().send(mep);
            fail("Exchange status is ERROR");
        } catch (Exception e) {
            // ok
        }
        // Nothing left
        assertNull(consumer.getChannel().accept(100L)); // receive in
        assertNull(provider.getChannel().accept(100L)); // receive in
    }

}
