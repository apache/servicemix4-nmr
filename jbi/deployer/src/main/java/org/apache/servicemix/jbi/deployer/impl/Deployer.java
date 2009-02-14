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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.JBIException;
import javax.jbi.management.LifeCycleMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.DeployedAssembly;
import org.apache.servicemix.jbi.deployer.descriptor.ComponentDesc;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceAssemblyDesc;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceUnitDesc;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryDesc;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryList;
import org.apache.servicemix.jbi.deployer.descriptor.Identification;
import org.apache.servicemix.jbi.deployer.descriptor.Target;
import org.apache.servicemix.jbi.runtime.ComponentWrapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.osgi.util.BundleDelegatingClassLoader;
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

    private Map<String, ComponentImpl> components;

    private Map<String, Boolean> wrappedComponents;

    private Map<Bundle, List<ServiceRegistration>> services;

    private List<Bundle> pendingBundles;

    private File jbiRootDir;

    private PreferencesService preferencesService;

    private boolean autoStart = true;

    private ServiceTracker tracker;

    private ServiceTracker deployedAssembliesTracker;
    private AssemblyReferencesListener endpointListener;

    public Deployer() throws JBIException{
        sharedLibraries = new ConcurrentHashMap<String, SharedLibraryImpl>();
        serviceAssemblies = new ConcurrentHashMap<String, ServiceAssemblyImpl>();
        components = new ConcurrentHashMap<String, ComponentImpl>();
        wrappedComponents = new ConcurrentHashMap<String, Boolean>();
        services = new ConcurrentHashMap<Bundle, List<ServiceRegistration>>();
        pendingBundles = new ArrayList<Bundle>();
        // TODO: control that using properties
        jbiRootDir = new File(System.getProperty("servicemix.base"), "data/jbi");
        jbiRootDir.mkdirs();
    }

    public PreferencesService getPreferencesService() {
        return preferencesService;
    }

    public void setPreferencesService(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public void setEndpointListener(AssemblyReferencesListener endpointListener) {
        this.endpointListener = endpointListener;
    }

    public AssemblyReferencesListener getEndpointListener() {
        return endpointListener;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        tracker = new ServiceTracker(getBundleContext(), javax.jbi.component.Component.class.getName(), null) {
            public Object addingService(ServiceReference serviceReference) {
                Object o = super.addingService(serviceReference);
                maybeWrapComponent(serviceReference, (javax.jbi.component.Component) o);
                return o;
            }

            public void removedService(ServiceReference serviceReference, Object o) {
                maybeUnwrapComponent(serviceReference, (javax.jbi.component.Component) o);
                super.removedService(serviceReference, o);
            }
        };
        tracker.open();

        deployedAssembliesTracker = new ServiceTracker(getBundleContext(), DeployedAssembly.class.getName(), null) {
            public Object addingService(ServiceReference serviceReference) {
                Object o = super.addingService(serviceReference);
                registerDeployedServiceAssembly(serviceReference, (DeployedAssembly) o);
                return o;
            }

            public void removedService(ServiceReference serviceReference, Object o) {
                unregisterDeployedServiceAssembly(serviceReference, (DeployedAssembly) o);
                super.removedService(serviceReference, o);
            }
        };
        deployedAssembliesTracker.open();
    }

    @Override
    public void destroy() throws Exception {
        tracker.close();
        deployedAssembliesTracker.close();
        super.destroy();
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
            } else if (descriptor.getSharedLibrary() != null) {
                installSharedLibrary(descriptor.getSharedLibrary(), bundle);
            } else {
                throw new IllegalStateException("Unrecognized JBI descriptor: " + url);
            }
        } catch (PendingException e) {
            pendingBundles.add(e.getBundle());
            LOGGER.warn("Requirements not met for JBI artifact in bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle) + ". Installation pending. " + e);
        } catch (Exception e) {
            LOGGER.error("Error handling bundle start event", e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Override
    protected void unregister(Bundle bundle) {
        // Other bundles are JBI artifacts
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
        try {
            URL url = bundle.getResource(JBI_DESCRIPTOR);
            Descriptor descriptor = DescriptorFactory.buildDescriptor(url);
            if (descriptor.getComponent() != null) {
                uninstallComponent(descriptor.getComponent(), bundle);
            } else if (descriptor.getServiceAssembly() != null) {
                undeployServiceAssembly(descriptor.getServiceAssembly(), bundle);
            } else if (descriptor.getSharedLibrary() != null) {
                uninstallSharedLibrary(descriptor.getSharedLibrary(), bundle);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling bundle stop event", e);
        }
    }

    protected void installComponent(ComponentDesc componentDesc, Bundle bundle) throws Exception {
        LOGGER.info("Deploying bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' as a JBI component");
        // Check requirements
        if (componentDesc.getSharedLibraries() != null) {
            for (SharedLibraryList sl : componentDesc.getSharedLibraries()) {
                if (sharedLibraries.get(sl.getName()) == null) {
                    throw new PendingException(bundle, "SharedLibrary not installed: " + sl.getName());
                }
            }
        }
        String name = componentDesc.getIdentification().getName();
        // Create component class loader
        ClassLoader classLoader = createComponentClassLoader(componentDesc, bundle);
        Thread.currentThread().setContextClassLoader(classLoader);
        // Extract component (needed to feed the installRoot)
        // Few components actually use this, but Ode is one of them
        File installRoot = new File(System.getProperty("servicemix.base"), "data/jbi/" + name + "/install");
        installRoot.mkdirs();
        extractBundle(installRoot, bundle, "/");
        // Instanciate component
        Preferences prefs = preferencesService.getUserPreferences(name);
        Class clazz = classLoader.loadClass(componentDesc.getComponentClassName());
        javax.jbi.component.Component innerComponent = (javax.jbi.component.Component) clazz.newInstance();
        ComponentImpl component = new ComponentImpl(componentDesc, innerComponent, prefs, autoStart, this);
        components.put(name, component);
        // populate props from the component meta-data
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(NAME, name);
        props.put(TYPE, componentDesc.getType());
        // register the component in the OSGi registry
        LOGGER.debug("Registering JBI component");
        registerService(bundle, Component.class.getName(), component, props);
        registerService(bundle, ComponentWrapper.class.getName(), component, props);
        registerService(bundle, javax.jbi.component.Component.class.getName(), innerComponent, props);
    }

    private void extractBundle(File installRoot, Bundle bundle, String path) throws IOException {
        Enumeration e = bundle.getEntryPaths(path);
        while (e != null && e.hasMoreElements()) {
            String entry = (String) e.nextElement();
            File fout = new File(installRoot, entry);
            if (entry.endsWith("/")) {
                fout.mkdirs();
                extractBundle(installRoot, bundle, entry);
            } else {
                InputStream in = bundle.getEntry(entry).openStream();
                OutputStream out = new FileOutputStream(fout);
                try {
                    FileUtil.copyInputStream(in, out);
                } finally {
                    in.close();
                    out.close();
                }
            }
        }
    }

    protected void deployServiceAssembly(ServiceAssemblyDesc serviceAssembyDesc, Bundle bundle) throws Exception {
        LOGGER.info("Deploying bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' as a JBI service assembly");
        // Check requirements
        for (ServiceUnitDesc sud : serviceAssembyDesc.getServiceUnits()) {
            String componentName = sud.getTarget().getComponentName();
            ComponentImpl component = components.get(componentName);
            if (component == null) {
                throw new PendingException(bundle, "Component not installed: " + componentName);
            }
            if (LifeCycleMBean.UNKNOWN.equals(component.getCurrentState())) {
                throw new PendingException(bundle, "Component is in an unknown state: " + componentName);
            }
        }
        // Create the SA directory
        File saDir = new File(jbiRootDir, Long.toString(bundle.getBundleId()));
        FileUtil.deleteFile(saDir);
        FileUtil.buildDirectory(saDir);
        // Iterate each SU and deploy it
        List<ServiceUnitImpl> sus = new ArrayList<ServiceUnitImpl>();
        boolean failure = false;
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
            ComponentImpl component = components.get(componentName);
            // Create service unit object
            ServiceUnitImpl su = new ServiceUnitImpl(sud, suRootDir, component);
            try {
                LOGGER.debug("Deploying SU " + su.getName());
                su.deploy();
                // Add it to the list
                sus.add(su);
            } catch (Exception e) {
                LOGGER.error("Error deploying SU " + su.getName(), e);
                failure = true;
                break;
            }
        }
        // If failure, undeploy SU and exit
        if (failure) {
            for (ServiceUnitImpl su : sus) {
                try {
                    LOGGER.debug("Undeploying SU " + su.getName());
                    su.undeploy();
                } catch (Exception e) {
                    LOGGER.warn("Error undeploying SU " + su.getName(), e);
                }
            }
            return;
        }
        registerSA(serviceAssembyDesc, bundle, sus);

    }

    protected void registerSA(ServiceAssemblyDesc serviceAssembyDesc, Bundle bundle, List<ServiceUnitImpl> sus) throws JBIException {
        // Now create the SA and initialize it
        Preferences prefs = preferencesService.getUserPreferences(serviceAssembyDesc.getIdentification().getName());
        ServiceAssemblyImpl sa = new ServiceAssemblyImpl(serviceAssembyDesc, sus, prefs, endpointListener, autoStart);
        sa.init();
        serviceAssemblies.put(sa.getName(), sa);
        // populate props from the component meta-data
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(NAME, serviceAssembyDesc.getIdentification().getName());
        // register the service assembly in the OSGi registry
        LOGGER.debug("Registering JBI service assembly");
        registerService(bundle, ServiceAssembly.class.getName(), sa, props);
    }

    protected void installSharedLibrary(SharedLibraryDesc sharedLibraryDesc, Bundle bundle) {
        LOGGER.info("Deploying bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' as a JBI shared library");
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

    protected void uninstallComponent(ComponentDesc componentDesc, Bundle bundle) throws Exception {
        String name = componentDesc.getIdentification().getName();
        ComponentImpl component = components.remove(name);
        if (component != null) {
            if (component.getState() == ComponentImpl.State.Started) {
                component.stop(false);
            }
            if (component.getState() == ComponentImpl.State.Stopped) {
                component.shutDown(false, false);
            }
            Preferences prefs = preferencesService.getUserPreferences(name);
            prefs.clear();
            prefs.flush();
            File file = new File(System.getProperty("servicemix.base"), "data/jbi/" + name);
            FileUtil.deleteFile(file);
        }
    }
    protected void undeployServiceAssembly(ServiceAssemblyDesc serviceAssembyDesc, Bundle bundle) throws Exception {
        String name = serviceAssembyDesc.getIdentification().getName();
        unregisterSA(name);
    }

    protected void unregisterSA(String name) throws JBIException {
        ServiceAssemblyImpl sa = serviceAssemblies.remove(name);
        if (sa != null) {
            if (sa.getState() == ServiceAssemblyImpl.State.Started) {
                sa.stop(false);
            }
            if (sa.getState() == ServiceAssemblyImpl.State.Stopped) {
                sa.shutDown(false, false);
            }
            for (ServiceUnit su : sa.getServiceUnits()) {
                ((ServiceUnitImpl) su).undeploy();
            }
        }
    }

    protected void uninstallSharedLibrary(SharedLibraryDesc sharedLibraryDesc, Bundle bundle) throws JBIException {
        sharedLibraries.remove(sharedLibraryDesc.getIdentification().getName());
    }

    protected void maybeWrapComponent(ServiceReference reference, javax.jbi.component.Component component) {
        String name = (String) reference.getProperty(NAME);
        if (name != null && !components.containsKey(name)) {
            String type = (String) reference.getProperty(TYPE);
            Preferences prefs = preferencesService.getUserPreferences(name);
            ComponentDesc componentDesc = new ComponentDesc();
            componentDesc.setIdentification(new Identification());
            componentDesc.getIdentification().setName(name);
            componentDesc.setType(type);
            ComponentImpl wrapper = new ComponentImpl(componentDesc, component, prefs, autoStart, this);
            wrappedComponents.put(name, true);
            components.put(name, wrapper);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put(NAME, name);
            props.put(TYPE, componentDesc.getType());
            registerService(reference.getBundle(), Component.class.getName(), wrapper, props);
            registerService(reference.getBundle(), ComponentWrapper.class.getName(), wrapper, props);
        }
    }

    protected void maybeUnwrapComponent(ServiceReference reference, javax.jbi.component.Component component) {
        String name = (String) reference.getProperty(NAME);
        if (name != null && Boolean.TRUE.equals(wrappedComponents.remove(name))) {
            ComponentImpl ci = components.remove(name);
            if (ci != null) {
                try {
                    ci.stop(false);
                    ci.shutDown(false, false);
                    pendingBundles.remove(reference.getBundle());
                    List<ServiceRegistration> registrations = services.remove(reference.getBundle());
                    if (registrations != null) {
                    	for (ServiceRegistration reg : registrations) {
                            try {
                            	reg.unregister();
                            } catch (IllegalStateException e) {
                            	e.printStackTrace();
                                // Ignore
                            }
                        }
                    }
                } catch (JBIException e) {
                    LOGGER.warn("Error when shutting down component", e);
                }
            }
        }
    }

    protected void checkPendingBundles() {
        if (!pendingBundles.isEmpty()) {
            final List<Bundle> pending = pendingBundles;
            pendingBundles = new ArrayList<Bundle>();
            // Synchronous call because if using a separate thread
            // we run into deadlocks
            for (Bundle bundle : pending) {
                register(bundle);
            }
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

    protected ClassLoader createComponentClassLoader(ComponentDesc component, Bundle bundle) {
        // Create parents classloaders
        ClassLoader[] parents;
        if (component.getSharedLibraries() != null) {
            parents = new ClassLoader[component.getSharedLibraries().length + 2];
            for (int i = 0; i < component.getSharedLibraries().length; i++) {
                parents[i + 2] = getSharedLibraryClassLoader(component.getSharedLibraries()[i]);
            }
        } else {
            parents = new ClassLoader[2];
        }
        parents[0] = BundleDelegatingClassLoader.createBundleClassLoaderFor(getBundleContext().getBundle(0));
        parents[1] = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, getClass().getClassLoader());

        // Create urls
        String[] classPathNames = component.getComponentClassPath().getPathElements();
        URL[] urls = new URL[classPathNames.length];
        for (int i = 0; i < classPathNames.length; i++) {
            urls[i] = bundle.getResource(classPathNames[i]);
            if (urls[i] == null) {
                throw new IllegalArgumentException("SharedLibrary classpath entry not found: '" +  classPathNames[i] + "'");
            }
            Enumeration en = bundle.findEntries(classPathNames[i], null, false);
            if (en != null && en.hasMoreElements()) {
                try {
                    urls[i] = new URL(urls[i].toString() + "/");
                } catch (MalformedURLException e) {
                    // Ignore
                }
            }
        }

        // Create classloader
        return new OsgiMultiParentClassLoader(
                        bundle,
                        component.getIdentification().getName(),
                        urls,
                        parents,
                        component.isComponentClassLoaderDelegationSelfFirst(),
                        new String[] {"javax.xml.bind"},
                        new String[] {"java.", "javax." });
    }

    protected ClassLoader getSharedLibraryClassLoader(SharedLibraryList sharedLibraryList) {
        SharedLibraryImpl sl = sharedLibraries.get(sharedLibraryList.getName());
        if (sl != null) {
            return sl.getClassLoader();
        } else {
            throw new IllegalStateException("SharedLibrary not installed: " + sharedLibraryList.getName());
        }
    }

    public void registerDeployedServiceAssembly(ServiceReference serviceReference, DeployedAssembly assembly) {
        try {
            assembly.deploy();
            ServiceAssemblyDesc desc = new ServiceAssemblyDesc();
            desc.setIdentification(new Identification());
            desc.getIdentification().setName(assembly.getName());
            List<ServiceUnitImpl> sus = new ArrayList<ServiceUnitImpl>();
            for (Map.Entry<String, String> unit : assembly.getServiceUnits().entrySet()) {
                ServiceUnitDesc suDesc = new ServiceUnitDesc();
                suDesc.setIdentification(new Identification());
                suDesc.getIdentification().setName(unit.getKey());
                suDesc.setTarget(new Target());
                suDesc.getTarget().setComponentName(unit.getValue());
                ServiceUnitImpl su = new ServiceUnitImpl(suDesc, null, components.get(unit.getValue()));
                sus.add(su);
            }
            registerSA(desc, serviceReference.getBundle(), sus);
        } catch (Exception e) {
            LOGGER.error("Error registering deployed service assembly", e);
        }
    }

    public void unregisterDeployedServiceAssembly(ServiceReference serviceReference, DeployedAssembly assembly) {
        try {
            unregisterSA(assembly.getName());
        } catch (Exception e) {
            LOGGER.error("Error unregistering deployed service assembly", e);
        }
    }
    
    public Set<String> getInstalledComponents() {
    	return this.components.keySet();
    }
    
    public Set<String> getInstalledSharedLibararies() {
    	return this.sharedLibraries.keySet();
    }
    
    public Set<String> getDeployServiceAssemblies() {
    	return this.serviceAssemblies.keySet();
    }

}
