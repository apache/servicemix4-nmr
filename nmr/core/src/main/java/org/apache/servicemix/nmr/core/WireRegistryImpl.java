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

import java.util.Map;

import org.apache.servicemix.nmr.api.Wire;
import org.apache.servicemix.nmr.api.WireRegistry;
import org.apache.servicemix.nmr.api.service.ServiceHelper;

/**
 * Default implementation for a {@link WireRegistry}
 */
public class WireRegistryImpl extends ServiceRegistryImpl<Wire> implements WireRegistry {

    public Wire getWire(Map<String, ?> properties) {
        for (Wire wire : getServices()) {
            //TODO: we are using matches here instead of equals, so we should find a way to deal with multiple wires
            if (ServiceHelper.matches(properties, getProperties(wire))) {
                return wire;
            }
        }
        return null;
    }

    public void register(Wire wire) {
        register(wire, wire.getFrom());
    }

    public void unregister(Wire wire) {
        unregister(wire, wire.getFrom());
    }
}
