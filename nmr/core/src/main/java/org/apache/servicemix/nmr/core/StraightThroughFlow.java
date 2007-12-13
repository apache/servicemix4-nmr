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

import org.apache.servicemix.nmr.api.internal.Flow;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalExchange;
import org.apache.servicemix.nmr.api.Role;


/**
 * The StraightThrough flow is the simpliest possible flow.
 * It will just put the exchange to its destination endpoint's
 * channel by calling:
 * <code><pre>
 *     exchange.getDestination().getChannel().deliver(exchange);
 * </pre></code>
 *
 * @version $Revision: $
 * @since 4.0
 */
public class StraightThroughFlow implements Flow {
    /**
     * Check if this flow can be used to dispatch the given Exchange
     *
     * @param exchange the exchange to check
     * @return <code>true</code> if the flow can be used, <code>false</code> otherwise
     */
    public boolean canDispatch(InternalExchange exchange, InternalEndpoint endpoint) {
        return true;
    }

    /**
     * Dispatch the Exchange using this flow.
     *
     * @param exchange the exchange to dispatch
     */
    public void dispatch(InternalExchange exchange) {
        InternalEndpoint endpoint = exchange.getRole() == Role.Consumer ? exchange.getDestination()
                                                                        : exchange.getSource();
        endpoint.getChannel().deliver(exchange);
    }
}
