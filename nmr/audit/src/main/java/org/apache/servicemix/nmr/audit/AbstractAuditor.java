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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.event.ExchangeListener;

/**
 * Base class for ServiceMix auditors implementations.
 * 
 * @author Guillaume Nodet (gnt)
 * @since 1.0.0
 * @version $Revision: 550578 $
 */
public abstract class AbstractAuditor implements AuditorMBean, ExchangeListener {

    protected final Log log = LogFactory.getLog(getClass());
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchangeCount()
     */
    public abstract int getExchangeCount() throws AuditorException;
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchangeId(int)
     */
    public String getExchangeIdByIndex(int index) throws AuditorException {
        if (index < 0) {
            throw new IllegalArgumentException("index should be greater or equal to zero");
        }
        return getExchangeIdsByRange(index, index + 1)[0];
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchangeIds()
     */
    public String[] getAllExchangeIds() throws AuditorException {
        return getExchangeIdsByRange(0, getExchangeCount());
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchangeIds(int, int)
     */
    public abstract String[] getExchangeIdsByRange(int fromIndex, int toIndex)  throws AuditorException;
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchange(int)
     */
    public Exchange getExchangeByIndex(int index) throws AuditorException {
        if (index < 0) {
            throw new IllegalArgumentException("index should be greater or equal to zero");
        }
        return getExchangesByRange(index, index + 1)[0];
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchange(java.lang.String)
     */
    public Exchange getExchangeById(String id) throws AuditorException {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id should be non null and non empty");
        }
        return getExchangesByIds(new String[] {id })[0];
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchanges()
     */
    public Exchange[] getAllExchanges() throws AuditorException {
        return getExchangesByRange(0, getExchangeCount());
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchanges(int, int)
     */
    public Exchange[] getExchangesByRange(int fromIndex, int toIndex) throws AuditorException {
        return getExchangesByIds(getExchangeIdsByRange(fromIndex, toIndex));
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchanges(java.lang.String[])
     */
    public abstract Exchange[] getExchangesByIds(String[] ids) throws AuditorException;

    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#deleteExchanges()
     */
    public int deleteAllExchanges() throws AuditorException {
        return deleteExchangesByRange(0, getExchangeCount());
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#deleteExchange(int)
     */
    public boolean deleteExchangeByIndex(int index) throws AuditorException {
        if (index < 0) {
            throw new IllegalArgumentException("index should be greater or equal to zero");
        }
        return deleteExchangesByRange(index, index + 1) == 1;
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#deleteExchange(java.lang.String)
     */
    public boolean deleteExchangeById(String id) throws AuditorException {
        return deleteExchangesByIds(new String[] {id }) == 1;
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#deleteExchanges(int, int)
     */
    public int deleteExchangesByRange(int fromIndex, int toIndex) throws AuditorException {
        return deleteExchangesByIds(getExchangeIdsByRange(fromIndex, toIndex));
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#deleteExchanges(java.lang.String[])
     */
    public abstract int deleteExchangesByIds(String[] ids) throws AuditorException;
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#resendExchange(javax.jbi.messaging.MessageExchange)
     */
    public void resendExchange(Exchange exchange) throws AuditorException {
        // TODO
        //container.resendExchange(exchange);
    }

    public void exchangeDelivered(Exchange exchange) {
    }

    public void exchangeFailed(Exchange exchange) {
    }

}
