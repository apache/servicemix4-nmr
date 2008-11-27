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

import java.io.File;

import javax.jbi.JBIException;
import javax.jbi.management.ComponentLifeCycleMBean;
import javax.jbi.management.DeploymentException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceUnitDesc;


public class ServiceUnitImpl implements ServiceUnit {
	
	private static final Log Logger = LogFactory.getLog(ServiceUnitImpl.class);
	
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

    public File getRootDir() {
		return rootDir;
	}

    public void deploy() throws JBIException {
        component.getComponent().getServiceUnitManager().deploy(getName(), getRootDir() != null ? getRootDir().getAbsolutePath() : null);
        component.addServiceUnit(this);
    }

	public void init() throws JBIException {
        component.getComponent().getServiceUnitManager().init(getName(), getRootDir() != null ? getRootDir().getAbsolutePath() : null);
	}
		
	public void start() throws JBIException {
        checkComponentStarted("start");
        component.getComponent().getServiceUnitManager().start(getName());
    }

	public void stop() throws JBIException {
        checkComponentStarted("stop");
        component.getComponent().getServiceUnitManager().stop(getName());
    }

    public void shutdown() throws JBIException {
        checkComponentStartedOrStopped("shutDown");
        component.getComponent().getServiceUnitManager().shutDown(getName());
    }

    public void undeploy() throws JBIException {
        component.getComponent().getServiceUnitManager().undeploy(getName(), getRootDir() != null ? getRootDir().getAbsolutePath() : null);
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
