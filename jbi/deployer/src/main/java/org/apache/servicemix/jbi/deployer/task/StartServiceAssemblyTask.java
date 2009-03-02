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
import org.apache.tools.ant.BuildException;

/**
 * Start a Service Assembly
 *
 * @version $Revision$
 */
public class StartServiceAssemblyTask extends JbiTask {

    private String name; //assembly name to get descriptor for

    /**
     * @return Returns the assembly name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The assembly name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * execute the task
     *
     * @throws BuildException
     */
    public void doExecute(AdminCommandsService acs) throws Exception {
        if (name == null) {
            throw new BuildException("null service assembly name");
        }
        acs.startServiceAssembly(name);
    }

}