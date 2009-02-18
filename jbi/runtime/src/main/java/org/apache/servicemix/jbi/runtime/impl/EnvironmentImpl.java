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
package org.apache.servicemix.jbi.runtime.impl;

import java.util.List;

import javax.naming.InitialContext;
import javax.management.MBeanServer;

import org.apache.servicemix.jbi.runtime.Environment;

/**
 */
public class EnvironmentImpl implements Environment {

    private Object transactionManager;
    private List transactionManagers;
    private InitialContext namingContext;
    private List<InitialContext> namingContexts;
    private MBeanServer mbeanServer;
    private List<MBeanServer> mbeanServers;

    public MBeanServer getMBeanServer() {
        if (mbeanServer != null) {
            return mbeanServer;
        }
        if (mbeanServers != null && !mbeanServers.isEmpty()) {
            return mbeanServers.get(0);
        }
        return null;
    }

    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public List<MBeanServer> getMbeanServers() {
        return mbeanServers;
    }

    public void setMbeanServers(List<MBeanServer> mbeanServers) {
        this.mbeanServers = mbeanServers;
    }


    public Object getTransactionManager() {
        if (transactionManager != null) {
            return transactionManager;
        }
        if (transactionManagers != null && !transactionManagers.isEmpty()) {
            return transactionManagers.get(0);
        }
        return null;
    }

    public void setTransactionManager(Object transactionManager) {
        this.transactionManager = transactionManager;
    }

    public List getTransactionManagers() {
        return transactionManagers;
    }

    public void setTransactionManagers(List transactionManagers) {
        this.transactionManagers = transactionManagers;
    }

    public InitialContext getNamingContext() {
        if (namingContext != null) {
            return namingContext;
        }
        if (namingContexts != null && !namingContexts.isEmpty()) {
            return namingContexts.get(0);
        }
        return null;
    }

    public void setNamingContext(InitialContext namingContext) {
        this.namingContext = namingContext;
    }

    public List<InitialContext> getNamingContexts() {
        return namingContexts;
    }

    public void setNamingContexts(List namingContexts) {
        this.namingContexts = namingContexts;
    }
}
