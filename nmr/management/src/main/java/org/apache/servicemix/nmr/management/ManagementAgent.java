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
package org.apache.servicemix.nmr.management;

import java.util.EventObject;
import java.util.Set;
import java.util.HashSet;

import javax.jbi.component.ComponentContext;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.JMException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.InstanceAlreadyExistsException;
import javax.management.StandardMBean;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;
import javax.management.modelmbean.InvalidTargetObjectTypeException;

import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.beans.factory.DisposableBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.nmr.management.stats.CountStatistic;
import org.apache.servicemix.nmr.management.stats.TimeStatistic;
import org.fusesource.commons.management.ManagementStrategy;
import org.fusesource.commons.management.Statistic;
import org.fusesource.commons.management.Statistic.UpdateMode;

/**
 */
public class ManagementAgent implements ManagementStrategy, DisposableBean {

    private static final transient Log LOG = LogFactory.getLog(ManagementAgent.class);

    private MBeanServer mbeanServer;
    private MetadataMBeanInfoAssembler assembler;
    private Set<ObjectName> mbeans = new HashSet<ObjectName>();
    private NamingStrategy namingStrategy;

    public ManagementAgent() {
        assembler = new MetadataMBeanInfoAssembler();
        assembler.setAttributeSource(new AnnotationJmxAttributeSource());
    }

    /**
     * @see org.fusesource.commons.management.ManagementStrategy#manageObject(java.lang.Object)
     */
    public void manageObject(Object managedObject) throws Exception {
        ObjectName objectName = getManagedObjectName(managedObject, null, ObjectName.class);
        manageNamedObject(managedObject, objectName);
    }
    
    /**
     * @see org.fusesource.commons.management.ManagementStrategy#getObjectName(java.lang.Object)
     */
    public <T> T getManagedObjectName(Object managableObject, 
                                      String customName, 
                                      Class<T> nameType) throws Exception {
        return String.class.equals(nameType) && managableObject == null && customName == null
               ? nameType.cast(namingStrategy.getJmxDomainName())
               : ObjectName.class.equals(nameType) 
                 ? nameType.cast(getTypeSpecificObjectName(managableObject, customName))
                 : null;
    }

    /**
     * @see org.fusesource.commons.management.ManagementStrategy#manageNamedObject(java.lang.Object)
     */
    public void manageNamedObject(Object managedObject, Object preferredName) throws Exception {
        managedObject = getTypeSpecificManagedObject(managedObject);
        if (preferredName instanceof ObjectName && managedObject != null) {
            try {
                register(managedObject, (ObjectName)preferredName);
            } catch (Exception ex) {
                throw new JMException(ex.getMessage());
            }
        }
    }
    
    /**
     * @see org.fusesource.commons.management.ManagementStrategy#unmanageObject(java.lang.Object)
     */
    public void unmanageObject(Object managedObject) throws Exception {
        ObjectName objectName = getManagedObjectName(managedObject, null, ObjectName.class);
        unmanageNamedObject(objectName);
    }

    /**
     * @see org.fusesource.commons.management.ManagementStrategy#unmanageNamedObject(java.lang.Object)
     */
    public void unmanageNamedObject(Object name) throws Exception {
        if (name instanceof ObjectName) {
            unregister((ObjectName)name);
        }
    }
    
    /**
     * @see org.fusesource.commons.management.ManagementStrategy#isManaged(java.lang.Object)
     */
    public boolean isManaged(Object managableObject, Object name) {
        try {
            return managableObject != null 
                   ? getMbeanServer().isRegistered(
                         getManagedObjectName(managableObject, null, ObjectName.class))
                   : name != null && name instanceof ObjectName
                     ? getMbeanServer().isRegistered((ObjectName)name)
                     : false;
        } catch (Exception e) {
            return false;
        }    
    }

    /**
     * @see org.fusesource.commons.management.ManagementStrategy#createStatistic(java.lang.Object)
     */
    public Statistic createStatistic(String name, Object owner, UpdateMode updateMode) {
        return updateMode == UpdateMode.COUNTER
               ? new TimeStatistic(name, null)
               : updateMode == UpdateMode.VALUE
                 ? new CountStatistic(name, null)
                 : null;
    }

    /**
     * A place-holder implementation of notify that logs events to the commons
     * logging Log.
     */
    public void notify(EventObject event) throws Exception {
        if (LOG.isTraceEnabled()) {
            LOG.trace(event.toString());
        }
    }
 
    public MBeanServer getMbeanServer() {
        return mbeanServer;
    }

    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }
    
    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public void destroy() throws Exception {
        // Using the array to hold the busMBeans to avoid the
        // CurrentModificationException
        Object[] mBeans = mbeans.toArray();
        int caught = 0;
        for (Object name : mBeans) {
            mbeans.remove((ObjectName)name);
            try {
                unregister((ObjectName)name);
            } catch (JMException jmex) {
                LOG.info("Exception unregistering MBean", jmex);
                caught++;
            }
        }
        if (caught > 0) {
            LOG.warn("A number of " + caught
                     + " exceptions caught while unregistering MBeans during stop operation.  "
                     + "See INFO log for details.");
        }
    }

    public void register(Object obj, ObjectName name) throws JMException {
        register(obj, name, !(obj instanceof ManagedEndpoint));
    }

    public void register(Object obj, ObjectName name, boolean forceRegistration) throws JMException {
        try {
            registerMBeanWithServer(obj, name, forceRegistration);
        } catch (NotCompliantMBeanException e) {
            // If this is not a "normal" MBean, then try to deploy it using JMX
            // annotations
            ModelMBeanInfo mbi = assembler.getMBeanInfo(obj, name.toString());
            RequiredModelMBean mbean = (RequiredModelMBean) mbeanServer.instantiate(RequiredModelMBean.class.getName());
            mbean.setModelMBeanInfo(mbi);
            try {
                mbean.setManagedResource(obj, "ObjectReference");
            } catch (InvalidTargetObjectTypeException itotex) {
                throw new JMException(itotex.getMessage());
            }
            registerMBeanWithServer(mbean, name, forceRegistration);
        }
    }

    public void unregister(ObjectName name) throws JMException {
        mbeanServer.unregisterMBean(name);
    }

    private void registerMBeanWithServer(Object obj, ObjectName name, boolean forceRegistration) throws JMException {
        ObjectInstance instance = null;
        try {
            instance = mbeanServer.registerMBean(obj, name);
        } catch (InstanceAlreadyExistsException e) {
            if (forceRegistration) {
                mbeanServer.unregisterMBean(name);
                instance = mbeanServer.registerMBean(obj, name);
            } else {
                throw e;
            }
        }

        if (instance != null) {
            mbeans.add(name);
        }
    }
    
    private ObjectName getTypeSpecificObjectName(Object mo, String customName) throws MalformedObjectNameException {
        return mo instanceof ManagedEndpoint
               ? namingStrategy.getObjectName((ManagedEndpoint)mo)
               : mo instanceof Nameable
                 ? (customName != null
                    ? namingStrategy.getCustomObjectName(customName, ((Nameable)mo).getName())
                    : namingStrategy.getObjectName((Nameable)mo))
                 : mo instanceof ComponentContext
                   ? namingStrategy.getCustomObjectName(customName, 
                                                        ((ComponentContext)mo).getComponentName())
                   : null;
    }

    
    private Object getTypeSpecificManagedObject(Object object) throws NotCompliantMBeanException {
        return object instanceof ManagedEndpoint
               ? object
               : object instanceof Nameable
                 ? new StandardMBean(object, ((Nameable)object).getPrimaryInterface())
                 : null;
    }
}
