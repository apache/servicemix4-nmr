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

import org.apache.servicemix.nmr.api.Reference;

/**
 * @version $Revision: $
 * @since 4.0
 */
public interface InternalReference extends Reference {

    /**
     * Creates an iterator to be used for choosing the physical target for an exchange.
     *
     * When dispatching an exchange, the NMR will select the first endpoint and choose a
     * flow that can handle it. If no flow can be used, try the next endpoint until
     * no more endpoints can be used.
     *
     * @return an iterator on matching endpoints
     */
    Iterable<InternalEndpoint> choose();

}
