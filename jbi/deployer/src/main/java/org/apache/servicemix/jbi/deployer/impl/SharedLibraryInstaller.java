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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.jbi.JBIException;
import javax.management.ObjectName;

import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.descriptor.ClassPath;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryDesc;
import org.apache.servicemix.jbi.deployer.utils.FileUtil;
import org.apache.servicemix.jbi.deployer.utils.ManagementSupport;
import org.apache.xbean.classloader.MultiParentClassLoader;
import org.osgi.service.prefs.BackingStoreException;
import org.springframework.osgi.util.BundleDelegatingClassLoader;

public class SharedLibraryInstaller extends AbstractInstaller {

    public SharedLibraryInstaller(Deployer deployer, Descriptor descriptor, File jbiArtifact, boolean autoStart) {
        super(deployer, descriptor, jbiArtifact, autoStart);
        installRoot = new File(System.getProperty("servicemix.base"), "data/jbi/" + getName() + "/install");
        installRoot.mkdirs();
    }

    public String getName() {
        return descriptor.getSharedLibrary().getIdentification().getName();
    }

    public ObjectName install() throws JBIException {
        try {
            SharedLibrary sl = deployer.registerSharedLibrary(bundle, descriptor.getSharedLibrary(), createClassLoader());
            return deployer.getNamingStrategy().getObjectName(sl);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new JBIException(e);
        }
    }

    public void uninstall() throws javax.jbi.JBIException {
        try {
            uninstall(false);
        } catch (JBIException e) {
            throw e;
        } catch (Exception e) {
            throw new JBIException(e);
        }
    }

    public void stop(boolean force) throws Exception {
        // Nothing to do for shared libraries
    }

    public void uninstall(boolean force) throws Exception {
        // Shut down
        stop(force);
        // Retrieve shared library
        SharedLibrary library = deployer.getSharedLibrary(getName());
        if (library == null && !force) {
            throw ManagementSupport.failure("uninstallSharedLibrary", "SharedLibrary '" + getName() + "' is not installed.");
        }
        // Check that it is not used by a running component
        if (library.getComponents().length > 0 && !force) {
            StringBuilder sb = new StringBuilder();
            for (Component comp : library.getComponents()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(comp.getName());
            }
            throw ManagementSupport.failure("uninstallSharedLibrary", "Shared library " + getName() + " is in use by components " + sb.toString());
        }
        // Unregister library
        deployer.unregisterSharedLibrary(library);
        // Remove preferences
        try {
            deletePreferences();
        } catch (BackingStoreException e) {
            LOGGER.warn("Error cleaning persistent state for component: " + getName(), e);
        }
        // Uninstall bundle
        uninstallBundle();
        // Remove files
        FileUtil.deleteFile(installRoot);
    }

    protected ClassLoader createClassLoader() {
        SharedLibraryDesc library = descriptor.getSharedLibrary();
        // Make the current ClassLoader the parent
        ClassLoader parent = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, getClass().getClassLoader());
        boolean parentFirst = library.isParentFirstClassLoaderDelegation();
        ClassPath cp = library.getSharedLibraryClassPath();
        String[] classPathNames = cp.getPathElements();
        List<URL> urls = new ArrayList<URL>();
        for (String classPathName : classPathNames) {
            File f = new File(installRoot, classPathName);
            if (!f.exists()) {
                LOGGER.warn("Shared library classpath entry not found: '" + classPathName + "'");
            }
            try {
                urls.add(f.getCanonicalFile().toURL());
            } catch (IOException e) {
                throw new IllegalArgumentException("Shared library classpath entry not found: '" + classPathName + "'");
            }
        }
        return new MultiParentClassLoader(
                library.getIdentification().getName(),
                urls.toArray(new URL[urls.size()]),
                parent,
                !parentFirst,
                new String[0],
                new String[]{"java.", "javax."});
    }
}
