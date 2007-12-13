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
package org.apache.servicemix.nmr.spring;

import junit.framework.TestCase;
import org.osgi.framework.*;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.apache.servicemix.nmr.spring.BundleExtUrlPostProcessor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;

public class BundleExtTest extends TestCase {

    public void test() {
        final long bundleId = 32;
        BundleExtUrlPostProcessor processor = new BundleExtUrlPostProcessor();
        processor.setBundleContext((BundleContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                               new Class[] { BundleContext.class },
                               new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("getBundle".equals(method.getName())) {
                    return (Bundle) Proxy.newProxyInstance(getClass().getClassLoader(),
                                                           new Class[] { Bundle.class },
                                                           new InvocationHandler() {
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if ("getBundleId".equals(method.getName())) {
                                return bundleId;
                            }
                            return null;
                        }
                    });
                }
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        }));
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] {"bundle.xml"}, false);
        ctx.addBeanFactoryPostProcessor(processor);
        ctx.refresh();
        Object str = ctx.getBean("string");
        System.err.println(str);
        assertNotNull(str);
        assertEquals("bundle://" + bundleId + "///schema.xsd", str);
    }

}