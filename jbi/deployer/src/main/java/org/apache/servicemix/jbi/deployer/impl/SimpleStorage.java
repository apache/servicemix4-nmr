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
package org.apache.servicemix.jbi.deployer.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;

public class SimpleStorage implements Storage {

    private final File file;
    private final Properties props = new Properties();

    public SimpleStorage(File file) {
        this.file = file;
    }

    public void load() throws IOException {
        if (file.exists()) {
            InputStream is = new FileInputStream(file);
            try {
                props.load(is);
            } finally {
                is.close();
            }
        }
    }

    public String get(String key) {
        return props.getProperty(key);
    }

    public String get(String key, String def) {
        return props.getProperty(key, def);
    }

    public void put(String key, String value) {
        props.setProperty(key, value);
    }

    public void clear() {
        clear(null);
    }

    public void clear(String prefix) {
        if (prefix == null) {
            prefix = "";
        }
        for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
            String k = (String) e.nextElement();
            if (k.startsWith(prefix)) {
                props.remove(k);
            }
        }
    }

    public void save() throws IOException {
        OutputStream os = new FileOutputStream(file);
        try {
            props.store(os, null);
        } finally {
            os.close();
        }
    }

    public Storage getStorage(String name) {
        return new SubStorage(this, name + ".");
    }

    static class SubStorage implements Storage {

        private final Storage parent;
        private final String prefix;

        public SubStorage(Storage parent, String prefix) {
            this.parent = parent;
            this.prefix = prefix;
        }

        public String get(String key) {
            return parent.get(prefix + key);
        }

        public String get(String key, String def) {
            return parent.get(prefix + key, def);
        }

        public void put(String key, String value) {
            parent.put(prefix + key, value);
        }

        public void clear() {
            clear(null);
        }

        public void clear(String prefix) {
            parent.clear(this.prefix + (prefix != null ? prefix : ""));
        }

        public void save() throws IOException {
            parent.save();
        }

        public Storage getStorage(String name) {
            return new SubStorage(this, name + ".");
        }
    }
}
