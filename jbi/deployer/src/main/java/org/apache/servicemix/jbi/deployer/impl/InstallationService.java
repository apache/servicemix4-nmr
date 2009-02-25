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
import java.util.Properties;

import javax.jbi.management.DeploymentException;
import javax.jbi.management.InstallationServiceMBean;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.handler.Transformer;
import org.apache.servicemix.jbi.deployer.utils.ManagementSupport;

public class InstallationService implements InstallationServiceMBean {

    private static final Log LOG = LogFactory.getLog(InstallationService.class);

    private Deployer deployer;

    /**
     * Load the installer for a new component from a component installation
     * package.
     *
     * @param installJarURL -
     *                      URL locating a jar file containing a JBI Installable
     *                      Component.
     * @return - the JMX ObjectName of the InstallerMBean loaded from
     *         installJarURL.
     */
    public synchronized ObjectName loadNewInstaller(String installJarURL) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loading new installer from " + installJarURL);
            }
            ComponentInstaller installer = doLoadNewInstaller(installJarURL, false);
            return installer.getObjectName();
        } catch (Throwable t) {
            LOG.error("Deployment failed", t);
            if (t instanceof Error) {
                throw (Error) t;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException("Deployment failed: " + t.getMessage());
            }
        }
    }

    /**
     * Load the InstallerMBean for a previously installed component.
     *
     * @param componentName -
     *                       the component name identifying the installer to load.
     * @return - the JMX ObjectName of the InstallerMBean loaded from an
     *         existing installation context.
     */
    public ObjectName loadInstaller(String componentName) {
        try {
            ComponentInstaller installer = getComponentInstaller(componentName);
            if (installer != null) {
                installer.register();
            }
            return installer != null ? installer.getObjectName() : null;
        } catch (Exception e) {
            String errStr = "Error loading installer: " + componentName;
            LOG.error(errStr, e);
            return null;
        }
    }

    /**
     * Unload a JBI Installable Component installer.
     *
     * @param componentName -
     *                      the component name identifying the installer to unload.
     * @param isToBeDeleted -
     *                      true if the component is to be deleted as well.
     * @return - true if the operation was successful, otherwise false.
     */
    public boolean unloadInstaller(String componentName, boolean isToBeDeleted) {
        boolean result = false;
        try {
            ComponentInstaller installer = getComponentInstaller(componentName);
            if (installer != null) {
                installer.unregister();
                if (isToBeDeleted) {
                    installer.uninstall();
                }
                result = true;
            }
        } catch (Exception e) {
            String errStr = "Error unloading installer: " + componentName;
            LOG.error(errStr, e);
        }
        return result;
    }

    protected ComponentInstaller getComponentInstaller(String name) {
        Component component = deployer.getComponent(name);
        return (ComponentInstaller) deployer.getInstaller(component);
    }

    /**
     * Install a shared library jar.
     *
     * @param location URI locating a jar file containing a shared library.
     * @return the name of the shared library loaded from aSharedLibURI.
     */
    public String installSharedLibrary(String location) {
        try {
            return doInstallSharedLibrary(location);
        } catch (DeploymentException e) {
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            throw ManagementSupport.failure("installSharedibrary", location, e);
        }
    }

    /**
     * Uninstall a shared library.
     *
     * @param aSharedLibName -
     *                       the name of the shared library to uninstall.
     * @return - true iff the uninstall was successful.
     */
    public boolean uninstallSharedLibrary(String aSharedLibName) {
        try {
            doUninstallSharedLibrary(aSharedLibName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Install an archive
     *
     * @param location
     * @param props
     * @param autoStart
     * @throws DeploymentException
     */
    public void install(String location, Properties props, boolean autoStart) throws Exception {
        ComponentInstaller installer = doLoadNewInstaller(location, autoStart);
        try {
            installer.configure(props);
            installer.install();
        } catch (Exception e) {
            installer.uninstall(true);
            throw e;
        }
    }

    public String doInstallSharedLibrary(String location) throws Exception {
        File jarfile = new File(location);
        if (jarfile.exists()) {
            Descriptor desc = Transformer.getDescriptor(jarfile);
            if (desc.getSharedLibrary() == null) {
                throw new DeploymentException("JBI descriptor is not a shared library descriptor");
            }
            String slName = desc.getSharedLibrary().getIdentification().getName();
            LOG.info("Installing shared library " + slName);
            if (deployer.getSharedLibrary(slName) != null) {
                throw new DeploymentException("ShareLib " + slName + " is already installed");
            }
            SharedLibraryInstaller installer = new SharedLibraryInstaller(deployer, desc, jarfile, false);
            installer.installBundle();
            installer.init();
            installer.install();
            return slName;
        } else {
            throw new Exception("Unable to find shared library " + location);
        }
    }

    public void doUninstallSharedLibrary(String aSharedLibName) throws Exception {
        SharedLibrary library = deployer.getSharedLibrary(aSharedLibName);
        if (library == null) {
            throw ManagementSupport.failure("uninstall", "Shared library has not been installed: " + aSharedLibName);
        }
        AbstractInstaller installer = deployer.getInstaller(library);
        if (installer == null) {
            throw ManagementSupport.failure("uninstall", "Could not find shared library installer: " + aSharedLibName);
        }
        installer.uninstall(false);
    }

    protected ComponentInstaller doLoadNewInstaller(String location, boolean autoStart) throws Exception {
        File jarfile = new File(location);
        if (jarfile.exists()) {
            Descriptor desc = Transformer.getDescriptor(jarfile);
            if (desc.getComponent() != null) {
                String componentName = desc.getComponent().getIdentification().getName();
                ComponentInstaller installer = getComponentInstaller(componentName);
                if (installer == null) {
                    installer = new ComponentInstaller(deployer, desc, jarfile, autoStart);
                    installer.installBundle();
                    installer.init();
                    installer.register();
                    return installer;
                    // TODO: if something goes wrong, we need to clean up what has been done so far
                } else {
                    throw new Exception("An installer already exists for " + componentName);
                }
            } else {
                throw new Exception("JBI descriptor is not a component descriptor");
            }
        } else {
            throw new Exception("Unable to find component " + location);
        }
    }

    public Deployer getDeployer() {
        return deployer;
    }

    public void setDeployer(Deployer deployer) {
        this.deployer = deployer;
    }

}
