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

import javax.jbi.component.ComponentContext;
import javax.jbi.component.Component;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.Definition;
import javax.wsdl.PortType;

import org.w3c.dom.Document;

import junit.framework.TestCase;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.core.ServiceMix;
import org.apache.servicemix.jbi.runtime.impl.ClientComponentContext;
import org.apache.servicemix.jbi.runtime.impl.ComponentRegistryImpl;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.document.impl.DocumentRepositoryImpl;

public class ComponentContextTest extends TestCase {

    private NMR nmr;
    private ComponentRegistry componentRegistry;

    @Override
    protected void setUp() throws Exception {
        ServiceMix smx = new ServiceMix();
        smx.init();
        this.nmr = smx;
        ComponentRegistryImpl reg = new ComponentRegistryImpl();
        reg.setNmr(nmr);
        reg.setDocumentRepository(new DocumentRepositoryImpl());
        this.componentRegistry = reg;

        DefaultComponent component = new DefaultComponent();
        component.addEndpoint(new TestEndpoint(component.getServiceUnit(),
                                               new QName("urn:test", "service"),
                                               "endpoint",
                                               new QName("urn:test", "interface")));
        this.componentRegistry.register(new SimpleComponentWrapper(component), null);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testQueryEndpoint() throws Exception {
        ComponentContext context = componentRegistry.createComponentContext();
        ServiceEndpoint ep = context.getEndpoint(new QName("urn:test", "service"), "endpoint");
        assertNotNull(ep);
    }

    public void testQueryEndpointForService() throws Exception {
        ComponentContext context = componentRegistry.createComponentContext();
        ServiceEndpoint[] eps = context.getEndpointsForService(new QName("urn:test", "service"));
        assertNotNull(eps);
        assertEquals(1, eps.length);
        assertNotNull(eps[0]);
    }

    public void testQueryEndpointsForInterface() throws Exception {
        ComponentContext context = componentRegistry.createComponentContext();
        ServiceEndpoint[] eps = context.getEndpoints(new QName("urn:test", "interface"));
        assertNotNull(eps);
        assertEquals(1, eps.length);
        assertNotNull(eps[0]);
    }

    public void testQueryEndpoints() throws Exception {
        ComponentContext context = componentRegistry.createComponentContext();
        ServiceEndpoint[] eps = context.getEndpoints(null);
        assertNotNull(eps);
        assertEquals(1, eps.length);
        assertNotNull(eps[0]);
    }

    public static class TestEndpoint extends ProviderEndpoint {
        public TestEndpoint(ServiceUnit serviceUnit, QName service, String endpoint, QName interfaceName) {
            super(serviceUnit, service, endpoint);
            setInterfaceName(interfaceName);
            setDescription(createDescription(interfaceName));
        }

        private Document createDescription(QName interfaceName) {
            try {
                WSDLFactory factory = WSDLFactory.newInstance();
                Definition def = factory.newDefinition();
                def.setTargetNamespace(interfaceName.getNamespaceURI());
                PortType port = def.createPortType();
                port.setQName(interfaceName);
                port.setUndefined(false);
                def.addPortType(port);
                Document doc = factory.newWSDLWriter().getDocument(def);
                //System.err.println(DOMUtil.asXML(doc));
                return doc;
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
        }

    }
}
