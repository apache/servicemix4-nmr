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
package org.apache.servicemix.nmr.core.util;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

public class MapToDictionaryTest extends TestCase {

    public void testConstructorWithNullArgument() {
        try {
            new MapToDictionary(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            new IteratorToEnumeration(null);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    public void testMap() {
        Map<Integer, Boolean> map = new TreeMap<Integer, Boolean>();
        map.put(3, true);
        map.put(5, false);
        Dictionary<Integer, Boolean> dic = new MapToDictionary(map);
        assertTrue(2 == dic.size());
        assertTrue(true == dic.get(3));
        assertTrue(false == dic.get(5));

        Enumeration<Integer> e1 = dic.keys();
        assertNotNull(e1);
        assertTrue(e1.hasMoreElements());
        assertTrue(3 == e1.nextElement());
        assertTrue(e1.hasMoreElements());
        assertTrue(5 == e1.nextElement());
        assertFalse(e1.hasMoreElements());

        Enumeration<Boolean> e2 = dic.elements();
        assertNotNull(e2);
        assertTrue(e2.hasMoreElements());
        assertTrue(true == e2.nextElement());
        assertTrue(e2.hasMoreElements());
        assertTrue(false == e2.nextElement());
        assertFalse(e2.hasMoreElements());


        try {
            dic.put(3, false);
            fail();
        } catch (UnsupportedOperationException e) {
        }

        try {
            dic.remove(3);
            fail();
        } catch (UnsupportedOperationException e) {
        }

        assertNotNull(dic.toString());
        assertFalse(dic.isEmpty());
    }
}
