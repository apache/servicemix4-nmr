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
package org.apache.servicemix.nmr.core.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import junit.framework.TestCase;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.Message;
import org.apache.servicemix.nmr.core.ExchangeImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExchangeUtilsTest extends TestCase {

    private static final Log LOG = LogFactory.getLog(ExchangeUtilsTest.class);

    public void testReReadable() throws Exception {
        Exchange e = new ExchangeImpl(Pattern.InOnly);
        Message msg = e.getIn();
        msg.addAttachment("id", new BufferedInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 })));
        msg.setBody(new DOMSource(parse("<hello/>")));

        e.ensureReReadable();

        assertNotNull(msg.getBody());
        assertTrue(msg.getBody() instanceof StringSource);
        assertNotNull(msg.getAttachment("id"));
        assertTrue(msg.getAttachment("id") instanceof ByteArrayInputStream);
    }

    public void testDisplay() throws Exception {
        Exchange e = new ExchangeImpl(Pattern.InOnly);
        e.setOperation(new QName("op"));
        e.setProperty("key", "value");
        e.setStatus(Status.Done);
        Message msg = e.getIn();
        msg.setHeader("header", "value");
        msg.addAttachment("id", new BufferedInputStream(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 })));
        msg.setBody(new StringSource("<hello/>"));

        String str = e.display(false);
        LOG.info(str);
        assertNotNull(msg.getBody());
        assertTrue(msg.getBody() instanceof StringSource);
        assertNotNull(msg.getAttachment("id"));
        assertTrue(msg.getAttachment("id") instanceof BufferedInputStream);

        str = e.display(true);
        LOG.info(str);
        assertNotNull(msg.getBody());
        assertTrue(msg.getBody() instanceof StringSource);
        assertNotNull(msg.getAttachment("id"));
        assertTrue(msg.getAttachment("id") instanceof ByteArrayInputStream);
    }

    private Document parse(String str) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(str.getBytes()));
    }

}
