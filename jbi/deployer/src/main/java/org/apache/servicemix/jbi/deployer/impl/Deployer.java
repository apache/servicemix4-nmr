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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.JBIException;
import javax.management.MBeanServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.DeployedAssembly;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.events.LifeCycleListener;
import org.apache.servicemix.jbi.deployer.events.LifeCycleEvent;
import org.apache.servicemix.jbi.deployer.artifacts.AbstractLifecycleJbiArtifact;
import org.apache.servicemix.jbi.deployer.artifacts.ComponentImpl;
import org.apache.servicemix.jbi.deployer.artifacts.ServiceAssemblyImpl;
import org.apache.servicemix.jbi.deployer.artifacts.ServiceUnitImpl;
import org.apache.servicemix.jbi.deployer.artifacts.SharedLibraryImpl;
import org.apache.servicemix.jbi.deployer.artifacts.AssemblyReferencesListener;
import org.apache.servicemix.jbi.deployer.descriptor.ComponentDesc;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.descriptor.Identification;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceAssemblyDesc;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceUnitDesc;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryDesc;
import org.apache.servicemix.jbi.deployer.descriptor.Target;
import org.apache.servicemix.jbi.runtime.ComponentWrapper;
import org.apache.servicemix.jbi.runtime.Environment;
import org.apache.servicemix.nmr.api.event.ListenerRegistry;
import org.apache.servicemix.nmr.core.ListenerRegistryImpl;
import org.fusesource.commons.management.ManagementStrategy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.BundleEvent;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * Deployer for JBI artifacts
 */
public class Deployer implements SynchronousBundleListener, LifeCycleListener {

    public static final String NAME = "NAME";
    public static final String TYPE = "TYPE";
    public static final String TYPE_SERVICE_ENGINE = "service-engine";
    public static final String TYPE_BINDING_COMPONENT = "binding-component";

    private static final Log LOGGER = LogFactory.getLog(Deployer.class);

    private BundleContext bundleContext;
    private final Set<Bundle> bundles = new HashSet<Bundle>();

    private final Map<String, SharedLibraryImpl> sharedLibraries = new ConcurrentHashMap<String, SharedLibraryImpl>();
    private final Map<String, ComponentImpl> components = new ConcurrentHashMap<String, ComponentImpl>();
    private final Map<String, ServiceAssemblyImpl> serviceAssemblies = new ConcurrentHashMap<String, ServiceAssemblyImpl>();

    private final Map<Bundle, AbstractInstaller> installers = new ConcurrentHashMap<Bundle, AbstractInstaller>();

    private final ThreadLocal<AbstractInstaller> jmxManaged = new ThreadLocal<AbstractInstaller>();

    private final Map<String, Boolean> wrappedComponents = new ConcurrentHashMap<String, Boolean>();

    private final Map<Bundle, List<ServiceRegistration>> services = new ConcurrentHashMap<Bundle, List<ServiceRegistration>>();

    private final Set<AbstractInstaller> pendingInstallers = new HashSet<AbstractInstaller>();

    private final Set<ServiceAssemblyImpl> pendingAssemblies = new HashSet<ServiceAssemblyImpl>();

    private File jbiRootDir;

    private PreferencesService preferencesService;

    private boolean autoStart = true;

    private ServiceTracker deployedComponentsTracker;

    private ServiceTracker deployedAssembliesTracker;

    private AssemblyReferencesListener endpointListener;
    
    private int shutdownTimeout;

    // Helper beans
    private ManagementStrategy managementStrategy;
    private Environment environment;

    private ListenerRegistry listenerRegistry;
    
    private MBeanServer mbeanServer;

    public Deployer() throws JBIException {
        // TODO: control that using properties
        jbiRootDir = new File(System.getProperty("servicemix.base"), "data/jbi");
        jbiRootDir.mkdirs();
        // Create listener registry
        listenerRegistry = new ListenerRegistryImpl();
        listenerRegistry.register(this, null);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
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

    public ManagementStrategy getManagementStrategy() {
        return managementStrategy;
    }

    public void setManagementStrategy(ManagementStrategy managementStrategy) {
        this.managementStrategy = managementStrategy;
    }
    
    public MBeanServer getMbeanServer() {
        return mbeanServer;
    }

    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public Environment getEnvironment() {
        return environment;
    }

    protected AbstractInstaller getJmxManaged() {
        return jmxManaged.get();
    }

    public void setJmxManaged(AbstractInstaller installer) {
        jmxManaged.set(installer);
    }

    public Map<String, SharedLibraryImpl> getSharedLibraries() {
        return sharedLibraries;
    }

    public Map<String, ComponentImpl> getComponents() {
        return components;
    }

    public Map<String, ServiceAssemblyImpl> getServiceAssemblies() {
        return serviceAssemblies;
    }

    public SharedLibraryImpl getSharedLibrary(String name) {
        return name != null ? sharedLibraries.get(name) : null;
    }

    public ComponentImpl getComponent(String name) {
        return name != null ? components.get(name) : null;
    }

    public ServiceAssemblyImpl getServiceAssembly(String name) {
        return name != null ? serviceAssemblies.get(name) : null;
    }
    
    public void setShutdownTimeout(int shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }
    
    public int getShutdownTimeout() {
        return shutdownTimeout;
    }

    public void init() throws Exception {
        // Track bundles
        bundleContext.addBundleListener(this);
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getState() == Bundle.ACTIVE) {
                bundleChanged(new BundleEvent(BundleEvent.STARTED, bundle));
            }
        }
        // Track deployed components
        deployedComponentsTracker = new ServiceTracker(bundleContext, javax.jbi.component.Component.class.getName(), null) {
            public Object addingService(ServiceReference serviceReference) {
                Object o = super.addingService(serviceReference);
                registerDeployedComponent(serviceReference, (javax.jbi.component.Component) o);
                return o;
            }

            public void removedService(ServiceReference serviceReference, Object o) {
                unregisterDeployedComponent(serviceReference, (javax.jbi.component.Component) o);
                super.removedService(serviceReference, o);
            }
        };
        deployedComponentsTracker.open();
        // Track deployed service assemblies
        deployedAssembliesTracker = new ServiceTracker(bundleContext, DeployedAssembly.class.getName(), null) {
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

    public synchronized void destroy() throws Exception {
        for (Bundle bundle : bundles) {
            bundleChanged(new BundleEvent(BundleEvent.STOPPING, bundle));
        }
        deployedComponentsTracker.close();
        deployedAssembliesTracker.close();
        bundleContext.removeBundleListener(this);
    }

    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTED:
                if (match(event.getBundle())) {
                    bundles.add(event.getBundle());
                    onBundleStarted(event.getBundle());
                }
                break;
            case BundleEvent.STOPPING:
                if (bundles.contains(event.getBundle())) {
                    onBundleStopping(event.getBundle());
                }
                break;
            case BundleEvent.UNINSTALLED:
                if (bundles.remove(event.getBundle())) {
                    onBundleUninstalled(event.getBundle());
                }
                break;
        }
    }

    protected boolean match(Bundle bundle) {
        LOGGER.debug("Checking bundle: '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "'");
        URL url = bundle.getResource(DescriptorFactory.DESCRIPTOR_FILE);
        if (url == null) {
            LOGGER.debug("Bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' does not contain any JBI descriptor.");
            return false;
        }
        return true;
    }

    /**
     * Callback called when a new bundle has been started.
     * This method will check if the bundle is a JBI artifact and will process it accordingly.
     *
     * @param bundle
     */
    protected void onBundleStarted(Bundle bundle) {
        // If an installer has been registered, this means that we are using JMX or ant tasks to deploy the JBI artifact.
        // In such a case, let the installer do the work.
        // Else, the bundle has been deployed through the deploy folder or the command line or any other
        // non JBI way, which means we need to create an installer and install the artifact now.
        AbstractInstaller installer = getJmxManaged();
        if (installer != null) {
            installers.put(bundle, installer);
        } else {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                // Check if there is an already existing installer
                // This is certainly the case when a bundle has been stopped and is restarted.
                installer = installers.get(bundle);
                if (installer == null) {
                    URL url = bundle.getResource(DescriptorFactory.DESCRIPTOR_FILE);
                    Descriptor descriptor = DescriptorFactory.buildDescriptor(url);
                    DescriptorFactory.checkDescriptor(descriptor);
                    if (descriptor.getSharedLibrary() != null) {
                        LOGGER.info("Deploying bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' as a JBI shared library");
                        installer = new SharedLibraryInstaller(this, descriptor, null, true);
                    } else if (descriptor.getComponent() != null) {
                        LOGGER.info("Deploying bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' as a JBI component");
                        installer = new ComponentInstaller(this, descriptor, null, true);
                    } else if (descriptor.getServiceAssembly() != null) {
                        LOGGER.info("Deploying bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' as a JBI service assembly");
                        installer = new ServiceAssemblyInstaller(this, descriptor, (File) null, true);
                    } else {
                        throw new IllegalStateException("Unrecognized JBI descriptor: " + url);
                    }
                    installer.setBundle(bundle);
                }

                // TODO: handle the case where the bundle is restarted: i.e. the artifact is already installed
                try {
                    installer.init();
                    installer.install();
                    // only register installer if installation has been successfull
                    installers.put(bundle, installer);
                } catch (PendingException e) {
                    pendingInstallers.add(installer);
                    LOGGER.warn("Requirements not met for JBI artifact in bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle) + ". Installation pending. " + e);
                }
            } catch (Exception e) {
                LOGGER.error("Error handling bundle start event", e);
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
    }

    protected void onBundleStopping(Bundle bundle) {
        AbstractInstaller installer = getJmxManaged();
        if (installer == null) {
            installer = installers.get(bundle);
            if (installer != null) {
                try {
                    installer.stop(true);
                } catch (Exception e) {
                    LOGGER.warn("Error shutting down JBI artifact", e);
                }
            }
        }
        unregisterServices(bundle);
    }

    protected void onBundleUninstalled(Bundle bundle) {
        AbstractInstaller installer = getJmxManaged();
        if (installer == null) {
            installer = installers.remove(bundle);
            if (installer != null) {
                try {
                    pendingInstallers.remove(installer);
                    installer.setUninstallFromOsgi(true);
                    installer.uninstall(true);

                } catch (Exception e) {
                    LOGGER.warn("Error uninstalling JBI artifact", e);
                }
            }
        } else {
            installers.remove(bundle);
        }
    }

    public ServiceUnitImpl createServiceUnit(ServiceUnitDesc sud, File suRootDir, ComponentImpl component) {
        return new ServiceUnitImpl(sud, suRootDir, component);
    }

    public SharedLibraryImpl registerSharedLibrary(Bundle bundle, SharedLibraryDesc sharedLibraryDesc, ClassLoader classLoader) throws Exception {
        // Create shared library
        SharedLibraryImpl sl = new SharedLibraryImpl(bundle, sharedLibraryDesc, classLoader);
        sharedLibraries.put(sl.getName(), sl);
        Dictionary<String, String> props = new Hashtable<String, String>();
        // populate props from the library meta-data
        props.put(NAME, sharedLibraryDesc.getIdentification().getName());
        LOGGER.debug("Registering JBI Shared Library");
        registerService(bundle, SharedLibrary.class.getName(), sl, props);
        getManagementStrategy().manageObject(sl);
        // Check pending bundles
        checkPendingInstallers();
        return sl;
    }

    public ComponentImpl registerComponent(Bundle bundle, ComponentDesc componentDesc, javax.jbi.component.Component innerComponent, SharedLibrary[] sharedLibraries) throws Exception {
        String name = componentDesc.getIdentification().getName();
        Preferences prefs = preferencesService.getUserPreferences(name);
        ComponentImpl component = new ComponentImpl(bundle, componentDesc, innerComponent, prefs, autoStart, sharedLibraries);
        component.setListenerRegistry(listenerRegistry);
        // populate props from the component meta-data
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(NAME, name);
        props.put(TYPE, componentDesc.getType());
        for (SharedLibrary lib : sharedLibraries) {
            ((SharedLibraryImpl) lib).addComponent(component);
        }
        components.put(name, component);
        // register the component in the OSGi registry
        LOGGER.debug("Registering JBI component");
        registerService(bundle, new String[] { Component.class.getName(), ComponentWrapper.class.getName() },
                        component, props);
        // Now, register the inner component
        if (!wrappedComponents.containsKey(name)) {
            registerService(bundle, javax.jbi.component.Component.class.getName(), innerComponent, props);
        }
        getManagementStrategy().manageObject(component);
        return component;
    }

    public ServiceAssemblyImpl registerServiceAssembly(Bundle bundle, ServiceAssemblyDesc serviceAssemblyDesc, List<ServiceUnitImpl> sus) throws Exception {
        // Now create the SA and initialize it
        Preferences prefs = preferencesService.getUserPreferences(serviceAssemblyDesc.getIdentification().getName());
        ServiceAssemblyImpl sa = new ServiceAssemblyImpl(bundle, serviceAssemblyDesc, sus, prefs, endpointListener, autoStart);
        sa.setShutdownTimeout(shutdownTimeout);
        sa.setListenerRegistry(listenerRegistry);
        sa.init();
        serviceAssemblies.put(sa.getName(), sa);
        // populate props from the component meta-data
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(NAME, serviceAssemblyDesc.getIdentification().getName());
        // register the service assembly in the OSGi registry
        LOGGER.debug("Registering JBI service assembly");
        registerService(bundle, ServiceAssembly.class.getName(), sa, props);
        getManagementStrategy().manageObject(sa);
        return sa;
    }

    protected void unregisterComponent(ComponentImpl component) {
        if (component != null) {
            try {
                component.stop(false);
                component.shutDown(false, true);
                // TODO: Undeploy SAs and put their bundles in the pending state
                // Undeploy SAs
                Set<ServiceAssemblyImpl> sas = new HashSet<ServiceAssemblyImpl>();
                for (ServiceUnit su : component.getServiceUnits()) {
                    sas.add((ServiceAssemblyImpl) su.getServiceAssembly());
                }
                for (ServiceAssemblyImpl sa : sas) {
                    Bundle bundle = sa.getBundle();
                    ServiceAssemblyInstaller installer = (ServiceAssemblyInstaller) installers.get(bundle);
                    if (installer != null) {
                        try {
                            installer.stop(true);
                            pendingAssemblies.remove(sa);
                            if (installer.getDeployedAssembly() == null) {
                                unregisterServiceAssembly(sa);
                                pendingInstallers.add(installer);
                            } else {
                                installers.remove(bundle);
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Error uninstalling service assembly", e);
                        }
                    }
                }
                unregisterServices(component.getBundle());
            } catch (JBIException e) {
                LOGGER.warn("Error when shutting down component", e);
            }
            for (SharedLibrary lib : component.getSharedLibraries()) {
                ((SharedLibraryImpl) lib).removeComponent(component);
            }
            components.remove(component.getName());
        }
    }

    protected void unregisterServiceAssembly(ServiceAssemblyImpl assembly) {
        if (assembly != null) {
            serviceAssemblies.remove(assembly.getName());
            pendingAssemblies.remove(assembly);
            unregisterServices(assembly.getBundle());
            for (ServiceUnitImpl su : assembly.getServiceUnitsList()) {
                su.getComponentImpl().removeServiceUnit(su);
            }
        }
    }

    protected void unregisterSharedLibrary(SharedLibrary library) {
        if (library != null) {
            // TODO: shutdown all components
            sharedLibraries.remove(library.getName());
        }
    }

    public AbstractInstaller getInstaller(Object o) {
        if (o instanceof SharedLibraryImpl) {
            return installers.get(((SharedLibraryImpl) o).getBundle());
        }
        if (o instanceof ComponentImpl) {
            return installers.get(((ComponentImpl) o).getBundle());
        }
        if (o instanceof ServiceAssemblyImpl) {
            return installers.get(((ServiceAssemblyImpl) o).getBundle());
        }
        return null;
    }

    //===============================================================================
    //
    //   Pending artifacts support
    //
    //===============================================================================

    protected void checkPendingInstallers() {
        if (!pendingInstallers.isEmpty()) {
            final List<AbstractInstaller> pending = new ArrayList<AbstractInstaller>(pendingInstallers);
            pendingInstallers.clear();
            // Synchronous call because if using a separate thread
            // we run into deadlocks
            for (AbstractInstaller installer : pending) {
                try {
                    installer.init();
                    installer.install();
                    installers.put(installer.getBundle(), installer);
                } catch (PendingException e) {
                    pendingInstallers.add(installer);
                } catch (Exception e) {
                    LOGGER.warn("Error installing JBI artifact", e);
                }
            }
        }
    }

    protected void checkPendingAssemblies() {
        List<ServiceAssemblyImpl> sas = new ArrayList<ServiceAssemblyImpl>(pendingAssemblies);
        pendingAssemblies.clear();
        for (ServiceAssemblyImpl sa : sas) {
            try {
                sa.init();
            } catch (JBIException e) {
                pendingAssemblies.add(sa);
            }
        }
    }

    public void lifeCycleChanged(LifeCycleEvent event) throws JBIException {
        if (event.getLifeCycleMBean() instanceof ComponentImpl) {
            ComponentImpl comp = (ComponentImpl) event.getLifeCycleMBean();
            switch (event.getType()) {
                case Stopping:
                    if (comp.getState() == AbstractLifecycleJbiArtifact.State.Started) {
                        // Stop deployed SAs
                        for (ServiceAssemblyImpl sa : comp.getServiceAssemblies()) {
                            if (sa.getState() == ServiceAssemblyImpl.State.Started) {
                                sa.stop(false);
                                pendingAssemblies.add(sa);
                            }
                        }
                    }
                    break;
                case ShuttingDown:
                    if (comp.getState() == AbstractLifecycleJbiArtifact.State.Stopped) {
                        // Shutdown deployed SAs
                        for (ServiceAssemblyImpl sa : comp.getServiceAssemblies()) {
                            if (sa.getState() == ServiceAssemblyImpl.State.Stopped) {
                                sa.shutDown(false, event.isForced());
                                pendingAssemblies.add(sa);
                            }
                        }
                    }
                    break;
                case Started:
                    checkPendingInstallers();
                    checkPendingAssemblies();
                    break;
            }
        }

        // propagate lifecycle event to management strategy
        // 
        try {
            getManagementStrategy().notify(event);
        } catch (Exception e) {
            // ignore
        }
    }

    //===============================================================================
    //
    //   OSGi packaging support
    //
    //===============================================================================


    /**
     * Register an already deployed JBI component.
     * If the component has been deployed using a JBI packaging, the deployer should already have
     * registered it, so it will do nothing.  If the component has been deployed directly through
     * an OSGi packaging (by simply registering the component in the OSGi registry), the deployer
     * will perform the needed tasks.
     *
     * @param reference
     * @param component
     */
    protected void registerDeployedComponent(ServiceReference reference, javax.jbi.component.Component component) {
        String name = (String) reference.getProperty(NAME);
        if (name != null && !components.containsKey(name)) {
            String type = (String) reference.getProperty(TYPE);
            Descriptor descriptor = new Descriptor();
            ComponentDesc componentDesc = new ComponentDesc();
            componentDesc.setIdentification(new Identification());
            componentDesc.getIdentification().setName(name);
            componentDesc.setType(type);
            descriptor.setComponent(componentDesc);

            try {
                wrappedComponents.put(name, true);
                ComponentInstaller installer = new ComponentInstaller(this, descriptor, null, autoStart);
                installer.setBundle(reference.getBundle());
                installer.setInnerComponent(component);
                installer.init();
                installer.install();
                bundles.add(reference.getBundle());
            } catch (Exception e) {
                LOGGER.warn("Error registering deployed component", e);
            }

//            Preferences prefs = preferencesService.getUserPreferences(name);
//            ComponentImpl wrapper = new ComponentImpl(reference.getBundle(), componentDesc, component, prefs, autoStart, new SharedLibrary[0]);
//            wrapper.setListenerRegistry(listenerRegistry);
//            wrappedComponents.put(name, true);
//            components.put(name, wrapper);
//            Dictionary<String, String> props = new Hashtable<String, String>();
//            props.put(NAME, name);
//            props.put(TYPE, componentDesc.getType());
//            registerService(reference.getBundle(), new String[] { Component.class.getName(), ComponentWrapper.class.getName() },
//                            wrapper, props);
        }
    }

    protected void unregisterDeployedComponent(ServiceReference reference, javax.jbi.component.Component component) {
        String name = (String) reference.getProperty(NAME);
        if (name != null) {
            wrappedComponents.remove(name);
            unregisterComponent(getComponent(name));
        }
    }

    public void registerDeployedServiceAssembly(ServiceReference reference, DeployedAssembly assembly) {
        try {
            ServiceAssemblyDesc desc = new ServiceAssemblyDesc();
            desc.setIdentification(new Identification());
            desc.getIdentification().setName(assembly.getName());
            List<ServiceUnitDesc> sus = new ArrayList<ServiceUnitDesc>();
            for (Map.Entry<String, String> unit : assembly.getServiceUnits().entrySet()) {
                ServiceUnitDesc suDesc = new ServiceUnitDesc();
                suDesc.setIdentification(new Identification());
                suDesc.getIdentification().setName(unit.getKey());
                suDesc.setTarget(new Target());
                suDesc.getTarget().setComponentName(unit.getValue());
                sus.add(suDesc);
            }
            desc.setServiceUnits(sus.toArray(new ServiceUnitDesc[sus.size()]));
            Descriptor descriptor = new Descriptor();
            descriptor.setServiceAssembly(desc);

            ServiceAssemblyInstaller installer = new ServiceAssemblyInstaller(this, descriptor, assembly, autoStart);
            installer.setBundle(reference.getBundle());
            try {
                installer.init();
                installer.install();
                installers.put(installer.getBundle(), installer);
            } catch (PendingException e) {
                pendingInstallers.add(installer);
                LOGGER.warn("Requirements not met for JBI artifact in bundle " + OsgiStringUtils.nullSafeNameAndSymName(reference.getBundle()) + ". Installation pending. " + e);
            }
            bundles.add(reference.getBundle());
        } catch (Exception e) {
            LOGGER.error("Error registering deployed service assembly", e);
        }
    }

    public void unregisterDeployedServiceAssembly(ServiceReference reference, DeployedAssembly assembly) {
        // TODO: what to do here ? we should not uninstall the bundle as it's managed externally
        // TODO: but we should maybe stop / shut it down
        ServiceAssemblyImpl sa = getServiceAssembly(assembly.getName());
        if (sa != null) {
            try {
                if (sa.getState() == AbstractLifecycleJbiArtifact.State.Started) {
                    sa.stop(false);
                }
                if (sa.getState() == AbstractLifecycleJbiArtifact.State.Stopped) {
                    sa.shutDown(false, true);
                }
                for (ServiceUnitImpl su : sa.getServiceUnitsList()) {
                    su.undeploy();
                }
            } catch (Exception e) {
                LOGGER.error("Error unregistering deployed service assembly", e);
            } finally {
                unregisterServiceAssembly(sa);
            }
        }
    }

    //===============================================================================
    //
    //   OSGi Services registrations
    //
    //===============================================================================


    /**
     * Register and keep track of an OSGi service
     *
     * @param bundle
     * @param clazz
     * @param service
     * @param props
     */
    protected void registerService(Bundle bundle, String clazz, Object service, Dictionary props) {
        registerService(bundle, new String[] { clazz }, service,  props);
    }

    protected void registerService(Bundle bundle, String[] clazz, Object service, Dictionary props) {
        BundleContext context = bundle.getBundleContext() != null ? bundle.getBundleContext() : getBundleContext();
        ServiceRegistration reg = context.registerService(clazz, service, props);
        List<ServiceRegistration> registrations = services.get(bundle);
        if (registrations == null) {
            registrations = new ArrayList<ServiceRegistration>();
            services.put(bundle, registrations);
        }
        registrations.add(reg);
    }

    protected void unregisterServices(Bundle bundle) {
        List<ServiceRegistration> registrations = services.remove(bundle);
        // If unregisterServices is called when the bundle is stopped (RESOLVED),
        // all services have already been unregistered, so no need to iterate
        if (registrations != null && bundle.getState() != Bundle.RESOLVED) {
            for (ServiceRegistration reg : registrations) {
                try {
                    reg.unregister();
                } catch (IllegalStateException e) {
                    // Ignore
                }
            }
        }
    }

}
