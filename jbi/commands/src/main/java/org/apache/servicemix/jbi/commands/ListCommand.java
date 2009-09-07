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

import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.felix.gogo.commands.Command;

/**
 * List JBI artifacts
 */
@Command(scope = "jbi", name = "list", description = "List JBI endpoints")
public class ListCommand extends JbiCommandSupport {
    
    private static final int NAME_COL_LENGTH = 30; 

    protected Object doExecute() throws Exception {
        List<SharedLibrary> libraries = getSharedLibraries();
        if (libraries != null && !libraries.isEmpty()) {
            System.out.println("Shared Libraries");
            System.out.println("----------------");
            for (SharedLibrary library : libraries) {
                System.out.println(library.getName() + " - " + library.getVersion() + " - " + (library.getDescription() != null ? library.getDescription() : ""));
            }
            System.out.println();
        }

        List<Component> components = getComponents();
        if (components != null && !components.isEmpty()) {
            System.out.println("Components");
            System.out.println("----------");
            System.out.println("   State                  Name                  Description");
            for (Component component : components) {
                System.out.println("[" + getStateString(component.getCurrentState())+ "] ["
                        + getNameString(component.getName(), NAME_COL_LENGTH) + "]     "
                        + (component.getDescription() != null ? component.getDescription() : ""));
            }
            System.out.println();
        }

        List<ServiceAssembly> assemblies = getServiceAssemblies();
        if (assemblies != null && !assemblies.isEmpty()) {
            System.out.println("Service Assemblies");
            System.out.println("------------------");
            System.out.println("   State                  Name                  Description");
            for (ServiceAssembly assembly : assemblies) {
                System.out.println("[" + getStateString(assembly.getCurrentState())+ "] ["
                        + getNameString(assembly.getName(), NAME_COL_LENGTH) + "]     "
                        + (assembly.getDescription() != null ? assembly.getDescription() : ""));
            }
            System.out.println();
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    
    
    

    private String getNameString(String name, int colLength) {
        String ret = name;
        for (int i = 0; i < colLength - name.length(); i++) {
            ret = ret + " ";
        }
        return ret;
    }


    private String getStateString(String state) {
        if (state.equals(LifeCycleMBean.SHUTDOWN)) {
            return "Shutdown";
        } else if (state.equals(LifeCycleMBean.STARTED)) {
            return "Started ";
        } else if (state.equals(LifeCycleMBean.STOPPED)) {
            return "Stopped ";
        } else {
            return "Unknown ";
        }
    }
}
