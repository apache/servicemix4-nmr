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

import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import org.apache.xbean.classloader.MultiParentClassLoader;
import org.osgi.framework.Bundle;

public class OsgiMultiParentClassLoader extends MultiParentClassLoader {

    private Bundle bundle;
    private URL[] urls;

    public OsgiMultiParentClassLoader(Bundle bundle, String name, URL[] urls) {
        this(bundle, name, urls, ClassLoader.getSystemClassLoader());
    }

    public OsgiMultiParentClassLoader(Bundle bundle, String name, URL[] urls, ClassLoader parent) {
        this(bundle, name, urls, new ClassLoader[] {parent});
    }

    public OsgiMultiParentClassLoader(Bundle bundle, String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        this(bundle, name, urls, new ClassLoader[] {parent}, factory);
    }

    public OsgiMultiParentClassLoader(Bundle bundle, String name, URL[] urls, ClassLoader[] parents) {
        this(bundle, name, urls, parents, false, new String[0], new String[0]);
    }

    public OsgiMultiParentClassLoader(Bundle bundle, String name, URL[] urls, ClassLoader parent, boolean inverseClassLoading, String[] hiddenClasses, String[] nonOverridableClasses) {
        this(bundle, name, urls, new ClassLoader[]{parent}, inverseClassLoading, hiddenClasses, nonOverridableClasses);
    }

    public OsgiMultiParentClassLoader(Bundle bundle, String name, URL[] urls, ClassLoader[] parents, URLStreamHandlerFactory factory) {
        super(name, getNonDirUrls(urls), parents, factory);
        this.bundle = bundle;
        this.urls = getDirUrls(urls);
    }

    public OsgiMultiParentClassLoader(Bundle bundle, String name, URL[] urls, ClassLoader[] parents, boolean inverseClassLoading, Collection hiddenClasses, Collection nonOverridableClasses) {
        this(bundle, name, urls, parents, inverseClassLoading, (String[]) hiddenClasses.toArray(new String[hiddenClasses.size()]), (String[]) nonOverridableClasses.toArray(new String[nonOverridableClasses.size()]));
    }

    public OsgiMultiParentClassLoader(Bundle bundle, String name, URL[] urls, ClassLoader[] parents, boolean inverseClassLoading, String[] hiddenClasses, String[] nonOverridableClasses) {
        super(name, getNonDirUrls(urls), parents, inverseClassLoading, hiddenClasses, nonOverridableClasses);
        this.bundle = bundle;
        this.urls = getDirUrls(urls);
    }


    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        Class clazz = null;
        // Call parent
        try {
            clazz = super.findClass(name);
        } catch (ClassNotFoundException e) {
            // Ignore
        }
        // Search for class in module.
        if (clazz == null) {
            String path = name.replace('.', '/').concat(".class");
            byte[] bytes = null;
            for (int i = 0; (bytes == null) && (i < urls.length); i++) {
                try {
                    String p = urls[i].getPath() + path;
                    if (p.startsWith("/")) {
                        p = p.substring(1);
                    }
                    URL url = bundle.getEntry(p);
                    if (url != null) {
                        InputStream is = url.openStream();
                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            FileUtil.copyInputStream(is, baos);
                            bytes = baos.toByteArray();
                        } finally {
                            is.close();
                        }
                    }
                } catch (Throwable t) {
                    // Ignore
                }
            }
            if (bytes != null) {
                String pkgName = getClassPackage(name);
                if (pkgName.length() > 0) {
                    if (getPackage(pkgName) == null) {
                        definePackage(pkgName, null, null, null, null, null, null, null);
                    }
                }
                if (clazz == null) {
                    clazz = defineClass(name, bytes, 0, bytes.length);
                }
            }
        }
        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }
        return clazz;
    }

    public static String getClassPackage(String className) {
        if (className == null) {
            className = "";
        }
        return (className.lastIndexOf('.') < 0) ? "" : className.substring(0, className.lastIndexOf('.'));
    }

    private static URL[] getDirUrls(URL[] urls) {
        List<URL> l = new ArrayList<URL>();
        for (URL u : urls) {
            if (u.toString().endsWith("/")) {
                l.add(u);
            }
        }
        return l.toArray(new URL[l.size()]);
    }

    private static URL[] getNonDirUrls(URL[] urls) {
        List<URL> l = new ArrayList<URL>();
        for (URL u : urls) {
            if (!u.toString().endsWith("/")) {
                l.add(u);
            }
        }
        return l.toArray(new URL[l.size()]);
    }
}
