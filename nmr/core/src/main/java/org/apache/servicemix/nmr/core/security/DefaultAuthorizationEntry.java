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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.servicemix.nmr.api.security.GroupPrincipal;
import org.apache.servicemix.nmr.api.security.AuthorizationEntry;

/**
 * A simple authorization entry
 *
 * @author gnodet
 */
public class DefaultAuthorizationEntry implements AuthorizationEntry {

    private Set<GroupPrincipal> acls;
    private String endpoint;
    private QName operation;
    private Type type = Type.Add;
    private int rank;

    public DefaultAuthorizationEntry() {
    }

    public DefaultAuthorizationEntry(String endpoint, QName operation, String roles) {
        this.endpoint = endpoint;
        this.operation = operation;
        this.acls = buildRoles(roles);
    }

    public DefaultAuthorizationEntry(String endpoint, QName operation, String roles, Type type) {
        this.endpoint = endpoint;
        this.operation = operation;
        this.acls = buildRoles(roles);
        this.type = type;
    }

    public DefaultAuthorizationEntry(String endpoint, QName operation, String roles, Type type, int rank) {
        this.endpoint = endpoint;
        this.operation = operation;
        this.acls = buildRoles(roles);
        this.type = type;
        this.rank = rank;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return the rank
     */
    public int getRank() {
        return rank;
    }

    /**
     * @param rank the rank to set
     */
    public void setRank(int rank) {
        this.rank = rank;
    }

    /**
     * @return the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint the endpoint to set
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return the service
     */
    /**
     * @return the operation
     */
    public QName getOperation() {
        return operation;
    }

    /**
     * @param operation the operation to set
     */
    public void setOperation(QName operation) {
        this.operation = operation;
    }

    /**
     * @return the acls
     */
    public Set<GroupPrincipal> getAcls() {
        return acls;
    }

    /**
     * @param acls the acls to set
     */
    public void setAcls(Set<GroupPrincipal> acls) {
        this.acls = acls;
    }

    public void setRoles(String roles) {
        this.acls = buildRoles(roles);
    }

    public String getRoles() {
        StringBuffer sb = new StringBuffer();
        if (this.acls != null) {
            for (Iterator<GroupPrincipal> iter = this.acls.iterator(); iter.hasNext();) {
                GroupPrincipal p = iter.next();
                sb.append(p);
                if (iter.hasNext()) {
                    sb.append(",");
                }
            }
        }
        return sb.toString();
    }

    public String toString() {
        return "AuthorizationEntry[endpoint=" + endpoint + ", roles=" + getRoles() + ",type=" + type + "]";
    }

    private Set<GroupPrincipal> buildRoles(String roles) {
        Set<GroupPrincipal> s = new HashSet<GroupPrincipal>();
        StringTokenizer iter = new StringTokenizer(roles, ",");
        while (iter.hasMoreTokens()) {
            String name = iter.nextToken().trim();
            s.add(new GroupPrincipal(name));
        }
        return s;
    }
}
