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

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.jms.ConnectionFactory;
import javax.xml.namespace.QName;
import javax.transaction.TransactionManager;

import junit.framework.TestCase;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.Service;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.XaPooledConnectionFactory;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.core.ServiceMix;
import org.apache.servicemix.nmr.core.ExchangeImpl;
import org.apache.servicemix.nmr.core.util.StringSource;
import org.apache.servicemix.jbi.runtime.impl.MessageExchangeImpl;
import org.apache.servicemix.jbi.runtime.impl.DeliveryChannelImpl;
import org.apache.servicemix.jbi.runtime.ExchangeCompletedListener;
import org.apache.servicemix.jbi.cluster.requestor.Transacted;
import org.apache.servicemix.jbi.cluster.requestor.GenericJmsRequestorPool;
import org.apache.servicemix.jbi.cluster.requestor.AbstractPollingRequestorPool;
import org.jencks.GeronimoPlatformTransactionManager;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.beans.factory.DisposableBean;

public abstract class AbstractClusterEndpointTest extends AutoFailTestSupport {

    public static final String PROXY_ENDPOINT_NAME = "proxy";
    public static final String RECEIVER_ENDPOINT_NAME = "receiver";

    protected NMR nmr1;
    protected NMR nmr2;
    protected Service broker;
    protected ConnectionFactory connectionFactory;
    protected ExchangeCompletedListener listener;
    protected TransactionManager transactionManager;
    protected TaskExecutor executor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ExchangeImpl.getConverter();
        this.executor = createTaskExecutor();
        this.transactionManager = new GeronimoPlatformTransactionManager();
        this.broker = createBroker();
        this.connectionFactory = createConnectionFactory();
        this.nmr1 = createNmr();
        this.nmr2 = createNmr();
        this.listener = new ExchangeCompletedListener();
        this.nmr1.getListenerRegistry().register(this.listener, null);
        this.nmr2.getListenerRegistry().register(this.listener, null);
    }

    protected ConnectionFactory createConnectionFactory() {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?jms.prefetchPolicy.queuePrefetch=1000");
        XaPooledConnectionFactory cnf = new XaPooledConnectionFactory(cf);
        cnf.setTransactionManager(transactionManager);
        return cnf;
    }

    @Override
    protected void tearDown() throws Exception {
        ((DisposableBean) executor).destroy();
        listener.assertExchangeCompleted();
        if (executor instanceof DisposableBean) {
            ((DisposableBean) executor).destroy();
        }
        if (connectionFactory instanceof Service) {
            ((Service) connectionFactory).stop();
        }
        if (broker != null) {
            broker.stop();
        }
        super.tearDown();
    }

    protected TaskExecutor createTaskExecutor() {
        ThreadPoolTaskExecutor exec = new CleanThreadPoolTaskExecutor();
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setQueueCapacity(0);
        exec.afterPropertiesSet();
        return exec;
    }

    protected Service createBroker() throws Exception {
        BrokerService broker = new BrokerService();
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.start();
        return broker;
    }

    protected NMR createNmr() throws Exception {
        ServiceMix nmr = new ServiceMix();
        nmr.init();
        return nmr;
    }

    protected ClusterEngine createCluster(NMR nmr, String name, Transacted transacted, boolean rollbackOnErrors) throws Exception {
        ClusterEngine cluster = new ClusterEngine();
        AbstractPollingRequestorPool pool = createPool();
        pool.setDestinationName("destination");
        pool.setConnectionFactory(connectionFactory);
        pool.setTransactionManager(transactionManager);
        pool.setTransacted(transacted);
        pool.setAutoStartup(false);
        pool.setTaskExecutor(executor);
        pool.afterPropertiesSet();
        cluster.setPool(pool);
        cluster.setName(name);
        cluster.setRollbackOnErrors(rollbackOnErrors);
        nmr.getEndpointRegistry().register(cluster,
                ServiceHelper.createMap(Endpoint.NAME, name));
        nmr.getListenerRegistry().register(cluster, null);
        return cluster;
    }

    protected AbstractPollingRequestorPool createPool() {
        return new GenericJmsRequestorPool();
    }

    protected ProxyEndpoint createProxy(NMR nmr, ClusterEngine cluster) throws Exception {
        ProxyEndpoint proxy = new ProxyEndpoint(QName.valueOf("{urn:test}receiver"));
        nmr.getEndpointRegistry().register(proxy,
                ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME,
                                        Endpoint.SERVICE_NAME, "{urn:test}proxy",
                                        Endpoint.ENDPOINT_NAME, "endpoint"));
        SimpleClusterRegistration reg = new SimpleClusterRegistration();
        reg.setEndpoint(proxy);
        reg.init();
        cluster.register(reg, null);
        return proxy;
    }

    protected ReceiverEndpoint createReceiver(NMR nmr, boolean fault, boolean error) throws Exception {
        ReceiverEndpoint receiver = new ReceiverEndpoint(fault, error);
        nmr.getEndpointRegistry().register(receiver,
                ServiceHelper.createMap(Endpoint.NAME, RECEIVER_ENDPOINT_NAME,
                                        Endpoint.SERVICE_NAME, "{urn:test}receiver",
                                        Endpoint.ENDPOINT_NAME, "endpoint"));
        return receiver;
    }

    public static class ProxyEndpoint implements Endpoint {

        private final QName service;
        private Channel channel;

        public ProxyEndpoint(QName service) {
            this.service = service;
        }

        public void setChannel(Channel channel) {
            this.channel = channel;
        }

        public void process(Exchange exchange) {
            Holder<Exchange> holder = (Holder<Exchange>) exchange.getProperty("correlated");
            Exchange correlated = holder != null ? holder.get() : null;
            if (exchange.getStatus() == Status.Error) {
                correlated.setError(exchange.getError());
                correlated.setStatus(Status.Error);
            } else if (exchange.getStatus() == Status.Done) {
                correlated.setStatus(Status.Done);
            } else if (exchange.getFault(false) != null) {
                correlated.setFault(exchange.getFault());
            } else if (exchange.getOut(false) != null) {
                correlated.setOut(exchange.getOut());
            } else if (exchange.getIn(false) != null) {
                correlated = channel.createExchange(exchange.getPattern());
                exchange.setProperty("correlated", new Holder<Exchange>(correlated));
                correlated.setProperty("correlated", new Holder<Exchange>(exchange));

                //correlated.setProperty(JbiConstants.SENDER_ENDPOINT, getService() + ":" + getEndpoint());
                correlated.setProperty(MessageExchangeImpl.SERVICE_NAME_PROP, service);
                DeliveryChannelImpl.createTarget(channel.getNMR(), correlated);
                correlated.setIn(exchange.getIn());
            } else {
                throw new IllegalStateException();
            }
            channel.send(correlated);
        }
    }

    public static class Holder<T> {

        private final T t;

        public Holder(T t) {
            this.t = t;
        }

        public T get() {
            return t;
        }
    }

    public static class ReceiverEndpoint implements Endpoint {

        private final List<Exchange> exchanges = new LinkedList<Exchange>();
        private final boolean sendFault;
        private final boolean sendError;
        private Map<String, Boolean> faultSent = new ConcurrentHashMap<String, Boolean>();
        private Map<String, Boolean> errorSent = new ConcurrentHashMap<String, Boolean>();
        private Channel channel;

        public ReceiverEndpoint(boolean sendFault, boolean sendError) {
            this.sendFault = sendFault;
            this.sendError = sendError;
        }

        public List<Exchange> getExchanges() {
            return exchanges;
        }

        public void setChannel(Channel channel) {
            this.channel = channel;
        }

        public synchronized void process(Exchange exchange) {
            synchronized (exchanges) {
                exchanges.add(exchange.copy());
                exchanges.notifyAll();
            }
            if (exchange.getStatus() == Status.Active) {
                String key = exchange.getIn().getBody(String.class);
                if (sendFault && !faultSent.containsKey(key)) {
                    exchange.getFault().setBody(new StringSource("<fault/>"));
                    channel.send(exchange);
                    faultSent.put(key, true);
                } else if (sendError && !errorSent.containsKey(key)) {
                    exchange.setError(new Exception("error"));
                    exchange.setStatus(Status.Error);
                    channel.send(exchange);
                    errorSent.put(key, true);
                } else if (exchange.getPattern() == Pattern.InOut || exchange.getPattern() == Pattern.InOptionalOut) {
                    exchange.getOut().setBody(new StringSource("<out/>"));
                    channel.send(exchange);
                } else {
                    exchange.setStatus(Status.Done);
                    channel.send(exchange);
                }
            }
        }

        public void assertExchangesReceived(int count, long timeout) {
            synchronized (exchanges) {
                long cur = System.currentTimeMillis();
                long end = cur + timeout;
                while (cur < end) {
                    try {
                        if (exchanges.size() >= count) {
                            break;
                        }
                        exchanges.wait(end - cur);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    cur = System.currentTimeMillis();
                }
                assertTrue("expected number of messages when received: " + exchanges.size(), count <= exchanges.size());
            }
        }
    }

    public static class CleanThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {
        private long shutdownTimeout = 5 * 60 * 1000;
        private boolean waitForTasksToCompleteOnShutdown = false;

        public long getShutdownTimeout() {
            return shutdownTimeout;
        }

        public void setShutdownTimeout(long shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
        }

        @Override
        public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
            this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
            super.setWaitForTasksToCompleteOnShutdown(waitForJobsToCompleteOnShutdown);
        }

        @Override
        public void shutdown() {
            super.shutdown();
            if (waitForTasksToCompleteOnShutdown) {
                try {
                    getThreadPoolExecutor().awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
