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
package org.apache.servicemix.jbi.management;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.JBIException;
import javax.jbi.management.DeploymentException;
import javax.jbi.management.InstallationServiceMBean;
import javax.management.Attribute;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.handler.Transformer;

public class InstallationService implements InstallationServiceMBean {

    private static final Log LOG = LogFactory.getLog(InstallationService.class);

    private Map<String, Installer> installers = new ConcurrentHashMap<String, Installer>();

    private Map<String, Installer> nonLoadedInstallers = new ConcurrentHashMap<String, Installer>();

    private AdminService adminService;
    
    private NamingStrategy namingStrategy;
    private ManagementAgent managementAgent;
    
    /**
     * Load the installer for a new component from a component installation
     * package.
     *
     * @param installJarURL -
     *            URL locating a jar file containing a JBI Installable
     *            Component.
     * @return - the JMX ObjectName of the InstallerMBean loaded from
     *         installJarURL.
     */
    public synchronized ObjectName loadNewInstaller(String installJarURL) {
    	try {
            ObjectName result = null;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loading new installer from " + installJarURL);
            }
            File jarfile = new File(installJarURL);
            if (jarfile.exists()) {
                Descriptor desc = Transformer.getDescriptor(jarfile);
                if (desc != null && desc.getComponent() != null) {
                    String componentName = desc.getComponent().getIdentification().getName();
                    if (!installers.containsKey(componentName)) {
                        Installer installer = doInstallArchive(desc, jarfile);
                        if (installer != null) {
                            result = installer.getObjectName();
                            installers.put(componentName, installer);
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
            return result;
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

    private Installer doInstallArchive(Descriptor desc, File jarfile) throws Exception {
		return new Installer(new InstallationContextImpl(desc.getComponent(), getNamingStrategy(), getManagementAgent()), 
			jarfile, getAdminService());
	}

	/**
     * Load the InstallerMBean for a previously installed component.
     *
     * @param aComponentName -
     *            the component name identifying the installer to load.
     * @return - the JMX ObjectName of the InstallerMBean loaded from an
     *         existing installation context.
     */
    public ObjectName loadInstaller(String aComponentName) {
        Installer installer = installers.get(aComponentName);
        if (installer == null) {
            installer = nonLoadedInstallers.get(aComponentName);
            if (installer != null) {
                try {
                    // create an MBean for the installer
                    ObjectName objectName = getNamingStrategy().createCustomComponentMBeanName("Installer", aComponentName);
                    installer.setObjectName(objectName);
                    getManagementAgent().register(installer, objectName);
                } catch (Exception e) {
                    throw new RuntimeException("Could not load installer", e);
                }
                return installer.getObjectName();
            }
        }
        return null;
    }

    /**
     * Unload a JBI Installable Component installer.
     *
     * @param componentName -
     *            the component name identifying the installer to unload.
     * @param isToBeDeleted -
     *            true if the component is to be deleted as well.
     * @return - true if the operation was successful, otherwise false.
     */
    public boolean unloadInstaller(String componentName, boolean isToBeDeleted) {
    	boolean result = false;
        try {
        	Installer installer = installers.remove(componentName);
            result = installer != null;
            if (result) {
            	getManagementAgent().unregister(installer.getObjectName());
                if (isToBeDeleted) {
                    installer.uninstall();
                } else {
                    nonLoadedInstallers.put(componentName, installer);
                }
            }
        } catch (Exception e) {
            String errStr = "Problem shutting down Component: " + componentName;
            LOG.error(errStr, e);
        } finally {
        }
        return result;
    }

    /**
     * Install a shared library jar.
     *
     * @param aSharedLibURI -
     *            URI locating a jar file containing a shared library.
     * @return - the name of the shared library loaded from aSharedLibURI.
     */
    public String installSharedLibrary(String aSharedLibURI) {
    	return ManagementSupport.createSuccessMessage("to be done");
    }

    /**
     * Uninstall a shared library.
     *
     * @param aSharedLibName -
     *            the name of the shared library to uninstall.
     * @return - true iff the uninstall was successful.
     */
    public boolean uninstallSharedLibrary(String aSharedLibName) {
        // TODO
    	return false;
        
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
    	File jarfile = new File(location);
        if (jarfile.exists()) {
        	Descriptor desc = null;
			try {
				desc = Transformer.getDescriptor(jarfile);
			} catch (Exception e) {
				LOG.error("install component failed", e);
				throw new DeploymentException("install component failed", e);
			}
            if (desc != null) {
                if (desc.getComponent() == null) {
                    throw new DeploymentException("JBI descriptor is not a component descriptor");
                }
                install(jarfile, props, desc, autoStart);
            } else {
                throw new DeploymentException("Could not find JBI descriptor");
            }
        } else {
            throw new DeploymentException("Could not find JBI descriptor");
        }
    }
    
    /**
     * Install an archive
     * 
     * @param tmpDir
     * @param root
     * @param autoStart
     * @throws DeploymentException
     */
    protected void install(File jarfile, Properties props, Descriptor desc, boolean autoStart) throws DeploymentException {
        if (desc.getComponent() != null) {
            String componentName = desc.getComponent().getIdentification().getName();
            if (installers.containsKey(componentName)) {
                throw new DeploymentException("Component " + componentName + " is already installed");
            }
            Installer installer = null;
			try {
				installer = doInstallArchive(desc, jarfile);
			} catch (Exception e1) {
				LOG.error("create installer for component " + desc.getComponent().getIdentification().getName()
						+ " failed", e1);
				throw new DeploymentException("create installer for component " + desc.getComponent().getIdentification().getName()
						+ " failed", e1);
			}
            if (installer != null) {
                try {
                    if (props != null && props.size() > 0) {
                        ObjectName on = installer.getInstallerConfigurationMBean();
                        if (on == null) {
                            LOG.warn("Could not find installation configuration MBean. Installation properties will be ignored.");
                        } else {
                            MBeanServer mbs = getManagementAgent().getMbeanServer();
                            for (Iterator it = props.keySet().iterator(); it.hasNext();) {
                                String key = (String) it.next();
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
                } catch (JBIException e) {
                    throw new DeploymentException(e);
                }
                installers.put(componentName, installer);
            }
        }
    }

	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}

	public AdminService getAdminService() {
		return adminService;
	}

	public void setNamingStrategy(NamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	public void setManagementAgent(ManagementAgent managementAgent) {
		this.managementAgent = managementAgent;
	}

	public ManagementAgent getManagementAgent() {
		return managementAgent;
	}
	
}
