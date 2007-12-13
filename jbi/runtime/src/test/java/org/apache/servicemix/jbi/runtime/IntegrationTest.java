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
package org.apache.servicemix.jbi.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.core.ServiceMix;
import org.apache.servicemix.eip.EIPComponent;
import org.apache.servicemix.eip.EIPEndpoint;
import org.apache.servicemix.eip.patterns.WireTap;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.jbi.runtime.impl.ComponentRegistryImpl;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 5, 2007
 * Time: 1:31:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class IntegrationTest {

    @Test
    public void testJbiComponent() throws Exception {
        ServiceMix smx = new ServiceMix();
        smx.init();
        ComponentRegistryImpl reg = new ComponentRegistryImpl();
        reg.setNmr(smx);

        Endpoint tep = new Endpoint() {
            private Channel channel;
            public void setChannel(Channel channel) {
                this.channel = channel;
            }
            public void process(Exchange exchange) {
                System.out.println("Received exchange: " + exchange);
                if (exchange.getStatus() == Status.Active) {
                    exchange.setStatus(Status.Done);
                    channel.send(exchange);
                }
            }
        };
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Endpoint.SERVICE_NAME, new QName("target"));
        smx.getEndpointRegistry().register(tep, props);

        EIPComponent eip = new EIPComponent();
        WireTap ep = new WireTap();
        ep.setService(new QName("uri:foo", "bar"));
        ep.setEndpoint("ep");
        ep.setTarget(new ExchangeTarget());
        ep.getTarget().setService(new QName("target"));
        eip.setEndpoints(new EIPEndpoint[] { ep });
        reg.register(eip, null);

        Channel channel = smx.createChannel();
        Exchange e = channel.createExchange(Pattern.InOnly);
        e.getIn().setBody("<hello/>");
        e.setTarget(smx.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, "{uri:foo}bar:ep")));
        channel.sendSync(e);
    }
}
