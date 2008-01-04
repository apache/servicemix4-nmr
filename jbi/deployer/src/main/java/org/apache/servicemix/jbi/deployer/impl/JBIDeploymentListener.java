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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.descriptor.Descriptor;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.runtime.filemonitor.DeploymentListener;


public class JBIDeploymentListener implements DeploymentListener {
	
	private static final Log Logger = LogFactory.getLog(JBIDeploymentListener.class);
	
	public boolean canHandle(File artifact) {
		if (!artifact.getName().endsWith(".zip")) {
			return false;
		}
		try {
			JarFile jar = new JarFile(artifact);
			JarEntry entry = jar.getJarEntry("META-INF/jbi.xml");
			if (entry == null) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	
	public File handle(File artifact, File tmpDir) {
		try{
	        JarFile jar = new JarFile(artifact);
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
	        
	        m.getMainAttributes().put(new Attributes.Name("Bundle-SymbolicName"), name);
	        m.getMainAttributes().put(new Attributes.Name("Bundle-Version"), version);
	        
	        return generateJBIArtifactBundle(artifact, tmpDir, m); 
	        
		} catch (Exception e) {
			Logger.error("Failed in transforming the JBI artifact to be OSGified");
			return null;
		}
	}


	private File generateJBIArtifactBundle(File artifact, File tmpDir, Manifest m) throws Exception {
		String bundleName = artifact.getName().substring(0, artifact.getName().length() -4 ) + ".jar";
		File destFile = new File(tmpDir, bundleName);
		if (destFile.exists()) {
			destFile.delete();
		}
		
		JarInputStream jis = new JarInputStream(new FileInputStream(artifact));
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(destFile), m);
		
		JarEntry entry = jis.getNextJarEntry();
		while (entry != null) {
		    jos.putNextEntry(entry);
		    copyInputStream(jis, jos);
		    jos.closeEntry();
		    entry = jis.getNextJarEntry();
		}
		
		jos.close();
		jis.close();
		
		Logger.debug("Converted the JBI artifact to OSGified bundle [" + destFile.getAbsolutePath() + "]");
		return destFile;
	}

    protected void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
    }

}
