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

import java.util.HashSet;
import java.util.Set;

import javax.jbi.management.AdminServiceMBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.utils.QueryUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.BundleContextAware;

/**
 */
public class AdminService implements AdminServiceMBean, BundleContextAware {

    public static final String DEFAULT_NAME = "ServiceMix4";

    public static final String DEFAULT_DOMAIN = "org.apache.servicemix";

    public static final String DEFAULT_CONNECTOR_PATH = "/jmxrmi";

    public static final int DEFAULT_CONNECTOR_PORT = 1099;

    private BundleContext bundleContext;
    private DefaultNamingStrategy namingStrategy;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }


    public void setNamingStrategy(DefaultNamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public DefaultNamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public ObjectName[] getBindingComponents() {
        String filter = "(" + Deployer.TYPE + "=" + Deployer.TYPE_BINDING_COMPONENT + ")";
        Component[] components = QueryUtils.getComponents(getBundleContext(), filter);
        ObjectName[] names = new ObjectName[components.length];
        for (int i = 0; i < components.length; i++) {
            try {
                names[i] = namingStrategy.getObjectName(components[i]);
            } catch (MalformedObjectNameException e) {
            }
        }
        return names;
    }

    public ObjectName getComponentByName(String name) {
        Component component = QueryUtils.getComponent(bundleContext, name);
        try {
            return namingStrategy.getObjectName(component);
        } catch (MalformedObjectNameException e) {
            return null;
        }
    }

    public ObjectName[] getEngineComponents() {
        String filter = "(" + Deployer.TYPE + "=" + Deployer.TYPE_SERVICE_ENGINE + ")";
        Component[] components = QueryUtils.getComponents(getBundleContext(), filter);
        ObjectName[] names = new ObjectName[components.length];
        for (int i = 0; i < components.length; i++) {
            try {
                names[i] = namingStrategy.getObjectName(components[i]);
            } catch (MalformedObjectNameException e) {
            }
        }
        return names;
    }

    public String getSystemInfo() {
        return "ServiceMix 4";
    }

    public ObjectName getSystemService(String serviceName) {
        return null;
    }

    public ObjectName[] getSystemServices() {
        return new ObjectName[0];
    }

    public boolean isBinding(String componentName) {
        return QueryUtils.isBinding(getBundleContext(), componentName);
    }

    public boolean isEngine(String componentName) {
        return QueryUtils.isEngine(getBundleContext(), componentName);
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
        // iterate through the service assembiliessalc
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
        // iterate through the service assembiliessalc
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
