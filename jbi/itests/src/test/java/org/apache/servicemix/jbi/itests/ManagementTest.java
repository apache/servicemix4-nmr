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

import org.apache.servicemix.jbi.deployer.AdminCommandsService;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.junit.Assert.fail;
import org.junit.Test;

public class ManagementTest extends AbstractIntegrationTest {

    @Test
    public void testInstallUninstall() throws Exception {
        String smxShared = localMavenBundle("org.apache.servicemix", "servicemix-shared",
                                            getArtifactVersion("org.apache.servicemix", "servicemix-shared"),
                                            "installer", "zip").getPath();
        String smxJsr181 = localMavenBundle("org.apache.servicemix", "servicemix-jsr181",
                                            getArtifactVersion("org.apache.servicemix", "servicemix-jsr181"),
                                            "installer", "zip").getPath();
        String smxHttp = localMavenBundle("org.apache.servicemix", "servicemix-http",
                                          getArtifactVersion("org.apache.servicemix", "servicemix-http"),
                                          "installer", "zip").getPath();
        String wsdlFirst = localMavenBundle("org.apache.servicemix.samples.wsdl-first", "wsdl-first-sa",
                                            getArtifactVersion("org.apache.servicemix.samples.wsdl-first", "wsdl-first-sa"),
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

    protected File localMavenBundle(String groupId, String artifact, String version, String classifier, String type) {
        String defaultHome = new File(new File(System.getProperty("user.home")), ".m2/repository").getAbsolutePath();
        File repositoryHome = new File(System.getProperty("localRepository", defaultHome));

        StringBuffer location = new StringBuffer(groupId.replace('.', '/'));
        location.append('/');
        location.append(artifact);
        location.append('/');
        location.append(getSnapshot(version));
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

    protected static String getSnapshot(String version) {
        if (isTimestamped(version)) {
            return version.substring(0, version.lastIndexOf('-', version.lastIndexOf('-') - 1)) + "-SNAPSHOT";
        }
        return version;
    }

    protected static boolean isTimestamped(String version) {
        return version.matches(".+-\\d\\d\\d\\d\\d\\d\\d\\d\\.\\d\\d\\d\\d\\d\\d-\\d+");
    }

    @Configuration
    public static Option[] configuration() {
        Option[] options = options(
            // this is how you set the default log level when using pax logging (logProfile)
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
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
