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

import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.InitialContextFactory;
import javax.naming.NamingException;
import javax.naming.Context;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.apache.xbean.naming.global.GlobalContextManager;

/**
 * An InitialContextFactoryBuilder used to return an XBean Naming context.
 */
public class GlobalInitialContextFactoryBuilder implements InitialContextFactoryBuilder, InitializingBean, DisposableBean {

    private Context globalContext;

    public Context getGlobalContext() {
        return globalContext;
    }

    public void setGlobalContext(Context globalContext) {
        this.globalContext = globalContext;
    }

    public void afterPropertiesSet() throws Exception {
        GlobalContextManager.setGlobalContext(globalContext);
    }

    public void destroy() throws Exception {
        GlobalContextManager.setGlobalContext(null);
    }

    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
        return new GlobalContextManager();
    }
}
