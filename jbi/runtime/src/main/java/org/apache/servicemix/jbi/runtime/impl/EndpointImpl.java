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
package org.apache.servicemix.jbi.runtime.impl;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.servicemix.nmr.api.Channel;
import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.ServiceMixException;
import org.apache.servicemix.nmr.core.ChannelImpl;

/**
 */
public class EndpointImpl extends ServiceEndpointImpl implements Endpoint {

    private Channel channel;
    private BlockingQueue<Exchange> queue;

    public EndpointImpl(Map<String, ?> properties) {
        super(properties);
    }

    public void process(Exchange exchange) {
        if (exchange.getProperty(ServiceEndpoint.class) == null) {
            exchange.setProperty(ServiceEndpoint.class, this);
        }
        try {
            queue.offer(exchange, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new ServiceMixException(e);
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        // We know process exchange is very fast, because those endpoints
        // will simply add the exchange to the queue, so speed things up
        // by allowing the delivery channel to deliver and process
        // exchanges synchronously
        if (channel instanceof ChannelImpl) {
            ((ChannelImpl) channel).setShouldRunSynchronously(true);
        }
    }

    public BlockingQueue<Exchange> getQueue() {
        return queue;
    }

    public void setQueue(BlockingQueue<Exchange> queue) {
        this.queue = queue;
    }

    public boolean equals(Object o) {
        return this == o;
    }

    public int hashCode() {
        return super.hashCode();
    }
}
