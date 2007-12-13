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

import org.apache.servicemix.nmr.api.Channel;

/**
 * InternalChannel is the private contract for channels.
 *
 * The {@link #deliver(InternalExchange)} method
 * is to be used by {@link Flow}s implementations so that they can
 * hand exchanges to the channels.
 *
 * @version $Revision: $
 * @since 4.0
 */
public interface InternalChannel extends Channel {

    /**
     * Deliver an exchange to the endpoint using this channel
     *
     * @param exchange the exchange to delivery
     */
    void deliver(InternalExchange exchange);
}
