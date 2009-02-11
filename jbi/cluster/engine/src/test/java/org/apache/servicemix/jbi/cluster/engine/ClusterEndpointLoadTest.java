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
package org.apache.servicemix.jbi.cluster.engine;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.core.util.StringSource;
import org.apache.servicemix.jbi.cluster.requestor.Transacted;
import org.springframework.beans.factory.DisposableBean;

public class ClusterEndpointLoadTest extends AbstractClusterEndpointTest {

    private static final long TIMEOUT = 10 * 60 * 1000;

    private ClusterEngine cluster1;
    private ClusterEngine cluster2;
    private ReceiverEndpoint receiver;
    private ProxyEndpoint proxy;

    public void testLoadInOnly() throws Exception {
        createRoute(Transacted.Jms, true, false, true);

        final int nbThreads = 10;
        final int nbExchanges = 10;
        final ReadWriteLock lock = new ReentrantReadWriteLock();
        final CountDownLatch latch = new CountDownLatch(nbThreads);
        final AtomicInteger id = new AtomicInteger();
        lock.writeLock().lock();
        for (int i = 0; i < nbThreads; i++) {
            new Thread() {
                public void run() {
                    Channel client = null;
                    try {
                        client = nmr1.createChannel();
                        lock.readLock().lock();
                        for (int i = 0; i < nbExchanges; i++) {
                            Exchange exchange = client.createExchange(Pattern.InOnly);
                            exchange.getIn().setBody(new StringSource("<hello id='" + id.getAndIncrement() + "'/>"));
                            exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
                            client.sendSync(exchange);
                            assertEquals(Status.Done, exchange.getStatus());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.readLock().unlock();
                        latch.countDown();
                        if (client != null) {
                            client.close();
                        }
                    }
                }
            }.start();
        }

        long t0, t1;

        t0 = System.currentTimeMillis();
        lock.writeLock().unlock();
       
        latch.await();

        receiver.assertExchangesReceived(nbThreads * nbExchanges, TIMEOUT);

        t1 = System.currentTimeMillis();

        System.err.println("Elapsed time: " + (t1 - t0) + " ms");
        System.err.println("Throuput: " + (nbThreads * nbExchanges * 1000 / (t1 - t0)) + " messages/sec");
    }

    public void testLoadInOut() throws Exception {
        createRoute(Transacted.Jms, true, false, false);

        final int nbThreads = 10;
        final int nbExchanges = 10;
        final ReadWriteLock lock = new ReentrantReadWriteLock();
        final CountDownLatch latch = new CountDownLatch(nbThreads);
        lock.writeLock().lock();
        for (int i = 0; i < nbThreads; i++) {
            new Thread() {
                public void run() {
                    Channel client = null;
                    try {
                        client = nmr1.createChannel();
                        lock.readLock().lock();
                        for (int i = 0; i < nbExchanges; i++) {
                            Exchange exchange = client.createExchange(Pattern.InOut);
                            exchange.getIn().setBody(new StringSource("<hello/>"));
                            exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
                            client.sendSync(exchange);
                            assertEquals(Status.Active, exchange.getStatus());
                            exchange.setStatus(Status.Done);
                            client.send(exchange);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.readLock().unlock();
                        latch.countDown();
                        if (client != null) {
                            client.close();
                        }
                    }
                }
            }.start();
        }

        cluster2.pause();
        lock.writeLock().unlock();
        Thread.sleep(0000);
        cluster2.resume();

        latch.await();

        receiver.assertExchangesReceived(nbThreads * nbExchanges * 2, TIMEOUT);
    }

    protected void createRoute(Transacted transacted,
                               boolean rollbackOnErrors,
                               boolean sendFault,
                               boolean sendError) throws Exception {
        cluster1 = createCluster(nmr1, "nmr1", transacted, rollbackOnErrors);
        cluster2 = createCluster(nmr2, "nmr2", transacted, rollbackOnErrors);
        receiver = createReceiver(nmr2, sendFault, sendError);
        proxy = createProxy(nmr1, cluster1);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        listener.assertExchangeCompleted();
        ((DisposableBean) cluster1.getPool()).destroy();
        cluster1.destroy();
        ((DisposableBean) cluster2.getPool()).destroy();
        cluster2.destroy();
        nmr1.getEndpointRegistry().unregister(cluster1, null);
        nmr2.getEndpointRegistry().unregister(cluster2, null);
        nmr1.getEndpointRegistry().unregister(proxy, null);
        nmr2.getEndpointRegistry().unregister(receiver, null);
        super.tearDown();
    }

}
