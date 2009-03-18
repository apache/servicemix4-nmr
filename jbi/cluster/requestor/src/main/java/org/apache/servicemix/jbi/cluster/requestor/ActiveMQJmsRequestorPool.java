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

import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.lang.reflect.Method;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.XASession;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.Connection;
import javax.transaction.xa.XAResource;

import org.apache.activemq.MessageAvailableListener;
import org.apache.activemq.MessageAvailableConsumer;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.pool.PooledSession;
import org.springframework.jms.JmsException;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.util.Assert;

public class ActiveMQJmsRequestorPool extends AbstractPollingRequestorPool implements ExceptionListener {

    /**
     * List of consumers currently waiting for messages
     */
    final protected List<ActiveMQRequestor> polling = new LinkedList<ActiveMQRequestor>();

    /**
     * List of requestors with active sessions, but no consumers
     */
    final protected Queue<ActiveMQRequestor> requestors = new ConcurrentLinkedQueue<ActiveMQRequestor>();

    boolean consumersStarting;

    protected boolean cacheSessions = true;

    public boolean isCacheSessions() {
        return cacheSessions;
    }

    public void setCacheSessions(boolean cacheSessions) {
        this.cacheSessions = cacheSessions;
    }

    protected boolean sharedConnectionEnabled() {
        return true;
    }

    public void onException(JMSException exception) {
        handleListenerException(exception);
        if (sharedConnectionEnabled()) {
            try {
                refreshSharedConnection();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
        recreateConsumers(true);
    }

    protected Connection createConnection() throws JMSException {
        Connection con =  getConnectionFactory().createConnection();
        con.setExceptionListener(this);
        return con;
    }

    @Override
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
//        Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
//        Assert.isTrue(connectionFactory instanceof ActiveMQConnectionFactory
//                            || connectionFactory instanceof PooledConnectionFactory,
//                           "'connectionFactory' must be an instance of " + ActiveMQConnectionFactory.class
//                                + " or " + PooledConnectionFactory.class);
        super.setConnectionFactory(connectionFactory);
    }

    public void doInitialize() throws JMSException {
        startConsumers();
    }

    protected void startNewConsumer() throws JMSException {
        logger.debug("Creating a new consumer");
        ActiveMQRequestor requestor = (ActiveMQRequestor) createRequestor(true);
        polling.add(requestor);
        // Start the consumer
        boolean success = false;
        try {
            requestor.getConsumer();
            success = true;
        } finally {
            if (!success) {
                polling.remove(requestor);
            }
        }
    }

    @Override
    public void setConcurrentConsumers(int concurrentConsumers) {
        super.setConcurrentConsumers(concurrentConsumers);
        adjustConsumers();
    }

    @Override
    public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
        super.setMaxConcurrentConsumers(maxConcurrentConsumers);
        adjustConsumers();
    }

    protected void adjustConsumers() {
        synchronized (lifecycleMonitor) {
            while (polling.size() > maxConcurrentConsumers) {
                ActiveMQRequestor requestor = polling.remove(0);
                try {
                    requestor.afterClose();
                } catch (Throwable t) {
                    requestor.destroy();
                }
            }
        }
    }

    protected void recreateConsumers(boolean destroyRequestors) {
        if (isRunning()) {
            List<ActiveMQRequestor> reqs;
            synchronized (lifecycleMonitor) {
                reqs = new ArrayList<ActiveMQRequestor>(polling);
                polling.clear();
                if (destroyRequestors) {
                    reqs.addAll(requestors);
                    requestors.clear();
                }
            }
            for (ActiveMQRequestor requestor : reqs) {
                if (destroyRequestors) {
                    requestor.destroy();
                } else {
                    requestor.destroyConsumer();
                    try {
                        requestor.afterClose();
                    } catch (Throwable t) {
                        requestor.destroy();
                    }
                }
            }
            startConsumers();
        }
    }

    protected void startConsumers() {
        if (!consumersStarting) {
            Runnable r = new Runnable() {
                public void run() {
                    boolean alreadyRecovered = false;
                    for (;;) {
                        synchronized (lifecycleMonitor) {
                            if (!isRunning() || polling.size() >= getConcurrentConsumers()) {
                                break;
                            }
                        }
                        try {
                            startNewConsumer();
                        } catch (Throwable ex) {
                            handleListenerSetupFailure(ex, alreadyRecovered);
                            alreadyRecovered = true;
                            recoverAfterListenerSetupFailure();
                        }
                    }
                    consumersStarting = false;
                }
            };
            if (rescheduleTaskIfNecessary(r)) {
                consumersStarting = true;
            }
        }
    }

    protected Requestor createRequestor(boolean consume) throws JMSException {
        Requestor requestor = requestors.poll();
        if (requestor != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Choosing a requestor from pool");
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Creating a new requestor");
            }
            requestor = doCreateRequestor();
        }
        requestor.reset();
        return requestor;
    }

    protected Requestor doCreateRequestor() {
        return new ActiveMQRequestor();
    }

    @Override
    public void setMessageSelector(String selector) {
        if (!sameSelector(selector, getMessageSelector())) {
            super.setMessageSelector(selector);
            recreateConsumers(false);
        }
    }

    protected boolean sameSelector(String s0, String s1) {
        return (s0 == null && s1 == null) ||
               (s0 != null && s0.equals(s1));
    }

    public class ActiveMQRequestor extends Requestor implements MessageAvailableListener, Runnable {

        public ActiveMQRequestor() {
        }

        @Override
        public void begin() throws JmsException {
            boolean enlist = (session != null);
            super.begin();
            if (enlist && transacted == Transacted.Xa) {
                try {
                    XAResource res;
                    if (session instanceof XASession) {
                        res = ((XASession) session).getXAResource();
                    } else if (session instanceof ActiveMQSession) {
                        res = ((ActiveMQSession) session).getTransactionContext();
                    } else if (session instanceof PooledSession) {
                        Method mth = session.getClass().getDeclaredMethod("getSession");
                        mth.setAccessible(true);
                        ActiveMQSession s = (ActiveMQSession) mth.invoke(session);
                        res = s.getTransactionContext();
                    } else {
                        throw new IllegalStateException("Unable to retrieve XAResource from session " + session);
                    }
                    transaction.enlistResource(res);
                } catch (Exception e) {
                    throw new TransactionException(e);
                }
            }
        }

        @Override
        public void close() {
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
                        afterClose();
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
                    throw new RuntimeException(e);
                }
            }
        }

        /**
         * Retrieve the jms consumer to use for receiving messages
         * @return the consumer
         * @throws javax.jms.JMSException if an error occur
         */
        protected synchronized MessageConsumer getConsumer() throws JMSException {
            if (consumer == null) {
                consumer = createConsumer(getSession());
                ((MessageAvailableConsumer) consumer).setAvailableListener(this);
            }
            return consumer;
        }

        @Override
        protected synchronized void afterClose() throws JMSException {
            if (isRunning() && consumer != null && sameSelector(getMessageSelector(), consumer.getMessageSelector())) {
                if (message != null) {
                    rescheduleTaskIfNecessary(this);
                    return;
                } else {
                    // This is not atomic, which means we may end up with more than maxConsumers
                    // elements in the polling queue
                    synchronized (lifecycleMonitor) {
                        if (polling.size() < getMaxConcurrentConsumers()) {
                            polling.add(this);
                            return;
                        }
                    }
                }
            }
            if (cacheSessions && session != null) {
                destroyConsumer();
                requestors.add(this);
            } else {
                destroy();
            }
        }

        public void onMessageAvailable(MessageConsumer consumer) {
            boolean isPolling;
            synchronized (lifecycleMonitor) {
                isPolling = polling.remove(this);
            }
            if (isPolling) {
                startConsumers();
                rescheduleTaskIfNecessary(this);
            }
        }

        public synchronized void run() {
            boolean failure = true;
            try {
                reset();
                begin();
                message = getConsumer().receiveNoWait();
                if (message != null) {
                    listener.onMessage(this);
                }
                failure = false;
            } catch (Exception e) {
                logger.debug("Error processing message", e);
                setRollbackOnly();
            } finally {
                close();
                if (failure) {
                    recreateConsumers(true);
                }
            }
        }
    }

}
