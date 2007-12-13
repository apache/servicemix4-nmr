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
package org.apache.servicemix.jbi.runtime.impl;

import org.apache.servicemix.nmr.api.Message;

import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.MessagingException;
import javax.activation.DataHandler;
import javax.xml.transform.Source;
import javax.security.auth.Subject;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Oct 5, 2007
 * Time: 4:11:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class NormalizedMessageImpl implements NormalizedMessage {

    private Message message;

    public NormalizedMessageImpl(Message message) {
        assert message != null : "Encapsulated message should never be null!";
        this.message = message;
    }

    public Message getInternalMessage() {
        return message;
    }

    public void addAttachment(String id, DataHandler content) throws MessagingException {
        message.addAttachment(id, content);
    }

    public Source getContent() {
        return message.getBody(Source.class);
    }

    public DataHandler getAttachment(String id) {
        Object attachment = message.getAttachment(id);
        if (attachment == null || attachment instanceof DataHandler) {
            return (DataHandler) attachment;
        } else {
            DataHandler dh = new DataHandler(attachment, null);
            return dh;
        }
    }

    public Set getAttachmentNames() {
        return message.getAttachments().keySet();
    }

    public void removeAttachment(String id) throws MessagingException {
        message.removeAttachment(id);
    }

    public void setContent(Source content) throws MessagingException {
        message.setBody(content);
    }

    public void setProperty(String name, Object value) {
        message.setHeader(name, value);
    }

    public void setSecuritySubject(Subject subject) {
        message.setHeader(Subject.class, subject);
    }

    public Set getPropertyNames() {
        return message.getHeaders().keySet();
    }

    public Object getProperty(String name) {
        return message.getHeader(name);
    }

    public Subject getSecuritySubject() {
        return message.getHeader(Subject.class);
    }
}
