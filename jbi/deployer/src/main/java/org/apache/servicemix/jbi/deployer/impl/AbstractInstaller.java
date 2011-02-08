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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.jbi.JBIException;
import javax.management.ObjectName;

import org.apache.servicemix.jbi.deployer.artifacts.AbstractLifecycleJbiArtifact;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.handler.Transformer;
import org.apache.servicemix.jbi.deployer.utils.FileUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installers are used to controll the installation / deployment process of JBI artifacts
 * and manage the installed OSGi bundle.
 *
 * TODO: refactor the use of installRoot and reuse the Deployer#jbiRootDir
 */
public abstract class AbstractInstaller {

    public static final String LAST_INSTALL = "jbi.deployer.install";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Deployer deployer;
    protected Descriptor descriptor;
    protected Bundle bundle;
    protected boolean autoStart = false;
    protected File jbiArtifact;
    protected File installRoot;
    protected boolean uninstallFromOsgi;
    protected boolean isFirstInstall;
    protected boolean isModified;

    protected AbstractInstaller(Deployer deployer, Descriptor descriptor, File jbiArtifact, boolean autoStart) {
        this.deployer = deployer;
        this.descriptor = descriptor;
        this.jbiArtifact = jbiArtifact;
        this.autoStart = autoStart;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    protected BundleContext getBundleContext() {
        return deployer.getBundleContext();
    }

    public abstract String getName();

    public void init() throws Exception {
        Storage storage = getStorage();
        long lastInstall = Long.parseLong(storage.get(LAST_INSTALL, "0"));
        isFirstInstall = lastInstall == 0; 
        isModified = lastInstall == 0 || getBundle().getLastModified() > lastInstall;
        if (isModified && installRoot != null) {
            extractBundle(installRoot, getBundle(), "/");
            lastInstall = getBundle().getLastModified();
            storage.put(AbstractLifecycleJbiArtifact.STATE, isAutoStart()
                            ? AbstractLifecycleJbiArtifact.State.Started.name()
                            : AbstractLifecycleJbiArtifact.State.Shutdown.name());
        }
    }

    protected void postInstall() throws Exception {
        Storage prefs = getStorage();
        prefs.put(LAST_INSTALL, Long.toString(getBundle().getLastModified()));
        prefs.save();
    }

    public abstract ObjectName install() throws JBIException;

    public abstract void stop(boolean force) throws Exception;

    public abstract void uninstall(boolean force) throws Exception;

    public void installBundle() throws Exception {
        InputStream is = null;
        try {
            deployer.setJmxManaged(this);
            File artifact = new File(jbiArtifact.getCanonicalPath());
            String bundleName = artifact.getName().substring(0, artifact.getName().length() - 4) + ".jar";
            File osgi = new File(getGenerateDir(), bundleName);
            Transformer.transformToOSGiBundle(artifact, osgi);
            is = new BufferedInputStream(new FileInputStream(osgi));
            bundle = getBundleContext().installBundle(artifact.getCanonicalFile().toURI().toString(), is);
            bundle.start();
        } catch (Exception e) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException io) {
                    logger.info("Failed to close stream.", io);
                }
            }
            throw e;
        } finally {
            deployer.setJmxManaged(null);
        }
    }

    protected void uninstallBundle() throws BundleException {
        if (bundle != null && bundle.getState() != Bundle.UNINSTALLED && !uninstallFromOsgi) {
            try {
                deployer.setJmxManaged(this);
                bundle.uninstall();
            } finally {
                deployer.setJmxManaged(null);
            }
        }
    }

    private File getGenerateDir() {
        String base = System.getProperty("servicemix.base", ".");
        return new File(base, "data/generated-bundles");
    }

    protected Storage getStorage() {
        return deployer.getStorage(getName());
    }

    protected void deleteStorage() throws IOException {
        Storage storage = getStorage();
        storage.clear();
        storage.save();
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isUninstallFromOsgi() {
        return uninstallFromOsgi;
    }

    public void setUninstallFromOsgi(boolean uninstallFromOsgi) {
        this.uninstallFromOsgi = uninstallFromOsgi;
    }

    protected void extractBundle(File installRoot, Bundle bundle, String path) throws IOException {
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

}
