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

import java.util.Properties;

import javax.jbi.component.Component;

import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.runtime.testing.support.AbstractIntegrationTest;

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
            getBundle("org.apache.geronimo.specs", "geronimo-stax-api_1.0_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-activation_1.1_spec"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.api"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.core"),
			getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.spring"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.osgi"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.api"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.runtime"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.deployer"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.offline"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.osgi"),
            getBundle("org.apache.servicemix.runtime", "org.apache.servicemix.runtime.filemonitor"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.ant-1.7.0"),
		};
	}

    public void testJbiComponent() throws Exception {
        System.out.println("Waiting for NMR");
        NMR nmr = getOsgiService(NMR.class);
        assertNotNull(nmr);
        installBundle("org.apache.servicemix", "servicemix-shared-compat", "installer", "zip");
        installBundle("org.apache.servicemix", "servicemix-eip", "installer", "zip");
        System.out.println("Waiting for JBI Component");
        Component cmp = (Component) getOsgiService(Component.class);
        assertNotNull(cmp);
    }

    /*
    public void testServiceAssembly() throws Exception {
        System.out.println("Waiting for NMR");
        NMR nmr = getOsgiService(NMR.class);
        assertNotNull(nmr);
        installBundle("org.apache.servicemix", "servicemix-shared-compat", "installer", "zip");
        installBundle("org.apache.servicemix", "servicemix-jsr181", "installer", "zip");
        installBundle("org.apache.servicemix", "servicemix-http", "installer", "zip");
        installBundle("org.apache.servicemix.samples.wsdl-first", "wsdl-first-sa", null, "zip");
        System.out.println("Waiting for JBI Service Assembly");
        ServiceAssembly sa = (ServiceAssembly) getOsgiService(ServiceAssembly.class);
        assertNotNull(sa);
    }
    */

}