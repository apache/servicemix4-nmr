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
package org.apache.servicemix.nmr.core.security;

import java.util.Set;

import org.apache.servicemix.nmr.api.security.GroupPrincipal;
import junit.framework.TestCase;

public class DefaultAuthorizationEntryTest extends TestCase {

    public void testSetRoles() {
        DefaultAuthorizationEntry entry = new DefaultAuthorizationEntry();
        entry.setRoles("role1, role2");
        Set<GroupPrincipal> acls = entry.getAcls();
        assertNotNull(acls);
        assertEquals(2, acls.size());
        assertTrue(acls.contains(new GroupPrincipal("role1")));
        assertTrue(acls.contains(new GroupPrincipal("role2")));
    }

    public void testAnyRole() {
        DefaultAuthorizationEntry entry = new DefaultAuthorizationEntry();
        entry.setRoles("*");
        Set<GroupPrincipal> acls = entry.getAcls();
        assertNotNull(acls);
        assertEquals(1, acls.size());
        assertTrue(acls.contains(GroupPrincipal.ANY));
    }
}
