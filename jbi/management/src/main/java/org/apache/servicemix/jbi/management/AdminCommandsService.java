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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jbi.JBIException;
import javax.jbi.management.LifeCycleMBean;




import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryList;
import org.apache.servicemix.jbi.deployer.impl.ComponentImpl;
import org.apache.servicemix.jbi.deployer.impl.Deployer;
import org.apache.servicemix.jbi.deployer.impl.ServiceAssemblyImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.BundleContextAware;

public class AdminCommandsService implements AdminCommandsServiceMBean, BundleContextAware {

	
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
    public String installComponent(String fileName) throws Exception {
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
    public String installSharedLibrary(String file) throws Exception {
    	return installationService.installSharedLibrary(file);
    }

    /**
     * Uninstalls a previously installed Shared Library.
     *
     * @param name
     * @return
     */
    public String uninstallSharedLibrary(String name) throws Exception {
    	//Check that the library is installed
        boolean isInstalled = getInstallationService().containsSharedLibrary(name);
        if (!isInstalled) {
            throw ManagementSupport.failure("uninstallSharedLibrary", "Shared library '" + name + "' is not installed.");
        }
        // Check that it is not used by a running component
        ServiceReference[] refs = getAdminService().getComponentServiceReferences(null);
        
        
        for (ServiceReference ref : refs) {
        	ComponentImpl component = (ComponentImpl) getBundleContext().getService(ref);
            if (!component.getCurrentState().equalsIgnoreCase(LifeCycleMBean.SHUTDOWN)) {
                SharedLibraryList[] sls = component.getSharedLibraries();
                if (sls != null) {
                    for (int i = 0; i < sls.length; i++) {
                    	if (name.equals(sls[i].getName())) {
                    		throw ManagementSupport.failure("uninstallSharedLibrary", "Shared library '" + name
                                            + "' is used by component '" + component.getName() + "'.");
                        }
                    }
                }
            }
        }
        boolean success = getInstallationService().uninstallSharedLibrary(name);
        if (success) {
            return ManagementSupport.createSuccessMessage("uninstallSharedLibrary", name);
        } else {
            throw ManagementSupport.failure("uninstallSharedLibrary", name);
        }
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
    public String deployServiceAssembly(String file) throws Exception {
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
     * Prints information about all components (Service Engine or Binding
     * Component) installedServiceReference[] refs = getAdminService().getComponentServiceReferences(filter);
        if (excludeBCs && excludeSEs) {
        	refs = new ServiceReference[0];
        }
        List<Component> components = new ArrayList<Component>();
        
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
    	//validate requiredState
        if (requiredState != null && requiredState.length() > 0
                        && !LifeCycleMBean.UNKNOWN.equalsIgnoreCase(requiredState)
                        && !LifeCycleMBean.SHUTDOWN.equalsIgnoreCase(requiredState)
                        && !LifeCycleMBean.STOPPED.equalsIgnoreCase(requiredState)
                        && !LifeCycleMBean.STARTED.equalsIgnoreCase(requiredState)) {
            throw ManagementSupport.failure("listComponents", "Required state '" + requiredState + "' is not a valid state.");
        }
        // Get components
        String filter = null;
        if (excludeSEs && !excludeBCs) {
        	filter = "(" + Deployer.TYPE + "=binding-component)";
        }
        if (excludeBCs && !excludeSEs) {
        	filter = "(" + Deployer.TYPE + "=service-engine)";
        }
        ServiceReference[] refs = getAdminService().getComponentServiceReferences(filter);
        if (excludeBCs && excludeSEs) {
        	refs = new ServiceReference[0];
        }
        List<Component> components = new ArrayList<Component>();
        for (ServiceReference ref : refs) {
            Component component = (Component) getBundleContext().getService(ref);
            
            // Check status
            if (requiredState != null && requiredState.length() > 0 && !requiredState.equalsIgnoreCase(component.getCurrentState())) {
                continue;
            }
            // Check shared library
            // TODO: check component dependency on SL
            if (sharedLibraryName != null && sharedLibraryName.length() > 0
                            && !getInstallationService().containsSharedLibrary(sharedLibraryName)) {
                continue;
            }
            // Check deployed service assembly
            if (serviceAssemblyName != null && serviceAssemblyName.length() > 0) {
            	ComponentImpl compImpl = (ComponentImpl)component;
            	Set<ServiceAssemblyImpl> saImpls = compImpl.getServiceAssemblies();
            	boolean found = false;
                for (ServiceAssemblyImpl sa : saImpls) {
                    if (serviceAssemblyName.equals(sa.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    continue;
                }
            }
            components.add(component);
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version='1.0'?>\n");
        buffer.append("<component-info-list xmlns='http://java.sun.com/xml/ns/jbi/component-info-list' version='1.0'>\n");
        for (Iterator<Component> iter = components.iterator(); iter.hasNext();) {
            Component component = iter.next();
            buffer.append("  <component-info");
            if (!getAdminService().isBinding(component.getName()) 
            		&& getAdminService().isEngine(component.getName())) {
                buffer.append(" type='service-engine'");
            } else if (getAdminService().isBinding(component.getName()) 
            		&& !getAdminService().isEngine(component.getName())) {
                buffer.append(" type='binding-component'");
            }
            buffer.append(" name='" + component.getName() + "'");
            buffer.append(" state='" + component.getCurrentState() + "'>\n");
            if (component.getDescription() != null) {
                buffer.append("    <description>");
                buffer.append(component.getDescription());
                buffer.append("</description>\n");
            }
            buffer.append("  </component-info>\n");
        }
        buffer.append("</component-info-list>");
        return buffer.toString();

    }

    /**
     * Prints information about shared libraries installed.
     *
     * @param componentName
     * @param sharedLibraryName
     * @return
     */
    public String listSharedLibraries(String componentName, String sharedLibraryName) throws Exception {
    	Set<String> libs = new HashSet<String>();
        if (sharedLibraryName != null && sharedLibraryName.length() > 0) {
        	if (getInstallationService().containsSharedLibrary(sharedLibraryName)) {
                libs.add(sharedLibraryName);
            }
        } else if (componentName != null && componentName.length() > 0) {
        	ServiceReference ref = getAdminService().getComponentServiceReference("(" + Deployer.NAME + "=" + componentName + ")");
            if (ref == null) {
                throw new JBIException("Component '" + componentName + "' not found");
            }
            ComponentImpl component = (ComponentImpl) bundleContext.getService(ref);
            for (SharedLibraryList sl : component.getSharedLibraries()) {
            	libs.add(sl.getName());
            }
        } else {
            libs = getInstallationService().getInstalledSharedLibs();
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version='1.0'?>\n");
        buffer.append("<component-info-list xmlns='http://java.sun.com/xml/ns/jbi/component-info-list' version='1.0'>\n");
        for (String sl : libs) {
            buffer.append("  <component-info type='shared-library' name='").append(sl).append("' state='Started'>");
            buffer.append("  </component-info>\n");
        }
        buffer.append("</component-info-list>");
        return buffer.toString();
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
