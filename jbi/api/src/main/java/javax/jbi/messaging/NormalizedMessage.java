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

import java.util.Set;

import javax.activation.DataHandler;
import javax.security.auth.Subject;
import javax.xml.transform.Source;

public interface NormalizedMessage {
    void addAttachment(String id, DataHandler content) throws MessagingException;

    Source getContent();

    DataHandler getAttachment(String id);

    Set getAttachmentNames();

    void removeAttachment(String id) throws MessagingException;

    void setContent(Source content) throws MessagingException;

    void setProperty(String name, Object value);

    void setSecuritySubject(Subject subject);

    Set getPropertyNames();

    Object getProperty(String name);

    Subject getSecuritySubject();
}
