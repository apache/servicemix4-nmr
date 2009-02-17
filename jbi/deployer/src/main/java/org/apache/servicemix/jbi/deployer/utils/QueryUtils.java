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
package org.apache.servicemix.jbi.deployer.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.impl.Deployer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;

/**
 * Helper class to query JBI artifacts from the OSGi registry
 * <p/>
 * TODO: the current code creates a lots of leak in OSGi services usage count
 * because nobody ever calls {@link BundleContext#ungetService(org.osgi.framework.ServiceReference)}.
 */
public final class QueryUtils {

    private QueryUtils() {
    }

    public static ServiceReference getComponentServiceReference(BundleContext bundleContext, String filter) {
        return OsgiServiceReferenceUtils.getServiceReference(
                bundleContext,
                Component.class.getName(),
                filter);
    }

    public static ServiceReference getSharedLibraryServiceReference(BundleContext bundleContext, String filter) {
        return OsgiServiceReferenceUtils.getServiceReference(
                bundleContext,
                SharedLibrary.class.getName(),
                filter);
    }

    public static ServiceReference getServiceAssemblyServiceReference(BundleContext bundleContext, String filter) {
        return OsgiServiceReferenceUtils.getServiceReference(
                bundleContext,
                ServiceAssembly.class.getName(),
                filter);
    }

    public static ServiceReference[] getComponentsServiceReferences(BundleContext bundleContext, String filter) {
        return OsgiServiceReferenceUtils.getServiceReferences(
                bundleContext,
                Component.class.getName(),
                filter);
    }

    public static ServiceReference[] getSharedLibrariesServiceReferences(BundleContext bundleContext, String filter) {
        return OsgiServiceReferenceUtils.getServiceReferences(
                bundleContext,
                SharedLibrary.class.getName(),
                filter);
    }

    public static ServiceReference[] getServiceAssembliesServiceReferences(BundleContext bundleContext, String filter) {
        return OsgiServiceReferenceUtils.getServiceReferences(
                bundleContext,
                ServiceAssembly.class.getName(),
                filter);
    }

    public static Component[] getComponents(BundleContext bundleContext, String filter) {
        ServiceReference[] refs = getComponentsServiceReferences(bundleContext, filter);
        List<Component> components = new ArrayList<Component>();
        for (ServiceReference ref : refs) {
            Component comp = (Component) bundleContext.getService(ref);
            if (comp != null) {
                components.add(comp);
            }
        }
        return components.toArray(new Component[components.size()]);
    }

    public static Component[] getAllComponents(BundleContext bundleContext) {
        return getComponents(bundleContext, null);
    }

    public static SharedLibrary[] getAllSharedLibraries(BundleContext bundleContext) {
        ServiceReference[] refs = getSharedLibrariesServiceReferences(bundleContext, null);
        List<SharedLibrary> sharedLibraries = new ArrayList<SharedLibrary>();
        for (ServiceReference ref : refs) {
            SharedLibrary sl = (SharedLibrary) bundleContext.getService(ref);
            if (sl != null) {
                sharedLibraries.add(sl);
            }
        }
        return sharedLibraries.toArray(new SharedLibrary[sharedLibraries.size()]);
    }

    public static ServiceAssembly[] getAllServiceAssemblies(BundleContext bundleContext) {
        ServiceReference[] refs = getServiceAssembliesServiceReferences(bundleContext, null);
        List<ServiceAssembly> serviceAssemblies = new ArrayList<ServiceAssembly>();
        for (ServiceReference ref : refs) {
            ServiceAssembly assembly = (ServiceAssembly) bundleContext.getService(ref);
            if (assembly != null) {
                serviceAssemblies.add(assembly);
            }
        }
        return serviceAssemblies.toArray(new ServiceAssembly[serviceAssemblies.size()]);
    }

    public static Component getComponent(BundleContext bundleContext, String name) {
        ServiceReference ref = getComponentServiceReference(bundleContext, getByNameFilter(name));
        return ref != null ? (Component) bundleContext.getService(ref) : null;
    }

    public static SharedLibrary getSharedLibrary(BundleContext bundleContext, String name) {
        ServiceReference ref = getSharedLibraryServiceReference(bundleContext, getByNameFilter(name));
        return ref != null ? (SharedLibrary) bundleContext.getService(ref) : null;
    }

    public static ServiceAssembly getServiceAssembly(BundleContext bundleContext, String name) {
        ServiceReference ref = getServiceAssemblyServiceReference(bundleContext, getByNameFilter(name));
        return ref != null ? (ServiceAssembly) bundleContext.getService(ref) : null;
    }

    public static boolean isBinding(BundleContext bundleContext, String componentName) {
        ServiceReference ref = getComponentServiceReference(bundleContext, getByNameFilter(componentName));
        return ref != null && Deployer.TYPE_BINDING_COMPONENT.equals(ref.getProperty(Deployer.TYPE));
    }

    public static boolean isEngine(BundleContext bundleContext, String componentName) {
        ServiceReference ref = getComponentServiceReference(bundleContext, getByNameFilter(componentName));
        return ref != null && Deployer.TYPE_SERVICE_ENGINE.equals(ref.getProperty(Deployer.TYPE));
    }

    public static String getByNameFilter(String name) {
        return "(" + Deployer.NAME + "=" + name + ")";
    }

}
