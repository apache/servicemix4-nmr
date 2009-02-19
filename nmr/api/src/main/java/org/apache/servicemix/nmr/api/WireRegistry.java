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
package org.apache.servicemix.nmr.api;

import java.util.Map;

import org.apache.servicemix.nmr.api.service.ServiceRegistry;

/**
 * This registry is used to register/unregister {@link Wire}s.
 */
public interface WireRegistry extends ServiceRegistry<Wire> {

    /**
     * Get the wire that matches the given properties
     * 
     * @param properties the wire's properties
     * @return the wire or <code>null</code> if there's no wire to match this property set
     */
    public Wire getWire(Map<String, ?> properties);

    /**
     * Convenience method for registering a wire without having to specify the wire's properties. The wire will be registered using
     * the from properties map.
     * 
     * @param wire the wire to be registered
     */
    public void register(Wire wire);

    /**
     * Convenience method for unregistering a wire without having to specify the wire's properties. The wire will be unregistered
     * using the from properties map.
     * 
     * @param wire the wire to be unregistered
     */
    public void unregister(Wire wire);
}
