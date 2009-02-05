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

import javax.jbi.management.DeploymentServiceMBean;

public class DeploymentService implements DeploymentServiceMBean {

    public String deploy(String saZipURL) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String undeploy(String saName) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String[] getDeployedServiceUnitList(String componentName) throws Exception {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String[] getDeployedServiceAssemblies() throws Exception {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getServiceAssemblyDescriptor(String saName) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String[] getDeployedServiceAssembliesForComponent(String componentName) throws Exception {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String[] getComponentsForDeployedServiceAssembly(String saName) throws Exception {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isDeployedServiceUnit(String componentName, String suName) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean canDeployToComponent(String componentName) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String start(String serviceAssemblyName) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String stop(String serviceAssemblyName) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String shutDown(String serviceAssemblyName) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getState(String serviceAssemblyName) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
