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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.Session;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Message;
import javax.jms.JMSException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.springframework.util.Assert;
import org.springframework.jms.JmsException;
import org.springframework.jms.support.JmsUtils;

public abstract class AbstractPollingRequestorPool extends AbstractJmsRequestorPool implements JmsRequestorPool {

    protected JmsRequestorListener listener;
    protected Transacted transacted;
    protected TransactionManager transactionManager;
    protected final Map<String, Requestor> parked = new ConcurrentHashMap<String, Requestor>();

    /**
     * Return the message listener to register.
     * @return the message listener to register
     */
    public JmsRequestorListener getListener() {
        return listener;
    }

    /**
     * Set the message listener implementation to register.
     * @param listener the listener to register
     */
    public void setListener(JmsRequestorListener listener) {
        this.listener = listener;
    }

    public Transacted getTransacted() {
        return transacted;
    }

    public void setTransacted(Transacted transacted) {
        this.transacted = transacted;
        switch (transacted) {
            case None:
                setSessionTransacted(false);
                setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
                break;
            case ClientAck:
                setSessionTransacted(false);
                setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
                break;
            case Jms:
            case Xa:
                setSessionTransacted(true);
                break;
        }
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Internal use only.
     * Called by an item for later reuse.
     *
     * @param item the item to park
     * @param id the parking id
     */
    protected void parkItem(Requestor item, String id) {
        parked.put(id, item);
    }

    public JmsRequestor newRequestor() throws JMSException {
        return createRequestor(false);
    }

    public JmsRequestor resume(String id) {
        Requestor requestor = parked.remove(id);
        synchronized (requestor) {
            requestor.resume();
        }
        return requestor;
    }

    protected void doShutdown() throws JMSException {

    }

    protected abstract JmsRequestor createRequestor(boolean consume) throws JMSException;


    public class Requestor implements JmsRequestor {

        protected Connection connection;
        protected Session session;
        protected MessageConsumer consumer;
        protected MessageProducer producer;
        protected boolean suspended;
        protected boolean rollbackOnly;
        protected Message message;
        protected Transaction transaction;

        public Requestor() {
        }

        /**
         * Retrieve the jms session
         * @return the session
         */
        public synchronized Session getSession() throws JmsException {
            try {
                if (session == null) {
                    Connection conToUse;
                    if (connection == null) {
                        if (sharedConnectionEnabled()) {
                            conToUse = getSharedConnection();
                        } else {
                            connection = createConnection();
                            connection.start();
                            conToUse = connection;
                        }
                    } else {
                        conToUse = connection;
                    }
                    session = createSession(conToUse);
                }
                return session;
            } catch (JMSException e) {
                throw convertJmsAccessException(e);
            }
        }

        /**
         * Receive a JMS message
         *
         * @param timeout receive timeout
         * @return the JMS message
         * @throws javax.jms.JMSException if an error occur
         */
        public synchronized Message receive(long timeout) throws JMSException {
            if (timeout < 0) {
                message = getConsumer().receive();
            } else {
                message = getConsumer().receive(timeout);
            }
            return message;
        }

        /**
         * Retrieve the jms consumer to use for receiving messages
         * @return the consumer
         * @throws javax.jms.JMSException if an error occur
         */
        protected MessageConsumer getConsumer() throws JMSException {
            if (consumer == null) {
                consumer = createConsumer(session);
            }
            return consumer;
        }

        /**
         * Send a JMS message
         *
         * @param msg the message to send
         * @throws javax.jms.JMSException if an error occur
         */
        public synchronized void send(Message msg) throws JmsException {
            if (logger.isDebugEnabled()) {
                logger.debug("Sending JMS message: " + msg);
            }
            try {
                getProducer().send(msg);
            } catch (JMSException e) {
                throw convertJmsAccessException(e);
            }
        }

        /**
         * Retrieve the jms producer to use for sending messages
         * @return the producer
         * @throws javax.jms.JMSException if an error occur
         */
        protected MessageProducer getProducer() throws JMSException {
            if (producer == null) {
                producer = createProducer(session);
            }
            return producer;
        }

        /**
         * Forget about this item.
         * Should be called to put in back into the pool if needed when
         * the item is no longer used.  If an item has been parked previously,
         * this call will do nothing.
         */
        public synchronized void close() {
            if (session != null) {
                if (!suspended) {
                    try {
                        if (transacted == Transacted.Jms) {
                            if (rollbackOnly) {
                                session.rollback();
                            } else {
                                session.commit();
                            }
                            afterClose();
                        } else if (transacted == Transacted.Xa) {
                            destroy();
                            try {
                                if (rollbackOnly) {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Rolling back XA transaction");
                                    }
                                    transactionManager.rollback();
                                } else {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Committing XA transaction");
                                    }
                                    transactionManager.commit();
                                }
                            } catch (Exception e) {
                                throw new TransactionException(e);
                            }
                        } else if (transacted == Transacted.ClientAck) {
                            if (message != null) {
                                if (!rollbackOnly) {
                                    message.acknowledge();
                                } else {
                                    destroyConsumer();
                                }
                            }
                            afterClose();
                        } else {
                            afterClose();
                        }
                    } catch (JMSException e) {
                        destroy();
                        throw convertJmsAccessException(e);
                    }
                }
            }
        }

        protected void afterClose() throws JMSException {
            destroy();
        }

        /**
         * Park this item for later reuse.
         * This should be called when the same session need to be reused at a later time.
         * @param id the parking id
         */
        public synchronized void suspend(String id) {
            if (transacted == Transacted.Xa) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Suspending XA transaction");
                    }
                    transactionManager.suspend();
                } catch (Exception e) {
                    throw new TransactionException(e);
                }
            }
            if (id != null) {
                parkItem(this, id);
            }
            suspended = true;
        }

        /**
         * Internal use only.
         * Destroy this item and free associated JMS resources.
         */
        protected void destroy() {
            JmsUtils.closeSession(session);
            JmsUtils.closeConnection(connection);
            session = null;
            connection = null;
        }

        /**
         * Internal use only.
         * Destroy the consumer so that it will use the new selector next time it is used.
         */
        protected void destroyConsumer() {
            MessageConsumer c = consumer;
            consumer = null;
            JmsUtils.closeMessageConsumer(c);
        }

        /**
         * Internal use only.
         * Mark this item has not parked anymore so that it can later be
         * returned to the pool by a call to {@link #close()}.
         */
        protected synchronized void resume() {
            if (transaction != null) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Resuming XA transaction");
                    }
                    transactionManager.resume(transaction);
                } catch (Exception e) {
                    throw new TransactionException(e);
                }
            }
            suspended = false;
        }

        protected void reset() {
            rollbackOnly = false;
            message = null;
        }

        /**
         * Internal use only.
         * Prepare this item to be used again.
         */
        public synchronized void begin() throws JmsException {
            startXaTransaction();
            getSession();
        }

        protected void startXaTransaction() {
            if (transacted == Transacted.Xa) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Starting XA transaction");
                    }
                    transactionManager.begin();
                    transaction = transactionManager.getTransaction();
                } catch (Exception e) {
                    throw new TransactionException(e);
                }
            }
        }

        /**
         * Mark the underlying transaction has rollback only.
         * This means that the transaction will be rolled back instead of
         * being committed.
         */
        public synchronized void setRollbackOnly() {
            rollbackOnly = true;
            if (transacted == Transacted.Xa) {
                try {
                    transactionManager.setRollbackOnly();
                } catch (Exception e) {
                    throw new TransactionException(e);
                }
            }
        }

        /**
         * Retrieve the current XA transaction
         * @return
         */
        public Transaction getTransaction() {
            return transaction;
        }

        public Message getMessage() {
            return message;
        }
    }

    public static class TransactionException extends RuntimeException {
        public TransactionException(Throwable cause) {
            super(cause);
        }
    }
}
