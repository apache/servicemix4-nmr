package org.apache.servicemix.jbi.cluster;

import javax.jms.ConnectionFactory;

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.jbi.cluster.requestor.Transacted;
import org.apache.servicemix.jbi.cluster.requestor.GenericJmsRequestorPool;
import org.apache.servicemix.jbi.cluster.requestor.ActiveMQJmsRequestorPool;
import org.apache.servicemix.jbi.cluster.requestor.AbstractPollingRequestorPool;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQInOutClusterEndpointTest extends GenericInOutClusterEndpointTest {

    public void testInOutJmsTxRbInError() throws Exception {
        super.testInOutJmsTxRbInError();
    }

    protected ConnectionFactory createConnectionFactory() {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost");
        cf.setAlwaysSessionAsync(false);
        cf.setObjectMessageSerializationDefered(true);
        cf.setCopyMessageOnSend(false);
        return cf;
    }

    protected AbstractPollingRequestorPool createPool() {
        ActiveMQJmsRequestorPool pool = new ActiveMQJmsRequestorPool();
        pool.setCacheSessions(true);
        return pool;
    }

}