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

import java.util.List;

import javax.jbi.management.LifeCycleMBean;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;

/**
 * JBI artifact lifecycle command
 */
public abstract class JbiLifeCycleCommandSupport extends JbiCommandSupport {

    @Option(name = "-c", aliases = "--component", description = "a component")
    boolean isComponent;

    @Option(name = "-a", aliases = "--service-assembly", description = "a service assembly")
    boolean isAssembly;

    @Argument(required = true, multiValued = true)
    List<String> artifacts;

    protected Object doExecute() throws Exception {
        if (isComponent && isAssembly) {
            throw new IllegalArgumentException("Can not specify options -c and -a at the same time!");
        }
        for (String artifact : artifacts) {
            try {
                if ((!isComponent && !isAssembly) || isComponent) {
                    Component component = getComponent(artifact);
                    if (component != null) {
                        handle(component);
                        continue;
                    }
                }
                if ((!isComponent && !isAssembly) || isAssembly) {
                    ServiceAssembly assembly = getServiceAssembly(artifact);
                    if (assembly != null) {
                        handle(assembly);
                        continue;
                    }
                }
                io.out.println("Artifact " + artifact + " not found");
            }
            catch (Exception e) {
                io.out.println("Error processing " + artifact + ": " + e);
            }
        }
        return null;
    }

    protected abstract void handle(LifeCycleMBean artifact) throws Exception;

}
