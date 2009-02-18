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
package org.apache.servicemix.nmr.examples.nmr.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class Client implements InitializingBean, DisposableBean {

    private static final transient Log LOG = LogFactory.getLog(Client.class);
    private NMR nmr;

    private SendRequestThread sendRequestThread;
    
    public void afterPropertiesSet() throws Exception {
        sendRequestThread = new SendRequestThread();
        sendRequestThread.setRun(true);
        sendRequestThread.start();
        
    }

    public void destroy() throws Exception {
        sendRequestThread.setRun(false);
    }


    public void setNmr(NMR nmr) {
        this.nmr = nmr;
    }

    public NMR getNmr() {
        return nmr;
    }
    
    class SendRequestThread extends Thread {
        private boolean run;
        public void run() {
            while (run) {
                try {
                    Thread.sleep(5000);
                    if (run && nmr != null) {
		        Channel client = nmr.createChannel();
                	Exchange e = client.createExchange(Pattern.InOut);
	                e.setTarget(nmr.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, "EchoEndpoint")));
        	        e.getIn().setBody("Hello");
                	client.sendSync(e);
	                LOG.info("Response from Endpoint " + e.getOut().getBody());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.error(e.getMessage());
                }
            }
        }
        
        public void setRun(boolean run) {
            this.run = run;
        }
    }

    
}

