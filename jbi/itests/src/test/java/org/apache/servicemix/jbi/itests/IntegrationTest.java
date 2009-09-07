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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Properties;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jbi.component.Component;
import javax.jbi.management.LifeCycleMBean;

import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.handler.JBIDeploymentListener;
import org.apache.servicemix.nmr.api.NMR;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.junit.runner.RunWith;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

@RunWith(JUnit4TestRunner.class)
public class IntegrationTest extends AbstractIntegrationTest {

    @Test
    public void testJbiComponent() throws Exception {
        System.out.println("Waiting for NMR");
        NMR nmr = getOsgiService(NMR.class);
        assertNotNull(nmr);

        Bundle smxShared = installJbiBundle("org.apache.servicemix", "servicemix-shared", "installer", "zip");
        Bundle smxEip = installJbiBundle("org.apache.servicemix", "servicemix-eip", "installer", "zip");

        smxShared.start();

        System.err.println("servicemix-shared headers: [");
        for (Enumeration e = smxShared.getHeaders().keys(); e.hasMoreElements();) {
            Object k = e.nextElement();
            Object v = smxShared.getHeaders().get(k);
            System.err.println("\t" + k + " = " + v);
        }
        System.err.println("]");

        smxEip.start();

        System.out.println("Waiting for JBI Component");
        Component cmp = getOsgiService(Component.class);
        assertNotNull(cmp);
    }

    @Test
    public void testServiceAssembly() throws Throwable {
        System.out.println("Waiting for NMR");
        NMR nmr = getOsgiService(NMR.class);
        assertNotNull(nmr);

        Bundle smxShared = installJbiBundle("org.apache.servicemix", "servicemix-shared", "installer", "zip");
        Bundle smxJsr181 = installJbiBundle("org.apache.servicemix", "servicemix-jsr181", "installer", "zip");
        Bundle smxHttp = installJbiBundle("org.apache.servicemix", "servicemix-http", "installer", "zip");
        Bundle saBundle = installJbiBundle("org.apache.servicemix.samples.wsdl-first", "wsdl-first-sa", null, "zip");

        smxShared.start();
        smxJsr181.start();
        smxHttp.start();
        saBundle.start();

        System.out.println("Waiting for JBI Service Assembly");
        ServiceAssembly sa = getOsgiService(ServiceAssembly.class);
        assertNotNull(sa);

        Thread.sleep(500);
        
        final List<Throwable> errors = new CopyOnWriteArrayList<Throwable>();
        final int nbThreads = 2;
        final int nbMessagesPerThread = 10;
        final CountDownLatch latch = new CountDownLatch(nbThreads * nbMessagesPerThread);
        for (int i = 0; i < nbThreads; i++) {
            new Thread() {
                public void run() {
                    for (int i = 0; i < nbMessagesPerThread; i++) {
                        try {
                            URL url = new URL("http://localhost:8192/PersonService/");
                            URLConnection connection = url.openConnection();
                            connection.setDoInput(true);
                            connection.setDoOutput(true);
                            connection.getOutputStream().write(
                                   ("<env:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                                    "              xmlns:tns=\"http://servicemix.apache.org/samples/wsdl-first/types\">\n" +
                                    "  <env:Body>\n" +
                                    "    <tns:GetPerson>\n" +
                                    "      <tns:personId>world</tns:personId>\n" +
                                    "    </tns:GetPerson>\n" +
                                    "  </env:Body>\n" +
                                    "</env:Envelope>").getBytes());
                            byte[] buffer = new byte[8192];
                            int len = connection.getInputStream().read(buffer);
                            if (len == -1) {
                                throw new Exception("No response available");
                            }
                            String result = new String(buffer, 0, len);
                            System.out.println(result);
                        } catch (Throwable t) {
                            errors.add(t);
                            t.printStackTrace();
                        } finally {
                            latch.countDown();
                        }
                    }
                }
            }.start();
        }

        if (!latch.await(60, TimeUnit.SECONDS)) {
            fail("Test timed out");
        }
        if (!errors.isEmpty()) {
            throw errors.get(0);
        }
        //Thread.sleep(500);
        saBundle.uninstall();
        //sa.stop();
        //sa.shutDown();
    }

    @Test
    public void testJbiLifecycle() throws Exception {
        System.out.println("Waiting for NMR");
        NMR nmr = getOsgiService(NMR.class);
        assertNotNull(nmr);

        Bundle smxShared = installJbiBundle("org.apache.servicemix", "servicemix-shared", "installer", "zip");
        Bundle smxJsr181 = installJbiBundle("org.apache.servicemix", "servicemix-jsr181", "installer", "zip");
        Bundle smxHttp = installJbiBundle("org.apache.servicemix", "servicemix-http", "installer", "zip");
        Bundle saBundle = installJbiBundle("org.apache.servicemix.samples.wsdl-first", "wsdl-first-sa", null, "zip");

        smxShared.start();
        smxJsr181.start();
        smxHttp.start();
        saBundle.start();

        System.out.println("Waiting for JBI Service Assembly");
        ServiceAssembly sa = getOsgiService(ServiceAssembly.class);
        assertNotNull(sa);
        assertEquals(LifeCycleMBean.STARTED, sa.getCurrentState());

        saBundle.stop();

        saBundle.start();
        sa = getOsgiService(ServiceAssembly.class);
        assertNotNull(sa);
        assertEquals(LifeCycleMBean.STARTED, sa.getCurrentState());

        saBundle.update();
        sa = getOsgiService(ServiceAssembly.class);
        assertNotNull(sa);
        assertEquals(LifeCycleMBean.STARTED, sa.getCurrentState());

        smxHttp.stop();
        try {
            getOsgiService(ServiceAssembly.class, 1);
            fail("ServiceAssembly OSGi service should have been unregistered");
        } catch (RuntimeException e) {
            // Ignore
        }

        smxHttp.start();
        sa = getOsgiService(ServiceAssembly.class);
        assertNotNull(sa);
        assertEquals(LifeCycleMBean.STARTED, sa.getCurrentState());

    }

    protected Bundle installJbiBundle(String groupId, String artifactId, String classifier, String type) throws BundleException {
        getOsgiService(org.osgi.service.url.URLStreamHandlerService.class, "(url.handler.protocol=jbi)", DEFAULT_TIMEOUT);
        getOsgiService(org.osgi.service.url.URLStreamHandlerService.class, "(url.handler.protocol=mvn)", DEFAULT_TIMEOUT);

        MavenArtifactProvisionOption mvnUrl = mavenBundle(groupId, artifactId, getArtifactVersion(groupId, artifactId), classifier, type);
        return bundleContext.installBundle("jbi:" + mvnUrl.getURL());
//        String version = getBundleVersion(groupId, artifactId);
//        File loc = localMavenBundle(groupId, artifactId, version, classifier, type);
//        File tmpDir = new File("target/temp/");
//        tmpDir.mkdirs();
//        File out = new JBIDeploymentListener().transform(loc, tmpDir);
//        Bundle bundle = bundleContext.installBundle(out.toURI().toString());
//        bundle.start();
//        return bundle;
    }

    @Configuration
    public static Option[] configuration() {
        Option[] options = options(
            // this is how you set the default log level when using pax logging (logProfile)
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
            systemProperty("basedir").value(System.getProperty("basedir")),
            systemProperty("karaf.name").value("root"),
            systemProperty("karaf.home").value("target/karaf.home"),
            systemProperty("karaf.base").value("target/karaf.home"),
            systemProperty("karaf.startLocalConsole").value("false"),
            systemProperty("karaf.startRemoteShell").value("false"),

            // hack system packages
            systemPackages("org.apache.felix.karaf.main.spi;version=1.0.0", "org.apache.felix.karaf.jaas.boot;version=0.9.0"),
            bootClasspathLibrary(mavenBundle("org.apache.felix.karaf.jaas", "org.apache.felix.karaf.jaas.boot")).afterFramework(),
            bootClasspathLibrary(mavenBundle("org.apache.felix.karaf", "org.apache.felix.karaf.main")).afterFramework(),
            bootClasspathLibrary(mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec")).beforeFramework(),

            // Log
            mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
            mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
            // Felix Config Admin
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
            // Blueprint
            mavenBundle("org.apache.geronimo", "blueprint-bundle"),
            // Pax mvn handler
            mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),

            // Bundles
            mavenBundle("org.apache.mina", "mina-core"),
            mavenBundle("org.apache.sshd", "sshd-core"),
            mavenBundle("org.apache.felix.karaf", "org.apache.felix.karaf.management"),
            mavenBundle("org.apache.felix.karaf.jaas", "org.apache.felix.karaf.jaas.config"),
            mavenBundle("org.apache.felix.gogo", "org.apache.felix.gogo.runtime"),
            mavenBundle("org.apache.felix.karaf.gshell", "org.apache.felix.karaf.gshell.console"),
            mavenBundle("org.apache.felix.karaf.gshell", "org.apache.felix.karaf.gshell.osgi"),
            mavenBundle("org.apache.felix.karaf.gshell", "org.apache.felix.karaf.gshell.log").noStart(),

            equinox(),

            // Spring
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.aopalliance"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.cglib"),
            mavenBundle("org.springframework", "spring-core"),
            mavenBundle("org.springframework", "spring-beans"),
            mavenBundle("org.springframework", "spring-context"),
            mavenBundle("org.springframework", "spring-aop"),
            mavenBundle("org.springframework.osgi", "spring-osgi-core"),
            mavenBundle("org.springframework.osgi", "spring-osgi-io"),
            mavenBundle("org.springframework.osgi", "spring-osgi-extender"),

            // Bundles for NMR + JBI
            mavenBundle("org.fusesource.commonman", "commons-management"),
            mavenBundle("org.apache.xbean", "xbean-naming"),
            mavenBundle("org.apache.servicemix.naming", "org.apache.servicemix.naming"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.ant"),
			mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javax.mail"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.woodstox"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.wsdl4j"),
            mavenBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.stax-api-1.0"),
            mavenBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.jbi-api-1.0"),
            mavenBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.activation-api-1.1"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
            mavenBundle("org.apache.felix", "org.apache.felix.prefs"),
            mavenBundle("org.apache.xbean", "xbean-classloader"),
            mavenBundle("org.apache.felix", "org.apache.felix.fileinstall"),
            mavenBundle("org.apache.servicemix", "servicemix-utils"),
            mavenBundle("org.apache.servicemix.document", "org.apache.servicemix.document"),
            mavenBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.api"),
            mavenBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.core"),
            mavenBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.spring"),
            mavenBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.osgi"),
			mavenBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.management"),
            mavenBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.runtime"),
            mavenBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.deployer"),
            mavenBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.osgi")
        );
        return options;
    }

}
