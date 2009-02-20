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
package org.apache.servicemix.jbi.deployer;

/**
 * This interface represents a JBI Shared Library and will be registered in
 * the OSGi registry
 */
public interface SharedLibrary {

    /**
     * Retrieves the name of this shared library
     *
     * @return the name
     */
    String getName();

    /**
     * Retrieves the description of this shared library
     *
     * @return the description
     */
    String getDescription();

    /**
     * Retrieve the JBI descriptor for this service assembly
     *
     * @return the JBI descriptor
     */
    String getDescriptor();

    /**
     * Retrieves the version of this shared library
     *
     * @return the version
     */
    String getVersion();

    /**
     * Create a classloader for this shared library
     *
     * @return a new classloader
     */
    ClassLoader getClassLoader();

    /**
     * Retrieve a list of components using this shared library
     *
     * @return
     */
    Component[] getComponents();

}
