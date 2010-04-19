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
package org.apache.servicemix.nmr.audit;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;
import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.core.ServiceMix;
import org.apache.servicemix.nmr.core.util.StringSource;

public abstract class AbstractAuditorTest extends TestCase {

    public static final String RECEIVER_ENDPOINT_NAME = "receiver";

    protected NMR nmr;

    @Override
    protected void setUp() throws Exception {
        ServiceMix smx = new ServiceMix();
        smx.init();
        nmr = smx;
    }

    protected void sendExchange(Object content) {
        Channel client = nmr.createChannel();
        Exchange exchange = client.createExchange(Pattern.InOnly);
        exchange.setTarget(client.getNMR().getEndpointRegistry().lookup(
                ServiceHelper.createMap(Endpoint.NAME, RECEIVER_ENDPOINT_NAME)));
        exchange.setProperty("prop1", "value1");
        exchange.getIn().setBody(content);
        exchange.getIn().setHeader("prop1", "value2");
        exchange.getIn().setHeader("prop2", "value3");
        client.sendSync(exchange);

    }

    protected ReceiverEndpoint createReceiver(NMR nmr, boolean fault, boolean error) throws Exception {
        ReceiverEndpoint receiver = new ReceiverEndpoint(fault, error);
        nmr.getEndpointRegistry().register(receiver,
                ServiceHelper.createMap(Endpoint.NAME, RECEIVER_ENDPOINT_NAME));
        return receiver;
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
                if (sendFault && key != null && !faultSent.containsKey(key)) {
                    exchange.getFault().setBody(new StringSource("<fault/>"));
                    channel.send(exchange);
                    faultSent.put(key, true);
                } else if (sendError && key != null && !errorSent.containsKey(key)) {
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

}
