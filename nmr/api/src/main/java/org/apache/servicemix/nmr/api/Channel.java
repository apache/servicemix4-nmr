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

/**
 * Creates a channel to perform invocations through the NMR.
 * Channels are created by the {@link NMR}.  They are used
 * by {@link Endpoint}s to communicate with the NMR, but they
 * can also be used by external clients.  In such a case, the
 * Channel must be closed using the {@link #close()} method
 * after use.
 *
 * @see org.apache.servicemix.nmr.api.NMR#createChannel()
 * @version $Revision: $
 * @since 4.0
 */
public interface Channel {

    /**
     * Access to the bus
     *
     * @return the NMR
     */
    NMR getNMR();

    /**
     * Creates a new exchange.
     *
     * @param pattern specify the InOnly / InOut / RobustInOnly / RobustInOut
     * @return a new exchange of the given pattern
     */
    Exchange createExchange(Pattern pattern);

    /**
     * An asynchronous invocation of the service
     *
     * @param exchange the exchange to send
     */
    void send(Exchange exchange);

    /**
     * Synchronously send the exchange, blocking until the exchange is returned.
     *
     * @param exchange the exchange to send
     * @return <code>true</code> if the exchange has been processed succesfully
     */
    boolean sendSync(Exchange exchange);

    /**
     * Synchronously send the exchange
     * @param exchange the exchange to send
     * @param timeout time to wait in milliseconds
     * @return <code>true</code> if the exchange has been processed succesfully
     */
    boolean sendSync(Exchange exchange, long timeout);

    /**
     * Closes the channel, freeing up any resources (like sockets, threads etc).
     * Channel that are injected onto Endpoints will be closed automatically by
     * the NMR.
     */
    void close();

}

