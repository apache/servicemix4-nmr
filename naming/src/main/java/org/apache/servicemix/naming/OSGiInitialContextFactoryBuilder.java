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
package org.apache.servicemix.naming;

import java.util.Hashtable;
import java.lang.reflect.Field;

import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.NamingManager;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.NoInitialContextException;

import org.osgi.util.tracker.ServiceTracker;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;

/**
 * An InitialContextFactoryBuilder which delegates to any InitialContextFactoryBuilder found
 * in the OSGi registry. 
 */
public class OSGiInitialContextFactoryBuilder implements InitialContextFactoryBuilder, BundleContextAware,
                                                         InitializingBean, DisposableBean {

    private BundleContext bundleContext;
    private Context osgiContext;
    private ServiceTracker tracker;

    public OSGiInitialContextFactoryBuilder() {
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public Context getOsgiContext() {
        return osgiContext;
    }

    public void setOsgiContext(Context osgiContext) {
        this.osgiContext = osgiContext;
    }

    public void afterPropertiesSet() throws NamingException {
        Assert.notNull(bundleContext, "Required 'bundleContext' property was not set.");
        Assert.notNull(osgiContext, "Required 'osgiContext' property was not set.");
        tracker = new ServiceTracker(bundleContext, InitialContextFactoryBuilder.class.getName(), null);
        tracker.open();

        NamingManager.setInitialContextFactoryBuilder(this);
    }

    public void destroy() {
        // Close the tracker
        tracker.close();
        tracker = null;

        // Try to reset the InitialContextFactoryBuilder on the NamingManager
        // As there is no public API to do so, we try using reflection.
        // The following code will try to nullify all static fields of type
        // InitialContextFactoryBuilder on the NamingManager class.
        try {
            for (Field field : NamingManager.class.getDeclaredFields()) {
                if (InitialContextFactoryBuilder.class.equals(field.getType())) {
                    field.setAccessible(true);
                    field.set(null, null);
                }
            }
        } catch (Throwable t) {
            // Ignore
        }
    }

    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> env) throws NamingException {
        if (tracker == null) {
            throw new IllegalStateException("OSGiInitialContextFactoryBuilder is not initialized");
        }
        InitialContextFactory factory = null;
        InitialContextFactoryBuilder factoryBuilder = (InitialContextFactoryBuilder) tracker.getService();
        if (factoryBuilder != null) {
            factory = factoryBuilder.createInitialContextFactory(env);
        }
        if (factory == null && env != null) {
            String className = (String) env.get(Context.INITIAL_CONTEXT_FACTORY);
            if (className != null) {
                try {
                    factory = (InitialContextFactory) Class.forName(className).newInstance();
                } catch (Exception e) {
                    NoInitialContextException ne = new NoInitialContextException("Cannot instantiate class: " + className);
                    ne.setRootCause(e);
                    throw ne;
                }
            }
        }
        if (factory == null) {
            NoInitialContextException ne = new NoInitialContextException(
                "Need to specify class name in environment or system " +
                "property, or as an applet parameter, or in an " +
                "application resource file:  " +
                Context.INITIAL_CONTEXT_FACTORY);
            throw ne;
        }
        return new InitialContextFactoryWrapper(factory, osgiContext);
    }

}
