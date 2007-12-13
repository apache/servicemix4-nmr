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

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalChannel;

/**
 *
 */
public class InternalEndpointWrapper implements InternalEndpoint {

    private final Endpoint endpoint;
    private InternalChannel channel;

    public InternalEndpointWrapper(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public InternalChannel getChannel() {
        return channel;
    }

    /**
     * Set the channel so that the endpoint can send exchanges back
     * when they are processed or act as a consumer itself.
     * This method will be called by the NMR while the endpoint is registered.
     * Such a channel does not need to be closed as the NMR will close it
     * automatically when the endpoint is unregistered.
     *
     * @param channel the channel that this endpoint can use
     * @see org.apache.servicemix.nmr.api.EndpointRegistry#register(org.apache.servicemix.nmr.api.Endpoint,java.util.Map)
     */
    public void setChannel(Channel channel) {
        this.channel = (InternalChannel) channel;
        endpoint.setChannel(channel);
    }

    /**
     * Process the given exchange.  The processing can occur in the current thread
     * or asynchronously.
     * If an endpoint has sent an exchange asynchronously to another endpoint,
     * it will receive the exchange back using this method.  An endpoint can
     * recognized such exchanges by checking the role of the exchange.
     *
     * @param exchange the exchange to process
     */
    public void process(Exchange exchange) {
        endpoint.process(exchange);
    }

}
