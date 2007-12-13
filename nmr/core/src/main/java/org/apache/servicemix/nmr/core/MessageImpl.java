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

import org.apache.servicemix.nmr.api.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @version $Revision: $
 * @since 4.0
 */
public class MessageImpl implements Message {

    /**
	 * Generated serial version UID 
	 */
	private static final long serialVersionUID = -8621182821298293687L;

	private Object body;
    private String contentType;
    private String contentEncoding;
    private Map<String, Object> headers;
    private Map<String, Object> attachments;

    public MessageImpl() {
    }

    /**
     * Returns the body of the message in its default format.
     *
     * @return the servicemix body of this message
     */
    public Object getBody() {
        return body;
    }

    /**
     * Returns the body as the specified type.
     *
     * @param type the type in which the body is to be transformed
     * @return the transformed body
     */
    public <T> T getBody(Class<T> type) {
        // TODO: use converters
        if (type.isInstance(body)) {
            return (T) body;
        }
        return null;
    }

    /**
     * Set the body of the message.
     *
     * @param body the body of the message
     */
    public void setBody(Object body) {
        this.body = body;
    }

    /**
     * Set the body of the message.
     *
     * @param content the body of the message
     */
    public <T> void setBody(Object content, Class<T> type) {
        // TODO: use converters
        this.body = content;
    }

    /**
     * Get the mime body type describing the body of the message
     *
     * @return the mime body type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Set the mime body type describing the body of the message
     *
     * @param type the mime body type
     */
    public void setContentType(String type) {
        this.contentType = type;
    }

    /**
     * Get the encoding of the message
     *
     * @return the encoding
     */
    public String getContentEncoding() {
        return contentEncoding;
    }

    /**
     * Set the encoding of the message
     *
     * @param encoding the encoding
     */
    public void setContentEncoding(String encoding) {
        this.contentEncoding = encoding;
    }

    /**
     * Get a header on this message.
     *
     * @param name the name of the header
     * @return the value of the header of <code>null</code> if none has been set
     */
    public Object getHeader(String name) {
        if (headers == null) {
            return null;
        }
        return headers.get(name);
    }

    /**
     * Get a header, converting it to the desired type
     *
     * @param name the name of the header
     * @param type the desired type
     * @return the converted header or <code>null</code> if
     *          no header has been set or if it can not be transformed
     *          to the desired type
     */
    public <T> T getHeader(String name, Class<T> type) {
        if (headers == null) {
            return null;
        }
        return (T) headers.get(name);
    }

    /**
     * Get a typed header.
     * This is equivalent to:
     *   <code>exchange.getHeader(type.getName())</code>
     *
     * @param type the type of the header
     * @return the header
     */
    public <T> T getHeader(Class<T> type) {
        if (headers == null) {
            return null;
        }
        return (T) headers.get(type.getName());
    }

    /**
     * Set a header for this message
     * @param name the name of the header
     * @param value the value of the header
     */
    public void setHeader(String name, Object value) {
        if (headers == null) {
            headers = new HashMap<String, Object>();
        }
        headers.put(name, value);
    }

    /**
     * Set a typed header for this message.
     * This is equivalent to:
     *   <code>exchange.setHeader(type.getName(), value)</code>
     *
     *
     * @param type the type of the header
     * @param value the value of the header
     */
    public <T> void setHeader(Class<T> type, T value) {
        if (headers == null) {
            headers = new HashMap<String, Object>();
        }
        headers.put(type.getName(), value);
    }

    /**
     * Remove the given header and returns its value.
     *
     * @param name the name of the header
     * @return the previous value
     */
    public Object removeHeader(String name) {
        if (headers == null) {
            return null;
        }
        return headers.remove(name);
    }

    /**
     * Get a map of all the headers for this message
     *
     * @return a map of headers
     */
    public Map<String, Object> getHeaders() {
        if (headers == null) {
            headers = new HashMap<String, Object>();
        }
        return headers;
    }

    /**
     * Set all the headers
     *
     * @param headers the new map of headers
     */
    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    /**
     * Retrieve an attachment given its id.
     *
     * @param id the id of the attachment to retrieve
     * @return the attachement or <code>null</code> if none exists
     */
    public Object getAttachment(String id) {
        if (attachments != null) {
            return null;
        }
        return attachments.get(id);
    }

    /**
     * Add an attachment to this message
     *
     * @param id the id of the attachment
     * @param value the attachment to add
     */
    public void addAttachment(String id, Object value) {
        if (attachments != null) {
            attachments = new HashMap<String, Object>();
        }
        attachments.put(id, value);
    }

    /**
     * Remove an attachment on this message
     *
     * @param id the id of the attachment to remove
     */
    public void removeAttachment(String id) {
        if (attachments != null) {
            attachments.remove(id);
        }
    }

    /**
     * Retrieve a map of all attachments
     *
     * @return the map of attachments
     */
    public Map<String, Object> getAttachments() {
        if (attachments == null) {
            attachments = new HashMap<String, Object>();
        }
        return attachments;
    }

    /**
     * Make sure that all streams contained in the body and in
     * attachments are transformed to re-readable sources.
     * This method will be called by the framework when persisting
     * the message or when displaying it.
     *
     * TODO: do we really need this method
     */
    public void ensureReReadable() {
        // TODO: implement        
    }

    /**
     * Copies the contents of the other message into this message
     *
     * @param msg the message to copy from
     */
    public void copyFrom(Message msg) {
        body = msg.getBody();
        if (!msg.getHeaders().isEmpty()) {
            headers = new HashMap<String, Object>();
            for (Map.Entry<String, Object> e : msg.getHeaders().entrySet()) {
                headers.put(e.getKey(), e.getValue());
            }
        } else {
            headers = null;
        }
        if (!msg.getAttachments().isEmpty()) {
            attachments = new HashMap<String, Object>();
            for (Map.Entry<String, Object> e : msg.getAttachments().entrySet()) {
                attachments.put(e.getKey(), e.getValue());
            }
        } else {
            attachments = null;
        }
    }

    /**
     * Creates a copy of this message so that it can
     * be used and possibly modified further in another exchange
     *
     * @return a new message instance copied from this message
     */
    public Message copy() {
        MessageImpl copy = new MessageImpl();
        copy.copyFrom(this);
        return copy;
    }

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		ensureReReadable();
		out.defaultWriteObject();
	}

	public String display(boolean displayContent) {
		if (displayContent) {
			ensureReReadable();
		}
		return "Message []";
	}

	public String toString() {
		return display(true);
	}
}
