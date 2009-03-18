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

import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.core.util.StringSource;
import org.apache.servicemix.jbi.cluster.requestor.Transacted;
import org.springframework.beans.factory.DisposableBean;

public class GenericInOnlyClusterEndpointTest extends AbstractClusterEndpointTest {

    private static final long TIMEOUT = 60 * 1000;

    private ClusterEngine cluster1;
    private ClusterEngine cluster2;
    private ReceiverEndpoint receiver;
    private ProxyEndpoint proxy;

    public void testInOnlyNoTxNoRb() throws Exception {
        createRoute(Transacted.None, false, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyNoTxNoRbInError() throws Exception {
        createRoute(Transacted.None, false, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Error, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyNoTxRb() throws Exception {
        createRoute(Transacted.None, true, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyNoTxRbInError() throws Exception {
        createRoute(Transacted.None, true, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyJmsTxNoRb() throws Exception {
        createRoute(Transacted.Jms, false, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyJmsTxNoRbInError() throws Exception {
        createRoute(Transacted.Jms, false, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Error, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyJmsTxRb() throws Exception {
        createRoute(Transacted.Jms, true, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyJmsTxRbInError() throws Exception {
        createRoute(Transacted.Jms, true, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(2, TIMEOUT);
    }

    public void testInOnlyXaTxNoRb() throws Exception {
        createRoute(Transacted.Xa, false, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyXaTxNoRbInError() throws Exception {
        createRoute(Transacted.Xa, false, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Error, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyXaTxRb() throws Exception {
        createRoute(Transacted.Xa, true, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyXaTxRbInError() throws Exception {
        createRoute(Transacted.Xa, true, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(2, TIMEOUT);
    }

    public void testInOnlyAckTxNoRb() throws Exception {
        createRoute(Transacted.ClientAck, false, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyAckTxNoRbInError() throws Exception {
        createRoute(Transacted.ClientAck, false, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Error, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyAckTxRb() throws Exception {
        createRoute(Transacted.ClientAck, true, false, false);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(1, TIMEOUT);
    }

    public void testInOnlyAckTxRbInError() throws Exception {
        createRoute(Transacted.ClientAck, true, false, true);

        Channel client = nmr1.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.getIn().setBody(new StringSource("<hello/>"));
        exchange.setTarget(nmr1.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, PROXY_ENDPOINT_NAME)));
        client.sendSync(exchange);
        assertEquals(Status.Done, exchange.getStatus());
        client.close();
        receiver.assertExchangesReceived(2, TIMEOUT);
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
