/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.servicemix.nmr.core;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.core.util.Filter;

/**
 * A dynamic reference that holds a transient list of matching endpoints.
 * This list will be refreshed when #setDirty() has been called previsouly.
 */
public class DynamicReference implements CacheableReference {

    private final Filter<InternalEndpoint> filter;
    private transient volatile List<InternalEndpoint> matches;
    private transient EndpointRegistry registry;

    public DynamicReference(Filter<InternalEndpoint> filter) {
        this.filter = filter;
    }

    public Document toXml() {
        // TODO
        return null;
    }

    public synchronized Iterable<InternalEndpoint> choose(EndpointRegistry registry) {
        List<InternalEndpoint> result = matches;
        if (result == null || this.registry != registry) {
            result = new ArrayList<InternalEndpoint>();
            for (Endpoint ep : registry.query(null)) {
                InternalEndpoint iep = (InternalEndpoint) ep;
                if (Boolean.valueOf((String) iep.getMetaData().get(Endpoint.UNTARGETABLE))) {
                    continue;
                }
                if (filter.match(iep)) {
                    result.add(iep);
                }
            }
            this.registry = registry;
            this.matches = result;
        }
        return result;
    }

    public synchronized void setDirty() {
        this.matches = null;
    }

    public String toString() {
        return "DynamicReference[filter=" + filter + "]";
    }

}
