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

import java.lang.reflect.Field;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * An InitialContextFactoryBuilder which delegates to any InitialContextFactoryBuilder found
 * in the OSGi registry.
 */
public class OSGiInitialContextFactoryBuilder extends ServiceTracker implements InitialContextFactoryBuilder {

    private Context osgiContext;

    public OSGiInitialContextFactoryBuilder(BundleContext bundleContext, Context osgiContext) throws NamingException {
        super(bundleContext, InitialContextFactoryBuilder.class.getName(), null);
        open();
        this.osgiContext = osgiContext;
        NamingManager.setInitialContextFactoryBuilder(this);
    }

    public void destroy() {
        // Close the tracker
        close();
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
        InitialContextFactory factory = null;
        InitialContextFactoryBuilder factoryBuilder = (InitialContextFactoryBuilder) getService();
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
