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
package org.apache.servicemix.jbi.runtime.impl;

import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.ServiceMixException;
import org.apache.servicemix.nmr.core.ServiceRegistryImpl;
import org.apache.servicemix.jbi.runtime.ComponentRegistry;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 4, 2007
 * Time: 10:30:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ComponentRegistryImpl extends ServiceRegistryImpl<Component>  implements ComponentRegistry {

    private NMR nmr;

    /**
     * Register a service with the given metadata.
     *
     * @param component the component to register
     * @param properties the associated metadata
     */
    public void register(Component component, Map<String, ?> properties) {
        try {
            ComponentContext context = new ComponentContextImpl(nmr, component, properties);
            component.getLifeCycle().init(context);
            component.getLifeCycle().start();
        } catch (JBIException e) {
            throw new ServiceMixException(e);
        }
    }

    /**
     * Unregister a previously registered component.
     *
     * @param component the component to unregister
     */
    public void unregister(Component component, Map<String, ?> properties) {
        try {
            component.getLifeCycle().stop();
            component.getLifeCycle().shutDown();
        } catch (JBIException e) {
            throw new ServiceMixException(e);
        }
    }

    public NMR getNmr() {
        return nmr;
    }

    public void setNmr(NMR nmr) {
        this.nmr = nmr;
    }
}
