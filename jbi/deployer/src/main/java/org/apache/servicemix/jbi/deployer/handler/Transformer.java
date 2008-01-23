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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.impl.FileUtil;

/**
 * Helper class to transform JBI artifacts into OSGi bundles
 */
public class Transformer {

    public static void transformToOSGiBundle(File jbiArtifact, File jbiBundle) throws Exception {
    	JarFile jar = new JarFile(jbiArtifact);
        Manifest m = jar.getManifest();
        JarEntry jarEntry = jar.getJarEntry("META-INF/jbi.xml");
        InputStream is = jar.getInputStream(jarEntry);
        Descriptor desc = DescriptorFactory.buildDescriptor(is);

        String version = m.getMainAttributes().getValue("Implementation-Version");
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

		JarInputStream jis = new JarInputStream(new FileInputStream(jbiArtifact));
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(jbiBundle), m);

		JarEntry entry = jis.getNextJarEntry();
		while (entry != null) {
		    jos.putNextEntry(entry);
		    FileUtil.copyInputStream(jis, jos);
		    jos.closeEntry();
		    entry = jis.getNextJarEntry();
		}

		jos.close();
		jis.close();
    }

}
