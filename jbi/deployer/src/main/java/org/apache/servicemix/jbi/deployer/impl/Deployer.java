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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.JBIException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.descriptor.ComponentDesc;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceAssemblyDesc;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceUnitDesc;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryDesc;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryList;
import org.apache.xbean.classloader.MultiParentClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.util.BundleDelegatingClassLoader;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * Deployer for JBI artifacts
 *
 */
public class Deployer extends AbstractBundleWatcher {

    public static final String NAME = "NAME";
    public static final String TYPE = "TYPE";

    private static final Log LOGGER = LogFactory.getLog(Deployer.class);

    private static final String JBI_DESCRIPTOR = "META-INF/jbi.xml";

    private Map<String, SharedLibraryImpl> sharedLibraries;

    private Map<String, ServiceAssemblyImpl> serviceAssemblies;

    private Map<Bundle, List<ServiceRegistration>> services;

    private List<Bundle> pendingBundles;

    private File jbiRootDir;

    public Deployer() throws JBIException{
        sharedLibraries = new ConcurrentHashMap<String, SharedLibraryImpl>();
        serviceAssemblies = new ConcurrentHashMap<String, ServiceAssemblyImpl>();
        services = new ConcurrentHashMap<Bundle, List<ServiceRegistration>>();
        pendingBundles = new ArrayList<Bundle>();
        // TODO: control that using properties
        jbiRootDir = new File(System.getProperty("servicemix.base"), "data/jbi");
        jbiRootDir.mkdirs();
    }

    @Override
    protected boolean match(Bundle bundle) {
        LOGGER.debug("Checking bundle: '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "'");
        URL url = bundle.getResource(JBI_DESCRIPTOR);
        if (url == null) {
            LOGGER.debug("Bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' does not contain any JBI descriptor.");
            return false;
        }
        return true;
    }

    @Override
    protected void register(Bundle bundle) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            URL url = bundle.getResource(JBI_DESCRIPTOR);
            Descriptor descriptor = DescriptorFactory.buildDescriptor(url);
            DescriptorFactory.checkDescriptor(descriptor);
            if (descriptor.getComponent() != null) {
                installComponent(descriptor.getComponent(), bundle);
            } else if (descriptor.getServiceAssembly() != null) {
                deployServiceAssembly(descriptor.getServiceAssembly(), bundle);
            } else {
                installSharedLibrary(descriptor.getSharedLibrary(), bundle);
            }
        } catch (PendingException e) {
            pendingBundles.add(e.getBundle());
            LOGGER.warn("JBI artifact requirements not met. Installation pending.");
        } catch (Exception e) {
            LOGGER.error("Error handling bundle event", e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    protected void unregister(Bundle bundle) {
        pendingBundles.remove(bundle);
        List<ServiceRegistration> registrations = services.remove(bundle);
        if (registrations != null) {
            for (ServiceRegistration reg : registrations) {
                try {
                    reg.unregister();
                } catch (IllegalStateException e) {
                    // Ignore
                }
            }
        }
    }

    protected void installComponent(ComponentDesc componentDesc, Bundle bundle) throws Exception {
        LOGGER.debug("Bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' is a JBI component");
        // Check requirements
        if (componentDesc.getSharedLibraries() != null) {
            for (SharedLibraryList sl : componentDesc.getSharedLibraries()) {
                if (sharedLibraries.get(sl.getName()) == null) {
                    throw new PendingException(bundle, "SharedLibrary not installed: " + sl.getName());
                }
            }
        }
        // Create component class loader
        ClassLoader classLoader = createComponentClassLoader(componentDesc, bundle);
        Thread.currentThread().setContextClassLoader(classLoader);
        // Instanciate component
        Class clazz = classLoader.loadClass(componentDesc.getComponentClassName());
        javax.jbi.component.Component innerComponent = (javax.jbi.component.Component) clazz.newInstance();
        ComponentImpl component = new ComponentImpl(componentDesc, innerComponent);
        // populate props from the component meta-data
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(NAME, componentDesc.getIdentification().getName());
        props.put(TYPE, componentDesc.getType());
        // register the component in the OSGi registry
        LOGGER.debug("Registering JBI component");
        registerService(bundle, Component.class.getName(), component, props);
        registerService(bundle, javax.jbi.component.Component.class.getName(), component.getComponent(), props);
        // Check pending bundles
        checkPendingBundles();
    }

    protected void deployServiceAssembly(ServiceAssemblyDesc serviceAssembyDesc, Bundle bundle) throws Exception {
        LOGGER.debug("Bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' is a JBI service assembly");
        // Check requirements
        for (ServiceUnitDesc sud : serviceAssembyDesc.getServiceUnits()) {
            String componentName = sud.getTarget().getComponentName();
            Component component = getComponent(componentName);
            if (component == null) {
                throw new PendingException(bundle, "Component not installed: " + componentName);
            }
        }
        // Create the SA directory
        File saDir = new File(jbiRootDir, Long.toString(bundle.getBundleId()));
        FileUtil.deleteFile(saDir);
        FileUtil.buildDirectory(saDir);
        // Iterate each SU and deploy it
        List<ServiceUnitImpl> sus = new ArrayList<ServiceUnitImpl>();
        for (ServiceUnitDesc sud : serviceAssembyDesc.getServiceUnits()) {
            // Create directory for this SU
            File suRootDir = new File(saDir, sud.getIdentification().getName());
            suRootDir.mkdirs();
            // Unpack it
            String zip = sud.getTarget().getArtifactsZip();
            URL zipUrl = bundle.getResource(zip);
            FileUtil.unpackArchive(zipUrl, suRootDir);
            // Find component
            String componentName = sud.getTarget().getComponentName();
            Component component = getComponent(componentName);
            // Create service unit object
            ServiceUnitImpl su = new ServiceUnitImpl(sud, suRootDir, component);
            su.deploy();
            // Add it to the list
            sus.add(su);
        }
        // Now create the SA and initialize it
        ServiceAssemblyImpl sa = new ServiceAssemblyImpl(serviceAssembyDesc, sus);
        sa.init();
        // populate props from the component meta-data
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(NAME, serviceAssembyDesc.getIdentification().getName());
        // register the service assembly in the OSGi registry
        LOGGER.debug("Registering JBI service assembly");
        registerService(bundle, ServiceAssembly.class.getName(), sa, props);
    }

    protected void installSharedLibrary(SharedLibraryDesc sharedLibraryDesc, Bundle bundle) {
        LOGGER.debug("Bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' is a JBI shared library");
        SharedLibraryImpl sl = new SharedLibraryImpl(sharedLibraryDesc, bundle);
        sharedLibraries.put(sl.getName(), sl);
        Dictionary<String, String> props = new Hashtable<String, String>();
        // populate props from the library meta-data
        props.put(NAME, sharedLibraryDesc.getIdentification().getName());
        LOGGER.debug("Registering JBI Shared Library");
        registerService(bundle, SharedLibrary.class.getName(), sl, props);
        // Check pending bundles
        checkPendingBundles();
    }

    protected void checkPendingBundles() {
        List<Bundle> pending = pendingBundles;
        pendingBundles = new ArrayList<Bundle>();
        for (Bundle bundle : pending) {
            register(bundle);
        }
    }

    protected void registerService(Bundle bundle, String clazz, Object service, Dictionary props) {
        BundleContext context = bundle.getBundleContext() != null ? bundle.getBundleContext() : getBundleContext();
        ServiceRegistration reg = context.registerService(clazz, service, props);
        List<ServiceRegistration> registrations = services.get(bundle);
        if (registrations == null) {
            registrations = new ArrayList<ServiceRegistration>();
            services.put(bundle, registrations);
        }
        registrations.add(reg);
    }

    protected Component getComponent(String name) {
        String filter = "(" + NAME + "=" + name + ")";
        BundleContext context = getBundleContext();
        ServiceReference reference = OsgiServiceReferenceUtils.getServiceReference(context, Component.class.getName(), filter);
        return (Component) OsgiServiceUtils.getService(context, reference);
    }

    protected ClassLoader createComponentClassLoader(ComponentDesc component, Bundle bundle) {
        // Create parents classloaders
        ClassLoader[] parents;
        if (component.getSharedLibraries() != null) {
            parents = new ClassLoader[component.getSharedLibraries().length + 1];
            for (int i = 0; i < component.getSharedLibraries().length; i++) {
                parents[i + 1] = getSharedLibraryClassLoader(component.getSharedLibraries()[i]);
            }
        } else {
            parents = new ClassLoader[1];
        }
        parents[0] = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, getClass().getClassLoader());

        // Create urls
        String[] classPathNames = component.getComponentClassPath().getPathElements();
        URL[] urls = new URL[classPathNames.length];
        for (int i = 0; i < classPathNames.length; i++) {
            urls[i] = bundle.getResource(classPathNames[i]);
            if (urls[i] == null) {
                throw new IllegalArgumentException("SharedLibrary classpath entry not found: '" +  classPathNames[i] + "'");
            }
        }

        // Create classloader
        return new MultiParentClassLoader(
                        component.getIdentification().getName(),
                        urls,
                        parents,
                        component.isComponentClassLoaderDelegationSelfFirst(),
                        new String[0],
                        new String[] {"java.", "javax." });
    }

    protected ClassLoader getSharedLibraryClassLoader(SharedLibraryList sharedLibraryList) {
        SharedLibraryImpl sl = sharedLibraries.get(sharedLibraryList.getName());
        if (sl != null) {
            return sl.createClassLoader();
        } else {
            throw new IllegalStateException("SharedLibrary not installed: " + sharedLibraryList.getName());
        }
    }

}
