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
package org.apache.servicemix.nmr.api.security;

import junit.framework.TestCase;

/**
 *
 */
public class UserPrincipalTest extends TestCase {

    public void testArguments() {
        UserPrincipal principal = new UserPrincipal("FOO");

        assertEquals("FOO", principal.getName());

        try {
            new UserPrincipal(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ingore) {
            // Expected
        }
    }

    public void testHash() {
        UserPrincipal p1 = new UserPrincipal("FOO");
        UserPrincipal p2 = new UserPrincipal("FOO");

        assertEquals(p1.hashCode(), p1.hashCode());
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @SuppressWarnings("PMD.PositionLiteralsFirstInComparisons")
    public void testEquals() {
        UserPrincipal p1 = new UserPrincipal("FOO");
        UserPrincipal p2 = new UserPrincipal("FOO");
        UserPrincipal p3 = new UserPrincipal("BAR");

        assertTrue(p1.equals(p1));
        assertTrue(p1.equals(p2));
        assertFalse(p1.equals((Object) null)); // NOPMD just testing
        assertFalse(p1.equals(new Object())); // NOPMD just testing
        assertFalse(p1.equals(p3));
    }
}
