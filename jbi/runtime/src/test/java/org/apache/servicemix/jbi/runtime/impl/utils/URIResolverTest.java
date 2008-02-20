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

import javax.xml.namespace.QName;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import junit.framework.TestCase;

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
