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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.URLClassLoader;

import javax.xml.namespace.QName;

import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Message;
import org.apache.servicemix.nmr.api.Pattern;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.Type;
import org.apache.servicemix.nmr.api.internal.InternalExchange;

import junit.framework.TestCase;


public class ExchangeImplTest extends TestCase {

    public void testWrite() throws Exception {
        Exchange e = new ExchangeImpl(Pattern.InOnly);
        e.setOperation(new QName("op"));
        e.setProperty("key", "value");
        e.setStatus(Status.Done);
        Message msg = e.getIn();
        msg.setHeader("header", "value");
        msg.addAttachment("id", "att");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(e);
        os.close();
        ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        Exchange cpy = (Exchange) is.readObject();
        assertNotNull(cpy);
        assertEquals(e.getId(), cpy.getId());
        assertEquals(e.getStatus(), cpy.getStatus());
        assertEquals(e.getRole(), cpy.getRole());
        assertEquals(e.getPattern(), cpy.getPattern());
        assertEquals(e.getOperation(), cpy.getOperation());
        assertEquals(e.getProperty("key"), cpy.getProperty("key"));
        assertNotNull(cpy.getIn());
        assertNotNull(cpy.getIn().getHeader("header"));
        assertNotNull(cpy.getIn().getAttachment("id"));
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
        Class cls = cl.loadClass(ExchangeImpl.class.getName());
        Exchange e = (Exchange) cls.getConstructor(Pattern.class).newInstance(Pattern.InOut);
        e.setProperty("key", "<hello>world</hello>");
        assertNull(e.getProperty("key", byte[].class));
    }

    public void testCopy() {
        Exchange e = new ExchangeImpl(Pattern.InOut);
        Exchange cpy = e.copy();
        assertNotNull(cpy);
        assertNull(cpy.getIn(false));
        assertNull(cpy.getOut(false));
        assertNull(cpy.getFault(false));

        e = new ExchangeImpl(Pattern.InOut);
        e.setProperty("header", "value");
        e.getIn();
        cpy = e.copy();
        assertNotNull(cpy);
        assertNotNull(cpy.getProperty("header"));
        assertNotNull(cpy.getIn(false));
    }

    public void testMessages() {
        Exchange e = new ExchangeImpl(Pattern.InOut);
        assertNull(e.getMessage(Type.In, false));
        assertNull(e.getMessage(Type.Out, false));
        assertNull(e.getMessage(Type.Fault, false));
        assertNotNull(e.getMessage(Type.In));
        assertNotNull(e.getMessage(Type.Out));
        assertNotNull(e.getMessage(Type.Fault));
        assertNotNull(e.getMessage(Type.In));
        assertNotNull(e.getMessage(Type.Out));
        assertNotNull(e.getMessage(Type.Fault));
        assertNotNull(e.getMessage(Type.In, false));
        assertNotNull(e.getMessage(Type.Out, false));
        assertNotNull(e.getMessage(Type.Fault, false));
        e.setMessage(Type.In, null);
        e.setMessage(Type.Out, null);
        e.setMessage(Type.Fault, null);
    }

    public void testError() {
        Exchange e = new ExchangeImpl(Pattern.InOnly);
        assertNull(e.getError());
        assertEquals(Status.Active, e.getStatus());
        e.setError(new Exception());
        assertNotNull(e.getError());
        assertEquals(Status.Error, e.getStatus());
    }

    public void testProperties() {
        Exchange e = new ExchangeImpl(Pattern.InOnly);
        assertNotNull(e.getProperties());
        e.setProperty("name", "value");
        assertEquals("value", e.getProperty("name"));
        assertNotNull(e.getProperty("name", String.class));
        assertNotNull(e.getProperty("name", byte[].class));
        assertNotNull(e.removeProperty("name"));
        assertNull(e.getProperty("name"));
        e.setProperty(Exchange.class, new ExchangeImpl(Pattern.InOnly));
        assertNotNull(e.getProperty(Exchange.class.getName(), Exchange.class));
        assertNotNull(e.getProperty(Exchange.class));
        assertNotNull(e.removeProperty(Exchange.class));
        assertNull(e.getProperty(Exchange.class));
        assertTrue(e.getProperties().isEmpty());
        e.setProperties(createMap("key", "val"));
        assertNotNull(e.getProperties());
        assertFalse(e.getProperties().isEmpty());
        e.setProperties(null);
        assertNull(e.getProperty("name"));
        assertNull(e.getProperty(Exchange.class));
        assertNull(e.getProperty("name", byte[].class));
        assertNull(e.removeProperty("name"));
        e.setProperties(null);
        e.setProperty(Exchange.class, new ExchangeImpl(Pattern.InOnly));
        assertNotNull(e.getProperty(Exchange.class));
        e.setProperties(null);
        e.setProperty("name", "value");
        assertNotNull(e.getProperty("name"));
    }

    public void testDisplay() {
        Exchange e = new ExchangeImpl(Pattern.InOut);
        e.toString();
        e.display(false);
        e = new ExchangeImpl(Pattern.InOut);
        e.getIn();
        e.getOut();
        e.getFault();
        e.toString();
    }

    public void testLocks() {
        ExchangeImpl e = new ExchangeImpl(Pattern.InOut);
        assertNull(e.getConsumerLock(false));
        assertNotNull(e.getConsumerLock(true));
        assertNull(e.getProviderLock(false));
        assertNotNull(e.getProviderLock(true));
    }

	public void testInOnly() {
		Exchange e = new ExchangeImpl(Pattern.InOnly);
		assertNotNull(e.getIn());
		assertNull(e.getOut());
		assertNull(e.getFault());
	}

	public void testRobustInOnly() {
		Exchange e = new ExchangeImpl(Pattern.RobustInOnly);
		assertNotNull(e.getIn());
		assertNull(e.getOut());
		assertNotNull(e.getFault());
	}

	public void testInOut() {
		Exchange e = new ExchangeImpl(Pattern.InOut);
		assertNotNull(e.getIn());
		assertNotNull(e.getOut());
		assertNotNull(e.getFault());
	}

	public void testInOptionalOut() {
		Exchange e = new ExchangeImpl(Pattern.InOptionalOut);
		assertNotNull(e.getIn());
		assertNotNull(e.getOut());
		assertNotNull(e.getFault());
	}

	public void testCancel() throws InterruptedException {
	    final InternalExchange e = new ExchangeImpl(Pattern.InOnly);
	    final CountDownLatch latch = new CountDownLatch(1);
	    Thread thread = new Thread() {
	        @Override
	        public void run() {
	            try {
	                e.getConsumerLock(true).acquire();
	                latch.countDown();
	            } catch (InterruptedException e) {
	                fail(e.getMessage());
	            }
	        }
	    };
	    thread.start();
	    //let's sleep for a moment to make sure the thread has acquired the lock
	    Thread.sleep(150);
	    e.cancel();
	    assertTrue("Exchange should have been cancelled", latch.await(1, TimeUnit.SECONDS));
	    assertEquals(Status.Error, e.getStatus());
	}

    public static Map<String, Object> createMap(String... data) {
        Map<String, Object> props = new HashMap<String, Object>();
        for (int i = 0; i < data.length / 2; i++) {
            props.put(data[i*2], data[i*2+1]);
        }
        return props;
    }
}
