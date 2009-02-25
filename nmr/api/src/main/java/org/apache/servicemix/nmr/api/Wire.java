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

import org.apache.servicemix.nmr.api.service.ServiceHelper;

/**
 * Represents a wire to an endpoint.  It provides a means of linking another set of endpoint properties to an existing endpoint.
 * 
 * A wire can be created using {@link ServiceHelper#createWire(Map, Map)} and needs to be registered in the
 * {@link WireRegistry} to take effect.  
 * 
 * A wire allows you to link one 
 */
public interface Wire {
    
    /**
     * Get the new address made available by the wire.
     *
     * @return the new address
     */
    public Map<String, ?> getFrom();
 
    /**
     * Get the target endpoint that is accessed when sending something to the from address
     * 
     * @return the target endpoint
     */
    public Map<String, ?> getTo();

}
