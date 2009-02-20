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

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.spi.NamingManager;

/**
 * InitialContext wrapper allowing the use of the default context provided, the OSGi context
 * or any URL context supported by the JVM.
 */
public class InitialContextWrapper extends InitialContext {

    public static final String OSGI_SCHEME = "osgi";

    private final Context delegate;
    private final Context osgiContext;

    public InitialContextWrapper(Context delegate, Context osgiContext, Hashtable<?, ?> environment) throws NamingException {
        super(environment);
        this.delegate = delegate;
        this.osgiContext = osgiContext;
    }

    @Override
    protected Context getDefaultInitCtx() throws NamingException {
        return delegate;
    }

    @Override
    protected Context getURLOrDefaultInitCtx(String name) throws NamingException {
        String scheme = getURLScheme(name);
        if (OSGI_SCHEME.equals(scheme)) {
            return osgiContext;
        } else if (scheme != null) {
            Context ctx = NamingManager.getURLContext(scheme, delegate.getEnvironment());
            if (ctx != null) {
                return ctx;
            }
        }
        return delegate;
    }

    @Override
    protected Context getURLOrDefaultInitCtx(Name name) throws NamingException {
        if (name.size() > 0) {
            String first = name.get(0);
            String scheme = getURLScheme(first);
            if (OSGI_SCHEME.equals(scheme)) {
                return osgiContext;
            } else if (scheme != null) {
                Context ctx = NamingManager.getURLContext(scheme, delegate.getEnvironment());
                if (ctx != null) {
                    return ctx;
                }
            }
        }
        return delegate;
    }

    private static String getURLScheme(String str) {
        int colon_posn = str.indexOf(':');
        int slash_posn = str.indexOf('/');
        if (colon_posn > 0 && (slash_posn == -1 || colon_posn < slash_posn)) {
            return str.substring(0, colon_posn);
        }
        return null;
    }

}
