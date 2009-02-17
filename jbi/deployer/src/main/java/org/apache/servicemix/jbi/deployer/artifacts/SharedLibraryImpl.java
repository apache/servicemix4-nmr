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
package org.apache.servicemix.jbi.deployer.artifacts;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.classloader.OsgiMultiParentClassLoader;
import org.apache.servicemix.jbi.deployer.descriptor.ClassPath;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryDesc;
import org.osgi.framework.Bundle;
import org.springframework.osgi.util.BundleDelegatingClassLoader;

/**
 * SharedLibrary object
 */
public class SharedLibraryImpl implements SharedLibrary {

    protected final Log LOGGER = LogFactory.getLog(getClass());

    private SharedLibraryDesc library;
    private Bundle bundle;
    private ClassLoader classLoader;

    public SharedLibraryImpl(SharedLibraryDesc library, Bundle bundle) {
        this.library = library;
        this.bundle = bundle;
    }

    public String getName() {
        return library.getIdentification().getName();
    }

    public String getDescription() {
        return library.getIdentification().getDescription();
    }

    public String getVersion() {
        return library.getVersion();
    }

    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            // Make the current ClassLoader the parent
            ClassLoader parent = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, getClass().getClassLoader());
            boolean parentFirst = library.isParentFirstClassLoaderDelegation();
            ClassPath cp = library.getSharedLibraryClassPath();
            String[] classPathNames = cp.getPathElements();
            List<URL> urls = new ArrayList<URL>();
            for (String classPathName : classPathNames) {
                if (!".".equals(classPathName)) {
                    URL url = bundle.getResource(classPathName);
                    if (url == null) {
                        throw new IllegalArgumentException("SharedLibrary classpath entry not found: '" + classPathName + "'");
                    }
                    Enumeration en = bundle.findEntries(classPathName, null, false);
                    if (en != null && en.hasMoreElements()) {
                        try {
                            url = new URL(url.toString() + "/");
                        } catch (MalformedURLException e) {
                            // Ignore
                        }
                    }
                    urls.add(url);
                }
            }
            classLoader = new OsgiMultiParentClassLoader(
                    bundle,
                    library.getIdentification().getName(),
                    urls.toArray(new URL[urls.size()]),
                    parent,
                    !parentFirst,
                    new String[0],
                    new String[]{"java.", "javax."});
        }
        return classLoader;
    }
}
