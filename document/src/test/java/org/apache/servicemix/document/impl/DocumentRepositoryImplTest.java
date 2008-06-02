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
package org.apache.servicemix.document.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import junit.framework.TestCase;
import org.osgi.service.url.URLStreamHandlerSetter;

public class DocumentRepositoryImplTest extends TestCase {

    private DocumentRepositoryImpl repository;

    protected void setUp() {
        repository = new DocumentRepositoryImpl();
    }

    public void testRegisterUnregister() throws Exception {
        byte[] data = new byte[] { 1, 2, 3, 4 };

        String url = repository.register(data);
        assertNotNull(url);
        assertTrue(url.startsWith(DocumentRepositoryImpl.PROTOCOL_COLUMN));

        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            public URLStreamHandler createURLStreamHandler(String protocol) {
                if (protocol.equals(DocumentRepositoryImpl.PROTOCOL)) {
                    return new Handler();
                }
                return null;
            }
        });

        InputStream is = new URL(url).openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copyInputStream(is, os);
        byte[] d = os.toByteArray();
        assertEquals(4, d.length);
        for (int i = 0; i < 4; i++) {
            assertEquals(i + 1, d[i]);
        }

        repository.unregister(url);

        try {
            new URL(url).openStream();
            fail("Should have failed");
        } catch (FileNotFoundException e) {
        }

        repository.register(new ByteArrayInputStream(data));

        repository.unregister("dummy");
    }

    public class Handler extends URLStreamHandler implements URLStreamHandlerSetter {
        protected void parseURL(URL u, String spec, int start, int limit) {
            repository.parseURL(this, u, spec, start, limit);    //To change body of overridden methods use File | Settings | File Templates.
        }
        public URLConnection openConnection(URL u) throws IOException {
            return repository.openConnection(u);
        }
        public void setURL(URL u, String protocol, String host, int port, String file, String ref) {
            super.setURL(u, protocol, host, port, null, null, file, null, ref);
        }
        public void setURL(URL u, String protocol, String host, int port, String authority, String userInfo, String path, String query, String ref) {
            super.setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
        }
    }

    /**
     * Copy in stream to an out stream
     * 
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int len = in.read(buffer);
        while (len >= 0) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
    }

}

