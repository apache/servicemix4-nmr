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
package org.apache.servicemix.jbi.itests;

import java.util.Dictionary;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractIntegrationTest {

    public static final long DEFAULT_TIMEOUT = 30000;

    @Inject
    protected BundleContext bundleContext;

    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, DEFAULT_TIMEOUT);
    }

    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open(true);
            // Note that the tracker is not closed to keep the reference
            // This is buggy, as the service reference may change i think
            Object svc = type.cast(tracker.waitForService(timeout));
            if (svc == null) {

                Dictionary dic = bundleContext.getBundle().getHeaders();
                System.err.println("Test bundle headers: " + dic);
                ServiceReference[] refs = bundleContext.getAllServiceReferences(null, null);
                if (refs != null) {
                    for (ServiceReference ref : refs) {
                        System.err.println("ServiceReference: " + ref);
                    }
                } else {
                    System.err.println("No references");
                }
                refs = bundleContext.getAllServiceReferences(null, flt);
                if (refs != null) {
                    for (ServiceReference ref : refs) {
                        System.err.println("Filtered ServiceReference: " + ref);
                    }
                } else {
                    System.err.println("No filtered references");
                }
                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected Bundle installBundle(String groupId, String artifactId) throws Exception {
        MavenArtifactProvisionOption mvnUrl = mavenBundle(groupId, artifactId);
        return bundleContext.installBundle(mvnUrl.getURL());
    }

    protected Bundle installBundle(String groupId, String artifactId, String classifier, String type) throws Exception {
        MavenArtifactProvisionOption mvnUrl = mavenBundle(groupId, artifactId);
        return bundleContext.installBundle(mvnUrl.getURL());
    }

    protected Bundle getInstalledBundle(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        return null;
    }

    public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) {
        return mavenBundle(groupId, artifactId, null, null, null);
    }

    public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId, String version, String classifier, String type) {
        MavenArtifactProvisionOption m = CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId);
        if (version != null) {
            m.version(version);
        } else {
            m.versionAsInProject();
        }
        if (classifier != null) {
            m.classifier(classifier);
        }
        if (type != null) {
            m.type(type);
        }
        return m;
    }

    public static String getArtifactVersion( final String groupId, final String artifactId ) {
        final Properties dependencies = new Properties();
        try {
            dependencies.load(new FileInputStream(new File(System.getProperty("basedir"), "target/classes/META-INF/maven/dependencies.properties")));
            final String version = dependencies.getProperty( groupId + "/" + artifactId + "/version" );
            if( version == null ) {
                throw new RuntimeException(
                    "Could not resolve version. Do you have a dependency for " + groupId + "/" + artifactId
                    + " in your maven project?"
                );
            }
            return version;
        } catch( IOException e ) {
            // TODO throw a better exception
            throw new RuntimeException(
                "Could not resolve version. Did you configured the plugin in your maven project?"
                + "Or maybe you did not run the maven build and you are using an IDE?", e
            );
        }
    }

}
