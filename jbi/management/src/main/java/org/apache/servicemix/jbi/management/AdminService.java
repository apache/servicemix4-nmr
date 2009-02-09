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

import java.util.HashSet;
import java.util.Set;

import javax.jbi.management.AdminServiceMBean;
import javax.management.ObjectName;

import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.handler.JBIDeploymentListener;
import org.apache.servicemix.jbi.deployer.impl.Deployer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;

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

	protected ObjectName getComponentObjectName(ServiceReference ref) {
        String name = (String) ref.getProperty(Deployer.NAME);
        return namingStrategy.createCustomComponentMBeanName(name, "LifeCycle");
    }

    protected ServiceReference getComponentServiceReference(String filter) {
        return OsgiServiceReferenceUtils.getServiceReference(
                        getBundleContext(),
                        org.apache.servicemix.jbi.deployer.Component.class.getName(),
                        filter);
    }
    
    protected ServiceReference getSLServiceReference(String filter) {
        return OsgiServiceReferenceUtils.getServiceReference(
                        getBundleContext(),
                        org.apache.servicemix.jbi.deployer.SharedLibrary.class.getName(),
                        filter);
    }

    protected ServiceReference getSAServiceReference(String filter) {
        return OsgiServiceReferenceUtils.getServiceReference(
                        getBundleContext(),
                        org.apache.servicemix.jbi.deployer.ServiceAssembly.class.getName(),
                        filter);
    }
    
    protected ServiceReference[] getSAServiceReferences(String filter) {
        return OsgiServiceReferenceUtils.getServiceReferences(
                        getBundleContext(),
                        org.apache.servicemix.jbi.deployer.ServiceAssembly.class.getName(),
                        filter);
    }
    
    protected ServiceReference[] getComponentServiceReferences(String filter) {
        return OsgiServiceReferenceUtils.getServiceReferences(
                        getBundleContext(),
                        org.apache.servicemix.jbi.deployer.Component.class.getName(),
                        filter);
    }

    public ObjectName[] getBindingComponents() {
        String filter = "(" + Deployer.TYPE + "=binding-component)";
        ServiceReference refs[] = getComponentServiceReferences(filter);
        ObjectName[] names = new ObjectName[refs.length];
        for (int i = 0; i < refs.length; i++) {
            names[i] = getComponentObjectName(refs[i]);
        }
        return names;
    }

    public ObjectName getComponentByName(String name) {
        String filter = "(" + Deployer.NAME + "=" + name + ")";
        ServiceReference ref = getComponentServiceReference(filter);
        return getComponentObjectName(ref);
    }

    public ObjectName[] getEngineComponents() {
        String filter = "(" + Deployer.TYPE + "=service-engine)";
        ServiceReference refs[] = getComponentServiceReferences(filter);
        ObjectName[] names = new ObjectName[refs.length];
        for (int i = 0; i < refs.length; i++) {
            names[i] = getComponentObjectName(refs[i]);
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
        String filter = "(" + Deployer.NAME + "=" + componentName + ")";
        ServiceReference ref = getComponentServiceReference(filter);
        return "binding-component".equals(ref.getProperty(Deployer.TYPE));
    }

    public boolean isEngine(String componentName) {
        String filter = "(" + Deployer.NAME + "=" + componentName + ")";
        ServiceReference ref = getComponentServiceReference(filter);
        return "service-engine".equals(ref.getProperty(Deployer.TYPE));
    }
    
    public Deployer getDeployer() throws InvalidSyntaxException {
    	ServiceReference ref = OsgiServiceReferenceUtils.getServiceReference(
                getBundleContext(),
                JBIDeploymentListener.class.getName(),
                null);
    	return (Deployer) ((JBIDeploymentListener)getBundleContext().getService(ref)).getDeployer();
	}
    
    /**
     * Returns a list of Service Assemblies that contain SUs for the given component.
     * 
     * @param componentName name of the component.
     * @return list of Service Assembly names.
     */
    public String[] getDeployedServiceAssembliesForComponent(String componentName) {
        String[] result = null;
        // iterate through the service assembiliessalc
        Set<String> tmpList = new HashSet<String>();
        ServiceReference[] serviceRefs = getSAServiceReferences(null);
        for (ServiceReference ref : serviceRefs) {
        	ServiceAssembly sa = (ServiceAssembly) getBundleContext().getService(ref);
            ServiceUnit[] sus = sa.getServiceUnits();
            if (sus != null) {
                for (int i = 0; i < sus.length; i++) {
                    if (sus[i].getComponent().getName().equals(componentName)) {
                        tmpList.add(sa.getName());
                    }
                }
            }
        }
        result = new String[tmpList.size()];
        tmpList.toArray(result);
        return result;
    }
    
    public String[] getDeployedServiceUnitsForComponent(String componentName) {
        String[] result = null;
        // iterate through the service assembiliessalc
        Set<String> tmpList = new HashSet<String>();
        ServiceReference[] serviceRefs = getSAServiceReferences(null);
        for (ServiceReference ref : serviceRefs) {
        	ServiceAssembly sa = (ServiceAssembly) getBundleContext().getService(ref);
            ServiceUnit[] sus = sa.getServiceUnits();
            if (sus != null) {
                for (int i = 0; i < sus.length; i++) {
                    if (sus[i].getComponent().getName().equals(componentName)) {
                        tmpList.add(sus[i].getName());
                    }
                }
            }
        }
        result = new String[tmpList.size()];
        tmpList.toArray(result);
        return result;
    }
    
    public String[] getComponentsForDeployedServiceAssembly(String saName) {
        String[] result = null;
        // iterate through the service assembiliessalc
        Set<String> tmpList = new HashSet<String>();
        ServiceReference ref = getSAServiceReference("(" + Deployer.NAME + "=" + saName + ")");
        if (ref != null) {
        	ServiceAssembly sa = (ServiceAssembly) getBundleContext().getService(ref);
            ServiceUnit[] sus = sa.getServiceUnits();
            if (sus != null) {
                for (int i = 0; i < sus.length; i++) {
                    if (sus[i].getComponent().getName().equals(saName)) {
                        tmpList.add(sus[i].getComponent().getName());
                    }
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
     * @param suName - name of the Service Unit.
     * @return boolean value indicating whether the SU is currently deployed.
     */
    public boolean isDeployedServiceUnit(String componentName, String suName) {
        boolean result = false;
        ServiceReference[] serviceRefs = getSAServiceReferences(null);
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
