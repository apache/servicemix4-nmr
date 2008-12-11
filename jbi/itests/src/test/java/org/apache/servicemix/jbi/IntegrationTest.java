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
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.net.URL;
import java.net.URLConnection;

import javax.jbi.component.Component;

import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.kernel.testing.support.AbstractIntegrationTest;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.handler.JBIDeploymentListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class IntegrationTest extends AbstractIntegrationTest {

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
            getBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.stax-api-1.0"),
            getBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.jbi-api-1.0"),
            getBundle("org.apache.geronimo.specs", "geronimo-activation_1.1_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-javamail_1.4_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
            getBundle("org.apache.felix", "org.apache.felix.prefs"),
            getBundle("org.apache.xbean", "xbean-classloader"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.api"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.core"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.spring"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.osgi"),
            getBundle("org.apache.servicemix.document", "org.apache.servicemix.document"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.runtime"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.deployer"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.osgi"),
            getBundle("org.apache.servicemix.kernel", "org.apache.servicemix.kernel.filemonitor"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.woodstox"),
		};
	}

    public void testJbiComponent() throws Exception {
        System.out.println("Waiting for NMR");
        NMR nmr = getOsgiService(NMR.class);
        assertNotNull(nmr);
        installJbiBundle("org.apache.servicemix", "servicemix-shared", "installer", "zip");
        installJbiBundle("org.apache.servicemix", "servicemix-eip", "installer", "zip");
        System.out.println("Waiting for JBI Component");
        Component cmp = getOsgiService(Component.class);
        assertNotNull(cmp);
    }

    public void testServiceAssembly() throws Exception {
        System.out.println("Waiting for NMR");
        NMR nmr = getOsgiService(NMR.class);
        assertNotNull(nmr);
        installJbiBundle("org.apache.servicemix", "servicemix-shared", "installer", "zip");
        installJbiBundle("org.apache.servicemix", "servicemix-jsr181", "installer", "zip");
        installJbiBundle("org.apache.servicemix", "servicemix-http", "installer", "zip");
        Bundle saBundle = installJbiBundle("org.apache.servicemix.samples.wsdl-first", "wsdl-first-sa", null, "zip");
        System.out.println("Waiting for JBI Service Assembly");
        ServiceAssembly sa = getOsgiService(ServiceAssembly.class);
        assertNotNull(sa);

        final CountDownLatch latch = new CountDownLatch(50);
        for (int i = 0; i < 2; i++) {
            new Thread() {
                public void run() {
                    try {
                        for (;;) {
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
                            String result = new String(buffer, 0, len);
                            System.out.println(result);
                            latch.countDown();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }

        latch.await();
        //Thread.sleep(500);
        saBundle.uninstall();
        //sa.stop();
        //sa.shutDown();
    }

    protected Bundle installJbiBundle(String groupId, String artifactId, String classifier, String type) throws BundleException {
        String version = getBundleVersion(groupId, artifactId);
        File loc = localMavenBundle(groupId, artifactId, version, classifier, type);
        File tmpDir = new File("target/temp/");
        tmpDir.mkdirs();
        File out = new JBIDeploymentListener().handle(loc, tmpDir);
        Bundle bundle = bundleContext.installBundle(out.toURI().toString());
        bundle.start();
        return bundle;
    }
}
