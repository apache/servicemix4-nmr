package org.apache.servicemix.jbi.cluster;

import org.apache.servicemix.jbi.cluster.requestor.AbstractPollingRequestorPool;
import org.apache.servicemix.jbi.cluster.requestor.ActiveMQJmsRequestorPool;

public class ActiveMQReconnectTest extends ReconnectTest {

    protected AbstractPollingRequestorPool createPool() {
        ActiveMQJmsRequestorPool pool = new ActiveMQJmsRequestorPool();
        pool.setCacheSessions(true);
        return pool;
    }

}
