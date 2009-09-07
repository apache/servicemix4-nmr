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

import java.util.Properties;

import javax.naming.spi.InitialContextFactoryBuilder;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.apache.xbean.naming.context.WritableContext;
import org.apache.xbean.naming.global.GlobalContextManager;

/**
 */
public class Activator implements BundleActivator {

    private OSGiInitialContextFactoryBuilder osgiIcfb;
    private GlobalInitialContextFactoryBuilder globalIcfb;
    private ServiceRegistration registration;

    public void start(BundleContext bundleContext) throws Exception {
        osgiIcfb = new OSGiInitialContextFactoryBuilder(bundleContext, new OSGiContext(new OSGiServicesContext(bundleContext)));
        globalIcfb = new GlobalInitialContextFactoryBuilder();
        GlobalContextManager.setGlobalContext(new WritableContext());
        Properties props = new Properties();
        props.put(Constants.SERVICE_RANKING, Integer.valueOf(-1));
        registration = bundleContext.registerService(InitialContextFactoryBuilder.class.getName(), globalIcfb, props);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        registration.unregister();
        osgiIcfb.destroy();
        GlobalContextManager.setGlobalContext(null);
    }
}
