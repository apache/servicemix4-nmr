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

import static org.easymock.EasyMock.*;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.ServiceMixException;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.api.event.ExchangeListener;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class ChannelImplTest extends TestCase {

    private NMR nmr;
    private MyEndpoint ep1;
    private MyEndpoint ep2;

    public void setUp() {
        ServiceMix smx = new ServiceMix();
        smx.init();
        nmr = smx;
        ep1 = new MyEndpoint();
        nmr.getEndpointRegistry().register(ep1, ServiceHelper.createMap(Endpoint.NAME, "ep1"));
        ep2 = new MyEndpoint();
        nmr.getEndpointRegistry().register(ep2, ServiceHelper.createMap(Endpoint.NAME, "ep2"));
    }

    public void testDispatchAsync() throws Exception {

        IMocksControl control = EasyMock.createControl();
        ExchangeListener listener = control.createMock(ExchangeListener.class);
        control.makeThreadSafe(true);
        nmr.getListenerRegistry().register(listener, null);

        final Exchange e = ep1.channel.createExchange(Pattern.InOnly);

        listener.exchangeSent(same(e));
        listener.exchangeDelivered(same(e));
        replay(listener);

        e.setTarget(ep1.channel.getNMR().getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, "ep2")));
        ep1.channel.send(e);
        Thread.sleep(1000);
        verify(listener);

        reset(listener);
        control.makeThreadSafe(true);
        listener.exchangeSent(same(e));
        listener.exchangeDelivered(same(e));
        replay(listener);

        synchronized (ep1) {
            ep2.done();
            ep1.wait();
        }
        assertNotNull(ep1.exchange);

        verify(listener);
    }

    public void testDispatchSync() throws Exception {

        IMocksControl control = EasyMock.createControl();
        ExchangeListener listener = control.createMock(ExchangeListener.class);
        control.makeThreadSafe(true);
        nmr.getListenerRegistry().register(listener, null);

        final Exchange e = ep1.channel.createExchange(Pattern.InOnly);
        final CountDownLatch latch = new CountDownLatch(1);

        listener.exchangeSent(same(e));
        listener.exchangeDelivered(same(e));
        replay(listener);

        synchronized (ep2) {
            new Thread() {
                public void run() {
                    e.setTarget(ep1.channel.getNMR().getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, "ep2")));
                    ep1.channel.sendSync(e);
                    latch.countDown();
                }
            }.start();
            ep2.wait();
        }

        verify(listener);

        reset(listener);
        control.makeThreadSafe(true);
        listener.exchangeSent(same(e));
        listener.exchangeDelivered(same(e));
        replay(listener);

        ep2.done();
        latch.await();

        verify(listener);
    }

    public void testDispatchFailure() throws Exception {
        IMocksControl control = EasyMock.createControl();
        ExchangeListener listener = control.createMock(ExchangeListener.class);
        control.makeThreadSafe(true);
        nmr.getListenerRegistry().register(listener, null);

        Channel channel = nmr.createChannel();
        Exchange e = channel.createExchange(Pattern.InOnly);
        e.setTarget(nmr.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, "zz")));

        listener.exchangeSent(same(e));
        listener.exchangeFailed(same(e));

        replay(listener);

        try {
            channel.send(e);
            fail("Exepected an exception to be thrown");
        } catch (ServiceMixException t) {
            // ok
        }

        verify(listener);
    }

    public void testProcessingFailure() throws Exception {
        IMocksControl control = EasyMock.createControl();
        ExchangeListener listener = control.createMock(ExchangeListener.class);
        control.makeThreadSafe(true);
        nmr.getListenerRegistry().register(listener, null);

        final Exchange e = ep1.channel.createExchange(Pattern.InOnly);

        listener.exchangeSent(same(e));
        listener.exchangeDelivered(same(e));
        listener.exchangeSent(same(e));
        listener.exchangeDelivered(same(e));
        replay(listener);

        ep2.throwException = true;
        e.setTarget(ep1.channel.getNMR().getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, "ep2")));
        ep1.channel.sendSync(e);

        verify(listener);
        assertNotNull(ep2.exchange);
        assertEquals(Status.Error, e.getStatus());
    }
    
    public void testChangeThreadNameForSyncExchange() throws Exception {
        final BlockingEndpoint blocking = new BlockingEndpoint();
        final CountDownLatch sent = new CountDownLatch(1);
        final Map<String, Object> props = ServiceHelper.createMap(Endpoint.NAME, "blocking");
        nmr.getEndpointRegistry().register(blocking, props);
        nmr.getListenerRegistry().register(new ExchangeListener() {

            public void exchangeDelivered(Exchange exchange) {}

            public void exchangeFailed(Exchange exchange) {}

            public void exchangeSent(Exchange exchange) {
                sent.countDown();                
            }
            
        }, null);
        
        
        final CountDownLatch done = new CountDownLatch(1);
        final Channel channel = nmr.createChannel();
        final Exchange exchange = channel.createExchange(Pattern.InOnly);
        exchange.setTarget(nmr.getEndpointRegistry().lookup(props));
        
        Thread thread = new Thread(new Runnable() {
            public void run() {
                channel.sendSync(exchange);
                done.countDown();
            }
        });
        thread.start();
        
        //let's wait a sec for the exchange to be sent
        sent.await(1, TimeUnit.SECONDS);
        assertNotNull(exchange);
        assertNotNull("There should be a thread waiting for the exchange", 
                      findThread(exchange.getId()));
        
        blocking.lock.release();
        //let's wait another sec for the exchange to be done
        done.await(1, TimeUnit.SECONDS);
        assertNull("There shouldn't be any thread waiting for the exchange",
                   findThread(exchange.getId()));
    }

    private Object findThread(String id) {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        ThreadInfo[]  threadInfos = threads.getThreadInfo(threads.getAllThreadIds(), 0);

        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo.getThreadName().contains(id)) {
                return threadInfo;
            }
        }

        return null;
    }

    protected static class MyEndpoint implements Endpoint {
        private Channel channel;
        private Exchange exchange;
        private boolean throwException;

        public void setChannel(Channel channel) {
            this.channel = channel;
        }

        public synchronized void process(Exchange exchange) {
            this.exchange = exchange;
            this.notifyAll();
            if (throwException) {
                throw new RuntimeException("Error");
            }
        }

        public synchronized void done() {
            exchange.setStatus(Status.Done);
            channel.send(exchange);
        }

    }
    
    private static class BlockingEndpoint implements Endpoint {

        private Semaphore lock = new Semaphore(0);
        private Channel channel;

        public void process(Exchange exchange) {
            //let's make the endpoint block until we release it
            try {
                lock.acquire();
            } catch (InterruptedException e) {
                //nothing to do here
            }
            
            exchange.setStatus(Status.Done);
            channel.send(exchange);
        }

        public void setChannel(Channel channel) {
            this.channel = channel;
        }            
    }
}
