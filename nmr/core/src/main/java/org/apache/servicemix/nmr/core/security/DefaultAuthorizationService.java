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
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.servicemix.nmr.api.security.AuthorizationService;
import org.apache.servicemix.nmr.api.security.GroupPrincipal;
import org.apache.servicemix.nmr.api.security.AuthorizationEntry;

/**
 * A default implementation of the authorization service.
 */
public class DefaultAuthorizationService implements AuthorizationService {

    private List<AuthorizationEntry> authorizationEntries;
    private Comparator<AuthorizationEntry> comparator;
    private Map<String, Set<GroupPrincipal>> cache;

    public DefaultAuthorizationService() {
        authorizationEntries = new ArrayList<AuthorizationEntry>();
        cache = Collections.synchronizedMap(new LRUMap<String, Set<GroupPrincipal>>(64));
        comparator = new Comparator<AuthorizationEntry>() {
            public int compare(AuthorizationEntry o1, AuthorizationEntry o2) {
                if (o1.getRank() < o2.getRank()) {
                    return -1;
                } else if (o1.getRank() > o2.getRank()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
    }

    public void register(AuthorizationEntry entry, Map<String,?> props) {
        authorizationEntries.add(entry);
        Collections.sort(authorizationEntries, comparator);
        cache.clear();
    }

    public void unregister(AuthorizationEntry entry, Map<String,?> props) {
        authorizationEntries.remove(entry);
        Collections.sort(authorizationEntries, comparator);
        cache.clear();
    }

    public Set<GroupPrincipal> getAcls(String endpoint, QName operation) {
        String key = endpoint + "|" + (operation != null ? operation.toString() : "");
        Set<GroupPrincipal> acls = cache.get(key);
        if (acls == null) {
            acls = new HashSet<GroupPrincipal>();
            for (AuthorizationEntry entry : authorizationEntries) {
                if (match(entry, endpoint, operation)) {
                    if (AuthorizationEntry.Type.Add == entry.getType()) {
                        acls.addAll(entry.getAcls());
                    } else if (AuthorizationEntry.Type.Set == entry.getType()) {
                        acls.clear();
                        acls.addAll(entry.getAcls());
                    } else if (AuthorizationEntry.Type.Remove == entry.getType()) {
                        acls.removeAll(entry.getAcls());
                    }
                }
            }
            cache.put(key, acls);
        }
        return acls;
    }

    protected boolean match(AuthorizationEntry entry, String endpoint, QName operation) {
        return match(entry.getEndpoint(), endpoint)
            && (entry.getOperation() == null || operation == null || match(entry.getOperation(), operation));
    }

    private boolean match(QName acl, QName target) {
        return match(acl.getNamespaceURI(), target.getNamespaceURI())
            && match(acl.getLocalPart(), target.getLocalPart());
    }

    private boolean match(String acl, String target) {
        return acl == null
            || acl.equals("*")
            || Pattern.matches(acl, target);
    }

}
