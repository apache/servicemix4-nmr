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

import java.util.UUID;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.Executors;

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.servicemix.nmr.api.internal.InternalChannel;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.executors.Executor;

/**
 * A {@link Channel} to be used as a client.
 * Only sendSync should be used, else an exception will occur
 */
public class ClientChannel extends ChannelImpl {

    public ClientChannel(NMR nmr, Executor executor) {
        super(new ClientEndpoint(), executor, nmr);
        getEndpoint().setChannel(this);
    }

    protected static class ClientEndpoint implements InternalEndpoint {

        private InternalChannel channel;
        private String id = UUID.randomUUID().toString(); 

        public String getId() {
            return id;
        }

        public Map<String, ?> getMetaData() {
            return Collections.emptyMap();
        }

        public Endpoint getEndpoint() {
            return this;
        }

        public void setChannel(Channel channel) {
            this.channel = (InternalChannel) channel;
        }

        public InternalChannel getChannel() {
            return channel;
        }

        public void process(Exchange exchange) {
            throw new IllegalStateException();
        }

    }
}
