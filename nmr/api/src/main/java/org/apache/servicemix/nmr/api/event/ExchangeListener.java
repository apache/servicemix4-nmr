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
package org.apache.servicemix.nmr.api.event;

import org.apache.servicemix.nmr.api.Exchange;


/**
 * Listener interface for exchanges send troughout the bus.
 * Such a listener will be called each time an exchange is sent
 * or delivered to an endpoint.
 *
 */
public interface ExchangeListener extends Listener {

    /**
     * Method called each time an exchange is sent
     *
     * @param exchange the exchange sent
     */
    void exchangeSent(Exchange exchange);

    /**
     * Method called each time an exchange is delivered
     *
     * @param exchange the delivered exchange
     */
    void exchangeDelivered(Exchange exchange);
    
}
