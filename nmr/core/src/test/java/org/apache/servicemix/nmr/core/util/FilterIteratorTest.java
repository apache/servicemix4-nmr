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

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

public class FilterIteratorTest extends TestCase {

    public void test() {
        List<Integer> l = Arrays.asList(new Integer[] { 1, 2, 3 });
        FilterIterator<Integer> it = new FilterIterator<Integer>(l.iterator(), new Filter<Integer>() {
            public boolean match(Integer value) {
                return value % 2 == 1;
            }
        });
        try {
            it.remove();
            fail();
        } catch (UnsupportedOperationException e) {
        }
        assertTrue(it.hasNext());
        assertTrue(1 == it.next());
        assertTrue(it.hasNext());
        assertTrue(3 == it.next());
        assertTrue(!it.hasNext());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

}
