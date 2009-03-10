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

import org.apache.servicemix.nmr.api.Exchange;

/**
 * Main interface for ServiceMix auditor.
 * This interface may be used to view and delete exchanges
 * or to re-send an exchange on behalf of the component that
 * initiated the exchange. 
 * 
 * The implementation is free to offer additional features for
 * selecting message exchanges which are dependant of the underlying
 * store.
 * 
 * @author Guillaume Nodet (gnt)
 * @since 1.0.0
 * @version $Revision: 492738 $
 */
public interface AuditorMBean {

    /**
     * Get the number of exchanges stored by this auditor.
     * 
     * @return the number of exchanges stored 
     * @throws AuditorException if an error occurs accessing the data store.
     */
    int getExchangeCount() throws AuditorException;
    
    /**
     * Retrieve the exchange id of the exchange at the specified index.
     * Index must be a null or positive integer.
     * If index is greater than the number of exchanges stored,
     * a null string should be returned.
     * 
     * @param index the index of the exchange
     * @return the exchange id, or null of index is greater than the exchange count
     * @throws AuditorException if an error occurs accessing the data store.
     * @throws IllegalArgumentException if index is less than zero
     */
    String getExchangeIdByIndex(int index) throws AuditorException;
    
    /**
     * Retrieve all exchanges ids from the data store.
     * 
     * @return an array of exchange ids
     * @throws AuditorException if an error occurs accessing the data store.
     */
    String[] getAllExchangeIds() throws AuditorException;
    
    /**
     * Retrieve a range of message exchange ids.
     * The ids retrieved range from fromIndex (inclusive) to
     * toIndex (exclusive).
     * If fromIndex == toIndex, an empty array must be returned.
     * If fromIndex is less than zero, or if toIndex is less than
     * fromIndex, an exception will be thrown.
     * An array of exactly (toIndex - fromIndex) element should be
     * returned.
     * This array must be filled by null, for indexes that are greater
     * than the number of exchanges stored.
     * 
     * @param fromIndex the lower bound index of the ids to be retrieved.
     *                  fromIndex must be greater or equal to zero.
     * @param toIndex the upper bound (exclusive) of the ids to be retrieved.
     *                toIndex must be greater or equal to fromIndex
     * @return an array of exchange ids
     * @throws AuditorException if an error occurs accessing the data store.
     * @throws IllegalArgumentException if fromIndex is less than zero or if toIndex is 
     *                                  less than fromIndex.
     */
    String[] getExchangeIdsByRange(int fromIndex, int toIndex)  throws AuditorException;
    
    /**
     * Retrieve the exchange at the specified index.
     * Index must be a null or positive integer, and should be less than
     * the current exchange count stored. 
     * If index is greater than the number of exchanges stored,
     * a null exchange should be returned.
     * 
     * @param index the index of the exchange
     * @return the exchange, or null of index is greater than the exchange count
     * @throws AuditorException if an error occurs accessing the data store.
     * @throws IllegalArgumentException if index is less than zero
     */
    Exchange getExchangeByIndex(int index) throws AuditorException;
    
    /**
     * Retrieve the exchange for a specified id.
     * Id must be non null and non empty. 
     * If the exchange with the specified id is not found, null should be returned.
     * 
     * @param id the id of the exchange
     * @return the exchange with the specified id, or null if not found
     * @throws AuditorException if an error occurs accessing the data store.
     * @throws IllegalArgumentException if id is null or empty 
     */
    Exchange getExchangeById(String id) throws AuditorException;
    
    /**
     * Retrieve all exchanges =from the data store.
     * 
     * @return an array of exchange
     * @throws AuditorException if an error occurs accessing the data store.
     */
    Exchange[] getAllExchanges() throws AuditorException;
    
    /**
     * Retrieve a range of message exchange.
     * The exchanges retrieved range from fromIndex (inclusive) to
     * toIndex (exclusive).
     * If fromIndex == toIndex, an empty array must be returned.
     * If fromIndex is less than zero, or if toIndex is less than
     * fromIndex, an exception will be thrown.
     * An array of exactly (toIndex - fromIndex) element should be
     * returned.
     * This array must be filled by null, for indexes that are greater
     * than the number of exchanges stored.
     * 
     * @param fromIndex the lower bound index of the exchanges to be retrieved.
     *                  fromIndex must be greater or equal to zero.
     * @param toIndex the upper bound (exclusive) of the exchanges to be retrieved.
     *                toIndex must be greater or equal to fromIndex
     * @return an array of exchange
     * @throws AuditorException if an error occurs accessing the data store.
     * @throws IllegalArgumentException if fromIndex is less than zero or if toIndex is 
     *                                  less than fromIndex.
     */
    Exchange[] getExchangesByRange(int fromIndex, int toIndex) throws AuditorException;

    /**
     * Retrieve exchanges for the specified ids.
     * An array of exactly ids.length elements must be returned.
     * This array should be filled with null for exchanges that
     * have not been found in the store. 
     * 
     * @param ids the ids of exchanges to retrieve
     * @return an array of exchanges
     * @throws AuditorException if an error occurs accessing the data store.
     * @throws IllegalArgumentException if ids is null, or one of its
     *         element is null or empty.
     */
    Exchange[] getExchangesByIds(String[] ids) throws AuditorException;
    
    /**
     * Delete all exchanges =from the data store.
     * 
     * @return the number of exchanges deleted, or -1 if such information
     *         can not be provided
     * @throws AuditorException if an error occurs accessing the data store.
     */
    int deleteAllExchanges() throws AuditorException;
    
    /**
     * Delete a message, given its index.
     * Index must be a null or positive integer, and should be less than
     * the current exchange count stored. 
     * If index is greater than the number of exchanges stored,
     * false should be returned.
     * 
     * @param index the index of the exchange
     * @return true if the exchange has been successfully deleted,
     *         false if index is greater than the number of exchanges stored
     * @throws AuditorException if an error occurs accessing the data store.
     * @throws IllegalArgumentException if index is less than zero
     */
    boolean deleteExchangeByIndex(int index) throws AuditorException;
    
    /**
     * Delete the exchange with the specified id.
     * Id must be non null and non empty.
     * 
     * @param id the id of the exchange to delete
     * @return true if the exchange has been successfully deleted,
     *         false if the exchange was not found
     * @throws AuditorException if an error occurs accessing the data store.
     * @throws IllegalArgumentException if id is null or empty 
     */
    boolean deleteExchangeById(String id) throws AuditorException;

    /**
     * Delete exchanges ranging from fromIndex to toIndex.
     * 
     * @param fromIndex the lower bound index of the exchanges to be retrieved.
     *                  fromIndex must be greater or equal to zero.
     * @param toIndex the upper bound (exclusive) of the exchanges to be retrieved.
     *                toIndex must be greater or equal to fromIndex
     * @return the number of exchanges deleted
     * @throws AuditorException if an error occurs accessing the data store.
     * @throws IllegalArgumentException if fromIndex is less than zero or if toIndex is 
     *                                  less than fromIndex.
     */
    int deleteExchangesByRange(int fromIndex, int toIndex) throws AuditorException;

    /**
     * Delete exchanges given their ids.
     * 
     * @param ids the ids of exchanges to retrieve
     * @return the number of exchanges deleted
     * @throws AuditorException if an error occurs accessing the data store.
     * @throws IllegalArgumentException if ids is null, or one of its
     *         element is null or empty.
     */
    int deleteExchangesByIds(String[] ids) throws AuditorException;

    /**
     * Resend an exchange on behalf of the consumer component that initiated this exchange.
     * The exchange must have been retrieved from this auditor, else the behavior
     * is undefined.
     * The exchange will be given a new id and will be reset to its original state:
     * the out and fault messages will be removed (if they exist), the error will be
     * set to null, state to ACTIVE.
     * The consumer component must be prepared
     * to receive a response or a DONE status to an exchange it did not 
     * have directly initiated.
     * 
     * @param exchange the exchange to be sent
     * @throws AuditorException if an error occurs re-sending the exchange
     */
    void resendExchange(Exchange exchange) throws AuditorException;
}
