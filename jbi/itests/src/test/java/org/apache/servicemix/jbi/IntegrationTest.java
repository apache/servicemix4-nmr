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
package org.apache.servicemix.jbi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jbi.component.Component;

import org.apache.servicemix.jbi.offline.Main;
import org.apache.servicemix.nmr.api.NMR;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;
import org.springframework.osgi.util.OsgiFilterUtils;
import org.springframework.osgi.util.OsgiListenerUtils;
import org.springframework.osgi.internal.util.concurrent.Counter;

public class IntegrationTest extends AbstractConfigurableBundleCreatorTests {

    private Properties dependencies;

    /**
	 * The manifest to use for the "virtual bundle" created
	 * out of the test classes and resources in this project
	 *
	 * This is actually the boilerplate manifest with one additional
	 * import-package added. We should provide a simpler customization
	 * point for such use cases that doesn't require duplication
	 * of the entire manifest...
	 */
	protected String getManifestLocation() {
		return "classpath:org/apache/servicemix/MANIFEST.MF";
	}

	/**
	 * The location of the packaged OSGi bundles to be installed
	 * for this test. Values are Spring resource paths. The bundles
	 * we want to use are part of the same multi-project maven
	 * build as this project is. Hence we use the localMavenArtifact
	 * helper method to find the bundles produced by the package
	 * phase of the maven build (these tests will run after the
	 * packaging phase, in the integration-test phase).
	 *
	 * JUnit, commons-logging, spring-core and the spring OSGi
	 * test bundle are automatically included so do not need
	 * to be specified here.
	 */
	protected String[] getTestBundlesNames() {
        return new String[] {
            getBundle("org.apache.geronimo.specs", "geronimo-jms_1.1_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-servlet_2.5_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-j2ee-management_1.1_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-stax-api_1.0_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-activation_1.1_spec"),
            getBundle("org.apache.felix", "org.osgi.compendium"),
            getBundle("org.ops4j.pax.logging", "pax-logging-api"),
            getBundle("org.ops4j.pax.logging", "pax-logging-service"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.aopalliance"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxb-api"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxb-impl"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.httpcore"),
            getBundle("org.apache.activemq", "activemq-core"),
            getBundle("org.springframework", "spring-beans"),
            getBundle("org.springframework", "spring-core"),
            getBundle("org.springframework", "spring-context"),
            getBundle("org.springframework", "spring-aop"),
            getBundle("org.springframework", "spring-test"),
            getBundle("org.springframework", "spring-tx"),
            getBundle("org.springframework", "spring-jms"),
            getBundle("org.springframework.osgi", "spring-osgi-core"),
            getBundle("org.springframework.osgi", "spring-osgi-io"),
            getBundle("org.springframework.osgi", "spring-osgi-extender"),
            getBundle("org.springframework.osgi", "spring-osgi-test"),
            getBundle("org.springframework.osgi", "spring-osgi-annotation"),
            getBundle("org.springframework.osgi", "junit.osgi"),
            getBundle("org.springframework.osgi", "asm.osgi"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.api"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.core"),
			getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.spring"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.osgi"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.api"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.runtime"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.deployer"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.offline"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.osgi"),
		};
	}

    protected String getBundle(String groupId, String artifactId) {
        return groupId + "," + artifactId + "," + getBundleVersion(groupId, artifactId);
    }

    protected String getBundleVersion(String groupId, String artifactId) {
        if (dependencies == null) {
            try {
                File f = new File(System.getProperty("basedir"), "target/classes/META-INF/maven/dependencies.properties");
                Properties prop = new Properties();
                prop.load(new FileInputStream(f));
                dependencies = prop;
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load dependencies informations", e);
            }
        }
        String version = dependencies.getProperty(groupId + "/" + artifactId + "/version");
        if (version == null) {
            throw new IllegalStateException("Unable to find dependency information for: " + groupId + "/" + artifactId + "/version");
        }
        return version;
    }

    protected String[] getTestFrameworkBundlesNames() {
        return null;
    }

    public void testJbiComponent() throws Exception {
        System.out.println("Waiting for NMR");
        NMR nmr = getOsgiService(NMR.class);
        assertNotNull(nmr);
        installBundle(getArtifact("servicemix-shared-compat"));
        installBundle(getArtifact("servicemix-eip"));
        System.out.println("Waiting for JBI Component");
        Component cmp = (Component) getOsgiService(Component.class);
        assertNotNull(cmp);
    }

    protected File getArtifact(final String id) {
        return new File("target/components/").listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(id);
            }
        })[0];
    }

    protected void installArtifact(String groupId, String artifactId, String classifier, String type) throws Exception {
        String version = getBundleVersion(groupId, artifactId);
        File in = localMavenBundle(groupId, artifactId, version, classifier, type);
        File out = File.createTempFile("smx", ".jar");
        new Main().run(in.toString(), out.toString());
        installBundle(out);
    }

    protected void installBundle(File file) throws Exception {
        installBundle(file.getAbsoluteFile().toURI().toString());
    }

    protected void installBundle(String uri) throws Exception {
        System.out.println("Installing bundle " + uri);
        Bundle bundle = bundleContext.installBundle(uri);
        bundle.start();
    }

    protected File localMavenBundle(String groupId, String artifact, String version, String classifier, String type) {
        String defaultHome = new File(new File(System.getProperty("user.home")), ".m2/repository").getAbsolutePath();
        File repositoryHome = new File(System.getProperty("localRepository", defaultHome));

        StringBuffer location = new StringBuffer(groupId.replace('.', '/'));
        location.append('/');
        location.append(artifact);
        location.append('/');
        location.append(version);
        location.append('/');
        location.append(artifact);
        location.append('-');
        location.append(version);
        if (classifier != null) {
            location.append('-');
            location.append(classifier);
        }
        location.append(".");
        location.append(type);

        return new File(repositoryHome, location.toString());
    }

    public <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, DEFAULT_WAIT_TIME);
    }

    public <T> T getOsgiService(Class<T> type, long timeout) {
        // translate from seconds to miliseconds
        long time = timeout * 1000;

        // use the counter to make sure the threads block
        final Counter counter = new Counter("waitForOsgiService on bnd=" + type.getName());

        counter.increment();

        final List<T> services = new ArrayList<T>();

        ServiceListener listener = new ServiceListener() {
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    services.add((T) bundleContext.getService(event.getServiceReference()));
                    counter.decrement();
                }
            }
        };

        String filter = OsgiFilterUtils.unifyFilter(type.getName(), null);
        OsgiListenerUtils.addServiceListener(bundleContext, listener, filter);

        if (logger.isDebugEnabled())
            logger.debug("start waiting for OSGi service=" + type.getName());

        try {
            if (counter.waitForZero(time)) {
                logger.warn("waiting for OSGi service=" + type.getName() + " timed out");
                throw new RuntimeException("Gave up waiting for OSGi service '" + type.getName() + "' to be created");
            }
            else if (logger.isDebugEnabled()) {
                logger.debug("found OSGi service=" + type.getName());
            }
            return services.get(0);
        }
        finally {
            // inform waiting thread
            bundleContext.removeServiceListener(listener);
        }
    }

    /**
     * Place the current (test) thread to wait for the a Spring application
     * context to be published under the given symbolic name. This method allows
     * waiting for full initialization of Spring OSGi bundles before starting
     * the actual test execution.
     *
     * @param interfaceName
     * @param timeout
     */
    public void waitForOsgiService(String interfaceName, long timeout) {
        // translate from seconds to miliseconds
        long time = timeout * 1000;

        // use the counter to make sure the threads block
        final Counter counter = new Counter("waitForOsgiService on bnd=" + interfaceName);

        counter.increment();

        ServiceListener listener = new ServiceListener() {
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED)
                    counter.decrement();
            }
        };

        String filter = OsgiFilterUtils.unifyFilter(interfaceName, null);
        OsgiListenerUtils.addServiceListener(bundleContext, listener, filter);

        if (logger.isDebugEnabled())
            logger.debug("start waiting for OSGi service=" + interfaceName);

        try {
            if (counter.waitForZero(time)) {
                logger.warn("waiting for OSGi service=" + interfaceName + " timed out");
                throw new RuntimeException("Gave up waiting for OSGi service '" + interfaceName + "' to be created");
            }
            else if (logger.isDebugEnabled()) {
                logger.debug("found OSGi service=" + interfaceName);
            }
        }
        finally {
            // inform waiting thread
            bundleContext.removeServiceListener(listener);
        }
    }

}