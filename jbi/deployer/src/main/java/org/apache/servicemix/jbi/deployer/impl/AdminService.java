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
package org.apache.servicemix.jbi.deployer.impl;

import java.util.HashSet;
import java.util.Set;

import javax.jbi.management.AdminServiceMBean;
import javax.management.ObjectName;

import org.apache.servicemix.jbi.deployer.Component;

/**
 */
public class AdminService implements AdminServiceMBean {

    public static final String DEFAULT_NAME = "ServiceMix";

    public static final String DEFAULT_DOMAIN = "org.apache.servicemix";

    public static final String DEFAULT_CONNECTOR_PATH = "/jmxrmi";

    public static final int DEFAULT_CONNECTOR_PORT = 1099;

    private Deployer deployer;

    public Deployer getDeployer() {
        return deployer;
    }

    public void setDeployer(Deployer deployer) {
        this.deployer = deployer;
    }

    public ObjectName[] getBindingComponents() {
        Set<ObjectName> names = new HashSet<ObjectName>();
        for (Component component : deployer.getComponents().values()) {
            if (Deployer.TYPE_BINDING_COMPONENT.equals(component.getType())) {
                try {
                    names.add(deployer.getManagementStrategy().getManagedObjectName(component, null, ObjectName.class));
                } catch (Exception e) {
                }
            }
        }
        return names.toArray(new ObjectName[names.size()]);
    }

    public ObjectName getComponentByName(String name) {
        Component component = deployer.getComponent(name);
        if (component != null) {
            try {
                return deployer.getManagementStrategy().getManagedObjectName(component, null, ObjectName.class);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public ObjectName[] getEngineComponents() {
        Set<ObjectName> names = new HashSet<ObjectName>();
        for (Component component : deployer.getComponents().values()) {
            if (Deployer.TYPE_SERVICE_ENGINE.equals(component.getType())) {
                try {
                    names.add(deployer.getManagementStrategy().getManagedObjectName(component, null, ObjectName.class));
                } catch (Exception e) {
                }
            }
        }
        return names.toArray(new ObjectName[names.size()]);
    }

    public String getSystemInfo() {
        return "ServiceMix 4";
    }

    public ObjectName getSystemService(String serviceName) {
        // TODO
        return null;
    }

    public ObjectName[] getSystemServices() {
        // TODO
        return new ObjectName[0];
    }

    public boolean isBinding(String componentName) {
        Component component = deployer.getComponent(componentName);
        return component != null && Deployer.TYPE_BINDING_COMPONENT.equals(component.getType());
    }

    public boolean isEngine(String componentName) {
        Component component = deployer.getComponent(componentName);
        return component != null && Deployer.TYPE_SERVICE_ENGINE.equals(component.getType());
    }

}
