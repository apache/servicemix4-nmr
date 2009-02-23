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
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.JBIException;
import javax.management.StandardMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.DeployedAssembly;
import org.apache.servicemix.jbi.deployer.NamingStrategy;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.artifacts.AbstractLifecycleJbiArtifact;
import org.apache.servicemix.jbi.deployer.artifacts.ComponentImpl;
import org.apache.servicemix.jbi.deployer.artifacts.ServiceAssemblyImpl;
import org.apache.servicemix.jbi.deployer.artifacts.ServiceUnitImpl;
import org.apache.servicemix.jbi.deployer.artifacts.SharedLibraryImpl;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * Deployer for JBI artifacts
 */
public class Deployer extends AbstractBundleWatcher {

    public static final String NAME = "NAME";
    public static final String TYPE = "TYPE";
    public static final String TYPE_SERVICE_ENGINE = "service-engine";
    public static final String TYPE_BINDING_COMPONENT = "binding-component";

    private static final Log LOGGER = LogFactory.getLog(Deployer.class);

    private final Map<String, SharedLibraryImpl> sharedLibraries = new ConcurrentHashMap<String, SharedLibraryImpl>();
    private final Map<String, ComponentImpl> components = new ConcurrentHashMap<String, ComponentImpl>();
    private final Map<String, ServiceAssemblyImpl> serviceAssemblies = new ConcurrentHashMap<String, ServiceAssemblyImpl>();

    private final Map<Bundle, AbstractInstaller> installers = new ConcurrentHashMap<Bundle, AbstractInstaller>();

    private final ThreadLocal<AbstractInstaller> jmxManaged = new ThreadLocal<AbstractInstaller>();

    private final Map<String, Boolean> wrappedComponents = new ConcurrentHashMap<String, Boolean>();

    private final Map<Bundle, List<ServiceRegistration>> services = new ConcurrentHashMap<Bundle, List<ServiceRegistration>>();

    private final List<Bundle> pendingBundles = new ArrayList<Bundle>();

    private File jbiRootDir;

    private PreferencesService preferencesService;

    private boolean autoStart = true;

    private ServiceTracker deployedComponentsTracker;

    private ServiceTracker deployedAssembliesTracker;

    private AssemblyReferencesListener endpointListener;

    // Helper beans
    private NamingStrategy namingStrategy;
    private ManagementAgent managementAgent;
    private Environment environment;

    private Runnable checkPendingBundlesCallback;

    public Deployer() throws JBIException {
        checkPendingBundlesCallback = new Runnable() {
            public void run() {
                checkPendingBundles();
            }
        };
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

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public ManagementAgent getManagementAgent() {
        return managementAgent;
    }

    public void setManagementAgent(ManagementAgent managementAgent) {
        this.managementAgent = managementAgent;
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

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        // Track deployed components
        deployedComponentsTracker = new ServiceTracker(getBundleContext(), javax.jbi.component.Component.class.getName(), null) {
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
        deployedComponentsTracker.close();
        deployedAssembliesTracker.close();
        super.destroy();
    }

    @Override
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
    @Override
    protected void register(Bundle bundle) {
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
                URL url = bundle.getResource(DescriptorFactory.DESCRIPTOR_FILE);
                Descriptor descriptor = DescriptorFactory.buildDescriptor(url);
                DescriptorFactory.checkDescriptor(descriptor);
                if (descriptor.getSharedLibrary() != null) {
                    LOGGER.info("Deploying bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' as a JBI shared library");
                    installer = new SharedLibraryInstaller(this, descriptor, null);
                } else if (descriptor.getComponent() != null) {
                    LOGGER.info("Deploying bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' as a JBI component");
                    installer = new ComponentInstaller(this, descriptor, null);
                } else if (descriptor.getServiceAssembly() != null) {
                    LOGGER.info("Deploying bundle '" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "' as a JBI service assembly");
                    installer = new ServiceAssemblyInstaller(this, descriptor, null);
                } else {
                    throw new IllegalStateException("Unrecognized JBI descriptor: " + url);
                }
                installer.setAutoStart(true);
                installer.setBundle(bundle);
                installer.init();
                installer.install();
                installers.put(bundle, installer);
            } catch (PendingException e) {
                pendingBundles.add(e.getBundle());
                LOGGER.warn("Requirements not met for JBI artifact in bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle) + ". Installation pending. " + e);
            } catch (Exception e) {
                LOGGER.error("Error handling bundle start event", e);
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
    }

    @Override
    protected void unregister(Bundle bundle) {
        AbstractInstaller installer = getJmxManaged();
        if (installer == null) {
            installer = installers.get(bundle);
            if (installer != null) {
                try {
                    installer.setUninstallFromOsgi(true);
                    installer.uninstall(true);
                } catch (Exception e) {
                    LOGGER.warn("Error uninstalling JBI artifact", e);
                }
            }
        }
        pendingBundles.remove(bundle);
        unregisterServices(bundle);
    }

    public ServiceUnitImpl createServiceUnit(ServiceUnitDesc sud, File suRootDir, ComponentImpl component) {
        return new ServiceUnitImpl(sud, suRootDir, component);
    }

    public SharedLibrary registerSharedLibrary(Bundle bundle, SharedLibraryDesc sharedLibraryDesc, ClassLoader classLoader) throws Exception {
        // Create shared library
        SharedLibraryImpl sl = new SharedLibraryImpl(bundle, sharedLibraryDesc, classLoader);
        sharedLibraries.put(sl.getName(), sl);
        Dictionary<String, String> props = new Hashtable<String, String>();
        // populate props from the library meta-data
        props.put(NAME, sharedLibraryDesc.getIdentification().getName());
        LOGGER.debug("Registering JBI Shared Library");
        registerService(bundle, SharedLibrary.class.getName(), sl, props);
        getManagementAgent().register(new StandardMBean(sl, SharedLibrary.class), getNamingStrategy().getObjectName(sl));
        // Check pending bundles
        checkPendingBundles();
        return sl;
    }

    public Component registerComponent(Bundle bundle, ComponentDesc componentDesc, javax.jbi.component.Component innerComponent, SharedLibrary[] sharedLibraries) throws Exception {
        String name = componentDesc.getIdentification().getName();
        Preferences prefs = preferencesService.getUserPreferences(name);
        ComponentImpl component = new ComponentImpl(bundle, componentDesc, innerComponent, prefs, autoStart, checkPendingBundlesCallback, sharedLibraries);
        // populate props from the component meta-data
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(NAME, name);
        props.put(TYPE, componentDesc.getType());
        // register the component in the OSGi registry
        LOGGER.debug("Registering JBI component");
        registerService(bundle, new String[] { Component.class.getName(), ComponentWrapper.class.getName() },
                        component, props);
        components.put(name, component);
        // Now, register the inner component
        registerService(bundle, javax.jbi.component.Component.class.getName(), innerComponent, props);
        getManagementAgent().register(new StandardMBean(component, Component.class),
                                      getNamingStrategy().getObjectName(component));
        for (SharedLibrary lib : sharedLibraries) {
            ((SharedLibraryImpl) lib).addComponent(component);
        }
        return component;
    }

    public ServiceAssembly registerServiceAssembly(Bundle bundle, ServiceAssemblyDesc serviceAssemblyDesc, List<ServiceUnitImpl> sus) throws Exception {
        // Now create the SA and initialize it
        Preferences prefs = preferencesService.getUserPreferences(serviceAssemblyDesc.getIdentification().getName());
        ServiceAssemblyImpl sa = new ServiceAssemblyImpl(bundle, serviceAssemblyDesc, sus, prefs, endpointListener, autoStart);
        sa.init();
        serviceAssemblies.put(sa.getName(), sa);
        // populate props from the component meta-data
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(NAME, serviceAssemblyDesc.getIdentification().getName());
        // register the service assembly in the OSGi registry
        LOGGER.debug("Registering JBI service assembly");
        registerService(bundle, ServiceAssembly.class.getName(), sa, props);
        getManagementAgent().register(new StandardMBean(sa, ServiceAssembly.class), getNamingStrategy().getObjectName(sa));
        return sa;
    }

    protected void unregisterComponent(Component component) {
        if (component != null) {
            for (SharedLibrary lib : component.getSharedLibraries()) {
                ((SharedLibraryImpl) lib).removeComponent(component);
            }
            components.remove(component.getName());
        }
    }

    protected void unregisterServiceAssembly(ServiceAssembly assembly) {
        if (assembly != null) {
            serviceAssemblies.remove(assembly.getName());
        }
    }

    protected void unregisterSharedLibrary(SharedLibrary library) {
        if (library != null) {
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
            Preferences prefs = preferencesService.getUserPreferences(name);
            ComponentDesc componentDesc = new ComponentDesc();
            componentDesc.setIdentification(new Identification());
            componentDesc.getIdentification().setName(name);
            componentDesc.setType(type);
            ComponentImpl wrapper = new ComponentImpl(reference.getBundle(), componentDesc, component, prefs, autoStart, checkPendingBundlesCallback, new SharedLibrary[0]);
            wrappedComponents.put(name, true);
            components.put(name, wrapper);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put(NAME, name);
            props.put(TYPE, componentDesc.getType());
            registerService(reference.getBundle(), new String[] { Component.class.getName(), ComponentWrapper.class.getName() },
                            wrapper, props);
        }
    }

    protected void unregisterDeployedComponent(ServiceReference reference, javax.jbi.component.Component component) {
        String name = (String) reference.getProperty(NAME);
        if (name != null && Boolean.TRUE.equals(wrappedComponents.remove(name))) {
            ComponentImpl ci = components.remove(name);
            if (ci != null) {
                try {
                    ci.stop(false);
                    ci.shutDown(false, false);
                    pendingBundles.remove(reference.getBundle());
                    unregisterServices(reference.getBundle());
                } catch (JBIException e) {
                    LOGGER.warn("Error when shutting down component", e);
                }
            }
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
                ServiceUnitImpl su = createServiceUnit(suDesc, null, components.get(unit.getValue()));
                sus.add(su);
            }
            registerServiceAssembly(serviceReference.getBundle(), desc, sus);
        } catch (Exception e) {
            LOGGER.error("Error registering deployed service assembly", e);
        }
    }

    public void unregisterDeployedServiceAssembly(ServiceReference serviceReference, DeployedAssembly assembly) {
        // TODO: what to do here ? we should not uninstall the bundle as it's managed externally
        // TODO: but we should maybe stop / shut it down
        ServiceAssemblyImpl sa = getServiceAssembly(assembly.getName());
        if (sa != null) {
            try {
                if (sa.getState() == AbstractLifecycleJbiArtifact.State.Started) {
                    sa.stop();
                }
                if (sa.getState() == AbstractLifecycleJbiArtifact.State.Stopped) {
                    sa.shutDown();
                }
            } catch (Exception e) {
                LOGGER.error("Error unregistering deployed service assembly", e);
            } finally {
                unregisterServiceAssembly(sa);
            }
        }
    }

    protected void checkPendingBundles() {
        if (!pendingBundles.isEmpty()) {
            final List<Bundle> pending = new ArrayList<Bundle>(pendingBundles);
            pendingBundles.clear();
            // Synchronous call because if using a separate thread
            // we run into deadlocks
            for (Bundle bundle : pending) {
                register(bundle);
            }
        }
    }

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
                }
            }
        }
    }

}
