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
package org.apache.servicemix.jbi.management;

import java.util.Map;

import javax.jbi.JBIException;
import javax.jbi.management.ComponentLifeCycleMBean;
import javax.management.ObjectName;

import org.apache.servicemix.jbi.deployer.Component;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 */
@ManagedResource(description = "Managed Component", currencyTimeLimit = 15)
public class ManagedComponent implements ComponentLifeCycleMBean {

    private final Component component;
    private final Map<String, ?> properties;

    public ManagedComponent(Component component, Map<String, ?> properties) {
        this.component = component;
        this.properties = properties;
    }

    public String getName() {
        return component.getName();
    }

    @ManagedAttribute
    public ObjectName getExtensionMBeanName() throws JBIException {
        return component.getExtensionMBeanName();
    }

    @ManagedOperation
    public void start() throws JBIException {
        component.start();
    }

    @ManagedOperation
    public void stop() throws JBIException {
        component.stop();
    }

    @ManagedOperation
    public void shutDown() throws JBIException {
        component.shutDown();
    }

    @ManagedAttribute
    public String getCurrentState() {
        return component.getCurrentState();
    }
}
