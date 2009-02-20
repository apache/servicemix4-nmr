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

import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.NamingManager;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.xbean.naming.context.ContextFlyweight;

/**
 */
public class InitialContextFactoryWrapper implements InitialContextFactory {

    private final InitialContextFactory delegate;
    private final Context osgiContext;

    public InitialContextFactoryWrapper(InitialContextFactory delegate, Context osgiContext) {
        this.delegate = delegate;
        this.osgiContext = osgiContext;
    }

    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new ContextWrapper(delegate.getInitialContext(environment));
    }

    public class ContextWrapper extends ContextFlyweight {

        private final Context delegate;

        public ContextWrapper(Context delegate) {
            this.delegate = delegate;
        }

        protected Context getContext() throws NamingException {
            return delegate;
        }

        public Object lookup(String name) throws NamingException {
            if (name == null || name.length() == 0) {
                return this;
            } 
            
            if (name.startsWith("osgi:")) {
                return osgiContext.lookup(name);
            }
            
            int sep = name.indexOf(':');
            if (sep >=0 ) {
                String scheme = name.substring(0, sep);  
                Context ctx = NamingManager.getURLContext(scheme, getContext().getEnvironment());
                if (ctx != null) {
                    return ctx.lookup(name);
                }                    
            }            
            
            return delegate.lookup(name);
        }

    }
}
