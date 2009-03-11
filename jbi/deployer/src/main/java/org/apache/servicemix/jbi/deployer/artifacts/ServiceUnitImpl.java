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
package org.apache.servicemix.jbi.deployer.artifacts;

import java.io.File;

import javax.jbi.JBIException;
import javax.jbi.management.ComponentLifeCycleMBean;
import javax.jbi.management.DeploymentException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceUnitDesc;


public class ServiceUnitImpl implements ServiceUnit {

    protected final Log LOGGER = LogFactory.getLog(getClass());

    private ServiceUnitDesc serviceUnitDesc;

    private File rootDir;

    private ComponentImpl component;

    private ServiceAssemblyImpl serviceAssembly;

    public ServiceUnitImpl(ServiceUnitDesc serviceUnitDesc, File rootDir, ComponentImpl component) {
        this.serviceUnitDesc = serviceUnitDesc;
        this.rootDir = rootDir;
        this.component = component;
    }

    public String getKey() {
        return getComponentName() + "/" + getName();
    }

    public String getName() {
        return serviceUnitDesc.getIdentification().getName();
    }

    public String getDescription() {
        return serviceUnitDesc.getIdentification().getDescription();
    }

    public String getDescriptor() {
        // TODO: implement this
        throw new UnsupportedOperationException();
    }

    public String getComponentName() {
        return serviceUnitDesc.getTarget().getComponentName();
    }

    public ServiceAssembly getServiceAssembly() {
        return serviceAssembly;
    }

    protected ServiceAssemblyImpl getServiceAssemblyImpl() {
        return serviceAssembly;
    }

    protected void setServiceAssemblyImpl(ServiceAssemblyImpl serviceAssembly) {
        this.serviceAssembly = serviceAssembly;
    }

    public Component getComponent() {
        return component;
    }

    public ComponentImpl getComponentImpl() {
        return component;
    }

    public File getRootDir() {
        return rootDir;
    }

    public void deploy() throws JBIException {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(component.getComponentClassLoader());
            component.getComponent().getServiceUnitManager().deploy(getName(), getRootDir() != null ? getRootDir().getAbsolutePath() : null);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
        component.addServiceUnit(this);
    }

    public void init() throws JBIException {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(component.getComponentClassLoader());
            component.getComponent().getServiceUnitManager().init(getName(), getRootDir() != null ? getRootDir().getAbsolutePath() : null);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
        component.addServiceUnit(this);
    }

    public void start() throws JBIException {
        checkComponentStarted("start");
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(component.getComponentClassLoader());
            component.getComponent().getServiceUnitManager().start(getName());
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    public void stop() throws JBIException {
        checkComponentStarted("stop");
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(component.getComponentClassLoader());
            component.getComponent().getServiceUnitManager().stop(getName());
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    public void shutdown() throws JBIException {
        checkComponentStartedOrStopped("shutDown");
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(component.getComponentClassLoader());
            component.getComponent().getServiceUnitManager().shutDown(getName());
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    public void undeploy() throws JBIException {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(component.getComponentClassLoader());
            component.getComponent().getServiceUnitManager().undeploy(getName(), getRootDir() != null ? getRootDir().getAbsolutePath() : null);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
        component.removeServiceUnit(this);
    }

    protected void checkComponentStarted(String task) throws DeploymentException {
        if (!ComponentLifeCycleMBean.STARTED.equals(component.getCurrentState())) {
            throw new DeploymentException("Component " + component.getName() + " is not started!");
        }
    }

    protected void checkComponentStartedOrStopped(String task) throws DeploymentException {
        if (!ComponentLifeCycleMBean.STARTED.equals(component.getCurrentState())
                && !ComponentLifeCycleMBean.STOPPED.equals(component.getCurrentState())) {
            throw new DeploymentException("Component " + component.getName() + " is shut down!");
        }
    }

}
