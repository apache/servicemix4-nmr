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
package org.apache.servicemix.nmr.api.service;

import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.servicemix.nmr.api.Wire;

/**
 *
 */
public final class ServiceHelper {

    private ServiceHelper() {
    }

    public static Map<String, Object> createMap(String... data) {
        Map<String, Object> props = new HashMap<String, Object>();
        for (int i = 0; i < data.length / 2; i++) {
            props.put(data[i * 2], data[i * 2 + 1]);
        }
        return props;
    }

    /**
     * Check if two endpoint propery maps are equal. This will check both maps for equal sizes, keys and values. If either map is
     * <code>null</code>, it will return <code>false</code>.
     * 
     * @param first the first endpoint property map
     * @param second the second endpoint property map
     * @return <code>true</code> if the endpoint maps are equal, <code>false</code> if it
     */
    public static boolean equals(Map<String, ?> first, Map<String, ?> second) {
        return doMatch(first, second, true);
    }

    /**
     * Check if two endpoint propery maps match. If will return <code>true</code> if, for every key in the first map, there's a matching key
     * and value in the second map. If either map is <code>null</code>, it will return <code>false</code>.
     * 
     * Compared to the {@link ServiceHelper#equals(Map, Map)} method, this will allow for the second Map to contain more information than
     * the first Map and still be considered a match
     * 
     * @param first the first endpoint property map
     * @param second the second endpoint property map
     * @return <code>true</code> if the endpoint maps are equal, <code>false</code> if it
     */
    public static boolean matches(Map<String, ?> first, Map<String, ?> second) {
        return doMatch(first, second, false);
    }

    /*
     * Shared implementation for equals and matches
     */
    private static boolean doMatch(Map<String, ?> first, Map<String, ?> second, boolean equal) {
        if (first == null || second == null) {
            return false;
        }
        if (equal && first.size() != second.size()) {
            return false;
        }
        for (Entry<String, ?> entry : first.entrySet()) {
            if (!second.containsKey(entry.getKey())) {
                return false;
            }
            if (!second.get(entry.getKey()).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a {@link Wire} instance
     * 
     * @param to the target for the wire
     * @return the wire object
     */
    public static Wire createWire(final Map<String, ?> from, final Map<String, Object> to) {
        return new Wire() {
            public Map<String, ?> getFrom() {
                return from;
            }
            public Map<String, ?> getTo() {
                return to;
            }
        };
    }
}
