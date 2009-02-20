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

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;

import org.apache.xbean.naming.context.ImmutableContext;
import org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.util.ClassUtils;
import org.osgi.framework.BundleContext;

/**
 * A read-only JNDI context that allows access to OSGi services in the registry.
 * The result of a lookup will be a proxy to the filtered OSGi service.
 */
public class OSGiContext extends ImmutableContext implements BundleContextAware {

    private final Context services = new OSGiServicesContext();
    private BundleContext bundleContext;

    public OSGiContext() throws NamingException {
        super(Collections.<String, Object>emptyMap());
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public Object lookup(String name) throws NamingException {
        if (name.startsWith("osgi:")) {
            name = name.substring(5);
            if (name.length() == 0) {
                return this;
            }
            if ("services".equals(name)) {
                return services;
            } else if (name.startsWith("services/")) {
                return services.lookup(name.substring(9));
            } else if ("/services".equals(name)) {
                return services;
            } else if (name.startsWith("/services/")) {
                return services.lookup(name.substring(10));
            } else {
                 throw new NameNotFoundException("Unrecognized name, does not start with expected 'services': " + name);
             }
        }
        return super.lookup(name);
    }

    public class OSGiServicesContext extends ImmutableContext {

        public OSGiServicesContext() throws NamingException {
            super(Collections.<String, Object>emptyMap());
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
                OsgiServiceProxyFactoryBean factory = new OsgiServiceProxyFactoryBean();
                factory.setBundleContext(getBundleContext());
                factory.setBeanClassLoader(classLoader);
                factory.setFilter(filter);
                factory.setInterfaces(new Class[] { ClassUtils.resolveClassName(className, classLoader) });
                factory.afterPropertiesSet();
                return factory.getObject();
            } catch (org.springframework.osgi.service.ServiceUnavailableException e) {
                NameNotFoundException ex = new NameNotFoundException(e.getMessage());
                ex.initCause(e);
                throw ex;
            }
        }

    }
}
