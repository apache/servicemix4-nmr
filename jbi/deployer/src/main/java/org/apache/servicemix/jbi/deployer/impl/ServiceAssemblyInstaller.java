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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.jbi.JBIException;
import javax.jbi.management.LifeCycleMBean;
import javax.management.ObjectName;

import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.DeployedAssembly;
import org.apache.servicemix.jbi.deployer.artifacts.ComponentImpl;
import org.apache.servicemix.jbi.deployer.artifacts.ServiceAssemblyImpl;
import org.apache.servicemix.jbi.deployer.artifacts.ServiceUnitImpl;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceUnitDesc;
import org.apache.servicemix.jbi.deployer.utils.FileUtil;
import org.apache.servicemix.jbi.deployer.utils.ManagementSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.prefs.BackingStoreException;

public class ServiceAssemblyInstaller extends AbstractInstaller {

    private DeployedAssembly deployedAssembly;

    public ServiceAssemblyInstaller(Deployer deployer, Descriptor descriptor, File jbiArtifact, boolean autoStart) {
        super(deployer, descriptor, jbiArtifact, autoStart);
        this.installRoot = new File(System.getProperty("servicemix.base"), "data/jbi/" + getName() + "/install");
        this.installRoot.mkdirs();
    }

    public ServiceAssemblyInstaller(Deployer deployer, Descriptor descriptor, DeployedAssembly deployedAssembly, boolean autoStart) {
        super(deployer, descriptor, null, autoStart);
        this.deployedAssembly = deployedAssembly;
    }

    public String getName() {
        return descriptor.getServiceAssembly().getIdentification().getName();
    }

    public DeployedAssembly getDeployedAssembly() {
        return deployedAssembly;
    }

    public void init() throws Exception {
        // Check requirements
        for (ServiceUnitDesc sud : descriptor.getServiceAssembly().getServiceUnits()) {
            String componentName = sud.getTarget().getComponentName();
            Component component = deployer.getComponent(componentName);
            if (component == null) {
                throw new PendingException(bundle, "Component not installed: " + componentName);
            }
            if (!LifeCycleMBean.STARTED.equals(component.getCurrentState())) {
                throw new PendingException(bundle, "Component is not started: " + componentName);
            }
        }
        // Extract bundle
        super.init();
    }

    public ObjectName install() throws JBIException {
        try {
            List<ServiceUnitImpl> sus;
            if (deployedAssembly == null) {
                sus = deploySUs();
            } else {
                sus = new ArrayList<ServiceUnitImpl>();
                for (ServiceUnitDesc sud : descriptor.getServiceAssembly().getServiceUnits()) {
                    String componentName = sud.getTarget().getComponentName();
                    ComponentImpl component = deployer.getComponent(componentName);
                    // Create service unit object
                    ServiceUnitImpl su = deployer.createServiceUnit(sud, null, component);
                    sus.add(su);
                }
            }
            postInstall();
            ServiceAssembly sa = deployer.registerServiceAssembly(bundle, descriptor.getServiceAssembly(), sus);
            return deployer.getManagementStrategy().getManagedObjectName(sa, null, ObjectName.class);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new JBIException(e);
        }
    }

    public void stop(boolean force) throws Exception {
        ServiceAssemblyImpl assembly = deployer.getServiceAssembly(getName());
        if (assembly == null && !force) {
            throw ManagementSupport.failure("undeployServiceAssembly", "ServiceAssembly '" + getName() + "' is not deployed.");
        }
        // Check assembly state is shutdown
        if (assembly != null && !LifeCycleMBean.SHUTDOWN.equals(assembly.getCurrentState())) {
            if (!force) {
                throw ManagementSupport.failure("undeployServiceAssembly", "ServiceAssembly '" + getName() + "' is not shut down.");
            }
            if (LifeCycleMBean.STARTED.equals(assembly.getCurrentState())) {
                assembly.stop(false);
            }
            if (LifeCycleMBean.STOPPED.equals(assembly.getCurrentState())) {
                assembly.shutDown(false, force);
            }
        }
        if (deployedAssembly == null) {
            for (ServiceUnitImpl su : assembly.getServiceUnitsList()) {
                su.getComponentImpl().removeServiceUnit(su);
            }
        } else {
            deployedAssembly.undeploy(bundle.getState() == Bundle.ACTIVE);
        }
    }

    public void uninstall(boolean force) throws Exception {
        // Shutdown SA
        stop(force);
        // Retrieve SA
        ServiceAssemblyImpl assembly = deployer.getServiceAssembly(getName());
        if (assembly == null && !force) {
            throw ManagementSupport.failure("undeployServiceAssembly", "ServiceAssembly '" + getName() + "' is not deployed.");
        }
        if (assembly != null) {
            // Undeploy SUs
            if (assembly.getServiceUnitsList() != null) {
                for (ServiceUnitImpl su : assembly.getServiceUnitsList()) {
                    su.undeploy();
                }
            }
            // Unregister assembly
            deployer.unregisterServiceAssembly(assembly);
        }
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

    protected List<ServiceUnitImpl> deploySUs() throws Exception {
        // Create the SA directory
        File saDir = new File(installRoot.getParent(), "sus");
        // Quickly undeploy the SA if it has changed
        if (isModified && !isFirstInstall) {
            for (ServiceUnitDesc sud : descriptor.getServiceAssembly().getServiceUnits()) {
                File suRootDir = new File(saDir, sud.getIdentification().getName());
                String componentName = sud.getTarget().getComponentName();
                ComponentImpl component = deployer.getComponent(componentName);
                ServiceUnitImpl su = deployer.createServiceUnit(sud, suRootDir, component);
                try {
                    su.undeploy();
                } catch (Exception e) {
                    LOGGER.warn("Problem undeploying SU " + su.getName());
                }
            }
        }

        // Wipe out the SA dir
        if (isModified) {
            FileUtil.deleteFile(saDir);
            FileUtil.buildDirectory(saDir);
        }
        // Iterate each SU and deploy it
        List<ServiceUnitImpl> sus = new ArrayList<ServiceUnitImpl>();
        Exception failure = null;
        for (ServiceUnitDesc sud : descriptor.getServiceAssembly().getServiceUnits()) {
            // Create directory for this SU
            File suRootDir = new File(saDir, sud.getIdentification().getName());
            // Unpack it
            if (isModified) {
                String zip = sud.getTarget().getArtifactsZip();
                URL zipUrl = bundle.getResource(zip);
                FileUtil.unpackArchive(zipUrl, suRootDir);
            }
            // Find component
            String componentName = sud.getTarget().getComponentName();
            ComponentImpl component = deployer.getComponent(componentName);
            // Create service unit object
            ServiceUnitImpl su = deployer.createServiceUnit(sud, suRootDir, component);
            try {
                LOGGER.debug("Deploying SU " + su.getName());
                if (isModified) {
                    su.deploy();
                }
                // Add it to the list
                sus.add(su);
            } catch (Throwable e) {
                LOGGER.error("Error deploying SU " + su.getName(), e);
                failure = new Exception("Error deploying SU " + su.getName(), e);
                break;
            }
        }
        // If failure, undeploy SU and exit
        if (failure != null) {
            for (ServiceUnitImpl su : sus) {
                try {
                    LOGGER.debug("Undeploying SU " + su.getName());
                    su.undeploy();
                } catch (Exception e) {
                    LOGGER.warn("Error undeploying SU " + su.getName(), e);
                }
            }
            throw failure;
        }
        return sus;
    }

    public void undeploy() throws javax.jbi.JBIException {
        try {
            Bundle bundle = getBundle();

            if (bundle == null) {
                LOGGER.warn("Could not find Bundle for Service Assembly: " + getName());
            } else {
                bundle.stop();
                bundle.uninstall();
                try {
                    deletePreferences();
                } catch (BackingStoreException e) {
                    LOGGER.warn("Error cleaning persistent state for service assembly: " + getName(), e);
                }
            }
        } catch (BundleException e) {
            LOGGER.error("failed to uninstall Service Assembly: " + getName(), e);
            throw new JBIException(e);
        }
    }

}
