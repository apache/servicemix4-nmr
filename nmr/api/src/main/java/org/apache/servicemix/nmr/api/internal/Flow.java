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

/**
 * @version $Revision: $
 * @since 4.0
 */
public interface Flow {

    /**
     * Meta-data key for the unique flow identifier
     */
    String ID = "ID";

    /**
     * Check if this flow can be used to dispatch the given Exchange
     *
     * @param exchange the exchange to check
     * @param endpoint the endpoint where the exchange is to be dispatched
     * @return <code>true</code> if the flow can be used, <code>false</code> otherwise
     */
    boolean canDispatch(InternalExchange exchange, InternalEndpoint endpoint);

    /**
     * Dispatch the Exchange using this flow.
     *
     * @param exchange the exchange to dispatch
     */
    void dispatch(InternalExchange exchange);
}
