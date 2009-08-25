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

import java.io.ObjectInputStream;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;

import org.fusesource.commons.management.ManagementStrategy;

public class MBeanServerWrapper implements MBeanServer {

    private MBeanServer delegate;
    private ManagementStrategy strategy;
    
    MBeanServerWrapper(MBeanServer delegate, ManagementStrategy strategy) {
        this.delegate = delegate;
        this.strategy = strategy;
    }
    
    public void addNotificationListener(ObjectName name,
            NotificationListener listener, NotificationFilter filter,
            Object handback) throws InstanceNotFoundException {
        delegate.addNotificationListener(name, listener, filter, handback);
    }

    public void addNotificationListener(ObjectName name, ObjectName listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        delegate.addNotificationListener(name, listener, filter, handback);
    }

    public ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException,
            MBeanRegistrationException, MBeanException,
            NotCompliantMBeanException {
        return delegate.createMBean(className, name);
    }

    public ObjectInstance createMBean(String className, ObjectName name,
            ObjectName loaderName) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanRegistrationException,
            MBeanException, NotCompliantMBeanException,
            InstanceNotFoundException {
        return delegate.createMBean(className, name, loaderName);
    }

    public ObjectInstance createMBean(String className, ObjectName name,
            Object[] params, String[] signature) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanRegistrationException,
            MBeanException, NotCompliantMBeanException {
        return delegate.createMBean(className, name, params, signature);
    }

    public ObjectInstance createMBean(String className, ObjectName name,
            ObjectName loaderName, Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException,
            MBeanRegistrationException, MBeanException,
            NotCompliantMBeanException, InstanceNotFoundException {
        return delegate.createMBean(className, name, loaderName, params, signature);
    }

    public ObjectInputStream deserialize(ObjectName name, byte[] data)
            throws InstanceNotFoundException, OperationsException {
        return delegate.deserialize(name, data);
    }

    public ObjectInputStream deserialize(String className, byte[] data)
            throws OperationsException, ReflectionException {
        return delegate.deserialize(className, data);
    }

    public ObjectInputStream deserialize(String className,
            ObjectName loaderName, byte[] data)
            throws InstanceNotFoundException, OperationsException,
            ReflectionException {
        return delegate.deserialize(className, loaderName, data);
    }

    public Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException,
            InstanceNotFoundException, ReflectionException {
        return delegate.getAttribute(name, attribute);
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException {
        return delegate.getAttributes(name, attributes);
    }

    public ClassLoader getClassLoader(ObjectName loaderName)
            throws InstanceNotFoundException {
        return delegate.getClassLoader(loaderName);
    }

    public ClassLoader getClassLoaderFor(ObjectName mbeanName)
            throws InstanceNotFoundException {
        return delegate.getClassLoaderFor(mbeanName);
    }

    public ClassLoaderRepository getClassLoaderRepository() {
        return delegate.getClassLoaderRepository();
    }

    public String getDefaultDomain() {
        return delegate.getDefaultDomain();
    }

    public String[] getDomains() {
        return delegate.getDomains();
    }

    public Integer getMBeanCount() {
        return delegate.getMBeanCount();
    }

    public MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException,
            ReflectionException {
        return delegate.getMBeanInfo(name);
    }

    public ObjectInstance getObjectInstance(ObjectName name)
            throws InstanceNotFoundException {
        return delegate.getObjectInstance(name);
    }

    public Object instantiate(String className) throws ReflectionException,
            MBeanException {
        return delegate.instantiate(className);
    }

    public Object instantiate(String className, ObjectName loaderName)
            throws ReflectionException, MBeanException,
            InstanceNotFoundException {
        return delegate.instantiate(className, loaderName);
    }

    public Object instantiate(String className, Object[] params,
            String[] signature) throws ReflectionException, MBeanException {
        return delegate.instantiate(className, params, signature);
    }

    public Object instantiate(String className, ObjectName loaderName,
            Object[] params, String[] signature) throws ReflectionException,
            MBeanException, InstanceNotFoundException {
        return delegate.instantiate(className, loaderName, params, signature);
    }

    public Object invoke(ObjectName name, String operationName,
            Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException,
            ReflectionException {
        return delegate.invoke(name, operationName, params, signature);
    }

    public boolean isInstanceOf(ObjectName name, String className)
            throws InstanceNotFoundException {
        return delegate.isInstanceOf(name, className);
    }

    public boolean isRegistered(ObjectName name) {
        return strategy.isManaged(null, name);
    }

    public Set queryMBeans(ObjectName name, QueryExp query) {
        return delegate.queryMBeans(name, query);
    }

    public Set queryNames(ObjectName name, QueryExp query) {
        return delegate.queryNames(name, query);
    }

    public ObjectInstance registerMBean(Object object, ObjectName name)
            throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {
        ObjectInstance instance = null;
        try {
            strategy.manageNamedObject(object, name);
            instance = new ObjectInstance(name.toString(), object.getClass().getName());
        } catch (MalformedObjectNameException mone) {
            // ignore
        } catch (Exception ex) {
            if (ex.getCause() instanceof InstanceAlreadyExistsException) {
                throw (InstanceAlreadyExistsException)ex.getCause();
            } else if (ex.getCause() instanceof MBeanRegistrationException) {
                throw (MBeanRegistrationException)ex.getCause();
            } if (ex.getCause() instanceof NotCompliantMBeanException) {
                throw (NotCompliantMBeanException)ex.getCause();
            } else {
                throw (RuntimeException)ex;
            }
        }
        return instance;
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException {
        delegate.removeNotificationListener(name, listener);
    }

    public void removeNotificationListener(ObjectName name,
            NotificationListener listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        delegate.removeNotificationListener(name, listener);
    }

    public void removeNotificationListener(ObjectName name,
            ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        delegate.removeNotificationListener(name, listener, filter, handback);
    }

    public void removeNotificationListener(ObjectName name,
            NotificationListener listener, NotificationFilter filter,
            Object handback) throws InstanceNotFoundException,
            ListenerNotFoundException {
        delegate.removeNotificationListener(name, listener, filter, handback);
    }

    public void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        delegate.setAttribute(name, attribute);
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException {
        return delegate.setAttributes(name, attributes);
    }

    public void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException {
        try {
            strategy.unmanageNamedObject(name);
        } catch (Exception ex) {
            if (ex.getCause() instanceof InstanceNotFoundException) {
                throw (InstanceNotFoundException)ex.getCause();
            } else if (ex.getCause() instanceof MBeanRegistrationException) {
                throw (MBeanRegistrationException)ex.getCause();
            } else {
                throw (RuntimeException)ex;
            }
        }

    }

}
