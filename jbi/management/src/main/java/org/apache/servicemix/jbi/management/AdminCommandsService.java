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


import javax.jbi.JBIException;
import javax.jbi.management.LifeCycleMBean;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.impl.Deployer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.BundleContextAware;

public class AdminCommandsService implements AdminCommandsServiceMBean, BundleContextAware {

	private static final Log LOGGER = LogFactory.getLog(AdminCommandsService.class);
	
    private AdminService adminService;
    private InstallationService installationService;
    private BundleContext bundleContext;
    private DeploymentService deploymentService;
    

    protected interface ComponentCallback {
        void doWithComponent(Component component) throws JBIException;
    }

    protected void executeWithComponent(String name, ComponentCallback callback) throws JBIException {
        ServiceReference ref = getAdminService().getComponentServiceReference("(" + Deployer.NAME + "=" + name + ")");
        if (ref == null) {
            throw new JBIException("Component '" + name + "' not found");
        }
        Component component = (Component) bundleContext.getService(ref);
        try {
            callback.doWithComponent(component);
        } finally {
            bundleContext.ungetService(ref);
        }
    }

    /**
     * Install a JBI component (a Service Engine or Binding Component)
     *
     * @param file
     *            jbi component archive to install
     * @param props
     *            installation properties
     * @return
     */
    public String installComponent(String fileName, boolean deferException) throws Exception {
        try {
        	getInstallationService().install(fileName, null, false);
        	return ManagementSupport.createSuccessMessage("installComponent", fileName);
        } catch (Exception e) {
            throw ManagementSupport.failure("installComponent", fileName, e);
        }
    }

    
    
    /**
     * Uninstalls a previously install JBI Component (a Service Engine or
     * Binding Component)
     *
     * @param name
     * @return
     */
    public String uninstallComponent(String name) throws Exception {
		try {
			executeWithComponent(name, new ComponentCallback() {
				public void doWithComponent(Component comp) throws JBIException {
					try {
						if (comp == null) {
							throw ManagementSupport.failure(
									"uninstallComponent", "Component '"
											+ comp.getName()
											+ "' is not installed.");
						}
						if (!comp.getCurrentState().equals(
								LifeCycleMBean.SHUTDOWN)) {
							throw ManagementSupport.failure(
									"uninstallComponent", "Component '"
											+ comp.getName()
											+ "' is not shut down.");
						}
						boolean success = installationService.unloadInstaller(
								comp.getName(), true);
						if (!success) {
							throw new RuntimeException();
						}
					} catch (Exception e) {
						throw new JBIException(e);
					}

				}
			});
		} catch (Exception e) {
			throw ManagementSupport.failure("uninstallComponent", name, e);
		}
		return ManagementSupport.createSuccessMessage("uninstallComponent",
				name);
	}

    /**
	 * Installs a Shared Library.
	 * 
	 * @param file
	 * @return
	 */
    public String installSharedLibrary(String file, boolean deferException) throws Exception {
    	return null;
        /*if (deferException) {
            container.updateExternalArchive(file);
            return ManagementSupport.createSuccessMessage("installSharedLibrary", file);
        } else {
            return installationService.installSharedLibrary(file);
        }*/
    }

    /**
     * Uninstalls a previously installed Shared Library.
     *
     * @param name
     * @return
     */
    public String uninstallSharedLibrary(String name) throws Exception {
    	return ManagementSupport.createSuccessMessage("to be done");
    }

    /**
     * Starts a particular Component (Service Engine or Binding Component).
     *
     * @param name
     * @return
     */
    public String startComponent(String name) throws Exception {
        try {
            executeWithComponent(name, new ComponentCallback() {
                public void doWithComponent(Component component) throws JBIException {
                    component.start();
                }
            });
            return ManagementSupport.createSuccessMessage("startComponent", name);
        } catch (Exception e) {
            throw ManagementSupport.failure("startComponent", name, e);
        }
    }

    /**
     * Stops a particular Component (Service Engine or Binding Component).
     *
     * @param name
     * @return
     */
    public String stopComponent(String name) throws Exception {
        try {
            executeWithComponent(name, new ComponentCallback() {
                public void doWithComponent(Component component) throws JBIException {
                    component.stop();
                }
            });
            return ManagementSupport.createSuccessMessage("stopComponent", name);
        } catch (Exception e) {
            throw ManagementSupport.failure("stopComponent", name, e);
        }
    }

    /**
     * Shuts down a particular Component.
     *
     * @param name
     * @return
     */
    public String shutdownComponent(String name) throws Exception {
        try {
            executeWithComponent(name, new ComponentCallback() {
                public void doWithComponent(Component component) throws JBIException {
                    component.shutDown();
                }
            });
            return ManagementSupport.createSuccessMessage("shutdownComponent", name);
        } catch (Exception e) {
            throw ManagementSupport.failure("shutdownComponent", name, e);
        }
    }

    /**
     * Deploys a Service Assembly.
     *
     * @param file
     * @return
     */
    public String deployServiceAssembly(String file, boolean deferException) throws Exception {
    	return null;
        /*if (deferException) {
            container.updateExternalArchive(file);
            return ManagementSupport.createSuccessMessage("deployServiceAssembly", file);
        } else {
            return deploymentService.deploy(file);
        }*/
    }

    /**
     * Undeploys a previously deployed service assembly.
     *
     * @param name
     * @return
     */
    public String undeployServiceAssembly(String name) throws Exception {
        return getDeploymentService().undeploy(name);
    }

    /**
     * Starts a service assembly.
     *
     * @param name
     * @return
     */
    public String startServiceAssembly(String name) throws Exception {
        return getDeploymentService().start(name);
    }

    /**
     * Stops a particular service assembly.
     *
     * @param name
     * @return
     */
    public String stopServiceAssembly(String name) throws Exception {
        return getDeploymentService().stop(name);
    }

    /**
     * Shuts down a particular service assembly.
     *
     * @param name
     * @return
     */
    public String shutdownServiceAssembly(String name) throws Exception {
        return getDeploymentService().shutDown(name);
    }

    /**
     * load an archive from an external location and starts it The archive can
     * be a Component, Service Assembly or Shared Library.
     *
     * @param location -
     *            can either be a url or filename (if relative - must be
     *            relative to the container)
     * @return status
     * @throws Exception
     */
    public String installArchive(String location) throws Exception {
    	return ManagementSupport.createSuccessMessage("to be done");
    }

    /**
     * Prints information about all components (Service Engine or Binding
     * Component) installed
     *
     * @param serviceEngines
     * @param bindingComponents
     * @param state
     * @param sharedLibraryName
     * @param serviceAssemblyName
     * @return list of components in an XML blob
     */
    public String listComponents(boolean excludeSEs, boolean excludeBCs, String requiredState,
                    String sharedLibraryName, String serviceAssemblyName) throws Exception {
        // validate requiredState
    	return ManagementSupport.createSuccessMessage("to be done");
    }

    /**
     * Prints information about shared libraries installed.
     *
     * @param componentName
     * @param sharedLibraryName
     * @return
     */
    public String listSharedLibraries(String componentName, String sharedLibraryName) throws Exception {
    	return ManagementSupport.createSuccessMessage("to be done");
    }

    /**
     * Prints information about service assemblies deployed.
     *
     * @param state
     * @param componentName
     * @param serviceAssemblyName
     * @return
     */
    public String listServiceAssemblies(String state, String componentName, String serviceAssemblyName) throws Exception {
    	return ManagementSupport.createSuccessMessage("to be done");
    }

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;		
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}
	public void setInstallationService(InstallationService installationService) {
		this.installationService = installationService;
	}

	public InstallationService getInstallationService() {
		return installationService;
	}

	public void setDeploymentService(DeploymentService deploymentService) {
		this.deploymentService = deploymentService;
	}

	public DeploymentService getDeploymentService() {
		return deploymentService;
	}

	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}

	public AdminService getAdminService() {
		return adminService;
	}

}
