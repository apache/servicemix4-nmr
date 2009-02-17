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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import javax.jbi.JBIException;
import javax.jbi.component.Bootstrap;
import javax.jbi.management.DeploymentException;
import javax.jbi.management.InstallerMBean;
import javax.management.JMException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.classloader.OsgiMultiParentClassLoader;
import org.apache.servicemix.jbi.deployer.descriptor.ComponentDesc;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryList;
import org.apache.servicemix.jbi.deployer.utils.FileUtil;
import org.apache.servicemix.jbi.deployer.utils.QueryUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.prefs.BackingStoreException;
import org.springframework.osgi.util.BundleDelegatingClassLoader;

public class ComponentInstaller extends AbstractInstaller implements InstallerMBean {

    private final Deployer deployer;
    private final InstallationContextImpl installationContext;
    private final File jbiArtifact;
    private final File installRoot;
    private ObjectName objectName;
    private ObjectName extensionMBeanName;

    private boolean initialized;
    private Bootstrap bootstrap;


    public ComponentInstaller(Deployer deployer, ComponentDesc componentDesc, File jbiArtifact) throws Exception {
        this.deployer = deployer;
        this.bundleContext = deployer.getBundleContext();
        this.installationContext = new InstallationContextImpl(componentDesc, deployer.getEnvironment(),
                                                               deployer.getNamingStrategy(), deployer.getManagementAgent());
        this.jbiArtifact = jbiArtifact;
        installRoot = new File(System.getProperty("servicemix.base"), "data/jbi/" + installationContext.getComponentName() + "/install");
        installRoot.mkdirs();
        this.installationContext.setInstallRoot(installRoot);
    }

    /**
     * Get the installation root directory path for this BC or SE.
     *
     * @return the full installation path of this component.
     */
    public String getInstallRoot() {
        return installationContext.getInstallRoot();
    }

    public void register() throws JMException {
        deployer.getManagementAgent().register(new StandardMBean(this, InstallerMBean.class), getObjectName());
    }

    public void unregister() throws JMException {
        deployer.getManagementAgent().unregister(getObjectName());
    }

    public void init() throws Exception {
        // Extract component (needed to feed the installRoot)
        // Few components actually use this, but Ode is one of them
        extractBundle(installRoot, getBundle(), "/");
        initBootstrap();
    }

    /**
     * Install a BC or SE.
     *
     * @return JMX ObjectName representing the ComponentLifeCycle for the installed component, or null if the
     *         installation did not complete.
     * @throws javax.jbi.JBIException if the installation fails.
     */
    public ObjectName install() throws JBIException {
        try {
            if (isInstalled()) {
                throw new DeploymentException("Component is already installed");
            }
            initBootstrap();
            bootstrap.onInstall();
            try {
                try {
                    initializePreferences();
                } catch (BackingStoreException e) {
                    LOGGER.warn("Error initializing persistent state for component: " + installationContext.getComponentName(), e);
                }
                ObjectName name = initComponent();
                cleanUpBootstrap();
                installationContext.setInstall(false);
                return name;
            } catch (Exception e) {
                cleanUpBootstrap();
                throw e;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new JBIException(e);
        }
    }

    public void deployBundle() throws Exception {
        deployFile(jbiArtifact.getCanonicalPath());
    }


    /**
     * Determine whether or not the component is installed.
     *
     * @return true if this component is currently installed, false if not.
     */
    public boolean isInstalled() {
        return !installationContext.isInstall();
    }

    /**
     * Uninstall a BC or SE. This completely removes the component from the JBI system.
     *
     * @throws javax.jbi.JBIException if the uninstallation fails.
     */
    public void uninstall() throws javax.jbi.JBIException {
        // TODO: check component status
        // TODO: we should always uninstall the bundle
        // the component must not be started and not have any SUs deployed
        if (!isInstalled()) {
            throw new DeploymentException("Component is not installed");
        }
        String componentName = installationContext.getComponentName();
        try {
            Bundle bundle = getBundle();

            if (bundle == null) {
                LOGGER.warn("Could not find Bundle for component: " + componentName);
            } else {
                bundle.stop();
                bundle.uninstall();
                try {
                    deletePreferences();
                } catch (BackingStoreException e) {
                    LOGGER.warn("Error cleaning persistent state for component: " + componentName, e);
                }
            }
        } catch (BundleException e) {
            LOGGER.error("failed to uninstall component: " + componentName, e);
            throw new JBIException(e);
        }
    }

    /**
     * Get the installer configuration MBean name for this component.
     *
     * @return the MBean object name of the Installer Configuration MBean.
     * @throws javax.jbi.JBIException if the component is not in the LOADED state or any error occurs during processing.
     */
    public ObjectName getInstallerConfigurationMBean() throws javax.jbi.JBIException {
        return this.extensionMBeanName;
    }

    /**
     * @return Returns the objectName.
     */
    public ObjectName getObjectName() {
        if (objectName == null) {
            objectName = deployer.getNamingStrategy().createCustomComponentMBeanName("Installer", getName());
        }
        return objectName;
    }

    /**
     * @param objectName The objectName to set.
     */
    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    public String getName() {
        return installationContext.getComponentName();
    }

    protected ClassLoader createClassLoader(Bundle bundle, String name, String[] classPathNames, boolean parentFirst, SharedLibraryList[] sharedLibs) {
        // Create parents classloaders
        ClassLoader[] parents;
        if (sharedLibs != null) {
            parents = new ClassLoader[sharedLibs.length + 2];
            for (int i = 0; i < sharedLibs.length; i++) {
                parents[i + 2] = getSharedLibraryClassLoader(sharedLibs[i].getName());
            }
        } else {
            parents = new ClassLoader[2];
        }
        parents[0] = BundleDelegatingClassLoader.createBundleClassLoaderFor(getBundleContext().getBundle(0));
        parents[1] = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, getClass().getClassLoader());

        // Create urls
        URL[] urls = new URL[classPathNames.length];
        for (int i = 0; i < classPathNames.length; i++) {
            urls[i] = bundle.getResource(classPathNames[i]);
            if (urls[i] == null) {
                throw new IllegalArgumentException("SharedLibrary classpath entry not found: '" + classPathNames[i] + "'");
            }
            Enumeration en = bundle.findEntries(classPathNames[i], null, false);
            if (en != null && en.hasMoreElements()) {
                try {
                    urls[i] = new URL(urls[i].toString() + "/");
                } catch (MalformedURLException e) {
                    // Ignore
                }
            }
        }

        // Create classloader
        return new OsgiMultiParentClassLoader(
                bundle,
                name,
                urls,
                parents,
                !parentFirst,
                new String[]{"javax.xml.bind"},
                new String[]{"java.", "javax."});
    }

    private ClassLoader getSharedLibraryClassLoader(String name) {
        SharedLibrary sa = QueryUtils.getSharedLibrary(getBundleContext(), name);
        if (sa != null) {
            return sa.getClassLoader();
        }
        throw new IllegalStateException("Unable to retrieve class loader for shared library: " + name);
    }

    private void initBootstrap() throws DeploymentException {
        try {
            if (!initialized) {
                // Unregister a previously registered extension mbean,
                // in case the bootstrap has not done it
                try {
                    if (extensionMBeanName != null) {
                        deployer.getManagementAgent().unregister(extensionMBeanName);
                    }
                } catch (JMException e) {
                    // ignore
                }
                if (bootstrap == null) {
                    bootstrap = createBootstrap();
                }
                // Init bootstrap
                ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(bootstrap.getClass().getClassLoader());
                    bootstrap.init(this.installationContext);
                    extensionMBeanName = bootstrap.getExtensionMBeanName();
                } finally {
                    Thread.currentThread().setContextClassLoader(oldCl);
                }
                initialized = true;
            }
        } catch (JBIException e) {
            LOGGER.error("Could not initialize bootstrap", e);
            throw new DeploymentException(e);
        }
    }

    protected void cleanUpBootstrap() throws DeploymentException {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(bootstrap.getClass().getClassLoader());
            bootstrap.cleanUp();
        } catch (JBIException e) {
            LOGGER.error("Could not initialize bootstrap", e);
            throw new DeploymentException(e);
        } finally {
            initialized = false;
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    private Bootstrap createBootstrap() throws DeploymentException {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        ComponentDesc descriptor = installationContext.getDescriptor();
        try {
            ClassLoader cl = createClassLoader(
                    getBundle(),
                    installationContext.getInstallRoot(),
                    descriptor.getBootstrapClassPath().getPathElements(),
                    descriptor.isBootstrapClassLoaderDelegationParentFirst(),
                    null);
            Thread.currentThread().setContextClassLoader(cl);
            Class bootstrapClass = cl.loadClass(descriptor.getBootstrapClassName());
            return (Bootstrap) bootstrapClass.newInstance();
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class not found: " + descriptor.getBootstrapClassName(), e);
            throw new DeploymentException(e);
        } catch (InstantiationException e) {
            LOGGER.error("Could not instantiate : " + descriptor.getBootstrapClassName(), e);
            throw new DeploymentException(e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Illegal access on: " + descriptor.getBootstrapClassName(), e);
            throw new DeploymentException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    private ObjectName initComponent() throws Exception {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            ComponentDesc componentDesc = installationContext.getDescriptor();
            ClassLoader classLoader = createClassLoader(
                    getBundle(),
                    componentDesc.getIdentification().getName(),
                    (String[]) installationContext.getClassPathElements().toArray(new String[installationContext.getClassPathElements().size()]),
                    componentDesc.isComponentClassLoaderDelegationParentFirst(),
                    componentDesc.getSharedLibraries());
            Thread.currentThread().setContextClassLoader(classLoader);
            Class clazz = classLoader.loadClass(componentDesc.getComponentClassName());
            javax.jbi.component.Component innerComponent = (javax.jbi.component.Component) clazz.newInstance();
            Component component = deployer.registerComponent(getBundle(), componentDesc, innerComponent);
            ObjectName name = deployer.getNamingStrategy().getObjectName(component);
            deployer.getManagementAgent().register(new StandardMBean(component, Component.class), name, true);
            return name;
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    private void extractBundle(File installRoot, Bundle bundle, String path) throws IOException {
        Enumeration e = bundle.getEntryPaths(path);
        while (e != null && e.hasMoreElements()) {
            String entry = (String) e.nextElement();
            File fout = new File(installRoot, entry);
            if (entry.endsWith("/")) {
                fout.mkdirs();
                extractBundle(installRoot, bundle, entry);
            } else {
                InputStream in = bundle.getEntry(entry).openStream();
                OutputStream out = new FileOutputStream(fout);
                try {
                    FileUtil.copyInputStream(in, out);
                } finally {
                    in.close();
                    out.close();
                }
            }
        }
    }

}
