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
package javax.jbi.messaging;

import java.net.URI;

import javax.jbi.servicedesc.ServiceEndpoint;

import javax.xml.namespace.QName;

public interface MessageExchange {
    String JTA_TRANSACTION_PROPERTY_NAME = "javax.jbi.transaction.jta";

    URI getPattern();

    String getExchangeId();

    ExchangeStatus getStatus();

    void setStatus(ExchangeStatus status) throws MessagingException;

    void setError(Exception error);

    Exception getError();

    Fault getFault();

    void setFault(Fault fault) throws MessagingException;

    NormalizedMessage createMessage() throws MessagingException;

    Fault createFault() throws MessagingException;

    NormalizedMessage getMessage(String name);

    void setMessage(NormalizedMessage msg, String name) throws MessagingException;

    Object getProperty(String name);

    void setProperty(String name, Object obj);

    void setEndpoint(ServiceEndpoint endpoint);

    void setService(QName service);

    void setInterfaceName(QName interfaceName);

    void setOperation(QName name);

    ServiceEndpoint getEndpoint();

    QName getInterfaceName();

    QName getService();

    QName getOperation();

    boolean isTransacted();

    Role getRole();

    java.util.Set getPropertyNames();

    public static final class Role {
        public static final Role PROVIDER = new Role();

        public static final Role CONSUMER = new Role();

        private Role() {
        }
    }
}
