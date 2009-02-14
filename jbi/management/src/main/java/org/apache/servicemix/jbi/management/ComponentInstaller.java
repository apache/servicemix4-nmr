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


import java.io.File;

import javax.jbi.JBIException;
import javax.jbi.management.DeploymentException;
import javax.jbi.management.InstallerMBean;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.impl.ComponentImpl;
import org.osgi.framework.Bundle;

import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.BackingStoreException;

public class ComponentInstaller extends AbstractInstaller 
	implements InstallerMBean {
    
    private static final Log LOGGER = LogFactory.getLog(ComponentInstaller.class);

    
    private InstallationContextImpl context;
    private File jbiArtifact;
    private ObjectName objectName;
    private ObjectName extensionMBeanName;
    private AdminService adminService;

    


    public ComponentInstaller(InstallationContextImpl ic, File jbiArtifact, AdminService adminService) throws DeploymentException {
        this.context = ic;
        this.jbiArtifact = jbiArtifact;
        this.adminService = adminService;
        setBundleContext(this.adminService.getBundleContext());
        extensionMBeanName = ic.createCustomComponentMBeanName("Configuration");
    }

    /**
      * Get the installation root directory path for this BC or SE.
      *
      * @return the full installation path of this component.
      */
     public String getInstallRoot() {
         return context.getInstallRoot();
     }

     /**
      * Install a BC or SE.
      *
      * @return JMX ObjectName representing the ComponentLifeCycle for the installed component, or null if the
      * installation did not complete.
      * @throws javax.jbi.JBIException if the installation fails.
      */
     public ObjectName install() throws JBIException {
    	 
    	 try {
    		if (isInstalled()) {
                throw new DeploymentException("Component is already installed");
            }
    		File f = transformArtifact(jbiArtifact);
    		if (f == null) {
    			LOGGER.info("Unsupported deployment: " + f.getName());
    			return null;
            }
    		if (f.exists()) {
                try {
                    initializePreferences();
                } catch (BackingStoreException e) {
                    LOGGER.warn("Error initializing persistent state for component: " + context.getComponentName(), e);
                }
        		deployBundle(f);
            }
			context.setInstall(false);
			ObjectName ret = this.adminService.getComponentByName(context.getComponentName());
             if (ret == null) {
                 // something wrong happened
                 // TODO: we need to retrieve the correct problem somehow
                 if (getBundle() != null) {
                    getBundle().uninstall();
                 }
                 throw new Exception("Error installing component");
             }
			return ret;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw new JBIException(e);
		}
		
     }

    
    /**
     * Determine whether or not the component is installed.
     *
     * @return true if this component is currently installed, false if not.
     */
    public boolean isInstalled() {
        return !context.isInstall();
    }

    /**
     * Uninstall a BC or SE. This completely removes the component from the JBI system.
     *
     * @throws javax.jbi.JBIException if the uninstallation fails.
     */
    public void uninstall() throws javax.jbi.JBIException {
        // TODO: check component status
        // the component must not be started and not have any SUs deployed
        if (!isInstalled()) {
            throw new DeploymentException("Component is not installed");
        }
        String componentName = context.getComponentName();
        try {
        	Bundle bundle = getBundle();

            if (bundle == null) {
                LOGGER.warn("Could not find Bundle for component: " + componentName);
            }
            else {
                bundle.stop();
                bundle.uninstall();
                try {
                    deletePreferences();
                } catch (BackingStoreException e) {
                    LOGGER.warn("Error cleaning persistent state for component: " + componentName, e);
                }
            }
        } catch (BundleException e) {
        	LOGGER.error("failed to uninstall component: " + componentName, e);
        	throw new JBIException(e);
		} 
    }

    /**
     * Get the installer configuration MBean name for this component.
     *
     * @return the MBean object name of the Installer Configuration MBean.
     * @throws javax.jbi.JBIException if the component is not in the LOADED state or any error occurs during processing.
     */
    public ObjectName getInstallerConfigurationMBean() throws javax.jbi.JBIException {
    	return this.extensionMBeanName;
    }
    /**
     * @return Returns the objectName.
     */
    public ObjectName getObjectName() {
        return objectName;
    }
    /**
     * @param objectName The objectName to set.
     */
    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    protected void initializePreferences() throws BackingStoreException {
        PreferencesService preferencesService = getPreferencesService();
        Preferences prefs = preferencesService.getUserPreferences(context.getComponentName());
        prefs.put(ComponentImpl.STATE, ComponentImpl.State.Shutdown.name());
        prefs.flush();
    }

    protected void deletePreferences() throws BackingStoreException {
        PreferencesService preferencesService = getPreferencesService();
        Preferences prefs = preferencesService.getUserPreferences(context.getComponentName());
        prefs.clear();
        prefs.flush();
    }

    private PreferencesService getPreferencesService() throws BackingStoreException {
        PreferencesService preferencesService = null;
        for (Bundle bundle : getBundleContext().getBundles()) {
            if ("org.apache.servicemix.jbi.deployer".equals(bundle.getSymbolicName())) {
                ServiceReference ref = bundle.getBundleContext().getServiceReference(PreferencesService.class.getName());
                preferencesService = (PreferencesService) bundle.getBundleContext().getService(ref);
                break;
            }
        }
        if (preferencesService == null) {
            throw new BackingStoreException("Unable to find bundle 'org.apache.servicemix.jbi.deployer'");
        }
        return preferencesService;
    }

}
