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
package org.apache.servicemix.nmr.api.event;

import org.apache.servicemix.nmr.api.service.ServiceRegistry;

import java.util.Map;

/**
 * A registry of listeners.
 */
public interface ListenerRegistry extends ServiceRegistry<Listener> {

    /**
     * Add a listener to the registry.
     * In an OSGi world, listeners would be automatically added by a ServiceTracker.
     *
     * @param listener the listener to add
     * @param properties metadata associated with this listener. It may include data used for filtering events.
     */
    void register(Listener listener, Map<String, ?> properties);

    /**
     * Remove a listener.
     * In an OSGi world, this would be performed automatically by a ServiceTracker.
     *
     * @param listener the listener to remove
     */
    void unregister(Listener listener, Map<String, ?> properties);

    /**
     * Retrieve an iterator of listeners of a certain type
     *
     * @param type the type of listeners
     * @return an iterator over the registered listeners
     */
    <T extends Listener> Iterable<T> getListeners(Class<T> type);

}
