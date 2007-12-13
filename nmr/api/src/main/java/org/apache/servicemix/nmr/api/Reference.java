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

import org.w3c.dom.Document;

/**
 * Represents an endpoint reference or a logical endpoint.
 * References are usually obtained from the {@link EndpointRegistry} and used
 * as targets for {@link Exchange}s using the {@link Exchange#setTarget(Reference)}
 * method.
 *
 * @version $Revision: $
 * @since 4.0
 */
public interface Reference {

    /**
     * Obtains an xml document describing this endpoint reference.
     * 
     * @return 
     */
    Document toXml();

}


