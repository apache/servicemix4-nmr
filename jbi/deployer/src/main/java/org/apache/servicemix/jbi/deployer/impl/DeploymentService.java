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
import java.util.HashSet;
import java.util.Set;

import javax.jbi.management.DeploymentServiceMBean;
import javax.jbi.management.LifeCycleMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceAssemblyDesc;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceUnitDesc;
import org.apache.servicemix.jbi.deployer.handler.Transformer;
import org.apache.servicemix.jbi.deployer.utils.ManagementSupport;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.BundleContextAware;

public class DeploymentService implements DeploymentServiceMBean, BundleContextAware {

    private static final Log LOG = LogFactory.getLog(DeploymentService.class);

    private Deployer deployer;
    private BundleContext bundleContext;

    public Deployer getDeployer() {
        return deployer;
    }

    public void setDeployer(Deployer deployer) {
        this.deployer = deployer;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String deploy(String saZipURL) throws Exception {
        return deploy(saZipURL, false);
    }

    public String deploy(String saZipURL, boolean autoStart) throws Exception {
        try {
            if (saZipURL == null) {
                throw ManagementSupport.failure("deploy", "saZipURL must not be null");
            }
            File jarfile = new File(saZipURL);
            if (jarfile.exists() && jarfile.isFile()) {
                Descriptor root;
                try {
                    root = Transformer.getDescriptor(jarfile);
                } catch (Exception e) {
                    throw ManagementSupport.failure("deploy", "Unable to build jbi descriptor: " + saZipURL, e);
                }
                if (root == null) {
                    throw ManagementSupport.failure("deploy", "Unable to find jbi descriptor: " + saZipURL);
                }
                // TODO: Move the following code in the installer
                DescriptorFactory.checkDescriptor(root);
                ServiceAssemblyDesc sa = root.getServiceAssembly();
                if (sa == null) {
                    throw ManagementSupport.failure("deploy", "JBI descriptor is not an assembly descriptor: " + saZipURL);
                }
                checkSus(sa.getServiceUnits());
                String name = sa.getIdentification().getName();
                LOG.info("Deploy ServiceAssembly " + name);
                ServiceAssemblyInstaller installer = new ServiceAssemblyInstaller(deployer, root, jarfile, autoStart);
                installer.installBundle();
                installer.init();
                installer.install();
                return ManagementSupport.createSuccessMessage("deploy SA", name);
            } else {
                throw new RuntimeException("location for deployment SA: " + saZipURL + " isn't a valid file");
            }
        } catch (Exception e) {
            LOG.error("Error deploying service assembly", e);
            throw e;
        }
    }

    private void checkSus(ServiceUnitDesc[] sus) throws Exception {
        if (sus != null) {
            for (int i = 0; i < sus.length; i++) {
                String suName = sus[i].getIdentification().getName();
                String componentName = sus[i].getTarget().getComponentName();
                Component component = deployer.getComponent(componentName);
                if (component == null) {
                    throw ManagementSupport.failure("deploy", "Target component " + componentName
                            + " for service unit " + suName + " is not installed");
                }
                if (!component.getCurrentState().equals(LifeCycleMBean.STARTED)) {
                    throw ManagementSupport.failure("deploy", "Target component " + componentName
                            + " for service unit " + suName + " is not started");
                }
                if (component.getComponent().getServiceUnitManager() == null) {
                    throw ManagementSupport.failure("deploy", "Target component " + componentName
                            + " for service unit " + suName + " does not accept deployments");
                }

                if (isDeployedServiceUnit(componentName, suName)) {
                    throw ManagementSupport.failure("deploy", "Service unit " + suName
                            + " is already deployed on component " + componentName);
                }
            }
        }
    }

    public String undeploy(String saName) throws Exception {
        ServiceAssembly assembly = deployer.getServiceAssembly(saName);
        if (assembly == null) {
            throw ManagementSupport.failure("undeploy", "SA has not been deployed: " + saName);
        }
        AbstractInstaller installer = deployer.getInstaller(assembly);
        if (installer == null) {
            throw ManagementSupport.failure("undeploy", "Could not find service assembly installer: " + saName);
        }
        installer.uninstall(false);
        return ManagementSupport.createSuccessMessage("undeploy", "Service assembly " + saName + " undeployed");
    }

    public String[] getDeployedServiceUnitList(String componentName) throws Exception {
        Component component = deployer.getComponent(componentName);

        ServiceUnit[] serviceUnits = component.getServiceUnits();
        String[] sus = new String[serviceUnits.length];
        for (int i = 0; i < serviceUnits.length; i++) {
            sus[i] = serviceUnits[i].getName();
        }
        return sus;
    }

    public String[] getDeployedServiceAssemblies() throws Exception {
        Set<String> sas = deployer.getServiceAssemblies().keySet();
        return sas.toArray(new String[sas.size()]);
    }

    public String getServiceAssemblyDescriptor(String saName) throws Exception {
        ServiceAssembly sa = deployer.getServiceAssembly(saName);
        return sa != null ? sa.getDescriptor() : null;
    }

    public boolean canDeployToComponent(String componentName) {
        Component component = deployer.getComponent(componentName);
        return component != null
                && LifeCycleMBean.STARTED.equals(component.getCurrentState())
                && component.getComponent().getServiceUnitManager() != null;
    }

    public String start(String serviceAssemblyName) throws Exception {
        try {
            ServiceAssembly sa = deployer.getServiceAssembly(serviceAssemblyName);
            if (sa == null) {
                throw ManagementSupport.failure("start", "SA does not exist: " + serviceAssemblyName);
            }
            sa.start();
            return ManagementSupport.createSuccessMessage("start service assembly successfully", serviceAssemblyName);
        } catch (Exception e) {
            LOG.info("Error in start", e);
            throw e;
        }
    }

    public String stop(String serviceAssemblyName) throws Exception {
        try {
            ServiceAssembly sa = deployer.getServiceAssembly(serviceAssemblyName);
            if (sa == null) {
                throw ManagementSupport.failure("stop", "SA does not exist: " + serviceAssemblyName);
            }
            sa.stop();
            return ManagementSupport.createSuccessMessage("stop service assembly successfully", serviceAssemblyName);
        } catch (Exception e) {
            LOG.info("Error in stop", e);
            throw e;
        }
    }

    public String shutDown(String serviceAssemblyName) throws Exception {
        try {
            ServiceAssembly sa = deployer.getServiceAssembly(serviceAssemblyName);
            if (sa == null) {
                throw ManagementSupport.failure("shutdown", "SA does not exist: " + serviceAssemblyName);
            }
            sa.shutDown();
            return ManagementSupport.createSuccessMessage("shutdown service assembly successfully", serviceAssemblyName);
        } catch (Exception e) {
            LOG.info("Error in shutdown", e);
            throw e;
        }
    }

    public String getState(String serviceAssemblyName) throws Exception {
        try {
            ServiceAssembly sa = deployer.getServiceAssembly(serviceAssemblyName);
            if (sa == null) {
                throw ManagementSupport.failure("getState", "SA does not exist: " + serviceAssemblyName);
            }
            return sa.getCurrentState();
        } catch (Exception e) {
            LOG.info("Error in getState", e);
            throw e;
        }
    }

    /**
     * Returns a list of Service Assemblies that contain SUs for the given component.
     *
     * @param componentName name of the component.
     * @return list of Service Assembly names.
     */
    public String[] getDeployedServiceAssembliesForComponent(String componentName) {
        String[] result;
        // iterate through the service assemblies
        Set<String> tmpList = new HashSet<String>();
        for (ServiceAssembly sa : deployer.getServiceAssemblies().values()) {
            for (ServiceUnit su : sa.getServiceUnits()) {
                if (su.getComponent().getName().equals(componentName)) {
                    tmpList.add(sa.getName());
                }
            }
        }
        result = new String[tmpList.size()];
        tmpList.toArray(result);
        return result;
    }

    public String[] getDeployedServiceUnitsForComponent(String componentName) {
        String[] result;
        // iterate through the service assembilies
        Set<String> tmpList = new HashSet<String>();
        for (ServiceAssembly sa : deployer.getServiceAssemblies().values()) {
            for (ServiceUnit su : sa.getServiceUnits()) {
                if (su.getComponent().getName().equals(componentName)) {
                    tmpList.add(su.getName());
                }
            }
        }
        result = new String[tmpList.size()];
        tmpList.toArray(result);
        return result;
    }

    public String[] getComponentsForDeployedServiceAssembly(String saName) {
        String[] result;
        // iterate through the service assembilies
        Set<String> tmpList = new HashSet<String>();
        ServiceAssembly sa = deployer.getServiceAssembly(saName);
        if (sa != null) {
            for (ServiceUnit su : sa.getServiceUnits()) {
                if (su.getComponent().getName().equals(saName)) {
                    tmpList.add(su.getComponent().getName());
                }
            }
        }
        result = new String[tmpList.size()];
        tmpList.toArray(result);
        return result;
    }

    /**
     * Returns a boolean value indicating whether the SU is currently deployed.
     *
     * @param componentName - name of component.
     * @param suName        - name of the Service Unit.
     * @return boolean value indicating whether the SU is currently deployed.
     */
    public boolean isDeployedServiceUnit(String componentName, String suName) {
        for (ServiceAssembly sa : deployer.getServiceAssemblies().values()) {
            ServiceUnit[] sus = sa.getServiceUnits();
            if (sus != null) {
                for (ServiceUnit su : sus) {
                    if (su.getComponent().getName().equals(componentName) && su.getName().equals(suName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
