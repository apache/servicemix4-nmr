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
package org.apache.servicemix.nmr.api.internal;

import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Role;

import java.util.concurrent.Semaphore;

/**
 * 
 *
 * @version $Revision: $
 * @since 4.0
 */
public interface InternalExchange extends Exchange {

    /**
     * Set the role of the exchange.
     * This method is for internal use and will be called by the Channel
     * when delivering the exchange.
     *
     * @param role the new role
     */
    public void setRole(Role role);

    /**
     * Retrieve the source endpoint. I.e. the one that created the exchange.
     * This information will be set by the NMR when the exchange is sent
     * using one of {@link org.apache.servicemix.nmr.api.Channel#send(Exchange)},
     * {@link org.apache.servicemix.nmr.api.Channel#sendSync(Exchange)} or
     * {@link org.apache.servicemix.nmr.api.Channel#sendSync(Exchange, long)}
     *
     * @return the endpoint that sent the exchange
     */
    InternalEndpoint getSource();

    /**
     * Set the source endpoint.  This method should be called by the
     * {@link org.apache.servicemix.nmr.api.Channel}
     *
     * @param source the source endpoint
     */
    void setSource(InternalEndpoint source);

    /**
     * Retrieve the destination endpoint, i.e. the one that receive the exchange.
     * This information will be set by the {@link InternalChannel#deliver(InternalExchange)}
     * method, just before calling the listeners and actually delegating to the Endpoint for processing.
     *
     * @return the destination endpoint
     */
    InternalEndpoint getDestination();

    /**
     * Set the destination endpoint. This method should be called by the
     * {@link InternalChannel#deliver(InternalExchange)}
     *
     * @param destination the destination endpoint
     */
    void setDestination(InternalEndpoint destination);

    Semaphore getConsumerLock(boolean create);

    Semaphore getProviderLock(boolean create);
}
