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

import org.apache.servicemix.kernel.testing.support.AbstractIntegrationTest;
import org.apache.servicemix.jbi.deployer.AdminCommandsService;

public class ManagementTest extends AbstractIntegrationTest {

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
            getBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.activation-api-1.1"),
            getBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.javamail-api-1.4"),
            getBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec"),
            getBundle("org.apache.felix", "org.apache.felix.prefs"),
            getBundle("org.apache.xbean", "xbean-classloader"),
            getBundle("org.apache.servicemix.kernel", "org.apache.servicemix.kernel.filemonitor"),
            getBundle("org.apache.servicemix.kernel", "org.apache.servicemix.kernel.management"),
            getBundle("org.apache.servicemix", "servicemix-utils"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.api"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.core"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.spring"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.osgi"),
            getBundle("org.apache.servicemix.document", "org.apache.servicemix.document"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.runtime"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.deployer"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.osgi"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.ant"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.woodstox"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.wsdl4j"),
		};
	}

    public void testInstallUninstall() throws Exception {
        String smxShared = localMavenBundle("org.apache.servicemix", "servicemix-shared", getBundleVersion("org.apache.servicemix", "servicemix-shared"),
                                            "installer", "zip").getPath();
        String smxJsr181 = localMavenBundle("org.apache.servicemix", "servicemix-jsr181", getBundleVersion("org.apache.servicemix", "servicemix-jsr181"),
                                            "installer", "zip").getPath();
        String smxHttp = localMavenBundle("org.apache.servicemix", "servicemix-http", getBundleVersion("org.apache.servicemix", "servicemix-http"),
                                            "installer", "zip").getPath();
        String wsdlFirst = localMavenBundle("org.apache.servicemix.samples.wsdl-first", "wsdl-first-sa",
                                            getBundleVersion("org.apache.servicemix.samples.wsdl-first", "wsdl-first-sa"),
                                            null, "zip").getPath();

        AdminCommandsService admin = getAdminCommands();

        try {
            String res = admin.installComponent(smxJsr181, null, false);
            System.err.println(res);
            fail("Call should have failed: " + res);
        } catch (Throwable t) {
            // Expected
        }

        System.err.println(admin.installSharedLibrary(smxShared, false));
        System.err.println(admin.installComponent(smxJsr181, null, false));

        try {
            String res = admin.installComponent(smxJsr181, null, false);
            System.err.println(res);
            fail("Call should have failed: " + res);
        } catch (Throwable t) {
            // Expected
        }

        System.err.println(admin.uninstallComponent("servicemix-jsr181"));

        System.err.println(admin.installComponent(smxJsr181, null, false));
        System.err.println(admin.installComponent(smxHttp, null, false));

        System.err.println(admin.startComponent("servicemix-jsr181"));
        System.err.println(admin.startComponent("servicemix-http"));

        System.err.println(admin.deployServiceAssembly(wsdlFirst, false));
        System.err.println(admin.undeployServiceAssembly("wsdl-first-sa"));

        System.err.println(admin.deployServiceAssembly(wsdlFirst, false));
        System.err.println(admin.startServiceAssembly("wsdl-first-sa"));
        System.err.println(admin.stopServiceAssembly("wsdl-first-sa"));
        System.err.println(admin.shutdownServiceAssembly("wsdl-first-sa"));
        System.err.println(admin.undeployServiceAssembly("wsdl-first-sa"));

        System.err.println(admin.stopComponent("servicemix-jsr181"));
        System.err.println(admin.stopComponent("servicemix-http"));

        System.err.println(admin.shutdownComponent("servicemix-jsr181"));
        System.err.println(admin.shutdownComponent("servicemix-http"));

        System.err.println(admin.uninstallComponent("servicemix-http"));
        System.err.println(admin.uninstallComponent("servicemix-jsr181"));

        System.err.println(admin.uninstallSharedLibrary("servicemix-shared"));
    }

    protected AdminCommandsService getAdminCommands() {
        return getOsgiService(AdminCommandsService.class);
    }

}
