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

import javax.management.ObjectName;

import org.apache.servicemix.jbi.deployer.AdminCommandsService;
import org.apache.servicemix.nmr.management.Nameable;
import org.fusesource.commons.management.ManagementStrategy;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.localRepository;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4TestRunner.class)
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
        ManagementStrategy ms = getOsgiService(ManagementStrategy.class);

        assertTrue("expected AdminCommandsService MBean", ms.isManaged(null, getAdminCommandsName(ms)));

        try {
            String res = admin.installComponent(smxJsr181, null, false);
            System.err.println(res);
            fail("Call should have failed: " + res);
        } catch (Throwable t) {
            // Expected
        }

        assertComponentMBean(ms, "servicemix-jsr181", false);

        System.err.println(admin.installSharedLibrary(smxShared, false));
        System.err.println(admin.installComponent(smxJsr181, null, false));

        assertComponentMBean(ms, "servicemix-jsr181", true);

        try {
            String res = admin.installComponent(smxJsr181, null, false);
            System.err.println(res);
            fail("Call should have failed: " + res);
        } catch (Throwable t) {
            // Expected
        }

        System.err.println(admin.uninstallComponent("servicemix-jsr181"));

        System.err.println(admin.installComponent(smxJsr181, null, false));

        assertComponentMBean(ms, "servicemix-jsr181", true);
        assertComponentMBean(ms, "servicemix-http", false);

        System.err.println(admin.installComponent(smxHttp, null, false));

        assertComponentMBean(ms, "servicemix-http", true);

        System.err.println(admin.startComponent("servicemix-jsr181"));
        System.err.println(admin.startComponent("servicemix-http"));

        assertFalse("unexpected ServiceAssembly MBean", ms.isManaged(null, getServiceAssemblyName(ms, "wsdl-first-sa")));

        System.err.println(admin.deployServiceAssembly(wsdlFirst, false));

        assertTrue("expected ServiceAssembly MBean", ms.isManaged(null, getServiceAssemblyName(ms, "wsdl-first-sa")));

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

        assertComponentMBean(ms, "servicemix-jsr181", true);
        assertComponentMBean(ms, "servicemix-http", true);

        System.err.println(admin.uninstallComponent("servicemix-http"));
        System.err.println(admin.uninstallComponent("servicemix-jsr181"));

        System.err.println(admin.uninstallSharedLibrary("servicemix-shared"));
    }

    protected AdminCommandsService getAdminCommands() {
        return getOsgiService(AdminCommandsService.class);
    }

    private ObjectName getAdminCommandsName(ManagementStrategy ms) throws Exception {
        Nameable nameable = getNameable("AdminCommandsService", 
                                        "ServiceMix",
                                        "SystemService", 
                                        null, 
                                        null, 
                                        null);
        return ms.getManagedObjectName(nameable, 
                                       null, 
                                       ObjectName.class);
    }

    private ObjectName getComponentName(ManagementStrategy ms, String componentName) throws Exception {
        Nameable nameable = getNameable(componentName,
                                        null,
                                        "Component",
                                        "LifeCycle",
                                        null,
                                        null);
        ObjectName on = ms.getManagedObjectName(nameable,
                                       null,
                                       ObjectName.class);
        System.out.println("@@@ querying: " + on);
        return on;
    }

    private ObjectName getServiceAssemblyName(ManagementStrategy ms, String saName) throws Exception {
        Nameable nameable = getNameable(saName,
                                        null,
                                        "ServiceAssembly",
                                        null,
                                        null,
                                        null);
        return ms.getManagedObjectName(nameable,
                                       null,
                                       ObjectName.class);
    }


    protected Nameable getNameable(final String name,
                                   final String parent,
                                   final String type,
                                   final String subtype,
                                   final String version,
                                   final Class primary) {
        return new Nameable() {
            public String getName() {
                return name;
            }                    
            public String getParent() {
                return parent;
            }
            public String getMainType() {
                return type;
            }
            public String getSubType() {
                return subtype;
            }
            public String getVersion() {
                return version;
            }
            public Class getPrimaryInterface() {
                return primary;
            }
        };
    }

    protected void assertComponentMBean(ManagementStrategy ms, String componentName, boolean expected) throws Exception {
        if (expected) {
            assertTrue("expected Component MBean", ms.isManaged(null, getComponentName(ms, componentName)));
        } else {
            assertFalse("unexpected Component MBean", ms.isManaged(null, getComponentName(ms, componentName)));
        }
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
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),
            systemProperty("basedir").value(System.getProperty("basedir")),
            systemProperty("karaf.name").value("root"),
            systemProperty("karaf.home").value("target/karaf.home"),
            systemProperty("karaf.base").value("target/karaf.home"),
            systemProperty("karaf.startLocalConsole").value("false"),
            systemProperty("karaf.startRemoteShell").value("false"),
            when ((System.getProperty("maven.repo.local")!=null) || (System.getProperty("localRepository")!=null)).useOptions(
                localRepository(System.getProperty("maven.repo.local", System.getProperty("localRepository", ""))),
                systemProperty("localRepository").value(System.getProperty("maven.repo.local", System.getProperty("localRepository", "")))
            ),

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
            mavenBundle("org.apache.geronimo.blueprint", "geronimo-blueprint"),
            // Pax mvn handler
            mavenBundle("org.ops4j.pax.url", "pax-url-mvn"),

            // Bundles
            mavenBundle("org.apache.mina", "mina-core"),
            mavenBundle("org.apache.sshd", "sshd-core"),
            mavenBundle("org.apache.felix.karaf.jaas", "org.apache.felix.karaf.jaas.config"),
            mavenBundle("org.apache.felix.gogo", "org.apache.felix.gogo.runtime"),
            mavenBundle("org.apache.felix.karaf.shell", "org.apache.felix.karaf.shell.console"),
            mavenBundle("org.apache.felix.karaf.shell", "org.apache.felix.karaf.shell.osgi"),
            mavenBundle("org.apache.felix.karaf.shell", "org.apache.felix.karaf.shell.log").noStart(),
            mavenBundle("org.apache.felix.karaf", "org.apache.felix.karaf.management"),
            
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
            mavenBundle("org.codehaus.woodstox", "stax2-api"),
            mavenBundle("org.codehaus.woodstox", "woodstox-core-asl"),
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
