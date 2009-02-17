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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.artifacts.AbstractLifecycleJbiArtifact;
import org.apache.servicemix.jbi.deployer.handler.Transformer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;

public abstract class AbstractInstaller {

    protected final Log LOGGER = LogFactory.getLog(getClass());

    protected BundleContext bundleContext;
    protected Bundle bundle;
    private boolean autoStart = false;

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public abstract String getName();

    public synchronized void deployFile(String filename) {
        InputStream is = null;
        try {
            File artifact = new File(filename);
            String bundleName = artifact.getName().substring(0, artifact.getName().length() - 4) + ".jar";
            File osgi = new File(getGenerateDir(), bundleName);
            Transformer.transformToOSGiBundle(artifact, osgi);
            is = new BufferedInputStream(new FileInputStream(osgi));
            bundle = bundleContext.installBundle(artifact.getCanonicalFile().toURI().toString(), is);
            bundle.start();
        } catch (Exception e) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException io) {
                    LOGGER.info("Failed to close stream. " + io, io);
                }
            }
            LOGGER.info("Failed to process: " + filename + ". Reason: " + e, e);
        }
    }

    private File getGenerateDir() {
        String base = System.getProperty("servicemix.base", ".");
        return new File(base, "data/generated-bundles");
    }

    protected void initializePreferences() throws BackingStoreException {
        PreferencesService preferencesService = getPreferencesService();
        Preferences prefs = preferencesService.getUserPreferences(getName());
        prefs.put(AbstractLifecycleJbiArtifact.STATE, isAutoStart()
                        ? AbstractLifecycleJbiArtifact.State.Started.name()
                        : AbstractLifecycleJbiArtifact.State.Shutdown.name());
        prefs.flush();
    }

    protected void deletePreferences() throws BackingStoreException {
        PreferencesService preferencesService = getPreferencesService();
        Preferences prefs = preferencesService.getUserPreferences(getName());
        prefs.clear();
        prefs.flush();
    }

    private PreferencesService getPreferencesService() throws BackingStoreException {
        ServiceReference ref = getBundleContext().getServiceReference(PreferencesService.class.getName());
        PreferencesService preferencesService = (PreferencesService) getBundleContext().getService(ref);
        if (preferencesService == null) {
            throw new BackingStoreException("Unable to find bundle 'org.apache.servicemix.jbi.deployer'");
        }
        return preferencesService;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
}
