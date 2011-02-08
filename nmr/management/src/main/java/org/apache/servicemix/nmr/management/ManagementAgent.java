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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.servicemix.nmr.management.stats.CountStatistic;
import org.apache.servicemix.nmr.management.stats.TimeStatistic;
import org.fusesource.commons.management.ManagementStrategy;
import org.fusesource.commons.management.Statistic;
import org.fusesource.commons.management.Statistic.UpdateMode;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ManagementAgent implements ManagementStrategy {

    private final Logger logger = LoggerFactory.getLogger(ManagementAgent.class);

    private boolean enabled;
    private MBeanServer mbeanServer;
    private Map<ObjectName, Object> mbeans = new HashMap<ObjectName, Object>();
    private NamingStrategy namingStrategy;
    private BundleContext bundleContext;
    private ServiceRegistration serviceRegistration;

    public ManagementAgent() {
    }

    /**
     * @see org.fusesource.commons.management.ManagementStrategy#manageObject(java.lang.Object)
     */
    public synchronized void manageObject(Object managedObject) throws Exception {
        ObjectName objectName = getManagedObjectName(managedObject, null, ObjectName.class);
        manageNamedObject(managedObject, objectName);
    }

    /**
     * @see org.fusesource.commons.management.ManagementStrategy#getManagedObjectName(java.lang.Object, java.lang.String, java.lang.Class)
     */
    public synchronized <T> T getManagedObjectName(Object managableObject,
                                                   String customName,
                                                   Class<T> nameType) throws Exception {
        return String.class.equals(nameType) && managableObject == null && customName == null
                ? nameType.cast(namingStrategy.getJmxDomainName())
                : ObjectName.class.equals(nameType)
                ? nameType.cast(getTypeSpecificObjectName(managableObject, customName))
                : null;
    }

    /**
     * @see org.fusesource.commons.management.ManagementStrategy#manageNamedObject(java.lang.Object, java.lang.Object)
     */
    public synchronized void manageNamedObject(Object managedObject, Object preferredName) throws Exception {
        managedObject = getTypeSpecificManagedObject(managedObject);
        if (preferredName instanceof ObjectName && managedObject != null) {
            try {
                register(managedObject, (ObjectName) preferredName);
            } catch (Exception ex) {
                throw (JMException) new JMException(ex.getMessage()).initCause(ex);
            }
        }
    }

    /**
     * @see org.fusesource.commons.management.ManagementStrategy#unmanageObject(java.lang.Object)
     */
    public synchronized void unmanageObject(Object managedObject) throws Exception {
        ObjectName objectName = getManagedObjectName(managedObject, null, ObjectName.class);
        unmanageNamedObject(objectName);
    }

    /**
     * @see org.fusesource.commons.management.ManagementStrategy#unmanageNamedObject(java.lang.Object)
     */
    public synchronized void unmanageNamedObject(Object name) throws Exception {
        if (name instanceof ObjectName) {
            unregister((ObjectName) name);
        }
    }

    /**
     * @see org.fusesource.commons.management.ManagementStrategy#isManaged(java.lang.Object, java.lang.Object)
     */
    public synchronized boolean isManaged(Object managableObject, Object name) {
        try {
            return managableObject != null
                    ? getMbeanServer().isRegistered(
                    getManagedObjectName(managableObject, null, ObjectName.class))
                    : name != null && name instanceof ObjectName
                    ? getMbeanServer().isRegistered((ObjectName) name)
                    : false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @see org.fusesource.commons.management.ManagementStrategy#createStatistic(java.lang.String, java.lang.Object, UpdateMode)
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
        logger.trace(event.toString());
    }

    public void setBundleContext(BundleContext ctx) {
        bundleContext = ctx;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setEnabled(boolean b) {
        enabled = b;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MBeanServer getMbeanServer() {
        return mbeanServer;
    }

    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public synchronized void bindMBeanServer(ServiceReference reference) throws Exception {
        if (isEnabled()) {
            MBeanServer mbeanServer = (MBeanServer) bundleContext.getService(reference);
            bundleContext.ungetService(reference); // do not keep the reference count, as it's done by blueprint
            if (mbeanServer != this.mbeanServer) {
                unregisterObjects();
                this.mbeanServer = mbeanServer;
                registerObjects();
            }
            registerService();
        }
    }

    public synchronized void unbindMBeanServer(ServiceReference reference) {
        if (isEnabled()) {
            unregisterObjects();
            this.mbeanServer = null;
            unregisterService();
        }
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    protected void registerObjects() {
        ObjectName[] mBeans = mbeans.keySet().toArray(new ObjectName[mbeans.size()]);
        int caught = 0;
        for (ObjectName name : mBeans) {
            try {
                register(mbeans.get(name), name);
            } catch (JMException jmex) {
                logger.info("Exception unregistering MBean", jmex);
                caught++;
            } catch (ServiceUnavailableException sue) {
                // due to timing / shutdown ordering issue that we may
                // ignore as not unregistering from an already shutdown
                // blueprint container is quite harmless
            }
        }
        if (caught > 0) {
            logger.warn("A number of " + caught
                    + " exceptions caught while unregistering MBeans during stop operation.  "
                    + "See INFO log for details.");
        }
    }

    protected void register(Object obj, ObjectName name) throws JMException {
        register(obj, name, !(obj instanceof ManagedEndpoint));
    }

    protected void register(Object obj, ObjectName name, boolean forceRegistration) throws JMException {
        if (mbeanServer == null) {
            mbeans.put(name, obj);
        } else {
            try {
                registerMBeanWithServer(obj, name, forceRegistration);
            } catch (UndeclaredThrowableException ute) {
                if (ute.getCause() instanceof RuntimeException) {
                    logger.warn("MBean registration failed: ", ute.getCause());
                    throw (RuntimeException) ute.getCause();
                } else {
                    logger.warn("MBean registration failed: ", ute.getCause());
                    throw new JMException(ute.getCause().getMessage());
                }
            }
        }
    }

    protected void unregisterObjects() {
        ObjectName[] mBeans = mbeans.keySet().toArray(new ObjectName[mbeans.size()]);
        int caught = 0;
        for (ObjectName name : mBeans) {
            try {
                unregister(name);
            } catch (JMException jmex) {
                logger.info("Exception unregistering MBean", jmex);
                caught++;
            } catch (ServiceUnavailableException sue) {
                // due to timing / shutdown ordering issue that we may
                // ignore as not unregistering from an already shutdown
                // blueprint container is quite harmless
            }
        }
        if (caught > 0) {
            logger.warn("A number of " + caught
                    + " exceptions caught while unregistering MBeans during stop operation.  "
                    + "See INFO log for details.");
        }
    }

    protected void unregister(ObjectName name) throws JMException {
        mbeans.remove(name);
        if (mbeanServer != null) {
            mbeanServer.unregisterMBean(name);
        }
    }

    protected void registerMBeanWithServer(Object obj, ObjectName name, boolean forceRegistration) throws JMException {
        ObjectInstance instance;
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
            mbeans.put(name, obj);
        }
    }

    protected void registerService() {
        serviceRegistration = getBundleContext().registerService("org.fusesource.commons.management.ManagementStrategy",
                this, null);
    }

    protected void unregisterService() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

    protected ObjectName getTypeSpecificObjectName(Object mo, String customName) throws MalformedObjectNameException {
        return mo instanceof ManagedEndpoint
                ? namingStrategy.getObjectName((ManagedEndpoint) mo)
                : mo instanceof Nameable
                ? (customName != null
                ? namingStrategy.getCustomObjectName(customName, ((Nameable) mo).getName())
                : namingStrategy.getObjectName((Nameable) mo))
                : null;
    }


    protected Object getTypeSpecificManagedObject(Object object) throws NotCompliantMBeanException {
        return object instanceof ManagedEndpoint
                ? object
                : object instanceof Nameable
                ? new StandardMBean(object, ((Nameable) object).getPrimaryInterface())
                : object;
    }
}
