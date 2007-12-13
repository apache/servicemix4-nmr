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
package org.apache.servicemix.jbi.deployer.descriptor;

import java.util.Arrays;

import javax.xml.namespace.QName;

import org.w3c.dom.DocumentFragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Nov 8, 2007
 * Time: 5:35:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class DescriptorFactoryTest {

    @Test
    public void testServiceUnit() throws Exception {
        Descriptor root = DescriptorFactory.buildDescriptor(getClass().getResource("serviceUnit.xml"));
        assertNotNull("Unable to parse descriptor", root);

        Services services = root.getServices();
        Consumes[] consumes = services.getConsumes();
        assertNotNull("consumes are null", consumes);
        assertEquals("consumes size", 1, consumes.length);
    }

    @Test
    public void testSharedLibrary() throws Exception {
        Descriptor root = DescriptorFactory.buildDescriptor(getClass().getResource("sharedLibrary.xml"));
        assertNotNull("Unable to parse descriptor", root);

        SharedLibrary sl = root.getSharedLibrary();
        Identification identification = sl.getIdentification();
        assertEquals("getName", "TestSharedLibrary", identification.getName());
        assertEquals("getDescription", "This is a test shared library.", identification.getDescription());
    }

    @Test
    public void testServiceAssembly() throws Exception {
        Descriptor root = DescriptorFactory.buildDescriptor(getClass().getResource("serviceAssembly.xml"));
        assertNotNull("Unable to parse descriptor", root);

        ServiceAssembly serviceAssembly = root.getServiceAssembly();
        assertNotNull("serviceAssembly is null", serviceAssembly);

        Identification identification = serviceAssembly.getIdentification();
        assertNotNull("identification is null", identification);
        assertEquals("getName", "ServiceAssembly_041207153211-0800_saId", identification.getName());
        assertEquals("getDescription", "Description of Service Assembly : ServiceAssembly", identification.getDescription());

        ServiceUnit[] serviceUnits = serviceAssembly.getServiceUnits();
        assertNotNull("serviceUnits are null", serviceUnits);
        assertEquals("serviceUnits size", 4, serviceUnits.length);

        assertEquals("getIdentification().getName() for 0", "TransformationSU_041207152821-0800_suId",
                     serviceUnits[0].getIdentification().getName());
        assertEquals("getIdentification().getDescription() for 0", "Description of Serviceunit: TransformationSU",
                     serviceUnits[0].getIdentification().getDescription());
        assertEquals("getTarget().getArtifactsZip() for 0", "TransformationSU.zip", serviceUnits[0].getTarget().getArtifactsZip());
        assertEquals("getTarget().getComponentName() for 0", "SunTransformationEngine", serviceUnits[0].getTarget().getComponentName());

        assertEquals("getIdentification().getName() for 3", "SequencingEngineSU_041207152507-0800_suId", serviceUnits[3]
                        .getIdentification().getName());
        assertEquals("getIdentification().getDescription() for 3", "Description of Serviceunit: SequencingEngineSU",
                     serviceUnits[3].getIdentification().getDescription());
        assertEquals("getTarget().getArtifactsZip() for 3", "SequencingEngineSU.zip", serviceUnits[3].getTarget().getArtifactsZip());
        assertEquals("getTarget().getComponentName() for 3", "SunSequencingEngine", serviceUnits[3].getTarget().getComponentName());

        Connection[] connections = serviceAssembly.getConnections().getConnections();
        assertNotNull("connections are null", connections);
        assertEquals("connections size", 2, connections.length);

        assertEquals("getConsumer().getServiceName() for 0", new QName("urn:csi", "csi-service"), connections[0].getConsumer().getServiceName());
    }

    @Test
    public void testComponent() throws Exception {
        Descriptor root = DescriptorFactory.buildDescriptor(getClass().getResource("component.xml"));
        assertNotNull("Unable to parse descriptor", root);

        // component stuff
        Component component = root.getComponent();
        assertNotNull("component is null", component);
        assertEquals("getBootstrapClassName", "com.foo.Engine1Bootstrap", component.getBootstrapClassName());
        assertEquals("getComponentClassName", "com.foo.Engine1", component.getComponentClassName());
        assertEquals("getComponentClassPath", new ClassPath(new String[] {"Engine1.jar"}), component.getComponentClassPath());
        assertEquals("getBootstrapClassPath", new ClassPath(new String[] {"Engine2.jar"}), component.getBootstrapClassPath());

        assertEquals("getDescription", "foo", component.getDescription());

        assertArrayEquals("getSharedLibraries", new SharedLibraryList[] {new SharedLibraryList("slib1")}, component.getSharedLibraries());

        Identification identification = component.getIdentification();
        assertNotNull("identification is null", identification);
        assertEquals("getName", "example-engine-1", identification.getName());
        assertEquals("getDescription", "An example service engine", identification.getDescription());

        InstallationDescriptorExtension descriptorExtension = component.getDescriptorExtension();
        assertNotNull("descriptorExtension is null", descriptorExtension);

        DocumentFragment fragment = descriptorExtension.getDescriptorExtension();
        assertNotNull("fragment is null", fragment);
    }

    protected static void assertArrayEquals(String text, Object[] expected, Object[] actual) {
        assertTrue(text + ". Expected <" + toString(expected) + "> Actual <"  + toString(actual) + ">", Arrays.equals(expected, actual));
    }

    protected static String toString(Object[] objects) {
        if (objects == null) {
            return "null Object[]";
        }
        StringBuffer buffer = new StringBuffer("[");
        for (int i = 0; i < objects.length; i++) {
            Object object = objects[i];
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(object);
        }
        buffer.append("]");
        return buffer.toString();
    }

}
