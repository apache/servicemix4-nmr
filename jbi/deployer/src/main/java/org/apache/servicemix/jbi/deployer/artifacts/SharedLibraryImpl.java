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
package org.apache.servicemix.jbi.deployer.artifacts;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.deployer.descriptor.DescriptorFactory;
import org.apache.servicemix.jbi.deployer.descriptor.SharedLibraryDesc;
import org.apache.servicemix.jbi.deployer.impl.AdminService;
import org.apache.servicemix.nmr.management.Nameable;
import org.osgi.framework.Bundle;

/**
 * SharedLibrary object
 */
public class SharedLibraryImpl implements SharedLibrary, Nameable {

    protected final Log LOGGER = LogFactory.getLog(getClass());

    private SharedLibraryDesc library;
    private Bundle bundle;
    private ClassLoader classLoader;
    private List<Component> components;

    public SharedLibraryImpl(Bundle bundle, SharedLibraryDesc library, ClassLoader classLoader) {
        this.bundle = bundle;
        this.library = library;
        this.classLoader = classLoader;
        this.components = new ArrayList<Component>();
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getName() {
        return library.getIdentification().getName();
    }

    public String getDescription() {
        return library.getIdentification().getDescription();
    }

    public String getDescriptor() {
        URL url = bundle.getResource(DescriptorFactory.DESCRIPTOR_FILE);
        return DescriptorFactory.getDescriptorAsText(url);
    }

    public String getVersion() {
        return library.getVersion();
    }

    public ClassLoader getClassLoader() {
        if (classLoader == null) {
        }
        return classLoader;
    }

    public Component[] getComponents() {
        return components.toArray(new Component[components.size()]);
    }

    public void addComponent(Component component) {
        components.add(component);
    }

    public void removeComponent(Component component) {
        components.remove(component);
    }

    public String getParent() {
        return null;
    }
    
    public String getMainType() {
        return "SharedLibrary";
    }

    public String getSubType() {
        return null;
    }

    public Class getPrimaryInterface() {
        return SharedLibrary.class;
    }
}
