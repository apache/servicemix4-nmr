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
package org.apache.servicemix.jbi.cluster.requestor;

/**
 * Variant of the standard JMS {@link javax.jms.MessageListener} interface,
 * offering not only the received Message but also the underlying
 * JMS Session object. The latter can be used to send reply messages,
 * without the need to access an external Connection/Session,
 * i.e. without the need to access the underlying ConnectionFactory.
 *
 * @see javax.jms.MessageListener
 */
public interface JmsRequestorListener {

    /**
     * Callback for processing a received JMS message.
     * <p>Implementors are supposed to process the given Message,
     * typically sending reply messages through the given Session.
     * @param requestor the underlying JMS Session (never <code>null</code>)
     * @throws Exception if the message can not be processed
     */
    void onMessage(JmsRequestor requestor) throws Exception;
}
