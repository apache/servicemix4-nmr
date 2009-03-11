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

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Mar 10, 2009
 * Time: 4:18:36 PM
 * To change this template use File | Settings | File Templates.
 */
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
