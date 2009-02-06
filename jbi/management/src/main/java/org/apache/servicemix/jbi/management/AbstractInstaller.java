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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.kernel.filemonitor.DeploymentListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class AbstractInstaller {
	
	private static final Log LOGGER = LogFactory.getLog(ComponentInstaller.class);

    private BundleContext bundleContext;
    
    
    private Map<String, String> artifactToBundle = new HashMap<String, String>();
    private Bundle bundle;

	public synchronized void deployFile(String filename) {
    	File file = new File(filename);
        try {
        	LOGGER.info("File is: " + filename);
        	// Transformation step
        	if (file.exists()) {
        		LOGGER.info("File exist");
        		File f = transformArtifact(file);
        		if (f == null) {
        			LOGGER.info("Unsupported deployment: " + filename);
        			return;
                }
        		file = f;
            } else {
            	String transformedFile = artifactToBundle.get(filename);
            	if (transformedFile != null) {
            		file = new File(transformedFile);
            		if (file.exists()) {
            			file.delete();
                	}
                }
            }

            // Handle final bundles
        	if (file.exists()) {
        		deployBundle(file);
            }
        	
        } catch (Exception e) {
        	LOGGER.info("Failed to process: " + file + ". Reason: " + e, e);
        }
            	
    }
    
    protected Bundle getBundleForJarFile(File file) throws IOException {
        String absoluteFilePath = file.getAbsoluteFile().toURI().toString();
        Bundle bundles[] = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            String location = bundle.getLocation();
            if (filePathsMatch(absoluteFilePath, location)) {
                return bundle;
            }
        }
        return null;
    }
    
    protected static boolean filePathsMatch(String p1, String p2) {
        p1 = normalizeFilePath(p1);
        p2 = normalizeFilePath(p2);
        return (p1 != null && p1.equalsIgnoreCase(p2));
    }

    protected static String normalizeFilePath( String path ) {
        if (path != null) {
            path = path.replaceFirst("file:/*", "");
            path = path.replaceAll("[\\\\/]+", "/");
        }
        return path;
    }

    
    protected void deployBundle(File file) throws IOException, BundleException {
        LOGGER.info("Deploying: " + file.getCanonicalPath());

        InputStream in = new FileInputStream(file);

        try {
            Bundle bundle = getBundleForJarFile(file);
            if (bundle != null) {
            	bundle.update();
            }
            else {
                bundle = bundleContext.installBundle(file.getCanonicalFile().toURI().toString(), in);
                bundle.start();
            }
            setBundle(bundle);
        }
        finally {
            closeQuietly(in);
        }
    }
    
    protected void closeQuietly(Closeable in) {
        try {
            in.close();
        }
        catch (IOException e) {
            LOGGER.info("Failed to close stream. " + e, e);
        }
    }
    
    public File getGenerateDir() {
    	String base = System.getProperty("servicemix.base", ".");
        return new File(base, "data/generated-bundles");
    } 
    
    protected File transformArtifact(File file) throws Exception {
        // Check registered deployers
        ServiceReference[] srvRefs = bundleContext.getAllServiceReferences(DeploymentListener.class.getName(), null);
		if(srvRefs != null) {
		    for(ServiceReference sr : srvRefs) {
		    	try {
		    		DeploymentListener deploymentListener = (DeploymentListener) bundleContext.getService(sr);
		    		if (deploymentListener.canHandle(file)) {
		    			File transformedFile = deploymentListener.handle(file, getGenerateDir());
		    			artifactToBundle.put(file.getAbsolutePath(), transformedFile.getAbsolutePath());
		    			return transformedFile;
		    		}
		    	} finally {
		    		bundleContext.ungetService(sr);
		    	}
		    }
		}
        JarFile jar = null;
        try {
            // Handle OSGi bundles with the default deployer
            if (file.getName().endsWith("txt") || file.getName().endsWith("xml")
            		|| file.getName().endsWith("properties")) {
            	// that's file type which is not supported as bundle and avoid exception in the log
                return null;
            }
            jar = new JarFile(file);
            Manifest m = jar.getManifest();
            if (m.getMainAttributes().getValue(new Attributes.Name("Bundle-SymbolicName")) != null &&
                m.getMainAttributes().getValue(new Attributes.Name("Bundle-Version")) != null) {
                return file;
            }
        } catch (Exception e) {
            LOGGER.info("Error transforming artifact " + file.getName(), e);
        } finally {
            if (jar != null) {
                jar.close();
            }
        }
        return null;
	}
    
    public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public Bundle getBundle() {
		return bundle;
	}
	
	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;		
	}

}
