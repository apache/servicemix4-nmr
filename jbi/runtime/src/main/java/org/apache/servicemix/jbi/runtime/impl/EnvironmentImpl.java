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

import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;

import org.apache.servicemix.jbi.runtime.Environment;
import org.fusesource.commons.management.ManagementStrategy;
import org.fusesource.commons.management.Statistic;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 */
public class EnvironmentImpl implements Environment {

    private BundleContext bundleContext;
    private Object transactionManager;
    private InitialContext namingContext;

    private ManagementStrategyWrapper managementStrategy = new ManagementStrategyWrapper();
    private ManagementStrategy currentMs;
    private ServiceReference currentMsRef;
    private MBeanServerWrapper mbeanServer = new MBeanServerWrapper(null, managementStrategy);

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ManagementStrategy getManagementStrategy() {
        return managementStrategy;
    }

    public void bindManagementStrategy(ManagementStrategy ms) {
        managementStrategy.setDelegate(ms);
    }

    public void bindManagementStrategy(ServiceReference reference) {
        ManagementStrategy ms = (ManagementStrategy) bundleContext.getService(reference);
        managementStrategy.updateRef(currentMs, ms);
        if (currentMsRef != null) {
            bundleContext.ungetService(currentMsRef);
        }
        currentMs = ms;
        currentMsRef = reference;
    }

    public void unbindManagementStrategy(ServiceReference ref) {
        managementStrategy.updateRef(currentMs, null);
        if (currentMsRef != null) {
            bundleContext.ungetService(currentMsRef);
        }
        currentMs = null;
        currentMsRef = null;
    }

    public MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    public void bindMBeanServer(MBeanServer mbs) {
        mbeanServer.setDelegate(mbs);
    }

    public void unbindMBeanServer(MBeanServer mbs) {
    }

    public Object getTransactionManager() {
        return transactionManager;
    }

    public void bindTransactionManager(ServiceReference reference) {
        transactionManager = bundleContext.getService(reference);
        bundleContext.ungetService(reference); // do not keep the reference count, as it's done by blueprint
    }

    public void unbindTransactionManager(ServiceReference reference) {
        transactionManager = null;
    }

    public InitialContext getNamingContext() {
        return namingContext;
    }

    public void setNamingContext(InitialContext namingContext) {
        this.namingContext = namingContext;
    }

    public ObjectName getManagedObjectName(Object object) throws Exception {
        return getManagementStrategy().getManagedObjectName(object, null, ObjectName.class);
    }

    public ObjectName getManagedObjectName(Object object, String customName) throws Exception {
        return getManagementStrategy().getManagedObjectName(object, customName, ObjectName.class);
    }

    public String getJmxDomainName() throws Exception {
        return getManagementStrategy().getManagedObjectName(null, null, String.class);
    }

    public void manageObject(Object managedObject) throws Exception {
        getManagementStrategy().manageObject(managedObject);
    }

    public void unmanageObject(Object managedObject) throws Exception {
        getManagementStrategy().unmanageObject(managedObject);
    }

    public void unmanageNamedObject(ObjectName name) throws Exception {
        getManagementStrategy().unmanageNamedObject(name);
    }

    public boolean isManaged(Object managedObject) {
        return getManagementStrategy().isManaged(managedObject, null);
    }

    public void notify(EventObject event) throws Exception {
        getManagementStrategy().notify(event);
    }


    public static class ManagementStrategyWrapper implements ManagementStrategy {

        private ManagementStrategy delegate;
        private boolean isValid;
        private Set<Object> managedObjects = new HashSet<Object>();

        public synchronized ManagementStrategy getDelegate() {
            return delegate;
        }

        public synchronized void setDelegate(ManagementStrategy delegate) {
            this.delegate = delegate;
        }

        public synchronized void updateRef(ManagementStrategy oldMs, ManagementStrategy newMs) {
            if (oldMs != newMs) {
                if (oldMs != null) {
                    for (Object managedObject : managedObjects) {
                        try {
                            oldMs.unmanageObject(managedObject);
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }
                if (newMs != null) {
                    for (Object managedObject : managedObjects) {
                        try {
                            newMs.manageObject(managedObject);
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }
            }
            isValid = newMs != null;
        }

        public synchronized void manageObject(Object managedObject) throws Exception {
            if (isValid) {
                delegate.manageObject(managedObject);
            }
            managedObjects.add(managedObject);
        }

        public synchronized void manageNamedObject(Object managedObject, Object preferedName) throws Exception {
            delegate.manageNamedObject(managedObject, preferedName);
        }

        public synchronized <T> T getManagedObjectName(Object managableObject, String customName, Class<T> nameType) throws Exception {
            return delegate.getManagedObjectName(managableObject, customName, nameType);
        }

        public synchronized void unmanageObject(Object managedObject) throws Exception {
            if (isValid) {
                delegate.unmanageObject(managedObject);
            }
            managedObjects.remove(managedObject);
        }

        public synchronized void unmanageNamedObject(Object name) throws Exception {
            delegate.unmanageNamedObject(name);
        }

        public synchronized boolean isManaged(Object managableObject, Object name) {
            if (isValid) {
                return delegate.isManaged(managableObject, name);
            } else if (managableObject != null) {
                return managedObjects.contains(managableObject);
            } else {
                return false;
            }
        }

        public synchronized void notify(EventObject event) throws Exception {
            delegate.notify(event);
        }

        public synchronized Statistic createStatistic(String name, Object owner, Statistic.UpdateMode updateMode) {
            return delegate.createStatistic(name, owner, updateMode);
        }
    }
}
