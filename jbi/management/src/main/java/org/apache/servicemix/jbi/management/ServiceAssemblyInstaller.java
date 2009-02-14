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

import javax.jbi.JBIException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.impl.ComponentImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.service.prefs.Preferences;

public class ServiceAssemblyInstaller extends AbstractInstaller {

private static final Log LOGGER = LogFactory.getLog(SharedLibInstaller.class);
	
	private String name;
    private AdminService adminService;
	
	public ServiceAssemblyInstaller(String name, AdminService adminService) {
		this.name = name;
		this.adminService = adminService;
		setBundleContext(adminService.getBundleContext());
	}
	
	public void deploy(String filename) {
        try {
            initializePreferences();
        } catch (BackingStoreException e) {
            LOGGER.warn("Error initializing persistent state for service assembly: " + name, e);
        }
		deployFile(filename);
	}
	
	public void undeploy() throws javax.jbi.JBIException {
		try {
        	Bundle bundle = getBundle();

            if (bundle == null) {
                LOGGER.warn("Could not find Bundle for Service Assembly: " + name);
            }
            else {
                bundle.stop();
                bundle.uninstall();
                try {
                    deletePreferences();
                } catch (BackingStoreException e) {
                    LOGGER.warn("Error cleaning persistent state for service assembly: " + name, e);
                }
            }
        } catch (BundleException e) {
        	LOGGER.error("failed to uninstall Service Assembly: " + name, e);
        	throw new JBIException(e);
		} 
    }

    protected void initializePreferences() throws BackingStoreException {
        PreferencesService preferencesService = getPreferencesService();
        Preferences prefs = preferencesService.getUserPreferences(name);
        prefs.put(ComponentImpl.STATE, ComponentImpl.State.Shutdown.name());
        prefs.flush();
    }

    protected void deletePreferences() throws BackingStoreException {
        PreferencesService preferencesService = getPreferencesService();
        Preferences prefs = preferencesService.getUserPreferences(name);
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
