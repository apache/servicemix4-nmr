package org.apache.servicemix.jbi.cluster;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.ConnectionFactory;

import org.apache.activemq.Service;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.XaPooledConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.servicemix.jbi.cluster.requestor.Transacted;
import org.apache.servicemix.jbi.cluster.requestor.AbstractPollingRequestorPool;
import org.apache.servicemix.jbi.cluster.requestor.ActiveMQJmsRequestorPool;
import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.core.util.StringSource;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Feb 3, 2009
 * Time: 5:18:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReconnectTest extends AbstractClusterEndpointTest {

    private static final long TIMEOUT = 10 * 60 * 1000;

    private ClusterEndpoint cluster1;
    private ClusterEndpoint cluster2;
    private ReceiverEndpoint receiver;
    private ProxyEndpoint proxy;

    public void testLoadInOnly() throws Exception {
        createRoute(Transacted.Jms, true, false, false);

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

        cluster2.pause();

        t0 = System.currentTimeMillis();
        lock.writeLock().unlock();

        latch.await();

        Thread.sleep(1000);

        cluster2.resume();

        //Thread.sleep(100);

        broker.stop();
        Thread.sleep(1000);
        broker = createBroker();

        latch.await();

        receiver.assertExchangesReceived(nbThreads * nbExchanges, TIMEOUT);
        //Thread.sleep(500);
        //receiver.assertExchangesReceived(nbThreads * nbExchanges, TIMEOUT);

        t1 = System.currentTimeMillis();

        System.err.println("Elapsed time: " + (t1 - t0) + " ms");
        System.err.println("Throuput: " + (nbThreads * nbExchanges * 1000 / (t1 - t0)) + " messages/sec");
    }

    protected ConnectionFactory createConnectionFactory() {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616");
        return cf;
//        XaPooledConnectionFactory cnf = new XaPooledConnectionFactory(cf);
//        cnf.setTransactionManager(transactionManager);
//        return cnf;
    }

    protected Service createBroker() throws Exception {
        BrokerService broker = new BrokerService();
        broker.setPersistent(true);
        broker.setUseJmx(false);
        broker.addConnector("tcp://localhost:61616");
        broker.start();
        return broker;
    }

    protected void createRoute(Transacted transacted,
                               boolean rollbackOnErrors,
                               boolean sendFault,
                               boolean sendError) throws Exception {
        cluster1 = createCluster(nmr1, "nmr1", transacted, rollbackOnErrors);
        cluster2 = createCluster(nmr2, "nmr2", transacted, !rollbackOnErrors); // the rollbackOnErrors flag should not be used on the JMS consumer side
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
        cluster1.destroy();
        cluster2.destroy();
        super.tearDown();
    }
}
