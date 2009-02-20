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
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * A wrapper around an InitialContextFactory used to ensure the InitialContext returned by
 * the factory is correctly wrapped to allow access to the OSGi context and other URL contexts. 
 */
public class InitialContextFactoryWrapper implements InitialContextFactory {

    private final InitialContextFactory delegate;
    private final Context osgiContext;

    public InitialContextFactoryWrapper(InitialContextFactory delegate, Context osgiContext) {
        this.delegate = delegate;
        this.osgiContext = osgiContext;
    }

    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return new InitialContextWrapper(delegate.getInitialContext(environment), osgiContext, environment);
    }

}
