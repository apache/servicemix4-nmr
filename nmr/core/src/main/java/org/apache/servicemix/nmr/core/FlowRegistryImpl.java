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

import org.apache.servicemix.nmr.api.Role;
import org.apache.servicemix.nmr.api.ServiceMixException;
import org.apache.servicemix.nmr.api.internal.Flow;
import org.apache.servicemix.nmr.api.internal.FlowRegistry;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalExchange;
import org.apache.servicemix.nmr.api.internal.InternalReference;

/**
 * The default implementation of {@link FlowRegistry}.
 *
 * @version $Revision: $
 * @since 4.0
 */
public class FlowRegistryImpl extends ServiceRegistryImpl<Flow> implements FlowRegistry {

    public boolean canDispatch(InternalExchange exchange, InternalEndpoint endpoint) {
        for (Flow flow : getServices()) {
            if (flow.canDispatch(exchange, endpoint)) {
                return true;
            }
        }
        return false;
    }

    public void dispatch(InternalExchange exchange) {
        if (exchange.getRole() == Role.Consumer) {
            if (exchange.getDestination() == null) {
                InternalReference target = (InternalReference) exchange.getTarget();
                assert target != null;
                boolean match = false;
                for (InternalEndpoint endpoint : target.choose()) {
                    match = true;
                    if (internalDispatch(exchange, endpoint, true)) {
                        return;
                    }
                }
                if (!match) {
                    throw new ServiceMixException("Could not dispatch exchange. No matching endpoints.");
                } else {
                    throw new ServiceMixException("Could not dispatch exchange. No flow can handle it.");
                }
            } else {
                if (!internalDispatch(exchange, exchange.getDestination())) {
                    throw new ServiceMixException("Could not dispatch exchange. No flow can handle it.");
                }
            }
        } else {
            if (!internalDispatch(exchange, exchange.getSource())) {
                throw new ServiceMixException("Could not dispatch exchange. No flow can handle it.");
            }
        }
    }

    protected boolean internalDispatch(InternalExchange exchange, InternalEndpoint endpoint) {
        return internalDispatch(exchange, endpoint, false);
    }

    protected boolean internalDispatch(InternalExchange exchange, InternalEndpoint endpoint, boolean setDestination) {
        for (Flow flow : getServices()) {
            if (flow.canDispatch(exchange, endpoint)) {
                if (setDestination) {
                    exchange.setDestination(endpoint);
                }
                flow.dispatch(exchange);
                return true;
            }
        }
        return false;
    }
}
