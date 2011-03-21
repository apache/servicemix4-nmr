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

import junit.framework.TestCase;
import org.apache.servicemix.bean.BeanComponent;
import org.apache.servicemix.bean.BeanEndpoint;
import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.file.FileComponent;
import org.apache.servicemix.jbi.listener.MessageExchangeListener;
import org.apache.servicemix.jbi.runtime.impl.ComponentRegistryImpl;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.apache.servicemix.nmr.core.ServiceMix;
import org.apache.servicemix.nmr.core.util.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import javax.annotation.Resource;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.*;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.UUID;

/**
 * Test case to ensure sending to endpoint resolved by
 * {@link ComponentContext#resolveEndpointReference(org.w3c.dom.DocumentFragment)} works
 */
public class EndpointReferenceTest extends TestCase {

    private static final File DIRECTORY = new File("target");

    private static final QName SENDER = new QName("urn:test", "sender");
    private static final String ENDPOINT = "endpoint";

    public void testSendToEPR() throws Exception {
        ServiceMix smx = new ServiceMix();
        smx.init();

        ComponentRegistryImpl reg = new ComponentRegistryImpl();
        reg.setNmr(smx);

        BeanComponent bean = new BeanComponent();
        SenderBean sender = new SenderBean();
        BeanEndpoint endpoint = new BeanEndpoint();
        endpoint.setService(SENDER);
        endpoint.setEndpoint(ENDPOINT);
        endpoint.setBean(sender);
        bean.addEndpoint(endpoint);
        reg.register(new SimpleComponentWrapper(bean),
                ServiceHelper.createMap(ComponentRegistry.NAME, "servicemix-bean",
                                        ComponentRegistry.TYPE, "service-engine"));
        bean.getLifeCycle().start();

        FileComponent file = new FileComponent();
        reg.register(new SimpleComponentWrapper(file),
                ServiceHelper.createMap(ComponentRegistry.NAME, "servicemix-file",
                                        ComponentRegistry.TYPE, "binding-component"));
        file.getLifeCycle().start();

        String filename = UUID.randomUUID().toString();
        MessageExchange exchange = sender.send(filename);
        assertEquals("MessageExchange should have finished successfully",
                     ExchangeStatus.DONE, exchange.getStatus());
        assertTrue("File should have been created",
                   new File(DIRECTORY, filename).exists());

    }

    /*
     * Bean to allow testing with servicemix-bean
     */
    private static final class SenderBean implements MessageExchangeListener {

        @Resource
        private DeliveryChannel channel;

        @Resource
        private ComponentContext context;

        public MessageExchange send(String filename) throws Exception {
            ServiceEndpoint endpoint = context.resolveEndpointReference(createEndpointReference(DIRECTORY.toURI().toString()));

            MessageExchange exchange = channel.createExchangeFactory().createInOnlyExchange();
            exchange.setEndpoint(endpoint);

            NormalizedMessage in = exchange.createMessage();
            in.setContent(new StringSource("<hello/>"));
            in.setProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY, filename);
            exchange.setMessage(in,  "in");

            channel.sendSync(exchange);

            return exchange;
        }

        public void onMessageExchange(MessageExchange messageExchange) throws MessagingException {
            // ignore this -- we're only sending InOnly so nothing to do here
        }

        private DocumentFragment createEndpointReference(String uri) throws ParserConfigurationException {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            DocumentFragment result = document.createDocumentFragment();
            Element reference = document.createElementNS("http://www.w3.org/2005/08/addressing", "EndpointReference");
            Element address = document.createElementNS("http://www.w3.org/2005/08/addressing", "Address");
            address.setTextContent(uri);
            reference.appendChild(address);
            result.appendChild(reference);
            return result;
        }
    }

}
