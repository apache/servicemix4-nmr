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
package org.apache.servicemix.nmr.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;

/**
 * A Reference using an map of properties for matching endpoints
 */
public class PropertyMatchingReference implements CacheableReference, Serializable {

    private final Map<String, ?> properties;
    private transient volatile List<InternalEndpoint> matches;
    private transient EndpointRegistry registry;

    public PropertyMatchingReference(Map<String, ?> properties) {
        this.properties = properties;
    }

    public Iterable<InternalEndpoint> choose(EndpointRegistry registry) {
        List<InternalEndpoint> result = matches;
        if (this.matches == null || this.registry != registry) {
            result = new ArrayList<InternalEndpoint>();
            for (Endpoint ep : registry.query(null)) {
                InternalEndpoint iep = (InternalEndpoint) ep;
                if (Boolean.valueOf((String) iep.getMetaData().get(Endpoint.UNTARGETABLE))) {
                    continue;
                }
                if (match(registry, iep)) {
                    result.add(iep);
                }
            }
            this.registry = registry;
            this.matches = result;
        }
        return result;
    }

    protected boolean match(EndpointRegistry registry, InternalEndpoint endpoint) {
        Map<String, ?> epProps = registry.getProperties(endpoint);
        for (Map.Entry<String, ?> name : properties.entrySet()) {
            if (!name.getValue().equals(epProps.get(name.getKey()))) {
                return false;
            }
        }
        return true;
    }

    public Document toXml() {
        // TODO
        return null;
    }

    public void setDirty() {
        matches = null;
    }

    @Override
    public String toString() {
        return "PropertyMatchingReference[" + properties + "]";
    }
}
