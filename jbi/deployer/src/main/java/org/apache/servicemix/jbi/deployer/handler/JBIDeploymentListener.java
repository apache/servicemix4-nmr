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
package org.apache.servicemix.jbi.deployer.handler;

import java.io.File;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.kernel.filemonitor.DeploymentListener;

/**
 * A JBI DeploymentListener which transforms plain JBI artifacts to OSGi bundles.
 * The deployer will recognize zip and jar files containing a JBI descriptor and
 * without any OSGi manifest entries.
 */
public class JBIDeploymentListener implements DeploymentListener {

    private static final Log Logger = LogFactory.getLog(JBIDeploymentListener.class);

    /**
     * Check if the file is a recognized JBI artifact that needs to be
     * processed.
     *
     * @param artifact the file to check
     * @return <code>true</code> is the file is a JBI artifact that
     *         should be transformed into an OSGi bundle.
     */
    public boolean canHandle(File artifact) {
        try {
            // Accept jars and zips
            if (!artifact.getName().endsWith(".zip") &&
                    !artifact.getName().endsWith(".jar")) {
                return false;
            }
            JarFile jar = new JarFile(artifact);
            JarEntry entry = jar.getJarEntry(DescriptorFactory.DESCRIPTOR_FILE);
            // Only handle JBI artifacts
            if (entry == null) {
                return false;
            }
            // Only handle non OSGi bundles
            Manifest m = jar.getManifest();
            if (m != null &&
                    m.getMainAttributes().getValue(new Attributes.Name("Bundle-SymbolicName")) != null &&
                    m.getMainAttributes().getValue(new Attributes.Name("Bundle-Version")) != null) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Transform the file, which is a JBI artifact, into an OSGi bundle.
     *
     * @param artifact the file to transform.
     * @param tmpDir   the location where the file should be stored.
     * @return the location of the transformed OSGi bundle, or <code>null</code>
     *         if the transformation could not take place.
     */
    public File handle(File artifact, File tmpDir) {
        try {
            String bundleName = artifact.getName().substring(0, artifact.getName().length() - 4) + ".jar";
            File destFile = new File(tmpDir, bundleName);
            if (destFile.exists()) {
                destFile.delete();
            }
            Transformer.transformToOSGiBundle(artifact, destFile);
            return destFile;

        } catch (Exception e) {
            Logger.error("Failed in transforming the JBI artifact to be OSGified. error is: " + e);
            return null;
        }
    }


}
