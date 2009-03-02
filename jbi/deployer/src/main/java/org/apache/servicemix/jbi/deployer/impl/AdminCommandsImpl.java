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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.jbi.management.LifeCycleMBean;
import javax.management.StandardMBean;

import org.apache.servicemix.jbi.deployer.AdminCommandsService;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.utils.ManagementSupport;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

public class AdminCommandsImpl implements AdminCommandsService, InitializingBean, DisposableBean {

    private Deployer deployer;
    private InstallationService installationService;
    private DeploymentService deploymentService;


    /**
     * Install a JBI component (a Service Engine or Binding Component)
     *
     * @param file jbi component archive to install
     * @param props    installation properties
     * @return
     */
    public String installComponent(String file, Properties props, boolean deferException) throws Exception {
        // TODO: handle deferException
        try {
            getInstallationService().install(file, props, deferException);
            return ManagementSupport.createSuccessMessage("installComponent", file);
        } catch (Exception e) {
            throw ManagementSupport.failure("installComponent", file, e);
        }
    }


    /**
     * Uninstalls a previously install JBI Component (a Service Engine or
     * Binding Component)
     *
     * @param name
     * @return
     */
    public String uninstallComponent(final String name) throws Exception {
        try {
            if (installationService.unloadInstaller(name, true)) {
            	return ManagementSupport.createSuccessMessage("uninstallComponent", name);
            }
            throw ManagementSupport.failure("uninstallComponent", name);
        } catch (Exception e) {
            throw ManagementSupport.failure("uninstallComponent", name, e);
        }
    }

    /**
     * Installs a Shared Library.
     *
     * @param file
     * @return
     */
    public String installSharedLibrary(String file, boolean deferException) throws Exception {
        // TODO: handle deferException
        try {
            installationService.installSharedLibrary(file);
            return ManagementSupport.createSuccessMessage("installSharedLibrary", file);
        } catch (Exception e) {
            throw ManagementSupport.failure("installSharedLibrary", file, e);
        }
    }

    /**
     * Uninstalls a previously installed Shared Library.
     *
     * @param name
     * @return
     */
    public String uninstallSharedLibrary(String name) throws Exception {
        try {
            getInstallationService().doUninstallSharedLibrary(name);
            return ManagementSupport.createSuccessMessage("uninstallSharedLibrary", name);
        } catch (Throwable e) {
            throw ManagementSupport.failure("uninstallSharedLibrary", name, e);
        }
    }

    /**
     * Starts a particular Component (Service Engine or Binding Component).
     *
     * @param name
     * @return
     */
    public String startComponent(String name) throws Exception {
        Component component = deployer.getComponent(name);
        if (component == null) {
            throw ManagementSupport.failure("start", "Component does not exist: " + name);
        }
        try {
            component.start();
            return ManagementSupport.createSuccessMessage("Component started", name);
        } catch (Throwable e) {
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
        Component component = deployer.getComponent(name);
        if (component == null) {
            throw ManagementSupport.failure("stop", "Component does not exist: " + name);
        }
        try {
            component.stop();
            return ManagementSupport.createSuccessMessage("Component stopped", name);
        } catch (Throwable e) {
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
        Component component = deployer.getComponent(name);
        if (component == null) {
            throw ManagementSupport.failure("shutdown", "Component does not exist: " + name);
        }
        try {
            component.shutDown();
            return ManagementSupport.createSuccessMessage("Component shut down", name);
        } catch (Throwable e) {
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
        // TODO: handle deferException
        try {
            return deploymentService.deploy(file, deferException);
        } catch (Throwable e) {
            throw ManagementSupport.failure("deployServiceAssembly", file, e);
        }

    }

    /**
     * Undeploys a previously deployed service assembly.
     *
     * @param name
     * @return
     */
    public String undeployServiceAssembly(String name) throws Exception {
        ServiceAssembly sa = deployer.getServiceAssembly(name);
        if (sa == null) {
            throw ManagementSupport.failure("start", "Service assembly does not exist: " + name);
        }
        try {
            return getDeploymentService().undeploy(name);
        } catch (Throwable e) {
            throw ManagementSupport.failure("undeployServiceAssembly", name, e);
        }
    }

    /**
     * Starts a service assembly.
     *
     * @param name
     * @return
     */
    public String startServiceAssembly(String name) throws Exception {
        ServiceAssembly sa = deployer.getServiceAssembly(name);
        if (sa == null) {
            throw ManagementSupport.failure("start", "Service assembly does not exist: " + name);
        }
        try {
            sa.start();
            return ManagementSupport.createSuccessMessage("Service assembly started", name);
        } catch (Throwable e) {
            throw ManagementSupport.failure("startServiceAssembly", name, e);
        }
    }

    /**
     * Stops a particular service assembly.
     *
     * @param name
     * @return
     */
    public String stopServiceAssembly(String name) throws Exception {
        ServiceAssembly sa = deployer.getServiceAssembly(name);
        if (sa == null) {
            throw ManagementSupport.failure("stop", "Service assembly does not exist: " + name);
        }
        try {
            sa.stop();
            return ManagementSupport.createSuccessMessage("Service assembly stopped", name);
        } catch (Throwable e) {
            throw ManagementSupport.failure("stopServiceAssembly", name, e);
        }
    }

    /**
     * Shuts down a particular service assembly.
     *
     * @param name
     * @return
     */
    public String shutdownServiceAssembly(String name) throws Exception {
        ServiceAssembly sa = deployer.getServiceAssembly(name);
        if (sa == null) {
            throw ManagementSupport.failure("shutdown", "Service assembly does not exist: " + name);
        }
        try {
            sa.shutDown();
            return ManagementSupport.createSuccessMessage("Service assembly shut down", name);
        } catch (Throwable e) {
            throw ManagementSupport.failure("shutdownServiceAssembly", name, e);
        }
    }


    /**
     * Prints information about all components (Service Engine or Binding
     * Component) installedServiceReference[] refs = getAdminService().getComponentServiceReferences(filter);
     * if (excludeBCs && excludeSEs) {
     * refs = new ServiceReference[0];
     * }
     * List<Component> components = new ArrayList<Component>();
     *
     * @param excludeSEs
     * @param excludeBCs
     * @param requiredState
     * @param sharedLibraryName
     * @param serviceAssemblyName
     * @return list of components in an XML blob
     */
    public String listComponents(boolean excludeSEs, boolean excludeBCs, boolean excludePojos, String requiredState,
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
        List<Component> components = new ArrayList<Component>();
        for (Component component : deployer.getComponents().values()) {
            // Check type
            if (excludeSEs && Deployer.TYPE_SERVICE_ENGINE.equals(component.getType())) {
                continue;
            }
            // Check type
            if (excludeBCs && Deployer.TYPE_BINDING_COMPONENT.equals(component.getType())) {
                continue;
            }
            // Check status
            if (requiredState != null && requiredState.length() > 0 && !requiredState.equalsIgnoreCase(component.getCurrentState())) {
                continue;
            }
            // Check shared library
            if (StringUtils.hasLength(sharedLibraryName)) {
                boolean match = false;
                for (SharedLibrary lib : component.getSharedLibraries()) {
                    if (sharedLibraryName.equals(lib.getName())) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
            }
            // Check deployed service assembly
            if (StringUtils.hasLength(serviceAssemblyName)) {
                boolean match = false;
                for (ServiceUnit su : component.getServiceUnits()) {
                    if (serviceAssemblyName.equals(su.getServiceAssembly().getName())) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
            }
            components.add(component);
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version='1.0'?>\n");
        buffer.append("<component-info-list xmlns='http://java.sun.com/xml/ns/jbi/component-info-list' version='1.0'>\n");
        for (Component component : components) {
            buffer.append("  <component-info");
            buffer.append(" type='").append(component.getType()).append("'");
            buffer.append(" name='").append(component.getName()).append("'");
            buffer.append(" state='").append(component.getCurrentState()).append("'>\n");
            buffer.append("    <description>");
            if (component.getDescription() != null) {
                buffer.append(component.getDescription());
            }
            buffer.append("</description>\n");
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
        Set<SharedLibrary> libs = new HashSet<SharedLibrary>();
        if (sharedLibraryName != null && sharedLibraryName.length() > 0) {
            SharedLibrary lib = getDeployer().getSharedLibrary(sharedLibraryName);
            if (lib != null) {
                libs.add(lib);
            }
        } else if (componentName != null && componentName.length() > 0) {
            Component component = deployer.getComponent(componentName);
            if (component != null) {
                for (SharedLibrary lib : component.getSharedLibraries()) {
                    libs.add(lib);
                }
            }
        } else {
            libs.addAll(getDeployer().getSharedLibraries().values());
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version='1.0'?>\n");
        buffer.append("<component-info-list xmlns='http://java.sun.com/xml/ns/jbi/component-info-list' version='1.0'>\n");
        for (SharedLibrary sl : libs) {
            buffer.append("  <component-info type='shared-library' name='").append(sl.getName()).append("' state='Started'>\n");
            buffer.append("    <description>").append(sl.getDescription()).append("</description>\n");
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
        List<ServiceAssembly> assemblies = new ArrayList<ServiceAssembly>();
        Component component = null;
        if (StringUtils.hasLength(componentName)) {
            component = deployer.getComponent(componentName);
        }
        for (ServiceAssembly sa : deployer.getServiceAssemblies().values()) {
            boolean match = true;
            if (StringUtils.hasLength(serviceAssemblyName)) {
                match = serviceAssemblyName.equals(sa.getName());
            }
            if (match && StringUtils.hasLength(state)) {
                match = state.equalsIgnoreCase(sa.getCurrentState());
            }
            if (match && StringUtils.hasLength(componentName)) {
                match = false;
                if (component != null) {
                    for (ServiceUnit su : component.getServiceUnits()) {
                        if (sa.getName().equals(su.getServiceAssembly().getName())) {
                            match = true;
                            break;
                        }
                    }
                }
            }
            if (match) {
                assemblies.add(sa);
            }
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version='1.0'?>\n");
        buffer.append("<service-assembly-info-list xmlns='http://java.sun.com/xml/ns/jbi/service-assembly-info-list' version='1.0'>\n");
        for (ServiceAssembly sa : assemblies) {
            buffer.append("  <service-assembly-info");
            buffer.append(" name='").append(sa.getName()).append("'");
            buffer.append(" state='").append(sa.getCurrentState()).append("'>\n");
            buffer.append("    <description>").append(sa.getDescription()).append("</description>\n");
            for (ServiceUnit su : sa.getServiceUnits()) {
                buffer.append("    <service-unit-info");
                buffer.append(" name='").append(su.getName()).append("'");
                buffer.append(" state='").append(sa.getCurrentState()).append("'");
                buffer.append(" deployed-on='").append(su.getComponent().getName()).append("'>\n");
                buffer.append("      <description>").append(su.getDescription()).append("</description>\n");
                buffer.append("    </service-unit-info>\n");
            }
            buffer.append("  </service-assembly-info>\n");
        }
        buffer.append("</service-assembly-info-list>");
        return buffer.toString();
    }

    public void afterPropertiesSet() throws Exception {
        deployer.getManagementAgent().register(new StandardMBean(this, AdminCommandsService.class),
                                               deployer.getNamingStrategy().getObjectName(this));
    }

    public void destroy() throws Exception {
        deployer.getManagementAgent().unregister(deployer.getNamingStrategy().getObjectName(this));
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

    public Deployer getDeployer() {
        return deployer;
    }

    public void setDeployer(Deployer deployer) {
        this.deployer = deployer;
    }

}
