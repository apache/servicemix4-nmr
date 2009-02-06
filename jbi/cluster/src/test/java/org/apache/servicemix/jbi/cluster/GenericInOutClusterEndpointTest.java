package org.apache.servicemix.jbi.cluster;

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.jbi.cluster.requestor.Transacted;
import org.apache.camel.converter.jaxp.StringSource;

public class GenericInOutClusterEndpointTest extends AbstractClusterEndpointTest {

    private static final long TIMEOUT = 60 * 1000;

    private ClusterEndpoint cluster1;
    private ClusterEndpoint cluster2;
    private ReceiverEndpoint receiver;
    private ProxyEndpoint proxy;

    public void testInOutNoTxNoRb() throws Exception {
        createRoute(Transacted.None, false, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Active, exchange.getStatus());
        exchange.setStatus(Status.Done);
        client.send(exchange);
        client.close();
        receiver.assertExchangesReceived(2, TIMEOUT);
    }

    public void testInOutNoTxNoRbInError() throws Exception {
        createRoute(Transacted.None, false, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Error, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOutNoTxRb() throws Exception {
        createRoute(Transacted.None, true, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Active, exchange.getStatus());
        exchange.setStatus(Status.Done);
        client.send(exchange);
        client.close();
        receiver.assertExchangesReceived(2, TIMEOUT);
    }

    public void testInOutNoTxRbInError() throws Exception {
        createRoute(Transacted.None, true, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Error, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOutJmsTxNoRb() throws Exception {
        createRoute(Transacted.Jms, false, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Active, exchange.getStatus());
        exchange.setStatus(Status.Done);
        client.send(exchange);
        client.close();
        receiver.assertExchangesReceived(2, TIMEOUT);
    }

    public void testInOutJmsTxNoRbInError() throws Exception {
        createRoute(Transacted.Jms, false, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Error, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOutJmsTxRb() throws Exception {
        createRoute(Transacted.Jms, true, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Active, exchange.getStatus());
        exchange.setStatus(Status.Done);
        client.send(exchange);
        client.close();
        receiver.assertExchangesReceived(2, TIMEOUT);
    }

    public void testInOutJmsTxRbInError() throws Exception {
        createRoute(Transacted.Jms, true, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Active, exchange.getStatus());
        exchange.setStatus(Status.Done);
        client.send(exchange);
        client.close();
        receiver.assertExchangesReceived(3, TIMEOUT);
    }

    public void testInOutAckTxNoRb() throws Exception {
        createRoute(Transacted.ClientAck, false, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Active, exchange.getStatus());
        exchange.setStatus(Status.Done);
        client.send(exchange);
        client.close();
        receiver.assertExchangesReceived(2, TIMEOUT);
    }

    public void testInOutAckTxNoRbInError() throws Exception {
        createRoute(Transacted.ClientAck, false, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Error, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOutAckTxRb() throws Exception {
        createRoute(Transacted.ClientAck, true, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Active, exchange.getStatus());
        exchange.setStatus(Status.Done);
        client.send(exchange);
        client.close();
        receiver.assertExchangesReceived(2, TIMEOUT);
    }

    public void testInOutAckTxRbInError() throws Exception {
        createRoute(Transacted.ClientAck, true, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOut);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Active, exchange.getStatus());
        exchange.setStatus(Status.Done);
        client.send(exchange);
        client.close();
        receiver.assertExchangesReceived(3, TIMEOUT);
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
