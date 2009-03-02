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
package org.apache.servicemix.jbi.deployer.task;

import org.apache.servicemix.jbi.deployer.AdminCommandsService;
import org.apache.tools.ant.Project;

/**
 * List deployed Service Assemblies
 *
 * @version $Revision$
 */
public class ListServiceAssembliesTask extends JbiTask {

    private String state;

    private String componentName;

    private String serviceAssemblyName;

    private String xmlOutput;

    /**
     * @return the xmlOutput
     */
    public String isXmlOutput() {
        return xmlOutput;
    }

    /**
     * @param xmlOutput the xmlOutput to set
     */
    public void setXmlOutput(String xmlOutput) {
        this.xmlOutput = xmlOutput;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state Sets the state
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return the component name
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * @param componentName Sets the component name
     */
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    /**
     * @return service assembly name
     */
    public String getServiceAssemblyName() {
        return serviceAssemblyName;
    }

    /**
     * @param serviceAssemblynname Sets the service assembly name
     */
    public void setServiceAssemblyName(String serviceAssemblynname) {
        this.serviceAssemblyName = serviceAssemblynname;
    }

    /**
     * execute the task
     *
     * @throws BuildException
     */
    public void doExecute(AdminCommandsService acs) throws Exception {
        String result = acs.listServiceAssemblies(getState(), getComponentName(), getServiceAssemblyName());
        if (xmlOutput != null) {
            getProject().setProperty(xmlOutput, result);
        }
        log(result, Project.MSG_WARN);
    }

}