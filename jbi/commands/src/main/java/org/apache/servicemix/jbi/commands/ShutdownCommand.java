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
package org.apache.servicemix.jbi.commands;

import javax.jbi.JBIException;
import javax.jbi.management.LifeCycleMBean;

import org.apache.geronimo.gshell.clp.Option;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.Component;

/**
 * Shutdown a JBI artifact
 */
public class ShutdownCommand extends JbiLifeCycleCommandSupport {

    @Option(name = "--force")
    private boolean force;

    protected void handle(LifeCycleMBean artifact) throws JBIException {
        if (force) {
            if (artifact instanceof ServiceAssembly) {
                ((ServiceAssembly) artifact).forceShutDown();
            } else if (artifact instanceof Component) {
                ((Component) artifact).forceShutDown();
            }
        } else {
            artifact.shutDown();
        }
    }
}