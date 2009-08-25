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
package org.apache.servicemix.nmr.management;


/**
 * This interface avoids type leakage from the JBI-specific layer into 
 * the generic NMR layer when the names for managable objects are being 
 * constructed by the latter. 
 */
public interface Nameable {

    /**
     * @return the name of the parent
     */
    String getParent();
    
    /**
     * @return the name of the entity
     */
    String getName();
    
    /**
     * @return the (non-Java) type of the entity 
     */
    String getType();
    
    /**
     * @return the sub-type of the entity
     */
    String getSubType();
    
    /**
     * @return the version of the entity
     */
    String getVersion();
    
    /**
     * @return the primary interface of the entity from a management 
     * point of view
     */
    Class getPrimaryInterface();
}
