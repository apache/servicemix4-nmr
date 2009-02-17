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

/**
 * The <code>&lt;jbi:service-assembly&gt;</code> element.
 */
public class ServiceAssemblyDesc {

    private Connections connections = new Connections();

    private Identification identification;

    private ServiceUnitDesc[] serviceUnits;

    private String state = "";

    public Connections getConnections() {
        return connections;
    }

    public Identification getIdentification() {
        return identification;
    }

    public ServiceUnitDesc[] getServiceUnits() {
        return serviceUnits;
    }

    /**
     * @return Returns the state.
     */
    public String getState() {
        return state;
    }

    public void setConnections(Connections connections) {
        this.connections = connections;
    }

    public void setIdentification(Identification identification) {
        this.identification = identification;
    }

    public void setServiceUnits(ServiceUnitDesc[] serviceUnits) {
        this.serviceUnits = serviceUnits;
    }

    /**
     * @param state The state to set.
     */
    public void setState(String state) {
        this.state = state;
	}
}
