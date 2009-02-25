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
package org.apache.servicemix.nmr.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Message;
import org.apache.servicemix.nmr.api.Pattern;
import junit.framework.TestCase;

public class MessageImplTest extends TestCase {

    public void testBody() {
        Message msg = new MessageImpl();
        assertNull(msg.getBody());
        msg.setBody("<hello>world</hello>");
        assertTrue(msg.getBody() instanceof String);
        assertNotNull(msg.getBody(String.class));
        assertNotNull(msg.getBody(byte[].class));
        msg.setBody("<hello>world</hello>", byte[].class);
        assertTrue(msg.getBody() instanceof byte[]);
    }

    public void testConvertWithoutCamel() throws Exception {
        final URLClassLoader c = (URLClassLoader) getClass().getClassLoader();
        ClassLoader cl = new URLClassLoader(c.getURLs(), null) {
            protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("org.apache.camel")) {
                    throw new ClassNotFoundException(name);
                }
                if (name.startsWith("org.apache.servicemix.nmr.api")) {
                    return c.loadClass(name);
                }
                return super.loadClass(name, resolve);
            }
        };
        Class cls = cl.loadClass(MessageImpl.class.getName());
        Message msg = (Message) cls.newInstance();
        msg.setBody("<hello>world</hello>");
        assertNull(msg.getBody(byte[].class));
    }

    public void testHeaders() {
        Message msg = new MessageImpl();
        assertNotNull(msg.getHeaders());
        msg.setHeader("name", "value");
        assertEquals("value", msg.getHeader("name"));
        assertNotNull(msg.getHeader("name", byte[].class));
        assertNotNull(msg.removeHeader("name"));
        assertNull(msg.getHeader("name"));
        msg.setHeader(Exchange.class, new ExchangeImpl(Pattern.InOnly));
        assertNotNull(msg.getHeader(Exchange.class.getName(), Exchange.class));
        assertNotNull(msg.getHeader(Exchange.class));
        assertNotNull(msg.removeHeader(Exchange.class));
        assertNull(msg.getHeader(Exchange.class));
        assertTrue(msg.getHeaders().isEmpty());
        msg.setHeaders(createMap("key", "val"));
        assertNotNull(msg.getHeaders());
        assertFalse(msg.getHeaders().isEmpty());
        msg.setHeaders(null);
        assertNull(msg.getHeader("name"));
        assertNull(msg.getHeader(Exchange.class));
        assertNull(msg.getHeader("name", byte[].class));
        assertNull(msg.removeHeader("name"));
        msg.setHeaders(null);
        msg.setHeader(Exchange.class, new ExchangeImpl(Pattern.InOnly));
        assertNotNull(msg.getHeader(Exchange.class));
        msg.setHeaders(null);
        msg.setHeader("name", "value");
        assertNotNull(msg.getHeader("name"));
    }

    public void testContentType() {
        Message msg = new MessageImpl();
        assertNull(msg.getContentType());
        msg.setContentType("type");
        assertEquals("type", msg.getContentType());
    }

    public void testContentEncoding() {
        Message msg = new MessageImpl();
        assertNull(msg.getContentEncoding());
        msg.setContentEncoding("enc");
        assertEquals("enc", msg.getContentEncoding());
    }

    public void testAttachments() {
        Message msg = new MessageImpl();
        assertNull(msg.getAttachment("id"));
        msg.removeAttachment("id");
        assertNotNull(msg.getAttachments());
        assertNull(msg.getAttachment("id"));
        msg.addAttachment("id", "value");
        assertEquals("value", msg.getAttachment("id"));
        msg.removeAttachment("id");
        assertNull(msg.getAttachment("id"));
        assertTrue(msg.getAttachments().isEmpty());
    }

    public void testCopy() {
        Message msg = new MessageImpl();
        Message cpy = msg.copy();
        assertNotNull(cpy);

        msg = new MessageImpl();
        msg.setHeader("header", "value");
        msg.addAttachment("id", "att");
        cpy = msg.copy();
        assertNotNull(cpy);
        assertNotNull(cpy.getHeader("header"));
        assertNotNull(cpy.getAttachment("id"));
    }

    public void testWrite() throws Exception {
        Message msg = new MessageImpl();
        msg.setHeader("header", "value");
        msg.addAttachment("id", "att");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(msg);
        os.close();
        ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        Message cpy = (Message) is.readObject();
        assertNotNull(cpy);
        assertNotNull(cpy.getHeader("header"));
        assertNotNull(cpy.getAttachment("id"));
    }

    public void testDisplay() {
        Message msg = new MessageImpl();
        msg.toString();
        msg.display(false);
    }

    public static Map<String, Object> createMap(String... data) {
        Map<String, Object> props = new HashMap<String, Object>();
        for (int i = 0; i < data.length / 2; i++) {
            props.put(data[i*2], data[i*2+1]);
        }
        return props;
    }

}
