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

import org.apache.servicemix.nmr.api.internal.FlowRegistry;
import org.apache.servicemix.nmr.api.event.ListenerRegistry;

/**
 * The NMR interface is the primary interface to communicate with the Bus.
 * It contains methods to access the registries and to create a client channel
 * to communicate with the bus.
 *
 * @version $Revision: $
 * @since 4.0
 */
public interface NMR {

    /**
     * Access the endpoint registry.
     *
     * @return the endpoint registry
     */
    EndpointRegistry getEndpointRegistry();

    /**
     * Access the listener registry.
     *
     * @return the listener registry
     */
    ListenerRegistry getListenerRegistry();

    /**
     * Access the flow registry.
     *
     * @return the flow registry
     */
    FlowRegistry getFlowRegistry();

    /**
     * Create a channel to interact with the NMR without exposing an endpoint.
     * @return a channel
     */
    Channel createChannel();

}
