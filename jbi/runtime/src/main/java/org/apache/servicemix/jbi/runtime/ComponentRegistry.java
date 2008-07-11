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
package org.apache.servicemix.jbi.runtime;

import org.apache.servicemix.nmr.api.service.ServiceRegistry;

import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;

/**
 * Registry of JBI components.
 *
 * This registry will usually be populated by an OSGi service listener
 * configured via spring-osgi.
 */
public interface ComponentRegistry extends ServiceRegistry<ComponentWrapper>  {

    public static final String NAME = "NAME";
    public static final String TYPE = "TYPE";

    /**
     * Retrieve a component given its name
     * @param name the name of the component
     * @return the component, or null if not registered
     */
    ComponentWrapper getComponent(String name);

    /**
     * Create a JBI ComponentContext that can be used to send messages to the JBI bus.
     * The ComponentContext has limited capabilities and can not be used to activate endpoints.
     * @return
     */
    ComponentContext createComponentContext();

}