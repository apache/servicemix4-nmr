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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

import org.w3c.dom.Document;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.core.util.MapToDictionary;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * A Reference using an LDAP filter for matching endpoints
 */
public class FilterMatchingReference implements CacheableReference, Serializable {

    private final String filter;
    private transient volatile Filter osgiFilter;
    private transient volatile List<InternalEndpoint> matches;
    private transient EndpointRegistry registry;

    public FilterMatchingReference(String filter) throws InvalidSyntaxException {
        this.filter = filter;
        this.osgiFilter = org.osgi.framework.FrameworkUtil.createFilter(filter);
    }

    public Iterable<InternalEndpoint> choose(EndpointRegistry registry) {
        List<InternalEndpoint> result = matches;
        if (result == null || this.registry != registry) {
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
        if (osgiFilter == null) {
            synchronized (this) {
                if (osgiFilter == null) {
                    try {
                        this.osgiFilter = org.osgi.framework.FrameworkUtil.createFilter(filter);
                    } catch (InvalidSyntaxException e) {
                        // should not happen as this has been checked in the constructor
                    }
                }
            }
        }
        return osgiFilter.match(new MapToDictionary(epProps));
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
        return "FilterMatchingReference[" + filter + "]";
    }
}
