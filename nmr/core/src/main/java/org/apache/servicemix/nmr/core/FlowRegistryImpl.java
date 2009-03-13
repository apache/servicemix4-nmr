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

import java.util.Collection;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.Role;
import org.apache.servicemix.nmr.api.ServiceMixException;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.internal.Flow;
import org.apache.servicemix.nmr.api.internal.FlowRegistry;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalExchange;
import org.apache.servicemix.nmr.api.internal.InternalReference;
import org.apache.servicemix.nmr.api.security.AuthorizationService;
import org.apache.servicemix.nmr.api.security.GroupPrincipal;

/**
 * The default implementation of {@link FlowRegistry}.
 *
 * @version $Revision: $
 * @since 4.0
 */
public class FlowRegistryImpl extends ServiceRegistryImpl<Flow> implements FlowRegistry {

    private EndpointRegistry registry;
    private AuthorizationService authorizationService;

    public EndpointRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(EndpointRegistry registry) {
        this.registry = registry;
    }

    public AuthorizationService getAuthorizationService() {
        return authorizationService;
    }

    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public boolean canDispatch(InternalExchange exchange, InternalEndpoint endpoint) {
        for (Flow flow : getServices()) {
            if (flow.canDispatch(exchange, endpoint)) {
                return true;
            }
        }
        return false;
    }

    public void setNonOsgiFlows(Collection<Flow> flows) {
        for (Flow f : flows) {
            register(f, null);
        }
    }

    public void dispatch(InternalExchange exchange) {
        if (exchange.getRole() == Role.Consumer) {
            if (exchange.getDestination() == null) {
                InternalReference target = (InternalReference) exchange.getTarget();
                // TODO: possible NPE on target should be avoided
                assert target != null;
                boolean match = false;
                boolean securityMatch = false;
                for (InternalEndpoint endpoint : target.choose(registry)) {
                    if (Boolean.valueOf((String) endpoint.getMetaData().get(Endpoint.UNTARGETABLE))) {
                        continue;
                    }
                    match = true;
                    if (authorizationService != null) {
                        Set<GroupPrincipal> acls = authorizationService.getAcls(endpoint.getId(), exchange.getOperation());
                        if (!acls.contains(GroupPrincipal.ANY)) {
                            Subject subject = exchange.getIn().getSecuritySubject();
                            if (subject == null) {
                                continue;
                            }
                            acls.retainAll(subject.getPrincipals());
                            if (acls.size() == 0) {
                                continue;
                            }
                        }
                    }
                    securityMatch = true;
                    if (internalDispatch(exchange, endpoint, true)) {
                        return;
                    }
                }
                if (!match) {
                    throw new ServiceMixException("Could not dispatch exchange. No matching endpoints.");
                } else if (!securityMatch) {
                    throw new ServiceMixException("User not authenticated or not authorized to access any matching endpoint.");
                } else {
                    throw new ServiceMixException("Could not dispatch exchange. No flow can handle it.");
                }
            } else {
                if (!internalDispatch(exchange, exchange.getDestination(), false)) {
                    throw new ServiceMixException("Could not dispatch exchange. No flow can handle it.");
                }
            }
        } else {
            if (!internalDispatch(exchange, exchange.getSource(), false)) {
                throw new ServiceMixException("Could not dispatch exchange. No flow can handle it.");
            }
        }
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
