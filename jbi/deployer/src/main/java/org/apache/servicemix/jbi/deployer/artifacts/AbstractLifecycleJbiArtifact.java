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
package org.apache.servicemix.jbi.deployer.artifacts;

import javax.jbi.management.LifeCycleMBean;
import javax.jbi.JBIException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.nmr.api.event.ListenerRegistry;
import org.apache.servicemix.jbi.deployer.events.LifeCycleEvent;
import org.apache.servicemix.jbi.deployer.events.LifeCycleListener;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public abstract class AbstractLifecycleJbiArtifact implements LifeCycleMBean {

    public static final String STATE = "state";

    public enum State {
        Unknown,
        Started,
        Stopped,
        Shutdown,
    }

    protected final Log LOGGER = LogFactory.getLog(getClass());

    protected State state = State.Unknown;
    protected Preferences prefs;
    protected State runningState;
    protected ListenerRegistry listenerRegistry;

    public State getState() {
        return state;
    }

    public State getRunningState() {
        return runningState;
    }

    public ListenerRegistry getListenerRegistry() {
        return listenerRegistry;
    }

    public void setListenerRegistry(ListenerRegistry listenerRegistry) {
        this.listenerRegistry = listenerRegistry;
    }

    public String getCurrentState() {
        switch (state) {
            case Started:
                return LifeCycleMBean.STARTED;
            case Stopped:
                return LifeCycleMBean.STOPPED;
            case Shutdown:
                return LifeCycleMBean.SHUTDOWN;
            default:
                return LifeCycleMBean.UNKNOWN;
        }
    }

    protected State loadState(State def) {
        return State.valueOf(this.prefs.get(STATE, def.name()));
    }

    protected void saveState() {
        this.prefs.put(STATE, state.name());
        try {
            this.prefs.flush();
        } catch (BackingStoreException e) {
            LOGGER.warn("Unable to persist state", e);
        }
        this.runningState = state;
    }

    protected void fireEvent(LifeCycleEvent.LifeCycleEventType type) throws JBIException {
        fireEvent(type, false);
    }

    protected void fireEvent(LifeCycleEvent.LifeCycleEventType type, boolean force) throws JBIException {
        if (listenerRegistry != null) {
            LifeCycleEvent event = null;
            for (LifeCycleListener listener : listenerRegistry.getListeners(LifeCycleListener.class)) {
                if (event == null) {
                    event = new LifeCycleEvent(type, this, force);
                }
                listener.lifeCycleChanged(event);
            }
        }
    }

}
