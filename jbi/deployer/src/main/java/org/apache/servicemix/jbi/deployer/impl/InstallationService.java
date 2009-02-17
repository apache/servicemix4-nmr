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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.management.DeploymentException;
import javax.jbi.management.InstallationServiceMBean;
import javax.management.Attribute;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.NamingStrategy;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.handler.Transformer;
import org.apache.servicemix.jbi.deployer.utils.QueryUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.BundleContextAware;

public class InstallationService implements InstallationServiceMBean, BundleContextAware {

    private static final Log LOG = LogFactory.getLog(InstallationService.class);

    private Map<String, ComponentInstaller> componentInstallers = new ConcurrentHashMap<String, ComponentInstaller>();

    private Map<String, SharedLibraryInstaller> sharedLibinstallers = new ConcurrentHashMap<String, SharedLibraryInstaller>();

    private Deployer deployer;
    private BundleContext bundleContext;

    private NamingStrategy namingStrategy;
    private ManagementAgent managementAgent;

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
            ComponentInstaller installer = componentInstallers.get(componentName);
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
            ComponentInstaller installer = isToBeDeleted ? componentInstallers.remove(componentName)
                                                         : componentInstallers.get(componentName);
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

    /**
     * Install a shared library jar.
     *
     * @param location URI locating a jar file containing a shared library.
     * @return the name of the shared library loaded from aSharedLibURI.
     */
    public String installSharedLibrary(String location) {
        File jarfile = new File(location);
        String slName = null;
        try {
            if (jarfile.exists()) {
                Descriptor desc;
                try {
                    desc = Transformer.getDescriptor(jarfile);
                } catch (Exception e) {
                    LOG.error("install sharedLib failed", e);
                    throw new DeploymentException("install sharedLib failed", e);
                }
                if (desc != null) {
                    if (desc.getSharedLibrary() == null) {
                        throw new DeploymentException("JBI descriptor is not a sharedLib descriptor");
                    }
                    slName = desc.getSharedLibrary().getIdentification().getName();
                    LOG.info("Install ShareLib " + slName);
                    if (sharedLibinstallers.containsKey(slName)) {
                        ServiceReference ref = QueryUtils.getSharedLibraryServiceReference(bundleContext, "(" + Deployer.NAME + "=" + slName + ")");
                        if (ref == null) {
                            //the Shared lib bundle uninstalled from console using osgi/uninstall
                            sharedLibinstallers.remove(slName);
                        } else {
                            throw new DeploymentException("ShareLib " + slName + " is already installed");
                        }
                    }
                    SharedLibraryInstaller slInstaller = new SharedLibraryInstaller(bundleContext, slName);
                    slInstaller.install(location);
                    sharedLibinstallers.put(slName, slInstaller);

                    return slName;
                } else {
                    throw new DeploymentException("Could not find JBI descriptor");
                }
            } else {
                throw new DeploymentException("Could not find sharedLib");
            }
        } catch (Exception e) {
            LOG.error("install SharedLib failed", e);
        }
        return slName;
    }

    /**
     * Uninstall a shared library.
     *
     * @param aSharedLibName -
     *                       the name of the shared library to uninstall.
     * @return - true iff the uninstall was successful.
     */
    public boolean uninstallSharedLibrary(String aSharedLibName) {
        boolean result = false;
        try {
            SharedLibraryInstaller installer = sharedLibinstallers.remove(aSharedLibName);
            result = installer != null;
            if (result) {
                //the SL installed from Mbean
                installer.uninstall();

            } else {
                //the SL not installed from Mbeans, so check the SL bundle directly
                ServiceReference ref = QueryUtils.getSharedLibraryServiceReference(bundleContext, "(" + Deployer.NAME + "=" + aSharedLibName + ")");
                if (ref != null) {
                    Bundle bundle = ref.getBundle();
                    if (bundle != null) {
                        bundle.stop();
                        bundle.uninstall();
                        result = true;
                    }
                }
            }
        } catch (Exception e) {
            String errStr = "Problem uninstall SL: " + aSharedLibName;
            LOG.error(errStr, e);
        } finally {
        }
        return result;
    }

    protected ComponentInstaller doLoadNewInstaller(String installJarURL, boolean autoStart) throws Exception {
        File jarfile = new File(installJarURL);
        if (jarfile.exists()) {
            Descriptor desc = Transformer.getDescriptor(jarfile);
            if (desc != null && desc.getComponent() != null) {
                String componentName = desc.getComponent().getIdentification().getName();
                if (!componentInstallers.containsKey(componentName)) {
                    ComponentInstaller installer;
                    try {
                        installer = new ComponentInstaller(deployer, desc.getComponent(), jarfile);
                        deployer.setComponentInstaller(installer);
                        installer.deployBundle();
                        installer.setAutoStart(autoStart);
                        installer.init();
                        installer.register();
                        componentInstallers.put(componentName, installer);
                        return installer;
                    } finally {
                        deployer.setComponentInstaller(null);
                    }
                } else {
                    throw new RuntimeException("An installer already exists for " + componentName);
                }
            } else {
                throw new RuntimeException("Could not find Component from: " + installJarURL);
            }
        } else {
            throw new RuntimeException("location: " + installJarURL + " isn't valid");
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
    public void install(String location, Properties props, boolean autoStart) throws DeploymentException {
        ComponentInstaller installer = null;
        boolean success = false;
        try {
            installer = doLoadNewInstaller(location, autoStart);
            if (props != null && props.size() > 0) {
                ObjectName on = installer.getInstallerConfigurationMBean();
                if (on == null) {
                    LOG.warn("Could not find installation configuration MBean. Installation properties will be ignored.");
                } else {
                    MBeanServer mbs = getManagementAgent().getMbeanServer();
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
            installer.install();
            success = true;
        } catch (Exception e) {
            throw new DeploymentException(e);
        } finally {
            if (installer != null) {
                unloadInstaller(installer.getName(), !success);
            }
        }
    }

    public void setNamingStrategy(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public Deployer getDeployer() {
        return deployer;
    }

    public void setDeployer(Deployer deployer) {
        this.deployer = deployer;
    }

    public void setManagementAgent(ManagementAgent managementAgent) {
        this.managementAgent = managementAgent;
    }

    public ManagementAgent getManagementAgent() {
        return managementAgent;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public boolean containsSharedLibrary(String sharedLibraryName) {
        // TODO: remove this method
        return sharedLibinstallers.containsKey(sharedLibraryName);
    }

}
