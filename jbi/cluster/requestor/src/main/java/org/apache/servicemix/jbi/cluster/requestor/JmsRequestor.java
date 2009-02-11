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
package org.apache.servicemix.jbi.cluster.requestor;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.transaction.Transaction;

public interface JmsRequestor {

    /**
     * Retrieve the session.
     * Depending on cache levels, the same session may be reused several times.
     * If no session has been created yet, a new one will be established.
     *
     * @return
     */
    Session getSession();

    /**
     * Retrieve the message that has been consumed.
     * This method is to be used inside the {@link JmsRequestorListener} invocation.
     *
     * @return
     */
    Message getMessage();

    void begin();

    void send(Message message);

    void setRollbackOnly();

    /**
     * Close this requestor.
     * The transaction (if any) will be committed or rolled back depending if
     * the {@link #setRollbackOnly()} method has been called or not.
     */
    void close();

    /**
     * Suspend the transaction.  It can be resumed later by calling {@link JmsRequestorPool#resume(String)}.
     *
     * @param id
     */
    void suspend(String id);

    /**
     * Retreive the current XA transaction.
     * The transaction is only valid between a call to begin and close
     * @return
     */
    Transaction getTransaction();
}
