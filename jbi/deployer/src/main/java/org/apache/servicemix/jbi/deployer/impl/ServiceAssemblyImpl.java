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

import java.util.ArrayList;
import java.util.List;

import javax.jbi.JBIException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.ServiceUnit;
import org.apache.servicemix.jbi.deployer.descriptor.ServiceAssemblyDesc;
import org.osgi.framework.BundleContext;

/**
 * ServiceAssembly object
 */
public class ServiceAssemblyImpl implements ServiceAssembly {

	private static final Log Logger = LogFactory.getLog(ServiceAssemblyImpl.class);

    protected enum State {
        Unknown,
        Initialized,
        Started,
        Stopped,
        Shutdown,
    }

	private ServiceAssemblyDesc serviceAssemblyDesc;

    private List<ServiceUnitImpl> serviceUnits;

    private State state = State.Unknown;

    public ServiceAssemblyImpl(ServiceAssemblyDesc serviceAssemblyDesc, List<ServiceUnitImpl> serviceUnits) {
		this.serviceAssemblyDesc = serviceAssemblyDesc;
        this.serviceUnits = serviceUnits;
	}

	public String getName() {
		return serviceAssemblyDesc.getIdentification().getName();
	}

    public String getDescription() {
        return serviceAssemblyDesc.getIdentification().getDescription();
    }

    public ServiceUnit[] getServiceUnits() {
		return serviceUnits.toArray(new ServiceUnit[serviceUnits.size()]);
	}

	public void init() throws JBIException {
        transition(State.Initialized);
	}

	public void shutdown() throws JBIException {
        transition(State.Shutdown);
	}

	public void start() throws JBIException {
        transition(State.Started);
	}

	public void stop() throws JBIException {
        transition(State.Stopped);
	}
    
    protected void transition(State to) throws JBIException {
        // TODO: reject invalid transitions, for example Started -> Shutdown
        // we need to either automatically follow the intermediate steps, or just throw an exception
        State from = state;
        List<ServiceUnitImpl> success = new ArrayList<ServiceUnitImpl>();
        for (ServiceUnitImpl su : serviceUnits) {
            try {
                changeState(su, to);
                success.add(su);
            } catch (JBIException e) {
                if (from != State.Unknown) {
                    for (ServiceUnitImpl su2 : success) {
                        try {
                            changeState(su2, from);
                        } catch (JBIException e2) {
                            // Ignore
                        }
                    }
                }
                throw e;
            }
        }
        state = to;
    }

    protected void changeState(ServiceUnitImpl su, State state) throws JBIException {
        switch (state) {
            case Initialized:
                su.init();
                break;
            case Started:
                su.start();
                break;
            case Stopped:
                su.stop();
                break;
            case Shutdown:
                su.shutdown();
                break;
        }
    }

}
