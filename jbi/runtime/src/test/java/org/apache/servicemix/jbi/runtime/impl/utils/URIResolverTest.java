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
package org.apache.servicemix.jbi.runtime.impl.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import junit.framework.TestCase;
import org.apache.servicemix.jbi.runtime.impl.InOnlyImpl;
import org.apache.servicemix.jbi.runtime.impl.ServiceEndpointImpl;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.core.ExchangeImpl;

public class URIResolverTest extends TestCase {

    public void testCreateEpr() {
        DocumentFragment df = URIResolver.createWSAEPR("urn:test");
        assertNotNull(df);
        Element e = DOMUtil.getFirstChildElement(df);
        assertNotNull(e);
        assertEquals(new QName("epr"), DOMUtil.getQName(e));
        e = DOMUtil.getFirstChildElement(e);
        assertNotNull(e);
        assertEquals(new QName(WSAddressingConstants.WSA_NAMESPACE_200508, WSAddressingConstants.EL_ADDRESS),
                     DOMUtil.getQName(e));
        assertEquals("urn:test", DOMUtil.getElementText(e));
    }

    public void testConfigureExchange() {
        ComponentContext ctx = (ComponentContext) Proxy.newProxyInstance(
                                                        ComponentContext.class.getClassLoader(), 
                                                        new Class[] { ComponentContext.class },
                                                        new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                throw new UnsupportedOperationException();
            }
        });

        MessageExchange me = new InOnlyImpl(new ExchangeImpl(Pattern.InOnly));
        URIResolver.configureExchange(me, ctx, "interface:urn:test");
        assertEquals(new QName("urn", "test"), me.getInterfaceName());
        assertNull(me.getOperation());
        assertNull(me.getService());
        assertNull(me.getEndpoint());

        me = new InOnlyImpl(new ExchangeImpl(Pattern.InOnly));
        URIResolver.configureExchange(me, ctx, "operation:urn:test:op");
        assertEquals(new QName("urn", "test"), me.getInterfaceName());
        assertEquals(new QName("urn", "op"), me.getOperation());
        assertNull(me.getService());
        assertNull(me.getEndpoint());

        me = new InOnlyImpl(new ExchangeImpl(Pattern.InOnly));
        URIResolver.configureExchange(me, ctx, "service:urn:test");
        assertNull(me.getInterfaceName());
        assertNull(me.getOperation());
        assertEquals(new QName("urn", "test"), me.getService());
        assertNull(me.getEndpoint());

        ctx = (ComponentContext) Proxy.newProxyInstance(ComponentContext.class.getClassLoader(),
                                                        new Class[] { ComponentContext.class },
                                                        new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                assertEquals("getEndpoint", method.getName());
                assertEquals(new QName("urn", "svc"), args[0]);
                assertEquals("ep", args[1]);
                return new ServiceEndpointImpl((QName) args[0], (String) args[1]);
            }
        });
        me = new InOnlyImpl(new ExchangeImpl(Pattern.InOnly));
        URIResolver.configureExchange(me, ctx, "endpoint:urn:svc:ep");
        assertNull(me.getInterfaceName());
        assertNull(me.getOperation());
        assertNull(me.getService());
        assertNotNull(me.getEndpoint());

        ctx = (ComponentContext) Proxy.newProxyInstance(ComponentContext.class.getClassLoader(),
                                                        new Class[] { ComponentContext.class },
                                                        new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                assertEquals("resolveEndpointReference", method.getName());
                assertTrue(args[0] instanceof DocumentFragment);
                return new ServiceEndpointImpl(new QName("svc"), "ep");
            }
        });
        me = new InOnlyImpl(new ExchangeImpl(Pattern.InOnly));
        URIResolver.configureExchange(me, ctx, "http://urn/");
        assertNull(me.getInterfaceName());
        assertNull(me.getOperation());
        assertNull(me.getService());
        assertNotNull(me.getEndpoint());

        try {
            URIResolver.configureExchange(null, ctx, "service:urn:test");
            fail("Should have thrown a NPE");
        } catch (NullPointerException e) {
        }
        try {
            URIResolver.configureExchange(me, null, "service:urn:test");
            fail("Should have thrown a NPE");
        } catch (NullPointerException e) {
        }
        try {
            URIResolver.configureExchange(me, ctx, null);
            fail("Should have thrown a NPE");
        } catch (NullPointerException e) {
        }
    }

    public void testSplit2Column() {
        String[] parts = URIResolver.split2("urn:ns:svc");
        assertNotNull(parts);
        assertEquals(2, parts.length);
        assertEquals("urn:ns", parts[0]);
        assertEquals("svc", parts[1]);
    }

    public void testSplit2Slash() {
        String[] parts = URIResolver.split2("urn://ns/svc");
        assertNotNull(parts);
        assertEquals(2, parts.length);
        assertEquals("urn://ns", parts[0]);
        assertEquals("svc", parts[1]);
    }

    public void testSplit2SlashEnding() {
        String[] parts = URIResolver.split2("urn://ns//svc");
        assertNotNull(parts);
        assertEquals(2, parts.length);
        assertEquals("urn://ns/", parts[0]);
        assertEquals("svc", parts[1]);
    }

    public void testSplit2Bad() {
        try {
            String[] parts = URIResolver.split2("urn");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testSplit3Column() {
        String[] parts = URIResolver.split3("urn:ns:svc:ep");
        assertNotNull(parts);
        assertEquals(3, parts.length);
        assertEquals("urn:ns", parts[0]);
        assertEquals("svc", parts[1]);
        assertEquals("ep", parts[2]);
    }

    public void testSplit3ColumnEmpty() {
        String[] parts = URIResolver.split3("urn:ns::ep");
        assertNotNull(parts);
        assertEquals(3, parts.length);
        assertEquals("urn:ns", parts[0]);
        assertEquals("", parts[1]);
        assertEquals("ep", parts[2]);
    }

    public void testSplit3Slash() {
        String[] parts = URIResolver.split3("urn://ns/svc/ep");
        assertNotNull(parts);
        assertEquals(3, parts.length);
        assertEquals("urn://ns", parts[0]);
        assertEquals("svc", parts[1]);
        assertEquals("ep", parts[2]);
    }

    public void testSplit3SlashEnding() {
        String[] parts = URIResolver.split3("urn://ns//svc/ep");
        assertNotNull(parts);
        assertEquals(3, parts.length);
        assertEquals("urn://ns/", parts[0]);
        assertEquals("svc", parts[1]);
        assertEquals("ep", parts[2]);
    }

    public void testSplit3Bad() {
        try {
            String[] parts = URIResolver.split3("urn");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            String[] parts = URIResolver.split3("urn:test");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

}
