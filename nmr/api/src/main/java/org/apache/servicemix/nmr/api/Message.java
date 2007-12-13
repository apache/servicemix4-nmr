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
package org.apache.servicemix.nmr.api;

import java.io.Serializable;
import java.util.Map;

/**
 * The Message represents the content of a request, a response or a fault.
 * Messages are part of {@link Exchange}s are created using
 * {@link Exchange#getIn()}, {@link Exchange#getOut()} and {@link Exchange#getFault()}.
 *
 * If the Exchange has to go to a remote ServiceMix instance to be processed
 * (if the instance is part of a cluster), all headers, attachments and content
 * have to be Serializable.
 *
 * TODO: security
 *
 * @version $Revision: $
 * @since 4.0
 */
public interface Message extends Serializable {

    /**
     * Get a header on this message.
     *
     * @param name the name of the header
     * @return the value of the header of <code>null</code> if none has been set
     */
    Object getHeader(String name);

    /**
     * Get a typed header.
     * This is equivalent to:
     *   <code>exchange.getHeader(type.getName())</code>
     *
     * @param type the type of the header
     * @return the header
     */
    <T> T getHeader(Class<T> type);

    /**
     * Get a header, converting it to the desired type
     *
     * @param name the name of the header
     * @param type the desired type
     * @return the converted header or <code>null</code> if
     *          no header has been set or if it can not be transformed
     *          to the desired type
     */
    <T> T getHeader(String name, Class<T> type);

    /**
     * Set a header for this message
     * @param name the name of the header
     * @param value the value of the header
     */
    void setHeader(String name, Object value);

    /**
     * Set a typed header for this message.
     * This is equivalent to:
     *   <code>exchange.setHeader(type.getName(), value)</code>
     *
     *
     * @param type the type of the header
     * @param value the value of the header
     */
    <T> void setHeader(Class<T> type, T value);

    /**
     * Remove the given header and returns its value.
     *
     * @param name the name of the header
     * @return the previous value
     */
    Object removeHeader(String name);

    /**
     * Get a map of all the headers for this message
     *
     * @return a map of headers
     */
    Map<String, Object> getHeaders();

    /**
     * Set all the headers
     *
     * @param headers the new map of headers
     */
    void setHeaders(Map<String, Object> headers);

    /**
     * Retrieve an attachment given its id.
     *
     * @param id the id of the attachment to retrieve
     * @return the attachement or <code>null</code> if none exists
     */
    Object getAttachment(String id);

    /**
     * Add an attachment to this message
     *
     * @param id the id of the attachment
     * @param value the attachment to add
     */
    void addAttachment(String id, Object value);

    /**
     * Remove an attachment on this message
     *
     * @param id the id of the attachment to remove
     */
    void removeAttachment(String id);

    /**
     * Retrieve a map of all attachments
     *
     * @return the map of attachments
     */
    Map<String, Object> getAttachments();

    /**
     * Returns the body of the message in its default format.
     *
     * @return the servicemix body of this message
     */
    Object getBody();

    /**
     * Returns the body as the specified type.
     *
     * @param type the type in which the body is to be transformed
     * @return the transformed body
     */
    <T> T getBody(Class<T> type);

    /**
     * Set the body of the message.
     *
     * @param body the body of the message
     */
    void setBody(Object body);

    /**
     * Set the body of the message.
     *
     * @param body the body of the message
     */
    <T> void setBody(Object body, Class<T> type);

    /**
     * Get the mime content type describing the content of the message
     *
     * @return the mime content type
     */
    String getContentType();

    /**
     * Set the mime content type describing the content of the message
     *
     * @param type the mime content type
     */
    void setContentType(String type);

    /**
     * Get the encoding of the message
     *
     * @return the encoding
     */
    String getContentEncoding();

    /**
     * Set the encoding of the message
     *
     * @param encoding the encoding
     */
    void setContentEncoding(String encoding);

    /**
     * Copies the contents of the other message into this message
     * 
     * @param msg the message to copy from
     */
    void copyFrom(Message msg);

    /**
     * Creates a copy of this message so that it can
     * be used and possibly modified further in another exchange
     * 
     * @return a new message instance copied from this message
     */
    Message copy();

    /**
     * Make sure that all streams contained in the content and in
     * attachments are transformed to re-readable sources.
     * This method will be called by the framework when persisting
     * the message or when displaying it.
     *
     * TODO: do we really need this method
     */
    void        ensureReReadable();

    // TODO: is toString() sufficient ?
    String      display(boolean displayContent);

}
