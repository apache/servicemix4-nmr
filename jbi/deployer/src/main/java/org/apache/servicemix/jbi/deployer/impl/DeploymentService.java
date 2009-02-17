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
import org.apache.servicemix.jbi.deployer.utils.QueryUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.BundleContextAware;

public class DeploymentService implements DeploymentServiceMBean, BundleContextAware {

    private static final Log LOG = LogFactory.getLog(DeploymentService.class);

    private BundleContext bundleContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public String deploy(String saZipURL) throws Exception {
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
                DescriptorFactory.checkDescriptor(root);
                ServiceAssemblyDesc sa = root.getServiceAssembly();
                if (sa == null) {
                    throw ManagementSupport.failure("deploy", "JBI descriptor is not an assembly descriptor: " + saZipURL);
                }
                checkSus(sa.getServiceUnits());
                String name = sa.getIdentification().getName();
                LOG.info("Deploy ServiceAssembly " + name);
                ServiceAssemblyInstaller saInstaller = new ServiceAssemblyInstaller(bundleContext, name);
                saInstaller.deploy(saZipURL);
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
                Component component = QueryUtils.getComponent(bundleContext, componentName);
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
        if (saName == null) {
            throw ManagementSupport.failure("undeploy", "SA name must not be null");
        }
        ServiceReference ref = QueryUtils.getServiceAssemblyServiceReference(bundleContext, "(" + Deployer.NAME + "=" + saName + ")");
        if (ref == null) {
            throw ManagementSupport.failure("undeploy", "SA has not been deployed: " + saName);
        }
        Bundle bundle = ref.getBundle();
        ServiceAssembly sa = (ServiceAssembly) bundleContext.getService(ref);


        String state = sa.getCurrentState();
        if (!LifeCycleMBean.SHUTDOWN.equals(state)) {
            throw ManagementSupport.failure("undeploy", "SA must be shut down: " + saName);
        }
        try {
            // TODO: shutdown sa before uninstalling bundle
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
        Component component = QueryUtils.getComponent(bundleContext, componentName);
        ServiceUnit[] serviceUnits = component.getServiceUnits();
        String[] sus = new String[serviceUnits.length];
        for (int i = 0; i < serviceUnits.length; i++) {
            sus[i] = serviceUnits[i].getName();
        }
        return sus;
    }

    public String[] getDeployedServiceAssemblies() throws Exception {
        ServiceAssembly[] assemblies = QueryUtils.getAllServiceAssemblies(bundleContext);
        String[] sas = new String[assemblies.length];
        for (int i = 0; i < assemblies.length; i++) {
            sas[i] = assemblies[i].getName();
        }
        return sas;
    }

    public String getServiceAssemblyDescriptor(String saName) throws Exception {
        ServiceAssembly sa = QueryUtils.getServiceAssembly(bundleContext, saName);
        return sa != null ? sa.getDescriptor() : null;
    }

    public boolean canDeployToComponent(String componentName) {
        Component component = QueryUtils.getComponent(bundleContext, componentName);
        return component != null
                && LifeCycleMBean.STARTED.equals(component.getCurrentState())
                && component.getComponent().getServiceUnitManager() != null;
    }

    public String start(String serviceAssemblyName) throws Exception {
        try {
            if (serviceAssemblyName == null) {
                throw ManagementSupport.failure("start", "SA name must not be null");
            }
            ServiceAssembly sa = QueryUtils.getServiceAssembly(bundleContext, serviceAssemblyName);
            if (sa == null) {
                throw ManagementSupport.failure("start", "SA has not exist: " + serviceAssemblyName);
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
            if (serviceAssemblyName == null) {
                throw ManagementSupport.failure("stop", "SA name must not be null");
            }
            ServiceAssembly sa = QueryUtils.getServiceAssembly(bundleContext, serviceAssemblyName);
            if (sa == null) {
                throw ManagementSupport.failure("stop", "SA has not exist: " + serviceAssemblyName);
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
            if (serviceAssemblyName == null) {
                throw ManagementSupport.failure("shutdown", "SA name must not be null");
            }
            ServiceAssembly sa = QueryUtils.getServiceAssembly(bundleContext, serviceAssemblyName);
            if (sa == null) {
                throw ManagementSupport.failure("shutdown", "SA has not exist: " + serviceAssemblyName);
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
            if (serviceAssemblyName == null) {
                throw ManagementSupport.failure("getState", "SA name must not be null");
            }
            ServiceAssembly sa = QueryUtils.getServiceAssembly(bundleContext, serviceAssemblyName);
            if (sa == null) {
                throw ManagementSupport.failure("getState", "SA has not exist: " + serviceAssemblyName);
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
        ServiceReference[] serviceRefs = QueryUtils.getServiceAssembliesServiceReferences(getBundleContext(), null);
        for (ServiceReference ref : serviceRefs) {
            ServiceAssembly sa = (ServiceAssembly) getBundleContext().getService(ref);
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
        ServiceReference[] serviceRefs = QueryUtils.getServiceAssembliesServiceReferences(getBundleContext(), null);
        for (ServiceReference ref : serviceRefs) {
            ServiceAssembly sa = (ServiceAssembly) getBundleContext().getService(ref);
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
        ServiceAssembly sa = QueryUtils.getServiceAssembly(getBundleContext(), saName);
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
        boolean result = false;
        ServiceReference[] serviceRefs = QueryUtils.getServiceAssembliesServiceReferences(getBundleContext(), null);
        for (ServiceReference ref : serviceRefs) {
            ServiceAssembly sa = (ServiceAssembly) getBundleContext().getService(ref);
            ServiceUnit[] sus = sa.getServiceUnits();
            if (sus != null) {
                for (int i = 0; i < sus.length; i++) {
                    if (sus[i].getComponent().getName().equals(componentName)
                            && sus[i].getName().equals(suName)) {
                        result = true;
                        break;
                    }
                }
            }
        }
        return result;
    }

}
