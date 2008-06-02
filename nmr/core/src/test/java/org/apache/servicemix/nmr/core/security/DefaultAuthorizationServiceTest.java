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

import javax.xml.namespace.QName;

import junit.framework.TestCase;
import org.apache.servicemix.nmr.api.security.AuthorizationEntry;
import org.apache.servicemix.nmr.api.security.GroupPrincipal;

public class DefaultAuthorizationServiceTest extends TestCase {

    protected DefaultAuthorizationService service;

    protected void setUp() {
        service = new DefaultAuthorizationService();
    }

    public void testSet() {
        addEntry("*", null, "*", AuthorizationEntry.Type.Add);
        addEntry("ep1", null, "role1", AuthorizationEntry.Type.Set);

        Set<GroupPrincipal> acls = service.getAcls("ep1", null);
        assertNotNull(acls);
        assertEquals(1, acls.size());
        assertTrue(acls.contains(new GroupPrincipal("role1")));
        acls = service.getAcls("ep2", null);
        assertNotNull(acls);
        assertEquals(1, acls.size());
        assertTrue(acls.contains(GroupPrincipal.ANY));
    }

    public void testRemoveAdd() {
        addEntry("*", null, "*", AuthorizationEntry.Type.Add);
        addEntry("ep.*", null, "*", AuthorizationEntry.Type.Remove);
        addEntry("ep1", null, "role1", AuthorizationEntry.Type.Add);

        Set<GroupPrincipal> acls = service.getAcls("ep1", null);
        assertNotNull(acls);
        assertEquals(1, acls.size());
        assertTrue(acls.contains(new GroupPrincipal("role1")));
        acls = service.getAcls("ep2", null);
        assertNotNull(acls);
        assertEquals(0, acls.size());
        acls = service.getAcls("p3", null);
        assertNotNull(acls);
        assertEquals(1, acls.size());
        assertTrue(acls.contains(GroupPrincipal.ANY));
    }

    public void testRank() {
        addEntry("*", null, "*", AuthorizationEntry.Type.Add, 0);
        addEntry("*", null, "*", AuthorizationEntry.Type.Remove, -1);
        addEntry("ep1", null, "role1", AuthorizationEntry.Type.Add);

        Set<GroupPrincipal> acls = service.getAcls("ep1", null);
        assertNotNull(acls);
        assertEquals(2, acls.size());
        assertTrue(acls.contains(new GroupPrincipal("role1")));
        assertTrue(acls.contains(GroupPrincipal.ANY));
    }

    protected void addEntry(String endpoint, QName operation, String roles) {
        addEntry(endpoint, operation, roles, AuthorizationEntry.Type.Add);
    }

    protected void addEntry(String endpoint, QName operation, String roles, AuthorizationEntry.Type type) {
        addEntry(endpoint, operation, roles, type, 0);
    }

    protected void addEntry(String endpoint, QName operation, String roles, AuthorizationEntry.Type type, int rank) {
        DefaultAuthorizationEntry entry = new DefaultAuthorizationEntry(endpoint,  operation, roles, type, rank);
        service.register(entry, null);
    }

}
