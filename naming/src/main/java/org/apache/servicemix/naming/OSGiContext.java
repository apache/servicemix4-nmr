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
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.xbean.naming.context.ImmutableContext;

/**
 * A read-only JNDI context that allows access to OSGi services in the registry.
 * The result of a lookup will be a proxy to the filtered OSGi service.
 */
public class OSGiContext extends ImmutableContext {

    private Context services;

    /**
     * Create an instanceof <code>OSGiContext</code>
     * @param osgiServicesContext the context to lookup OSGi services
     * @throws NamingException
     */
    public OSGiContext(Context osgiServicesContext) throws NamingException {
        super(Collections.<String, Object>emptyMap());
        this.services = osgiServicesContext;
    }

    /**
     * Retrieves the named object.
     * See {@link #lookup(javax.naming.Name)} for details.
     * @param name the name of the object to look up
     * @return	the object bound to <tt>name</tt>
     * @throws	NamingException if a naming exception is encountered
     */
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

}
