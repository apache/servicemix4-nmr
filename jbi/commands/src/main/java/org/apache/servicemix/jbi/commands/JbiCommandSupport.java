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

import java.util.ArrayList;
import java.util.List;

import org.apache.geronimo.gshell.support.OsgiCommandSupport;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.impl.Deployer;
import org.osgi.framework.ServiceReference;

/**
 * Base class for JBI related commands
 */
public abstract class JbiCommandSupport extends OsgiCommandSupport {

    protected List<ServiceReference> usedReferences;

    protected List<SharedLibrary> getSharedLibraries() throws Exception {
        return getAllServices(SharedLibrary.class, null);
    }

    protected List<Component> getComponents() throws Exception {
        return getAllServices(Component.class, null);
    }

    protected List<ServiceAssembly> getServiceAssemblies() throws Exception {
        return getAllServices(ServiceAssembly.class, null);
    }

    protected Component getComponent(String name) throws Exception {
        List<Component> components = getAllServices(Component.class, "(" + Deployer.NAME + "=" + name + ")");
        if (components != null && components.size() == 1) {
            return components.get(0);
        }
        return null;
    }

    protected ServiceAssembly getServiceAssembly(String name) throws Exception {
        List<ServiceAssembly> assemblies = getAllServices(ServiceAssembly.class, "(" + Deployer.NAME + "=" + name + ")");
        if (assemblies != null && assemblies.size() == 1) {
            return assemblies.get(0);
        }
        return null;
    }

    public Object doExecute(final Object... args) throws Exception {
        try {
            return super.doExecute(args);
        } finally {
            ungetServices();
        }
    }

    protected <T> List<T> getAllServices(Class<T> clazz, String filter) throws Exception {
        ServiceReference[] references = getBundleContext().getAllServiceReferences(clazz.getName(), filter);
        if (references == null) {
            return null;
        }
        List<T> services = new ArrayList<T>();
        for (ServiceReference ref : references) {
            T t = getService(clazz, ref);
            services.add(t);
        }
        return services;
    }

    protected <T> T getService(Class<T> clazz, ServiceReference reference) {
        T t = (T) getBundleContext().getService(reference);
        if (t != null) {
            if (usedReferences == null) {
                usedReferences = new ArrayList<ServiceReference>();
            }
            usedReferences.add(reference);
        }
        return t;
    }

    protected void ungetServices() {
        if (usedReferences != null) {
            for (ServiceReference ref : usedReferences) {
                getBundleContext().ungetService(ref);
            }
        }
    }

}
