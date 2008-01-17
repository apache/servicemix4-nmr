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
package org.apache.servicemix.preferences;

import java.io.File;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 15, 2008
 * Time: 3:05:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class PreferencesServiceFactoryImpl implements ServiceFactory, BundleListener {

    private BundleContext bundleContext;
    private AbstractPreferencesImpl preferences;
    private File root;

    public PreferencesServiceFactoryImpl() {
    }

    public PreferencesServiceFactoryImpl(BundleContext bundleContext, File root) {
        this.bundleContext = bundleContext;
        this.root = root;
        init();
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public File getRoot() {
        return root;
    }

    public void setRoot(File root) {
        this.root = root;
    }

    public synchronized Object getService(Bundle bundle, ServiceRegistration serviceRegistration) {
        checkInit();
        return new PreferencesServiceImpl((AbstractPreferencesImpl) preferences.node("/" + bundle.getBundleId()));
    }

    public synchronized void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object o) {
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED) {
            try {
                if (preferences.nodeExists("/" + event.getBundle().getBundleId())) {
                    preferences.node("/" + event.getBundle().getBundleId()).removeNode();
                }
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }
        }
    }

    public void init() {
        if (bundleContext == null) {
            throw new NullPointerException("bundleContext is null");
        }
        bundleContext.addBundleListener(this);
        preferences = new FilePreferencesImpl(null, null, root);
    }

    public void destroy() throws BackingStoreException {
        if (bundleContext != null) {
            bundleContext.removeBundleListener(this);
        }
        preferences.flush();
    }

    protected void checkInit() {
        if (preferences == null) {
            throw new IllegalStateException("Preferences Service factory must be initialized");
        }
    }
}
