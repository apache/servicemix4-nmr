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

import org.apache.servicemix.jbi.deployer.descriptor.ClassPath;

/**
 * @version $Revision: 426415 $
 */
public class Component {
    private String type;
    private String componentClassLoaderDelegation = "parent-first";
    private String bootstrapClassLoaderDelegation = "parent-first";
    private Identification identification;
    private String componentClassName;
    private String description;
    private ClassPath componentClassPath;
    private String bootstrapClassName;
    private ClassPath bootstrapClassPath;
    private SharedLibraryList[] sharedLibraries;
    private InstallationDescriptorExtension descriptorExtension;

    public boolean isServiceEngine() {
        return type != null && type.equals("service-engine");
    }

    public boolean isBindingComponent() {
        return type != null && type.equals("binding-component");
    }

    public boolean isComponentClassLoaderDelegationParentFirst() {
        return isParentFirst(componentClassLoaderDelegation);
    }

    public boolean isComponentClassLoaderDelegationSelfFirst() {
        return isSelfFirst(componentClassLoaderDelegation);
    }

    public boolean isBootstrapClassLoaderDelegationParentFirst() {
        return isParentFirst(bootstrapClassLoaderDelegation);
    }

    public boolean isBootstrapClassLoaderDelegationSelfFirst() {
        return isSelfFirst(bootstrapClassLoaderDelegation);
    }


    // Properties
    //-------------------------------------------------------------------------
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComponentClassLoaderDelegation() {
        return componentClassLoaderDelegation;
    }

    public void setComponentClassLoaderDelegation(String componentClassLoaderDelegation) {
        this.componentClassLoaderDelegation = componentClassLoaderDelegation;
    }

    public String getBootstrapClassLoaderDelegation() {
        return bootstrapClassLoaderDelegation;
    }

    public void setBootstrapClassLoaderDelegation(String bootstrapClassLoaderDelegation) {
        this.bootstrapClassLoaderDelegation = bootstrapClassLoaderDelegation;
    }

    public Identification getIdentification() {
        return identification;
    }

    public void setIdentification(Identification identification) {
        this.identification = identification;
    }

    public String getComponentClassName() {
        return componentClassName;
    }

    public void setComponentClassName(String componentClassName) {
        this.componentClassName = componentClassName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ClassPath getComponentClassPath() {
        return componentClassPath;
    }

    public void setComponentClassPath(ClassPath componentClassPath) {
        this.componentClassPath = componentClassPath;
    }

    public String getBootstrapClassName() {
        return bootstrapClassName;
    }

    public void setBootstrapClassName(String bootstrapClassName) {
        this.bootstrapClassName = bootstrapClassName;
    }

    public ClassPath getBootstrapClassPath() {
        return bootstrapClassPath;
    }

    public void setBootstrapClassPath(ClassPath bootstrapClassPath) {
        this.bootstrapClassPath = bootstrapClassPath;
    }

    public SharedLibraryList[] getSharedLibraries() {
        return sharedLibraries;
    }

    public void setSharedLibraries(SharedLibraryList[] sharedLibraries) {
        this.sharedLibraries = sharedLibraries;
    }

    public InstallationDescriptorExtension getDescriptorExtension() {
        return descriptorExtension;
    }

    public void setDescriptorExtension(InstallationDescriptorExtension descriptorExtension) {
        this.descriptorExtension = descriptorExtension;
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected boolean isParentFirst(String text) {
        return text != null && text.equalsIgnoreCase("parent-first");
    }

    protected boolean isSelfFirst(String text) {
        return text != null && text.equalsIgnoreCase("self-first");
    }
    
    
}
