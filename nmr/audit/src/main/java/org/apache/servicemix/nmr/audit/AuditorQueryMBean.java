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
package org.apache.servicemix.nmr.audit;

import org.apache.servicemix.nmr.api.Status;

/**
 * Main interface for ServiceMix auditor query.
 * This interface may be used to query upon exchanges.
 * 
 * @author George Gastaldi (gastaldi)
 * @since 1.0.0
 * @version $Revision: 550578 $
 */
public interface AuditorQueryMBean extends AuditorMBean {

    String[] findExchangesIdsByQuery(String query) throws AuditorException;

    String[] findExchangesIdsByStatus(Status status) throws AuditorException;

    String[] findExchangesIdsByProperty(String property, String value) throws AuditorException;

    String[] findExchangesIdsByMessageContent(String type, String content) throws AuditorException;

    String[] findExchangesIdsByMessageHeader(String type, String property, String value) throws AuditorException;

    /**
     * Searches for Exchanges IDs using the supplied key-field and the expected content of the field 
     * @param field
     * @param fieldValue
     * @return exchange ids
     * @throws AuditorException if an error occurs
     */
    String[] getExchangeIds(String field, String fieldValue) throws AuditorException;
}