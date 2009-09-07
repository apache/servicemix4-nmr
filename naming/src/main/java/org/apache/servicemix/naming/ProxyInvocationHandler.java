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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.naming.ServiceUnavailableException;

import org.osgi.util.tracker.ServiceTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

/**
 * A very simple proxy to be used as the return value when looking for an OSGi service.
 * This proxy is very limited and the ServicerReference of the OSGi service will only
 * be released when this object is garbage collected.
 * If the underlying OSGi service is removed from the registry, the proxy will
 * wait for a new service to come up for a certain amount of time before throwing
 * an exception.
 */
public class ProxyInvocationHandler implements InvocationHandler {

    public static final long TIMEOUT = 5000L;

    private String listenerFilter;
    private ServiceTracker tracker;

    public ProxyInvocationHandler(BundleContext bundleContext, String filter) throws InvalidSyntaxException {
        this.listenerFilter = filter;
        this.tracker = new ServiceTracker(bundleContext, bundleContext.createFilter(filter), null);
        this.tracker.open();
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(getTarget(true), args);
    }

    public Object getTarget(boolean wait) throws InterruptedException, ServiceUnavailableException {
        Object svc = tracker.getService();
        if (svc == null && wait) {
            svc = tracker.waitForService(TIMEOUT);
        }
        if (svc == null) {
            throw new ServiceUnavailableException(listenerFilter);
        }
        return svc;
    }

    protected void finalize() throws Throwable {
        tracker.close();
        super.finalize();
    }
}
