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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Queue;
import javax.jms.MessageProducer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.jms.support.destination.CachingDestinationResolver;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.JmsException;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public abstract class AbstractJmsRequestorPool extends AbstractMessageListenerContainer {

   /**
    * Default thread name prefix: "DefaultMessageListenerContainer-".
    */
   public static final String DEFAULT_THREAD_NAME_PREFIX = "JmsRequestorPool-";

    /**
     * The default recovery interval: 5000 ms = 5 seconds.
     */
    public static final long DEFAULT_RECOVERY_INTERVAL = 5000;

    private long recoveryInterval = DEFAULT_RECOVERY_INTERVAL;

    private TaskExecutor taskExecutor;
    protected int concurrentConsumers = 1;
    protected int maxConcurrentConsumers = 1;

    /**
     * Set the Spring TaskExecutor to use for running the listener threads.
     * <p>Default is a {@link org.springframework.core.task.SimpleAsyncTaskExecutor},
     * starting up a number of new threads, according to the specified number
     * of concurrent consumers.
     * <p>Specify an alternative TaskExecutor for integration with an existing
     * thread pool. Note that this really only adds value if the threads are
     * managed in a specific fashion, for example within a J2EE environment.
     * A plain thread pool does not add much value, as this listener container
     * will occupy a number of threads for its entire lifetime.
     * @see #setConcurrentConsumers
     * @see org.springframework.core.task.SimpleAsyncTaskExecutor
     * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
     */
    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * Specify the interval between recovery attempts, in <b>milliseconds</b>.
     * The default is 5000 ms, that is, 5 seconds.
     * @see #handleListenerSetupFailure
     */
    public void setRecoveryInterval(long recoveryInterval) {
        this.recoveryInterval = recoveryInterval;
    }

    //-------------------------------------------------------------------------
    // Implementation of AbstractMessageListenerContainer's template methods
    //-------------------------------------------------------------------------

    public void initialize() {
        // Prepare taskExecutor and maxMessagesPerTask.
        synchronized (this.lifecycleMonitor) {
            if (this.taskExecutor == null) {
                this.taskExecutor = createDefaultTaskExecutor();
            }
        }

        // Proceed with actual listener initialization.
        super.initialize();
    }

    /**
     * Create a default TaskExecutor. Called if no explicit TaskExecutor has been specified.
     * <p>The default implementation builds a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
     * with the specified bean name (or the class name, if no bean name specified) as thread name prefix.
     * @see org.springframework.core.task.SimpleAsyncTaskExecutor#SimpleAsyncTaskExecutor(String)
     */
    protected TaskExecutor createDefaultTaskExecutor() {
        String beanName = getBeanName();
        String threadNamePrefix = (beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);
        return new SimpleAsyncTaskExecutor(threadNamePrefix);
    }

    /**
     * Re-executes the given task via this listener container's TaskExecutor.
     * @see #setTaskExecutor
     */
    protected void doRescheduleTask(Object task) {
        this.taskExecutor.execute((Runnable) task);
    }

    //-------------------------------------------------------------------------
    // Listener recovery
    //-------------------------------------------------------------------------

    /**
     * Overridden to accept a failure in the initial setup - leaving it up to the
     * asynchronous invokers to establish the shared Connection on first access.
     * @see #refreshConnectionUntilSuccessful()
     */
    protected void establishSharedConnection() {
        try {
            super.establishSharedConnection();
        }
        catch (Exception ex) {
            logger.debug("Could not establish shared JMS Connection - " +
                    "leaving it up to asynchronous invokers to establish a Connection as soon as possible", ex);
        }
    }

    /**
     * This implementations proceeds even after an exception thrown from
     * <code>Connection.start()</code>, relying on listeners to perform
     * appropriate recovery.
     */
    protected void startSharedConnection() {
        try {
            super.startSharedConnection();
        }
        catch (Exception ex) {
            logger.debug("Connection start failed - relying on listeners to perform recovery", ex);
        }
    }

    /**
     * This implementations proceeds even after an exception thrown from
     * <code>Connection.stop()</code>, relying on listeners to perform
     * appropriate recovery after a restart.
     */
    protected void stopSharedConnection() {
        try {
            super.stopSharedConnection();
        }
        catch (Exception ex) {
            logger.debug("Connection stop failed - relying on listeners to perform recovery after restart", ex);
        }
    }

    /**
     * Handle the given exception that arose during setup of a listener.
     * Called for every such exception in every concurrent listener.
     * <p>The default implementation logs the exception at error level
     * if not recovered yet, and at debug level if already recovered.
     * Can be overridden in subclasses.
     * @param ex the exception to handle
     * @param alreadyRecovered whether a previously executing listener
     * already recovered from the present listener setup failure
     * (this usually indicates a follow-up failure than can be ignored
     * other than for debug log purposes)
     * @see #recoverAfterListenerSetupFailure()
     */
    protected void handleListenerSetupFailure(Throwable ex, boolean alreadyRecovered) {
        if (ex instanceof JMSException) {
            invokeExceptionListener((JMSException) ex);
        }
        if (ex instanceof SharedConnectionNotInitializedException) {
            if (!alreadyRecovered) {
                logger.debug("JMS message listener invoker needs to establish shared Connection");
            }
        }
        else {
            // Recovery during active operation..
            if (alreadyRecovered) {
                logger.debug("Setup of JMS message listener invoker failed - already recovered by other invoker", ex);
            }
            else {
                StringBuffer msg = new StringBuffer();
                msg.append("Setup of JMS message listener invoker failed for destination '");
                msg.append(getDestinationDescription()).append("' - trying to recover. Cause: ");
                msg.append(ex instanceof JMSException ? JmsUtils.buildExceptionMessage(fixForSpring5470((JMSException) ex)) : ex.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.info(msg, ex);
                }
                else {
                    logger.info(msg);
                }
            }
        }
    }

    /**
     * Recover this listener container after a listener failed to set itself up,
     * for example reestablishing the underlying Connection.
     * <p>The default implementation delegates to DefaultMessageListenerContainer's
     * recovery-capable {@link #refreshConnectionUntilSuccessful()} method, which will
     * try to re-establish a Connection to the JMS provider both for the shared
     * and the non-shared Connection case.
     * @see #refreshConnectionUntilSuccessful()
     * @see #refreshDestination()
     */
    protected void recoverAfterListenerSetupFailure() {
        refreshConnectionUntilSuccessful();
        refreshDestination();
    }

    /**
     * Refresh the underlying Connection, not returning before an attempt has been
     * successful. Called in case of a shared Connection as well as without shared
     * Connection, so either needs to operate on the shared Connection or on a
     * temporary Connection that just gets established for validation purposes.
     * <p>The default implementation retries until it successfully established a
     * Connection, for as long as this message listener container is active.
     * Applies the specified recovery interval between retries.
     * @see #setRecoveryInterval
     */
    protected void refreshConnectionUntilSuccessful() {
        while (isRunning()) {
            try {
                if (sharedConnectionEnabled()) {
                    refreshSharedConnection();
                }
                else {
                    Connection con = createConnection();
                    JmsUtils.closeConnection(con);
                }
                logger.info("Successfully refreshed JMS Connection");
                break;
            }
            catch (Exception ex) {
                StringBuffer msg = new StringBuffer();
                msg.append("Could not refresh JMS Connection for destination '");
                msg.append(getDestinationDescription()).append("' - retrying in ");
                msg.append(this.recoveryInterval).append(" ms. Cause: ");
                msg.append(ex instanceof JMSException ? JmsUtils.buildExceptionMessage(fixForSpring5470((JMSException) ex)) : ex.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.info(msg, ex);
                }
                else if (logger.isInfoEnabled()) {
                    logger.info(msg);
                }
            }
            sleepInbetweenRecoveryAttempts();
        }
    }

    /**
     * Refresh the JMS destination that this listener container operates on.
     * <p>Called after listener setup failure, assuming that a cached Destination
     * object might have become invalid (a typical case on WebLogic JMS).
     * <p>The default implementation removes the destination from a
     * DestinationResolver's cache, in case of a CachingDestinationResolver.
     * @see #setDestinationName
     * @see org.springframework.jms.support.destination.CachingDestinationResolver
     */
    protected void refreshDestination() {
        String destName = getDestinationName();
        if (destName != null) {
            DestinationResolver destResolver = getDestinationResolver();
            if (destResolver instanceof CachingDestinationResolver) {
                ((CachingDestinationResolver) destResolver).removeFromCache(destName);
            }
        }
    }

    /**
     * Sleep according to the specified recovery interval.
     * Called inbetween recovery attempts.
     */
    protected void sleepInbetweenRecoveryAttempts() {
        if (this.recoveryInterval > 0) {
            try {
                Thread.sleep(this.recoveryInterval);
            }
            catch (InterruptedException interEx) {
                // Re-interrupt current thread, to allow other threads to react.
                Thread.currentThread().interrupt();
            }
        }
    }


    //-------------------------------------------------------------------------
    // JMS layer
    //-------------------------------------------------------------------------

    /**
     * Create a MessageConsumer for the given JMS Session.
     * @param session the JMS Session to work on
     * @return the MessageConsumer
     * @throws javax.jms.JMSException if thrown by JMS methods
     */
    protected MessageConsumer createConsumer(Session session) throws JMSException {
        Destination destination = getDestination();
        if (destination == null) {
            destination = resolveDestinationName(session, getDestinationName());
        }
        return createConsumer(session, destination);
    }

    /**
     * Create a JMS MessageConsumer for the given Session and Destination.
     * <p>This implementation uses JMS 1.1 API.
     * @param session the JMS Session to create a MessageConsumer for
     * @param destination the JMS Destination to create a MessageConsumer for
     * @return the new JMS MessageConsumer
     * @throws javax.jms.JMSException if thrown by JMS API methods
     */
    protected MessageConsumer createConsumer(Session session, Destination destination) throws JMSException {
        return session.createConsumer(destination, getMessageSelector());
    }

    protected MessageProducer createProducer(Session session) throws JMSException {
        Destination destination = getDestination();
        if (destination == null) {
            destination = resolveDestinationName(session, getDestinationName());
        }
        return createProducer(session, destination);
    }

    protected MessageProducer createProducer(Session session, Destination destination) throws JMSException {
        return session.createProducer(destination);
    }

    protected JmsException convertJmsAccessException(JMSException ex) {
        return JmsUtils.convertJmsAccessException(fixForSpring5470(ex));
    }

    private JMSException fixForSpring5470(JMSException ex) {
        if (ex.getCause() != null && ex.getCause().getMessage() == null) {
            ex.setLinkedException(new Exception("Unknown", ex.getCause()));
        }
        return ex;
    }

    /**
     * Specify the number of concurrent consumers to create. Default is 1.
     * <p>Specifying a higher value for this setting will increase the standard
     * level of scheduled concurrent consumers at runtime: This is effectively
     * the minimum number of concurrent consumers which will be scheduled
     * at any given time. This is a static setting; for dynamic scaling,
     * consider specifying the "maxConcurrentConsumers" setting instead.
     * <p>Raising the number of concurrent consumers is recommendable in order
     * to scale the consumption of messages coming in from a queue. However,
     * note that any ordering guarantees are lost once multiple consumers are
     * registered. In general, stick with 1 consumer for low-volume queues.
     * <p><b>Do not raise the number of concurrent consumers for a topic.</b>
     * This would lead to concurrent consumption of the same message,
     * which is hardly ever desirable.
     * <p><b>This setting can be modified at runtime, for example through JMX.</b>
     * @see #setMaxConcurrentConsumers
     */
    public void setConcurrentConsumers(int concurrentConsumers) {
        Assert.isTrue(concurrentConsumers > 0, "'concurrentConsumers' value must be at least 1 (one)");
        synchronized (this.lifecycleMonitor) {
            this.concurrentConsumers = concurrentConsumers;
            if (this.maxConcurrentConsumers < concurrentConsumers) {
                this.maxConcurrentConsumers = concurrentConsumers;
            }
        }
    }

    /**
     * Return the "concurrentConsumer" setting.
     * <p>This returns the currently configured "concurrentConsumers" value;
     * the number of currently scheduled/active consumers might differ.
     * @see #getScheduledConsumerCount()
     * @see #getActiveConsumerCount()
     */
    public final int getConcurrentConsumers() {
        synchronized (this.lifecycleMonitor) {
            return this.concurrentConsumers;
        }
    }

    /**
     * Specify the maximum number of concurrent consumers to create. Default is 1.
     * <p>If this setting is higher than "concurrentConsumers", the listener container
     * will dynamically schedule new consumers at runtime, provided that enough
     * incoming messages are encountered. Once the load goes down again, the number of
     * consumers will be reduced to the standard level ("concurrentConsumers") again.
     * <p>Raising the number of concurrent consumers is recommendable in order
     * to scale the consumption of messages coming in from a queue. However,
     * note that any ordering guarantees are lost once multiple consumers are
     * registered. In general, stick with 1 consumer for low-volume queues.
     * <p><b>Do not raise the number of concurrent consumers for a topic.</b>
     * This would lead to concurrent consumption of the same message,
     * which is hardly ever desirable.
     * <p><b>This setting can be modified at runtime, for example through JMX.</b>
     * @see #setConcurrentConsumers
     */
    public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
        Assert.isTrue(maxConcurrentConsumers > 0, "'maxConcurrentConsumers' value must be at least 1 (one)");
        synchronized (this.lifecycleMonitor) {
            this.maxConcurrentConsumers =
                    (maxConcurrentConsumers > this.concurrentConsumers ? maxConcurrentConsumers : this.concurrentConsumers);
        }
    }

    /**
     * Return the "maxConcurrentConsumer" setting.
     * <p>This returns the currently configured "maxConcurrentConsumers" value;
     * the number of currently scheduled/active consumers might differ.
     * @see #getScheduledConsumerCount()
     * @see #getActiveConsumerCount()
     */
    public final int getMaxConcurrentConsumers() {
        synchronized (this.lifecycleMonitor) {
            return this.maxConcurrentConsumers;
        }
    }

}
