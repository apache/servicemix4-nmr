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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.JBIException;
import javax.jbi.management.DeploymentServiceMBean;
import javax.jbi.management.LifeCycleMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceAssemblyDesc;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceUnitDesc;
import org.apache.servicemix.jbi.deployer.handler.Transformer;
import org.apache.servicemix.jbi.deployer.impl.ComponentImpl;
import org.apache.servicemix.jbi.deployer.impl.Deployer;
import org.apache.servicemix.jbi.deployer.impl.ServiceAssemblyImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class DeploymentService implements DeploymentServiceMBean {
	
	private static final Log LOG = LogFactory.getLog(DeploymentService.class);
	private AdminService adminService;
	
	private Map<String, ServiceAssemblyInstaller> serviceAssemblyInstallers = new ConcurrentHashMap<String, ServiceAssemblyInstaller>();

    public String deploy(String saZipURL) throws Exception {
    	try {
            if (saZipURL == null) {
                throw ManagementSupport.failure("deploy", "saZipURL must not be null");
            }
            File jarfile = new File(saZipURL);
            if (jarfile.exists()) {
            	Descriptor root = null;
                try {
                    root = Transformer.getDescriptor(jarfile);
                } catch (Exception e) {
                    throw ManagementSupport.failure("deploy", "Unable to build jbi descriptor: " + saZipURL, e);
                }
                if (root == null) {
                    throw ManagementSupport.failure("deploy", "Unable to find jbi descriptor: " + saZipURL);
                }
                ServiceAssemblyDesc sa = root.getServiceAssembly();
                if (sa == null) {
                    throw ManagementSupport.failure("deploy", "JBI descriptor is not an assembly descriptor: " + saZipURL);
                }
                return deployServiceAssembly(saZipURL, sa);
            } else {
                throw new RuntimeException("location for deployment SA: " + saZipURL + " isn't valid");
            }
            
    	} catch (Exception e) {
            LOG.error("Error deploying service assembly", e);
            throw e;
        }
    }

    private String deployServiceAssembly(String saZipURL, ServiceAssemblyDesc sa) throws Exception {
    	String assemblyName = sa.getIdentification().getName();
        	
        // Check all SUs requirements
        ServiceUnitDesc[] sus = sa.getServiceUnits();
        if (sus != null) {
            checkSus(sus);
        }
        LOG.info("Deploy ServiceAssembly " + assemblyName);
        ServiceAssemblyInstaller saInstaller;
        if (serviceAssemblyInstallers.containsKey(assemblyName)) {
            saInstaller = serviceAssemblyInstallers.get(assemblyName);
        } else {
        	saInstaller = new ServiceAssemblyInstaller(assemblyName, this.getAdminService());
        }
        saInstaller.deploy(saZipURL);
        serviceAssemblyInstallers.put(assemblyName, saInstaller);
        return ManagementSupport.createSuccessMessage("deploy SA", assemblyName);
    }

	private void checkSus(ServiceUnitDesc[] sus) throws Exception {
		for (int i = 0; i < sus.length; i++) {
            String suName = sus[i].getIdentification().getName();
            String componentName = sus[i].getTarget().getComponentName();
            ComponentImpl componentImpl = (ComponentImpl) getComponentByName(componentName);
            if (componentImpl == null) {
                throw ManagementSupport.failure("deploy", "Target component " + componentName
                                                            + " for service unit " + suName + " is not installed");
            }
            if (!componentImpl.getCurrentState().equals(LifeCycleMBean.STARTED)) {
                throw ManagementSupport.failure("deploy", "Target component " + componentName
                                                            + " for service unit " + suName + " is not started");
            }
            if (componentImpl.getComponent().getServiceUnitManager() == null) {
                throw ManagementSupport.failure("deploy", "Target component " + componentName
                                                            + " for service unit " + suName + " does not accept deployments");
            }

            if (isDeployedServiceUnit(componentName, suName)) {
                throw ManagementSupport.failure("deploy", "Service unit " + suName
                                                            + " is already deployed on component " + componentName);
            }
        }		
	}

	private Component getComponentByName(String name) throws Exception {
		ServiceReference ref = getAdminService().getComponentServiceReference("(" + Deployer.NAME + "=" + name + ")");
        if (ref == null) {
            throw new JBIException("Component '" + name + "' not found");
        }
        Component component = (Component) getAdminService().getBundleContext().getService(ref);
        return component;
	}
	
	public String undeploy(String saName) throws Exception {
		if (saName == null) {
            throw ManagementSupport.failure("undeploy", "SA name must not be null");
        }
		ServiceReference ref = getAdminService().getSAServiceReference("(" + Deployer.NAME + "=" + saName + ")");
		if (ref == null) {
            throw ManagementSupport.failure("undeploy", "SA has not been deployed: " + saName);
        }
        Bundle bundle = ref.getBundle();
        ServiceAssemblyImpl sa = (ServiceAssemblyImpl) getAdminService().getBundleContext().getService(ref);
        
        
        String state = sa.getCurrentState();
        if (!LifeCycleMBean.SHUTDOWN.equals(state)) {
            throw ManagementSupport.failure("undeploy", "SA must be shut down: " + saName);
        }
        try {
            if (bundle != null) {
            	bundle.stop();
            	bundle.uninstall();
            	return ManagementSupport.createSuccessMessage("undeploy service assembly successfully", saName);
            }

        } catch (Exception e) {
            LOG.info("Unable to undeploy assembly", e);
            throw e;
        }
        return "failed to undeploy service assembly" + saName;
    }

    public String[] getDeployedServiceUnitList(String componentName) throws Exception {
        return new String[0]; 
    }

    public String[] getDeployedServiceAssemblies() throws Exception {
        return new String[0];
    }

    public String getServiceAssemblyDescriptor(String saName) throws Exception {
        return null;
    }

    public String[] getDeployedServiceAssembliesForComponent(String componentName) throws Exception {
        return new String[0];
    }

    public String[] getComponentsForDeployedServiceAssembly(String saName) throws Exception {
        return new String[0];
    }

    public boolean isDeployedServiceUnit(String componentName, String suName) throws Exception {
        return false;
    }

    public boolean canDeployToComponent(String componentName) {
        return false;
    }

    public String start(String serviceAssemblyName) throws Exception {
    	try {
    		if (serviceAssemblyName == null) {
                throw ManagementSupport.failure("start", "SA name must not be null");
            }
    		ServiceReference ref = getAdminService().getSAServiceReference("(" + Deployer.NAME + "=" + serviceAssemblyName + ")");
    		if (ref == null) {
                throw ManagementSupport.failure("start", "SA has not exist: " + serviceAssemblyName);
            }
    		ServiceAssemblyImpl sa = (ServiceAssemblyImpl) getAdminService().getBundleContext().getService(ref);
    		sa.start();
    		return ManagementSupport.createSuccessMessage("start service assembly successfully", serviceAssemblyName);
        } catch (Exception e) {
            LOG.info("Error in start", e);
            throw e;
        }
    }

    public String stop(String serviceAssemblyName) throws Exception {
    	try {
    		if (serviceAssemblyName == null) {
                throw ManagementSupport.failure("stop", "SA name must not be null");
            }
    		ServiceReference ref = getAdminService().getSAServiceReference("(" + Deployer.NAME + "=" + serviceAssemblyName + ")");
    		if (ref == null) {
                throw ManagementSupport.failure("stop", "SA has not exist: " + serviceAssemblyName);
            }
    		ServiceAssemblyImpl sa = (ServiceAssemblyImpl) getAdminService().getBundleContext().getService(ref);
    		sa.stop();
    		return ManagementSupport.createSuccessMessage("stop service assembly successfully", serviceAssemblyName);
        } catch (Exception e) {
            LOG.info("Error in stop", e);
            throw e;
        }
    }

    public String shutDown(String serviceAssemblyName) throws Exception {
    	try {
    		if (serviceAssemblyName == null) {
                throw ManagementSupport.failure("shutdown", "SA name must not be null");
            }
    		ServiceReference ref = getAdminService().getSAServiceReference("(" + Deployer.NAME + "=" + serviceAssemblyName + ")");
    		if (ref == null) {
                throw ManagementSupport.failure("shutdown", "SA has not exist: " + serviceAssemblyName);
            }
    		ServiceAssemblyImpl sa = (ServiceAssemblyImpl) getAdminService().getBundleContext().getService(ref);
    		sa.shutDown();
    		return ManagementSupport.createSuccessMessage("shutdown service assembly successfully", serviceAssemblyName);
        } catch (Exception e) {
            LOG.info("Error in shutdown", e);
            throw e;
        }
    }

    public String getState(String serviceAssemblyName) throws Exception {
    	try {
    		if (serviceAssemblyName == null) {
                throw ManagementSupport.failure("getState", "SA name must not be null");
            }
    		ServiceReference ref = getAdminService().getSAServiceReference("(" + Deployer.NAME + "=" + serviceAssemblyName + ")");
    		if (ref == null) {
                throw ManagementSupport.failure("getState", "SA has not exist: " + serviceAssemblyName);
            }
    		ServiceAssemblyImpl sa = (ServiceAssemblyImpl) getAdminService().getBundleContext().getService(ref);
    		sa.getState();
    		return ManagementSupport.createSuccessMessage("getState service assembly successfully", serviceAssemblyName);
        } catch (Exception e) {
            LOG.info("Error in getState", e);
            throw e;
        }
    }

	public void setAdminService(AdminService adminService) {
		this.adminService = adminService;
	}

	public AdminService getAdminService() {
		return adminService;
	}
	
	
}
