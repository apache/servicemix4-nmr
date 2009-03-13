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
import java.util.Properties;

import javax.jbi.JBIException;
import javax.jbi.component.Bootstrap;
import javax.jbi.management.DeploymentException;
import javax.jbi.management.InstallerMBean;
import javax.jbi.management.LifeCycleMBean;
import javax.management.Attribute;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.artifacts.ComponentImpl;
import org.apache.servicemix.jbi.deployer.descriptor.ComponentDesc;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryList;
import org.apache.servicemix.jbi.deployer.utils.FileUtil;
import org.apache.servicemix.jbi.deployer.utils.ManagementSupport;
import org.apache.xbean.classloader.MultiParentClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;
import org.springframework.osgi.util.BundleDelegatingClassLoader;

public class ComponentInstaller extends AbstractInstaller implements InstallerMBean {

    private InstallationContextImpl installationContext;
    private ObjectName objectName;
    private ObjectName extensionMBeanName;
    private boolean initialized;
    private Bootstrap bootstrap;
    private javax.jbi.component.Component innerComponent;


    public ComponentInstaller(Deployer deployer, Descriptor descriptor, File jbiArtifact, boolean autoStart) throws Exception {
        super(deployer, descriptor, jbiArtifact, autoStart);
        this.installRoot = new File(System.getProperty("servicemix.base"), "data/jbi/" + getName() + "/install");
        this.installRoot.mkdirs();
        this.installationContext = new InstallationContextImpl(descriptor.getComponent(), deployer.getEnvironment(),
                                                               deployer.getNamingStrategy(), deployer.getManagementAgent());
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

    public String getName() {
        return descriptor.getComponent().getIdentification().getName();
    }

    public javax.jbi.component.Component getInnerComponent() {
        return innerComponent;
    }

    public void setInnerComponent(javax.jbi.component.Component innerComponent) {
        this.innerComponent = innerComponent;
    }

    public void init() throws Exception {
        // Check requirements
        if (descriptor.getComponent().getSharedLibraries() != null) {
            for (SharedLibraryList sl : descriptor.getComponent().getSharedLibraries()) {
                if (deployer.getSharedLibrary(sl.getName()) == null) {
                    throw new PendingException(bundle, "SharedLibrary not installed: " + sl.getName());
                }
            }
        }
        // Extract bundle
        super.init();
        // Init bootstrap
        if (isModified) {
            initBootstrap();
        }
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
            if (isModified) {
                if (isInstalled()) {
                    throw new DeploymentException("Component is already installed");
                }
                initBootstrap();
                if (bootstrap != null) {
                    bootstrap.onInstall();
                }
                try {
                    ObjectName name = initComponent();
                    cleanUpBootstrap();
                    installationContext.setInstall(false);
                    postInstall();
                    return name;
                } catch (Exception e) {
                    cleanUpBootstrap();
                    throw e;
                }
            } else {
                ObjectName name = initComponent();
                postInstall();
                return name;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new JBIException(e);
        }
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
        try {
            uninstall(false);
        } catch (JBIException e) {
            throw e;
        } catch (Exception e) {
            throw new JBIException(e);
        }
    }

    public void stop(boolean force) throws Exception {
        ComponentImpl comp = deployer.getComponent(getName());
        if (comp == null && !force) {
            throw ManagementSupport.failure("uninstallComponent", "Component '" + getName() + "' is not installed.");
        }
        // Check component state is shutdown
        if (comp != null && !LifeCycleMBean.SHUTDOWN.equals(comp.getCurrentState())) {
            if (!force) {
                throw ManagementSupport.failure("uninstallComponent", "Component '" + getName() + "' is not shut down.");
            }
            if (LifeCycleMBean.STARTED.equals(comp.getCurrentState())) {
                comp.stop(false);
            }
            if (LifeCycleMBean.STOPPED.equals(comp.getCurrentState())) {
                comp.shutDown(false, force);
            }
        }
    }

    public void uninstall(boolean force) throws Exception {
        // Shutdown component
        stop(force);
        // Retrieve component
        ComponentImpl comp = deployer.getComponent(getName());
        if (comp == null && !force) {
            throw ManagementSupport.failure("uninstallComponent", "Component '" + getName() + "' is not installed.");
        }
        // TODO: if there is any SA deployed onto this component, undeploy the SA and put it in a pending state
        // Bootstrap stuff
        if (hasBootstrap()) {
            try {
                initBootstrap();
                bootstrap.init(this.installationContext);
                bootstrap.getExtensionMBeanName();
                bootstrap.onUninstall();
                cleanUpBootstrap();
                installationContext.setInstall(true);
            } catch (Exception e) {
                cleanUpBootstrap();
                throw e;
            }
        }
        // Unregister component
        deployer.unregisterComponent(comp);
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

    protected ClassLoader createClassLoader(Bundle bundle, String name, String[] classPathNames, boolean parentFirst, SharedLibrary[] sharedLibs) {
        // Create parents classloaders
        ClassLoader[] parents;
        if (sharedLibs != null) {
            parents = new ClassLoader[sharedLibs.length + 2];
            for (int i = 0; i < sharedLibs.length; i++) {
                parents[i] = sharedLibs[i].getClassLoader();
            }
        } else {
            parents = new ClassLoader[2];
        }
        parents[parents.length - 2] = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, getClass().getClassLoader());
        parents[parents.length - 1] = BundleDelegatingClassLoader.createBundleClassLoaderFor(getBundleContext().getBundle(0));

        // Create urls
        List<URL> urls = new ArrayList<URL>();
        for (int i = 0; i < classPathNames.length; i++) {
            File f = new File(installRoot, classPathNames[i]);
            if (!f.exists()) {
                LOGGER.warn("Component classpath entry not found: '" + classPathNames[i] + "'");
            }
            try {
                urls.add(f.getCanonicalFile().toURL());
            } catch (IOException e) {
                throw new IllegalArgumentException("Component classpath entry not found: '" + classPathNames[i] + "'");
            }
        }

        // Create classloader
        return new MultiParentClassLoader(
                name,
                urls.toArray(new URL[urls.size()]),
                parents,
                !parentFirst,
                new String[0],
                new String[]{"java.", "javax."});
    }

    private void initBootstrap() throws DeploymentException {
        if (!hasBootstrap()) {
            return;
        }
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
        if (bootstrap != null) {
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
    }

    private boolean hasBootstrap() {
        ComponentDesc descriptor = installationContext.getDescriptor();
        return descriptor.getBootstrapClassName() != null;
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
        ComponentDesc componentDesc = installationContext.getDescriptor();
        List<SharedLibrary> libs = new ArrayList<SharedLibrary>();
        if (componentDesc.getSharedLibraries() != null) {
            for (SharedLibraryList sll : componentDesc.getSharedLibraries()) {
                SharedLibrary lib = deployer.getSharedLibrary(sll.getName());
                if (lib == null) {
                    // TODO: throw exception here ?
                } else {
                    libs.add(lib);
                }
            }
        }
        SharedLibrary[] aLibs = libs.toArray(new SharedLibrary[libs.size()]);

        if (innerComponent == null) {
            ClassLoader classLoader = createClassLoader(
                    getBundle(),
                    componentDesc.getIdentification().getName(),
                    (String[]) installationContext.getClassPathElements().toArray(new String[installationContext.getClassPathElements().size()]),
                    componentDesc.isComponentClassLoaderDelegationParentFirst(),
                    aLibs);
            ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                Class clazz = classLoader.loadClass(componentDesc.getComponentClassName());
                innerComponent = (javax.jbi.component.Component) clazz.newInstance();
            } finally {
                Thread.currentThread().setContextClassLoader(oldCl);
            }
        }
        Component component = deployer.registerComponent(getBundle(), componentDesc, innerComponent, aLibs);
        return deployer.getNamingStrategy().getObjectName(component);
    }

    public void configure(Properties props) throws Exception {
        if (props != null && props.size() > 0) {
            ObjectName on = getInstallerConfigurationMBean();
            if (on == null) {
                LOGGER.warn("Could not find installation configuration MBean. Installation properties will be ignored.");
            } else {
                MBeanServer mbs = deployer.getManagementAgent().getMbeanServer();
                for (Object o : props.keySet()) {
                    String key = (String) o;
                    String val = props.getProperty(key);
                    try {
                        mbs.setAttribute(on, new Attribute(key, val));
                    } catch (JMException e) {
                        throw new DeploymentException("Could not set installation property: (" + key + " = " + val, e);
                    }
                }
            }
        }
    }
}
