/**
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
package org.apache.servicemix.nmr.management;

import java.util.EventObject;
import java.util.HashMap;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.modelmbean.RequiredModelMBean;

import org.apache.servicemix.nmr.api.internal.InternalEndpoint;

import org.fusesource.commons.management.Statistic;
import org.fusesource.commons.management.Statistic.UpdateMode;

import static org.easymock.classextension.EasyMock.*;
import org.easymock.classextension.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ManagementStrategyTest extends Assert { //TestCase {

    private IMocksControl control; 
    private ManagementAgent strategy = null;
    private MBeanServer mbeanServer = null;

    private static final String JMX_DOMAIN = "smx_domain";
    private static final String EXTENSION_NAME = 
        JMX_DOMAIN + ":Type=Component,Name=extension,SubType=bootstrap";
    private static final String ENDPOINT_NAME = 
        JMX_DOMAIN + ":Type=Endpoint,Id=endpoint_foo";

    @Before
    public void setUp() {
        control = createNiceControl();
        mbeanServer = control.createMock(MBeanServer.class);
        strategy = setUpStrategy();
    } 

    @After
    public void tearDown() {
        strategy = null;
        mbeanServer = null;
    }

    @Test
    public void testGetJmxDomainName() throws Exception {
        Object name = strategy.getManagedObjectName(null, 
                                                    null, 
                                                    String.class);
        assertNotNull(name);
        assertTrue(name instanceof String);
        assertEquals("unexpected domain name", name, JMX_DOMAIN);
    }

    @Test
    public void testAdminCommandsServiceObjectName() throws Exception {
        Nameable nameable = getNameable("AdminCommandsService", 
                                        "ServiceMix",
                                        "SystemService", 
                                        null, 
                                        null, 
                                        null);
        Object name = strategy.getManagedObjectName(nameable, 
                                                    null, 
                                                    ObjectName.class);
        verifyObjectName(name, 
                         ":ContainerName=ServiceMix,Type=SystemService,"
                         + "Name=AdminCommandsService"); 
    }

    @Test
    public void testSharedLibraryObjectName() throws Exception {
        Nameable nameable = getNameable("servicemix-shared", 
                                        null,
                                        "SharedLibrary", 
                                        null, 
                                        "2008.01", 
                                        null);
        Object name = strategy.getManagedObjectName(nameable, 
                                                    null, 
                                                    ObjectName.class);
        verifyObjectName(name,
                         ":Type=SharedLibrary,Name=servicemix-shared,"
                         + "Version=2008.01");
    }

    @Test
    public void testServiceEngineObjectName() throws Exception {
        Nameable nameable = getNameable("servicemix-eip", 
                                        null,
                                        "service-engine", 
                                        "LifeCycle", 
                                        null,
                                        null);
        Object name = strategy.getManagedObjectName(nameable, 
                                                    null, 
                                                    ObjectName.class);
        verifyObjectName(name, 
                         ":Type=service-engine,Name=servicemix-eip,"
                         + "SubType=LifeCycle");
    };

    @Test
    public void testBindingComponentObjectName() throws Exception {
        Nameable nameable = getNameable("servicemix-http", 
                                        null,
                                        "binding-component", 
                                        "LifeCycle", 
                                        null,
                                        null);
        Object name = strategy.getManagedObjectName(nameable, 
                                                    null, 
                                                    ObjectName.class);
        verifyObjectName(name, 
                         ":Type=binding-component,Name=servicemix-http,"
                         + "SubType=LifeCycle");
    }

    @Test
    public void testServiceAssemblyObjectName() throws Exception {
        Nameable nameable = getNameable("wsdl-first-sa", 
                                        null,
                                        "ServiceAssembly", 
                                        null, 
                                        null,
                                        null);
        Object name = strategy.getManagedObjectName(nameable, 
                                                    null, 
                                                    ObjectName.class);
        verifyObjectName(name, 
                         ":Type=ServiceAssembly,Name=wsdl-first-sa");
    }

    @Test
    public void testCustomComponentObjectName() throws Exception {
        Nameable nameable = getNameable("extension", 
                                        null,
                                        null, 
                                        null, 
                                        null,
                                        FooMBean.class);
        Object name = strategy.getManagedObjectName(nameable, 
                                                    "bootstrap", 
                                                    ObjectName.class);
        verifyObjectName(name, 
                         ":Type=Component,Name=extension,SubType=bootstrap");
    }

    @Test
    public void testIsManagedNameManaged() throws Exception {
        doTestIsManagedName(true);
    }

    @Test
    public void testIsManagedNameUnmanaged() throws Exception {
        doTestIsManagedName(false);
    }

    private void doTestIsManagedName(boolean managed) throws Exception {
        ObjectName name = new ObjectName(EXTENSION_NAME);
        expect(mbeanServer.isRegistered(name)).andReturn(managed);
        control.replay();
        if (managed) {
            assertTrue(strategy.isManaged(null, name));
        } else {
            assertFalse(strategy.isManaged(null, name));
        }
        control.verify();
    }

    @Test
    public void testIsManagedObjectManaged() throws Exception {
        doTestIsManagedObject(true);
    }

    @Test
    public void testIsManagedObjectUnmanaged() throws Exception {
        doTestIsManagedObject(false);
    }

    private void doTestIsManagedObject(boolean managed) throws Exception {
        ObjectName name = new ObjectName(EXTENSION_NAME);
        expect(mbeanServer.isRegistered(name)).andReturn(managed);
        control.replay();
        Nameable nameable = getNameable("extension", 
                                        null, 
                                        "Component", 
                                        "bootstrap", 
                                        null, 
                                        FooMBean.class);
        if (managed) {
            assertTrue(strategy.isManaged(nameable, name));
        } else {
            assertFalse(strategy.isManaged(nameable, name));
        }
        control.verify();
    }

    @Test
    public void testManageObjectSingleStep() throws Exception {
        ObjectName name = new ObjectName(EXTENSION_NAME);
        Nameable nameable = new Foo("extension", 
                                    null, 
                                    "Component", 
                                    "bootstrap", 
                                    null, 
                                    "bar");
        ObjectInstance instance = new ObjectInstance(name, Nameable.class.getName());
        expect(mbeanServer.registerMBean(isA(StandardMBean.class), eq(name))).andReturn(instance);
        control.replay();
        strategy.manageObject(nameable);
        control.verify();
    }

    @Test
    public void testManageObjectDualStep() throws Exception {
        Nameable nameable = new Foo("extension", 
                                    null, 
                                    "Component", 
                                    "bootstrap", 
                                    null, 
                                    "bar");
        ObjectName name = strategy.getManagedObjectName(nameable, 
                                                        "bootstrap", 
                                                        ObjectName.class);
        ObjectInstance instance = new ObjectInstance(name, Nameable.class.getName());
        expect(mbeanServer.registerMBean(isA(StandardMBean.class), eq(name))).andReturn(instance);
        control.replay();
        strategy.manageNamedObject(nameable, name);
        control.verify();
    }

    @Test
    public void testRepeatManageObject() throws Exception {
        ObjectName name = new ObjectName(EXTENSION_NAME);
        Nameable nameable = new Foo("extension", 
                                    null, 
                                    "Component", 
                                    "bootstrap", 
                                    null, 
                                    "bar");
        ObjectInstance instance = new ObjectInstance(name, Nameable.class.getName());
        expect(mbeanServer.registerMBean(isA(StandardMBean.class), eq(name))).andReturn(instance).times(2);
        control.replay();
        strategy.manageObject(nameable);
        strategy.manageObject(nameable);
        control.verify();
    }

    @Test
    public void testRepeatManageManagedEndpoint() throws Exception {
        ObjectName name = new ObjectName(ENDPOINT_NAME);
        ObjectInstance instance = new ObjectInstance(name, Nameable.class.getName());
        InternalEndpoint internal = control.createMock(InternalEndpoint.class);
        HashMap<String, Object> props = new HashMap<String, Object>();
        ManagedEndpoint endpoint = 
            new ManagedEndpoint(internal, props, strategy);
        expect(internal.getId()).andReturn("endpoint_foo");
        Exception ex = new NotCompliantMBeanException();
        expect(mbeanServer.registerMBean(isA(ManagedEndpoint.class), eq(name))).andThrow(ex);
        RequiredModelMBean mbean = control.createMock(RequiredModelMBean.class);
        expect(mbeanServer.instantiate(RequiredModelMBean.class.getName())).andReturn(mbean);
        expect(mbeanServer.registerMBean(isA(RequiredModelMBean.class), eq(name))).andReturn(instance);
        control.replay();
        strategy.manageObject(endpoint);
        strategy.manageObject(endpoint);
        control.verify();
    }

    @Test
    public void testCreateStatistics() throws Exception {
        Statistic counter = strategy.createStatistic("counter", null, UpdateMode.COUNTER);
        counter.updateValue(150L);
        counter.updateValue(50L);        
        assertEquals(200L, counter.getValue());

        Statistic value = strategy.createStatistic("value", null, UpdateMode.VALUE);
        value.updateValue(150L);
        value.updateValue(50L);        
        assertEquals(50L, value.getValue());
    }

    @Test
    public void testEventNotify() throws Exception {
        // non-replacable static log factory awkward to mock 
        strategy.notify(new EventObject(this));
    }

    protected ManagementAgent setUpStrategy() {
        ManagementAgent ms = new ManagementAgent();
        DefaultNamingStrategy ns = new DefaultNamingStrategy();
        ns.setJmxDomainName(JMX_DOMAIN);
        ms.setNamingStrategy(ns);
        ms.setMbeanServer(mbeanServer);
        return ms;
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
            public String getType() {
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

    protected void verifyObjectName(Object name, String expected) {
        assertNotNull(name); 
        assertTrue(name instanceof ObjectName);
        ObjectName on = (ObjectName)name;
        assertEquals("unexpected object name", 
                     on.toString(), 
                     (JMX_DOMAIN + expected));
    }

    public interface FooMBean {
        String getBar();
    }

    private static class Foo implements Nameable, FooMBean {
        private String name;
        private String parent;
        private String type;
        private String subType;
        private String version;
        private String bar;
        
        Foo(String name,
            String parent,
            String type,
            String subType,
            String version,
            String bar) {
            this.name = name;
            this.parent = parent;
            this.type = type;
            this.subType = subType;
            this.version = version;
            this.bar = bar;
        }

        public String getName() {
            return name;
        }                
        public String getParent() {
            return parent;
        }
        public String getType() {
            return type;
        }
        public String getSubType() {
            return subType;
        }
        public String getVersion() {
            return version;
        }
        public Class getPrimaryInterface() {
            return FooMBean.class;
        }
        public String getBar() {
            return bar;
        }
    }
}
