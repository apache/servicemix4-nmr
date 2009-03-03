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

    protected AbstractPollingRequestorPool createPool() {
        ActiveMQJmsRequestorPool pool = new ActiveMQJmsRequestorPool();
        pool.setCacheSessions(true);
        return pool;
    }

}