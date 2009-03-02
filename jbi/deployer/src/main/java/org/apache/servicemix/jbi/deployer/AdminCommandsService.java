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

import java.util.Properties;

/**
 * Provides a simple interface to access ServiceMix administration commands.
 */
public interface AdminCommandsService {

    String installComponent(String file, Properties properties, boolean deferException) throws Exception;

    String uninstallComponent(String name) throws Exception;

    String installSharedLibrary(String file, boolean deferException) throws Exception;

    String uninstallSharedLibrary(String name) throws Exception;

    String startComponent(String name) throws Exception;

    String stopComponent(String name) throws Exception;

    String shutdownComponent(String name) throws Exception;

    String deployServiceAssembly(String file, boolean deferException) throws Exception;

    String undeployServiceAssembly(String name) throws Exception;

    String startServiceAssembly(String name) throws Exception;

    String stopServiceAssembly(String name) throws Exception;

    String shutdownServiceAssembly(String name) throws Exception;

    String listComponents(boolean excludeSEs,
                          boolean excludeBCs,
                          boolean excludePojos,
                          String requiredState,
                          String sharedLibraryName,
                          String serviceAssemblyName) throws Exception;

    String listSharedLibraries(String componentName,
                               String sharedLibraryName) throws Exception;

    String listServiceAssemblies(String state,
                                 String componentName,
                                 String serviceAssemblyName) throws Exception;
}
	                                                                                                                
