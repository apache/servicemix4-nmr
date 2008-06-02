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
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.servicemix.document.Resource;
import org.apache.servicemix.document.DocumentRepository;
import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * Simple document repository
 */
public class DocumentRepositoryImpl extends AbstractURLStreamHandlerService implements DocumentRepository {

    public static final String PROTOCOL = "document";
    public static final String PROTOCOL_COLUMN = PROTOCOL + ":";

    private volatile long index = 0;
    private Map<Long, Resource> documents = new ConcurrentHashMap<Long, Resource>();

    public String register(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len = in.read(buffer);
        while (len >= 0) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
        out.close();
        return register(out.toByteArray());
    }

    public String register(final byte[] data) {
        return register(new Resource() {
            public InputStream open() throws IOException {
                return new ByteArrayInputStream(data);
            }
        });

    }

    public String register(Resource res) {
        long idx = ++index;
        documents.put(idx, res);
        return PROTOCOL + ":" + idx;
    }

    public void unregister(String url) {
        if (url.startsWith(PROTOCOL_COLUMN)) {
            String idx = url.substring(PROTOCOL_COLUMN.length());
            documents.remove(Long.parseLong(idx));
        }
    }

    public URLConnection openConnection(URL url) throws IOException {
        return new URLConnection(url) {
            @Override
            public void connect() throws IOException {
            }
            @Override
            public InputStream getInputStream() throws IOException {
                connect();
                Long idx = Long.parseLong(getURL().getPath());
                Resource res = documents.get(idx);
                if (res == null) {
                    throw new FileNotFoundException(getURL().toString());
                }
                return res.open();
            }
        };
    }

}
