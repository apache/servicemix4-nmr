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
package org.apache.servicemix.preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 15, 2008
 * Time: 11:30:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class FilePreferencesImpl extends AbstractPreferencesImpl {

    private static final String PROPS_FILE = "node.properties";

    private File root;
    private Properties props;
    private Map<String, FilePreferencesImpl> children;
    private List<FilePreferencesImpl> deleted;

    public FilePreferencesImpl(AbstractPreferencesImpl parent, String name, File path) {
        super(parent, name);
        this.root = path;
    }

    String doGetValue(String key) {
        ensureLoaded();
        return props.getProperty(key);
    }

    void doSetValue(String key, String value) {
        ensureLoaded();
        props.setProperty(key, value);
    }

    boolean doRemoveValue(String key) {
        ensureLoaded();
        return props.remove(key) != null;
    }

    boolean doClear() {
        ensureLoaded();
        if (props.isEmpty()) {
            return false;
        }
        props.clear();
        return true;
    }

    List<String> doGetKeys() {
        ensureLoaded();
        List<String> keys = new ArrayList<String>();
        for (Object k : props.keySet()) {
            keys.add((String) k);
        }
        return keys;
    }

    List<AbstractPreferencesImpl> doGetChildren() {
        ensureLoaded();
        return new ArrayList<AbstractPreferencesImpl>(children.values());
    }

    AbstractPreferencesImpl doCreateChild(String name) {
        ensureLoaded();
        File f = new File(root, name);
        FilePreferencesImpl child = new FilePreferencesImpl(this, f.getName(), f);
        children.put(child.name(), child);
        return child;
    }

    void doRemoveNode(String node) {
        ensureLoaded();
        FilePreferencesImpl child = children.remove(node);
        if (child != null) {
            deleted.add(child);
        }
    }

    void doFlush() {
        ensureLoaded();
        if (!root.exists()) {
            root.mkdirs();
        }
        OutputStream os = null;
        try {
            os = new FileOutputStream(new File(root, PROPS_FILE));
            props.store(os, "Preferences for node " + name());
        } catch (IOException e) {
            error("Unable to store nore " + name(), e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        for (FilePreferencesImpl c : deleted) {
            deleteFile(c.root);
        }
        deleted.clear();
        for (FilePreferencesImpl c : children.values()) {
            c.doFlush();
        }
    }

    private void ensureLoaded() {
        if (props != null) {
            return;
        }
        props = new Properties();
        children = new HashMap<String, FilePreferencesImpl>();
        deleted = new ArrayList<FilePreferencesImpl>();
        if (root.isDirectory()) {
            File fp = new File(root, PROPS_FILE);
            if (fp.isFile()) {
                InputStream is = null;
                try {
                    is = new FileInputStream(fp);
                    props.load(is);
                } catch (IOException e) {
                    error("Unable to load node " + name(), e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            }
            for (File f : root.listFiles()) {
                if (f.isDirectory()) {
                    FilePreferencesImpl child = new FilePreferencesImpl(this, f.getName(), f);
                    children.put(child.name(), child);
                }
            }
        }
    }

    private void error(String s, IOException e) {
        System.out.println(s);
        e.printStackTrace();
    }

    /**
     * Delete a file
     *
     * @param fileToDelete
     * @return true if the File is deleted
     */
    public static boolean deleteFile(File fileToDelete) {
        if (fileToDelete == null || !fileToDelete.exists()) {
            return true;
        }
        boolean result = true;
        if (fileToDelete.isDirectory()) {
            File[] files = fileToDelete.listFiles();
            if (files == null) {
                result = false;
            } else {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (file.getName().equals(".") || file.getName().equals("..")) {
                        continue;
                    }
                    if (file.isDirectory()) {
                        result &= deleteFile(file);
                    } else {
                        result &= file.delete();
                    }
                }
            }
        }
        result &= fileToDelete.delete();
        return result;
    }
}
