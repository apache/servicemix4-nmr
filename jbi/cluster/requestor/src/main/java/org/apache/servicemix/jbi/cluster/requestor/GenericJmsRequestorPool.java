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
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.TransactionManager;
import javax.transaction.Transaction;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Message;

import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.JmsException;
import org.springframework.util.Assert;
import org.springframework.scheduling.SchedulingAwareRunnable;

/**
 * A pool of session / consumer / producer.
 *
 * Pool items are obtained using the {@link #getRequestor(String)} method and
 * released using {@link Requestor#close()} method.
 */
public class GenericJmsRequestorPool extends AbstractPollingRequestorPool {

    /**
     * The default receive timeout: 1000 ms = 1 second.
     */
    public static final long DEFAULT_RECEIVE_TIMEOUT = 1000;

    private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;
    private boolean sharedConnectionEnabled = true;

    private int maxMessagesPerTask = Integer.MIN_VALUE;

    private int idleTaskExecutionLimit = 1;

    private final Set scheduledInvokers = new HashSet();

    private int activeInvokerCount = 0;

    private Object currentRecoveryMarker = new Object();

    private final Object recoveryMonitor = new Object();

    private Runnable stopCallback;

    /**
     * Set the timeout to use for receive calls, in <b>milliseconds</b>.
     * The default is 1000 ms, that is, 1 second.
     * <p><b>NOTE:</b> This value needs to be smaller than the transaction
     * timeout used by the transaction manager (in the appropriate unit,
     * of course). -1 indicates no timeout at all; however, this is only
     * feasible if not running within a transaction manager.
     * @see javax.jms.MessageConsumer#receive(long)
     * @see javax.jms.MessageConsumer#receive()
     * @see #setTransactionTimeout
     */
    public void setReceiveTimeout(long receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    public long getReceiveTimeout() {
        return receiveTimeout;
    }

    public boolean isSharedConnectionEnabled() {
        return sharedConnectionEnabled;
    }

    public void setSharedConnectionEnabled(boolean sharedConnectionEnabled) {
        this.sharedConnectionEnabled = sharedConnectionEnabled;
    }

    protected boolean sharedConnectionEnabled() {
        return isSharedConnectionEnabled();
    }

    protected Requestor createRequestor(boolean consume) throws JMSException {
        Requestor item = new Requestor();
        return item;
    }

    /**
     * Return the number of currently scheduled consumers.
     * <p>This number will always be inbetween "concurrentConsumers" and
     * "maxConcurrentConsumers", but might be higher than "activeConsumerCount"
     * (in case of some consumers being scheduled but not executed at the moment).
     * @see #getConcurrentConsumers()
     * @see #getMaxConcurrentConsumers()
     * @see #getActiveConsumerCount()
     */
    public final int getScheduledConsumerCount() {
        synchronized (this.lifecycleMonitor) {
            return this.scheduledInvokers.size();
        }
    }

    /**
     * Return the number of currently active consumers.
     * <p>This number will always be inbetween "concurrentConsumers" and
     * "maxConcurrentConsumers", but might be lower than "scheduledConsumerCount".
     * (in case of some consumers being scheduled but not executed at the moment).
     * @see #getConcurrentConsumers()
     * @see #getMaxConcurrentConsumers()
     * @see #getActiveConsumerCount()
     */
    public final int getActiveConsumerCount() {
        synchronized (this.lifecycleMonitor) {
            return this.activeInvokerCount;
        }
    }

    /**
     * Specify the maximum number of messages to process in one task.
     * More concretely, this limits the number of message reception attempts
     * per task, which includes receive iterations that did not actually
     * pick up a message until they hit their timeout (see the
     * {@link #setReceiveTimeout "receiveTimeout"} property).
     * <p>Default is unlimited (-1) in case of a standard TaskExecutor,
     * reusing the original invoker threads until shutdown (at the
     * expense of limited dynamic scheduling).
     * <p>In case of a SchedulingTaskExecutor indicating a preference for
     * short-lived tasks, the default is 10 instead. Specify a number
     * of 10 to 100 messages to balance between rather long-lived and
     * rather short-lived tasks here.
     * <p>Long-lived tasks avoid frequent thread context switches through
     * sticking with the same thread all the way through, while short-lived
     * tasks allow thread pools to control the scheduling. Hence, thread
     * pools will usually prefer short-lived tasks.
     * <p><b>This setting can be modified at runtime, for example through JMX.</b>
     * @see #setTaskExecutor
     * @see #setReceiveTimeout
     * @see org.springframework.scheduling.SchedulingTaskExecutor#prefersShortLivedTasks()
     */
    public void setMaxMessagesPerTask(int maxMessagesPerTask) {
        Assert.isTrue(maxMessagesPerTask != 0, "'maxMessagesPerTask' must not be 0");
        synchronized (this.lifecycleMonitor) {
            this.maxMessagesPerTask = maxMessagesPerTask;
        }
    }

    /**
     * Return the maximum number of messages to process in one task.
     */
    public int getMaxMessagesPerTask() {
        synchronized (this.lifecycleMonitor) {
            return this.maxMessagesPerTask;
        }
    }

    /**
     * Specify the limit for idle executions of a receive task, not having
     * received any message within its execution. If this limit is reached,
     * the task will shut down and leave receiving to other executing tasks.
     * <p>Default is 1, closing idle resources early once a task didn't
     * receive a message. This applies to dynamic scheduling only; see the
     * {@link #setMaxConcurrentConsumers "maxConcurrentConsumers"} setting.
     * The minimum number of consumers
     * (see {@link #setConcurrentConsumers "concurrentConsumers"})
     * will be kept around until shutdown in any case.
     * <p>Within each task execution, a number of message reception attempts
     * (according to the "maxMessagesPerTask" setting) will each wait for an incoming
     * message (according to the "receiveTimeout" setting). If all of those receive
     * attempts in a given task return without a message, the task is considered
     * idle with respect to received messages. Such a task may still be rescheduled;
     * however, once it reached the specified "idleTaskExecutionLimit", it will
     * shut down (in case of dynamic scaling).
     * <p>Raise this limit if you encounter too frequent scaling up and down.
     * With this limit being higher, an idle consumer will be kept around longer,
     * avoiding the restart of a consumer once a new load of messages comes in.
     * Alternatively, specify a higher "maxMessagesPerTask" and/or "receiveTimeout" value,
     * which will also lead to idle consumers being kept around for a longer time
     * (while also increasing the average execution time of each scheduled task).
     * <p><b>This setting can be modified at runtime, for example through JMX.</b>
     * @see #setMaxMessagesPerTask
     * @see #setReceiveTimeout
     */
    public void setIdleTaskExecutionLimit(int idleTaskExecutionLimit) {
        Assert.isTrue(idleTaskExecutionLimit > 0, "'idleTaskExecutionLimit' must be 1 or higher");
        synchronized (this.lifecycleMonitor) {
            this.idleTaskExecutionLimit = idleTaskExecutionLimit;
        }
    }

    /**
     * Return the limit for idle executions of a receive task.
     */
    public int getIdleTaskExecutionLimit() {
        synchronized (this.lifecycleMonitor) {
            return this.idleTaskExecutionLimit;
        }
    }

    /**
     * Tries scheduling a new invoker, since we know messages are coming in...
     * @see #scheduleNewInvokerIfAppropriate()
     */
    protected void messageReceived(Object invoker, Session session) {
        ((AsyncMessageListenerInvoker) invoker).setIdle(false);
        scheduleNewInvokerIfAppropriate();
    }

    /**
     * Marks the affected invoker as idle.
     */
    protected void noMessageReceived(Object invoker, Session session) {
        ((AsyncMessageListenerInvoker) invoker).setIdle(true);
    }

    /**
     * Schedule a new invoker, increasing the total number of scheduled
     * invokers for this listener container, but only if the specified
     * "maxConcurrentConsumers" limit has not been reached yet, and only
     * if this listener container does not currently have idle invokers
     * that are waiting for new messages already.
     * <p>Called once a message has been received, to scale up while
     * processing the message in the invoker that originally received it.
     * @see #setTaskExecutor
     * @see #getMaxConcurrentConsumers()
     */
    protected void scheduleNewInvokerIfAppropriate() {
        if (isRunning()) {
            resumePausedTasks();
            synchronized (this.lifecycleMonitor) {
                if (this.scheduledInvokers.size() < this.maxConcurrentConsumers && getIdleInvokerCount() == 0) {
                    scheduleNewInvoker();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Raised scheduled invoker count: " + this.scheduledInvokers.size());
                    }
                }
            }
        }
    }

    /**
     * Schedule a new invoker, increasing the total number of scheduled
     * invokers for this listener container.
     */
    private void scheduleNewInvoker() {
        AsyncMessageListenerInvoker invoker = new AsyncMessageListenerInvoker();
        if (rescheduleTaskIfNecessary(invoker)) {
            // This should always be true, since we're only calling this when active.
            this.scheduledInvokers.add(invoker);
        }
    }

    /**
     * Determine whether the current invoker should be rescheduled,
     * given that it might not have received a message in a while.
     * @param idleTaskExecutionCount the number of idle executions
     * that this invoker task has already accumulated (in a row)
     */
    private boolean shouldRescheduleInvoker(int idleTaskExecutionCount) {
        boolean superfluous =
                (idleTaskExecutionCount >= this.idleTaskExecutionLimit && getIdleInvokerCount() > 1);
        return (this.scheduledInvokers.size() <=
                (superfluous ? this.concurrentConsumers : this.maxConcurrentConsumers));
    }

    /**
     * Determine whether this listener container currently has more
     * than one idle instance among its scheduled invokers.
     */
    private int getIdleInvokerCount() {
        int count = 0;
        for (Iterator it = this.scheduledInvokers.iterator(); it.hasNext();) {
            AsyncMessageListenerInvoker invoker = (AsyncMessageListenerInvoker) it.next();
            if (invoker.isIdle()) {
                count++;
            }
        }
        return count;
    }


    /**
     * Retrieve a parked session/consumer/producer triplet from the pool or a newly created one.
     * If an item has been previously parked with the same id, it will be unparked and returned,
     * else, one will be obtained from the pool or created.
     *
     * @param id the parking id
     * @return the parked item or a new one
     * @throws javax.jms.JMSException if an error occur
     */
    public Requestor getRequestor(String id) throws JMSException {
        Requestor item = null;
        if (id != null) {
            item = parked.remove(id);
            if (item != null) {
                item.resume();
            }
        }
        if (item == null) {
            item = createRequestor(id == null);
        }
        return item;
    }

    public void doInitialize() throws JMSException {
        synchronized (this.lifecycleMonitor) {
            for (int i = 0; i < this.concurrentConsumers; i++) {
                scheduleNewInvoker();
            }
        }
    }

    public void setMessageSelector(String selector) {
        super.setMessageSelector(selector);
        if (logger.isDebugEnabled()) {
            logger.debug("Using selector: " + selector);
        }
    }

    public class AsyncMessageListenerInvoker implements SchedulingAwareRunnable {

        private Object lastRecoveryMarker;

        private boolean lastMessageSucceeded;

        private int idleTaskExecutionCount = 0;

        private volatile boolean idle = true;

        public void run() {
            synchronized (lifecycleMonitor) {
                activeInvokerCount++;
                lifecycleMonitor.notifyAll();
            }
            updateRecoveryMarker();
            boolean messageReceived = false;
            try {
                if (maxMessagesPerTask < 0) {
                    messageReceived = executeOngoingLoop();
                }
                else {
                    int messageCount = 0;
                    while (isRunning() && messageCount < maxMessagesPerTask) {
                        messageReceived = (invokeListener() || messageReceived);
                        messageCount++;
                    }
                }
            } catch (Exception ex) {
                clearResources();
                if (!lastMessageSucceeded) {
                    // We failed more than once in a row - sleep for recovery interval
                    // even before first recovery attempt.
                    sleepInbetweenRecoveryAttempts();
                }
                this.lastMessageSucceeded = false;
                boolean alreadyRecovered = false;
                synchronized (recoveryMonitor) {
                    if (this.lastRecoveryMarker == currentRecoveryMarker) {
                        handleListenerSetupFailure(ex, false);
                        recoverAfterListenerSetupFailure();
                        currentRecoveryMarker = new Object();
                    } else {
                        alreadyRecovered = true;
                    }
                }
                if (alreadyRecovered) {
                    handleListenerSetupFailure(ex, true);
                }
            }
            synchronized (lifecycleMonitor) {
                decreaseActiveInvokerCount();
                lifecycleMonitor.notifyAll();
            }
            if (!messageReceived) {
                this.idleTaskExecutionCount++;
            } else {
                this.idleTaskExecutionCount = 0;
            }
            synchronized (lifecycleMonitor) {
                if (!shouldRescheduleInvoker(this.idleTaskExecutionCount) || !rescheduleTaskIfNecessary(this)) {
                    // We're shutting down completely.
                    scheduledInvokers.remove(this);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Lowered scheduled invoker count: " + scheduledInvokers.size());
                    }
                    lifecycleMonitor.notifyAll();
                    clearResources();
                }
                else if (isRunning()) {
                    int nonPausedConsumers = getScheduledConsumerCount() - getPausedTaskCount();
                    if (nonPausedConsumers < 1) {
                        logger.error("All scheduled consumers have been paused, probably due to tasks having been rejected. " +
                                "Check your thread pool configuration! Manual recovery necessary through a start() call.");
                    }
                    else if (nonPausedConsumers < getConcurrentConsumers()) {
                        logger.warn("Number of scheduled consumers has dropped below concurrentConsumers limit, probably " +
                                "due to tasks having been rejected. Check your thread pool configuration! Automatic recovery " +
                                "to be triggered by remaining consumers.");
                    }
                }
            }
        }

        protected boolean executeOngoingLoop() throws Exception {
            boolean messageReceived = false;
            boolean active = true;
            while (active) {
                synchronized (lifecycleMonitor) {
                    boolean interrupted = false;
                    boolean wasWaiting = false;
                    while ((active = isActive()) && !isRunning()) {
                        if (interrupted) {
                            throw new IllegalStateException("Thread was interrupted while waiting for " +
                                    "a restart of the listener container, but container is still stopped");
                        }
                        if (!wasWaiting) {
                            decreaseActiveInvokerCount();
                        }
                        wasWaiting = true;
                        try {
                            lifecycleMonitor.wait();
                        }
                        catch (InterruptedException ex) {
                            // Re-interrupt current thread, to allow other threads to react.
                            Thread.currentThread().interrupt();
                            interrupted = true;
                        }
                    }
                    if (wasWaiting) {
                        activeInvokerCount++;
                    }
                }
                if (active) {
                    messageReceived = (invokeListener() || messageReceived);
                }
            }
            return messageReceived;
        }

        protected void decreaseActiveInvokerCount() {
            activeInvokerCount--;
            if (stopCallback != null && activeInvokerCount == 0) {
                stopCallback.run();
                stopCallback = null;
            }
        }

        protected boolean invokeListener() throws Exception {
            boolean messageReceived = false;
            Requestor req = createRequestor(true);
            synchronized (req) {
                try {
                    req.begin();
                    messageReceived = req.receive(receiveTimeout) != null;
                    if (messageReceived) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Received message of type [" + req.getMessage().getClass() + "] from consumer");
                        }
                        messageReceived(this, req.getSession());
                        listener.onMessage(req);
                        lastMessageSucceeded = true;
                    } else {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Consumer did not receive a message");
                        }
                        noMessageReceived(this, req.getSession());
                        lastMessageSucceeded = true;
                    }
                } finally {
                    if (req != null) {
                        req.close();
                    }
                }
            }
            return messageReceived;
        }

        private void updateRecoveryMarker() {
            synchronized (recoveryMonitor) {
                this.lastRecoveryMarker = currentRecoveryMarker;
            }
        }

        private void clearResources() {

        }

        public boolean isLongLived() {
            return (maxMessagesPerTask < 0);
        }

        public void setIdle(boolean idle) {
            this.idle = idle;
        }

        public boolean isIdle() {
            return this.idle;
        }
    }

}
