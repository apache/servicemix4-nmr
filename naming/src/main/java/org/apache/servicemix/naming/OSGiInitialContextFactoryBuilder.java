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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * An InitialContextFactoryBuilder which delegates to any InitialContextFactoryBuilder found
 * in the OSGi registry.
 */
public class OSGiInitialContextFactoryBuilder extends ServiceTracker implements InitialContextFactoryBuilder, InitialContextFactory {

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

    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment)
        throws NamingException {
        return this;
    }

    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        Context toReturn = null;

        ServiceReference ref = context.getServiceReference(InitialContextFactoryWrapper.class.getName());

        if (ref != null) {
          try {
              InitialContextFactoryWrapper icf = (InitialContextFactoryWrapper) context.getService(ref);

              if (icf != null) {
                  toReturn = icf.getInitialContext(environment);
              }
          }
          finally {
            context.ungetService(ref);
          }
        }

        if (toReturn == null) {
            toReturn  = new InitialContextWrapper(createContext(environment), osgiContext, environment);
        }

      return toReturn;
    }

    /**
     * This method was borrowed from Aries.  Will eventually be replaced by Aries impl.
     * @param env
     * @return
     * @throws NamingException
     */
    public Context createContext (Hashtable<?,?> env) throws NamingException {

        InitialContextFactory icf = null;
        ServiceReference ref = null;

        String icfFactory = (String) env.get(Context.INITIAL_CONTEXT_FACTORY);

        boolean icfFactorySet = true;

        if (icfFactory == null) {
            icfFactory = InitialContextFactory.class.getName();
            icfFactorySet = false;
        }

        try {
            ServiceReference[] refs = context.getAllServiceReferences(icfFactory, null);
            if (refs != null) {
                ref = refs[0];
                icf = (InitialContextFactory) context.getService(ref);
            }
        } catch (InvalidSyntaxException e) {
            NamingException e4 = new NamingException("Argh this should never happen :)");
            e4.initCause(e);

            throw e4;
        }

        if (icf == null) {
            try {
                ServiceReference[] refs = context.getAllServiceReferences(InitialContextFactoryBuilder.class.getName(), null);

                if (refs != null) {
                    for (ServiceReference icfbRef : refs) {
                        InitialContextFactoryBuilder builder = (InitialContextFactoryBuilder) context.getService(icfbRef);

                        icf = builder.createInitialContextFactory(env);

                        context.ungetService(icfbRef);
                        if (icf != null) {
                            break;
                        }
                    }
                }
            } catch (InvalidSyntaxException e) {
                NamingException e4 = new NamingException("Argh this should never happen :)");
                e4.initCause(e);
                throw e4;
            }
        }

        if (icf == null && icfFactorySet) {
            try {
                Class<?> clazz = Class.forName(icfFactory, true, Thread.currentThread().getContextClassLoader());
                icf = (InitialContextFactory) clazz.newInstance();
            } catch (ClassNotFoundException e11) {
                NamingException e = new NamingException("Argh this should never happen :)");
                e.initCause(e11);
                throw e;
            } catch (InstantiationException e2) {
                NamingException e4 = new NamingException("Argh this should never happen :)");
                e4.initCause(e2);
                throw e4;
            } catch (IllegalAccessException e1) {
                NamingException e4 = new NamingException("Argh this should never happen :)");
                e4.initCause(e1);
                throw e4;
            }
        }

        if (icf == null) {
            NamingException e3 = new NoInitialContextException("We could not find an InitialContextFactory to use");

            throw e3;
        }

        Context ctx = icf.getInitialContext(env);

        if (ref != null) context.ungetService(ref);

        if (ctx == null) {
          NamingException e = new NamingException("The ICF returned a null context");
          throw e;
        }

        return ctx;

    }


}
