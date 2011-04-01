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
package org.apache.servicemix.nmr.core;

import junit.framework.TestCase;
import org.apache.servicemix.nmr.api.*;
import org.apache.servicemix.nmr.api.event.ExchangeListener;
import org.apache.servicemix.nmr.api.security.UserPrincipal;
import org.apache.servicemix.nmr.api.service.ServiceHelper;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import javax.security.auth.Subject;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.*;

/**
 * Test class to ensure the NMR handles Exchange with a security subject set on them correctly
 */
public class ExchangeWithSecuritySubjectTest extends TestCase {

    private NMR nmr;

    public void setUp() {
        ServiceMix smx = new ServiceMix();
        smx.init();
        nmr = smx;
    }

    /**
     * Ensure that endpoint code can be run as the passed in subject
     */
    public void testRunAsSubject() {
        // let's register the endpoint first, asking for the code to invoked on behalf of the subject
        SubjectCapturingEndpoint endpoint = new SubjectCapturingEndpoint();
        nmr.getEndpointRegistry().register(endpoint,
                                           ServiceHelper.createMap(Endpoint.NAME, "runas",
                                                                   Endpoint.RUN_AS_SUBJECT, "true"));


        Subject subject = new Subject();
        subject.getPrincipals().add(new UserPrincipal("VIP User"));

        Channel channel = nmr.createChannel();
        Exchange exchange = channel.createExchange(Pattern.InOnly);
        exchange.setTarget(
                nmr.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, "runas")));
        exchange.getIn().setSecuritySubject(subject);

        channel.sendSync(exchange);

        assertTrue("Endpoint should have been invoked 'as' the Subject",
                   endpoint.captures.contains(subject));
    }

    /**
     * Ensure that endpoint code is not invoked on behalf of the Subject if the option has not been
     * explicitly enabled first.
     */
    public void testDoNotRunAsSubject() {
        // let's register the endpoint first without asking for any special Subject handling
        SubjectCapturingEndpoint endpoint = new SubjectCapturingEndpoint();
        nmr.getEndpointRegistry().register(endpoint,
                                           ServiceHelper.createMap(Endpoint.NAME, "do_not_runas"));

        Subject subject = new Subject();
        subject.getPrincipals().add(new UserPrincipal("VIP User"));

        Channel channel = nmr.createChannel();
        Exchange exchange = channel.createExchange(Pattern.InOnly);
        exchange.setTarget(
                nmr.getEndpointRegistry().lookup(ServiceHelper.createMap(Endpoint.NAME, "do_not_runas")));
        exchange.getIn().setSecuritySubject(subject);

        channel.sendSync(exchange);

        assertTrue("Endpoint should not have been invoked 'as' the Subject",
                   endpoint.captures.isEmpty());
    }

    /*
     * Endpoint that captures the Subject it is being invoked as.
     */
    private static class SubjectCapturingEndpoint implements Endpoint {

        private Set<Subject> captures = new HashSet<Subject>();
        private Channel channel;

        public void process(Exchange exchange) {
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (subject != null) {
                captures.add(subject);
            }

            exchange.setStatus(Status.Done);
            channel.send(exchange);
        }

        public void setChannel(Channel channel) {
            this.channel = channel;
        }            
    }
}
