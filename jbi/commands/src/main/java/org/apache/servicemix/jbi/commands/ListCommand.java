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

import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.SharedLibrary;

/**
 * List JBI artifacts
 */
public class ListCommand extends JbiCommandSupport {

    protected Object doExecute() throws Exception {
        List<SharedLibrary> libraries = getSharedLibraries();
        if (libraries != null && !libraries.isEmpty()) {
            io.out.println("Shared Libraries");
            io.out.println("----------------");
            for (SharedLibrary library : libraries) {
                io.out.println(library.getName() + " - " + library.getVersion() + " - " + library.getDescription());
            }
            io.out.println();
        }

        List<Component> components = getComponents();
        if (components != null && !components.isEmpty()) {
            io.out.println("Components");
            io.out.println("----------");
            for (Component component : components) {
                io.out.println(component.getName() + " - " + component.getCurrentState() + " - " + component.getDescription());
            }
            io.out.println();
        }

        List<ServiceAssembly> assemblies = getServiceAssemblies();
        if (assemblies != null && !assemblies.isEmpty()) {
            io.out.println("Service Assemblies");
            io.out.println("------------------");
            for (ServiceAssembly assembly : assemblies) {
                io.out.println(assembly.getName() + " - " + assembly.getCurrentState() + " - " + assembly.getDescription());
            }
            io.out.println();
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
