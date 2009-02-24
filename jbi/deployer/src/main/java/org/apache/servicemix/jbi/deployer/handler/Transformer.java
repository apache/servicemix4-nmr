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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.utils.FileUtil;

/**
 * Helper class to transform JBI artifacts into OSGi bundles.
 */
public class Transformer {

    /**
     * Prevent instanciation.
     */
    private Transformer() {
    }

    /**
     * Create an OSGi bundle from the JBI artifact.
     * The process creates the following OSGi manifest entries:
     * <ul>
     * <li><b><code>Bundle-SymbolicName</code></b>: the name of the JBI artifact</li>
     * <li><b><code>Bundle-Version</code></b>: retrieved from the <code>Implementation-Version</code> manifest entry</li>
     * <li><b><code>DynamicImport-Package</code></b>: javax.*,org.xml.*,org.w3c.*</li>
     * </ul>
     *
     * @param jbiArtifact the input JBI artifact.
     * @param osgiBundle  the output OSGi bundle.
     * @throws Exception if an error occurs during the transformation process.
     */
    public static void transformToOSGiBundle(File jbiArtifact, File osgiBundle) throws Exception {
        transformToOSGiBundle(jbiArtifact, osgiBundle, null);
    }

    /**
     * Create an OSGi bundle from the JBI artifact.
     * The process creates the following OSGi manifest entries:
     * <ul>
     * <li><b><code>Bundle-SymbolicName</code></b>: the name of the JBI artifact</li>
     * <li><b><code>Bundle-Version</code></b>: retrieved from the <code>Implementation-Version</code> manifest entry</li>
     * <li><b><code>DynamicImport-Package</code></b>: javax.*,org.xml.*,org.w3c.*</li>
     * </ul>
     *
     * @param jbiArtifact the input JBI artifact.
     * @param osgiBundle  the output OSGi bundle.
     * @throws Exception if an error occurs during the transformation process.
     */
    public static void transformToOSGiBundle(File jbiArtifact, File osgiBundle, Properties properties) throws Exception {
        JarFile jar = new JarFile(jbiArtifact);
        Manifest m = jar.getManifest();
        if (m == null) {
            m = new Manifest();
            m.getMainAttributes().putValue("Manifest-Version", "1.0");
        }
        JarEntry jarEntry = jar.getJarEntry(DescriptorFactory.DESCRIPTOR_FILE);
        InputStream is = jar.getInputStream(jarEntry);
        Descriptor desc = DescriptorFactory.buildDescriptor(is);

        String version = m.getMainAttributes().getValue("Implementation-Version");
        if (version != null) {
            version = Builder.cleanupVersion(version);
        } else {
            version = "0.0.0";
        }
        String name = m.getMainAttributes().getValue("Implementation-Title");

        if (desc.getComponent() != null) {
            name = desc.getComponent().getIdentification().getName();
        } else if (desc.getSharedLibrary() != null) {
            name = desc.getSharedLibrary().getIdentification().getName();
        } else if (desc.getServiceAssembly() != null) {
            name = desc.getServiceAssembly().getIdentification().getName();
        }

        m.getMainAttributes().putValue("Bundle-SymbolicName", name);
        m.getMainAttributes().putValue("Bundle-Version", version);
        m.getMainAttributes().putValue("DynamicImport-Package", "javax.*,org.xml.*,org.w3c.*");

        if (properties != null) {
            Enumeration en = properties.propertyNames();
            while (en.hasMoreElements()) {
                String k = (String) en.nextElement();
                String v = properties.getProperty(k);
                m.getMainAttributes().putValue(k, v);
            }
        }

        osgiBundle.getParentFile().mkdirs();
        JarInputStream jis = new JarInputStream(new BufferedInputStream(new FileInputStream(jbiArtifact)));
        JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(osgiBundle)), m);

        JarEntry entry = jis.getNextJarEntry();
        while (entry != null) {
            if (!"META-INF/MANIFEST.MF".equals(entry.getName())) {
                JarEntry newEntry = new JarEntry(entry.getName());
                jos.putNextEntry(newEntry);
                FileUtil.copyInputStream(jis, jos);
                jos.closeEntry();
            }
            entry = jis.getNextJarEntry();
        }

        jos.close();
        jis.close();
    }

    public static Descriptor getDescriptor(File jbiArtifact) throws Exception {
        JarFile jar = new JarFile(jbiArtifact);
        JarEntry jarEntry = jar.getJarEntry(DescriptorFactory.DESCRIPTOR_FILE);
        InputStream is = jar.getInputStream(jarEntry);
        return DescriptorFactory.buildDescriptor(is);
    }

}
