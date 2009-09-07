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

import java.util.Collections;
import java.lang.reflect.Proxy;

import javax.naming.NamingException;
import javax.naming.NameNotFoundException;

import org.apache.xbean.naming.context.ImmutableContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * The naming context used to access OSGi services
 */
public class OSGiServicesContext extends ImmutableContext {

    private BundleContext bundleContext;

    public OSGiServicesContext(BundleContext bundleContext) throws NamingException {
        super(Collections.<String, Object>emptyMap());
        this.bundleContext = bundleContext;
    }

    @Override
    public Object lookup(String name) throws NamingException {
        String[] parts = name.split("/");
        if (parts.length == 0 || parts.length > 2) {
            throw new NameNotFoundException("Unrecognized name, should be osgi:services:<interface>[/<filter>]");
        }
        String className = parts[0];
        String filter = parts.length == 2 ? parts[1] : null;
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            Class clazz = classLoader.loadClass(className);
            String clazzFilter = "(" + Constants.OBJECTCLASS + "=" + clazz.getName() + ")";
            if (filter == null) {
                filter = clazzFilter;
            } else {
                if (filter.startsWith("(")) {
                    filter = "(&" + clazzFilter + filter + ")";
                } else {
                    filter = "(&" + clazzFilter + "(" + filter + "))";
                }
            }
            ProxyInvocationHandler handler = new ProxyInvocationHandler(bundleContext, filter);
            handler.getTarget(false);
            Object instance = Proxy.newProxyInstance(classLoader, new Class[] { clazz }, handler);
            return instance;
        } catch (Exception e) {
            NameNotFoundException ex = new NameNotFoundException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }
}
