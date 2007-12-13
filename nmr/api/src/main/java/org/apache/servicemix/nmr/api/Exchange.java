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

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Map;

/**
 * Represents a message exchange.
 *
 * An exchange is used to interact with a channel
 * representing a link to a logical endpoint.
 * Exchanges are created using the {@link Channel}.
 *
 * TODO: transactions
 *
 * @version $Revision: $
 * @since 4.0
 */
public interface Exchange extends Serializable {

    /**
     * The unique id of this exchange
     * @return
     */
    String getId();

    /**
     * The exchange pattern
     * @return
     */
    Pattern getPattern();

    /**
     * The role of the exchange.
     * @return
     */
    Role getRole();
    
    /**
     * The status of the exchange
     * @return
     */
    Status getStatus();

    /**
     * Set the status of the exchange
     *
     * @param status the new status
     */
    void setStatus(Status status);

    /**
     * The target used for this exchange
     * @return
     */
    Reference getTarget();
    
    /**
     * The target used for this exchange
     *
     * @param target the target endpoint
     */
    void setTarget(Reference target);

    /**
     * The service operation of this exchange
     *
     * @return  the operation
     */
    QName getOperation();

    /**
     * The service operation of this exchange
     *
     * @param operation the operation
     */
    void setOperation(QName operation);

    /**
     * Get a given property by its name.
     *
     * @param name the name of the property to retrieve
     * @return the value of the property or <code>null</code> if none has been set
     */
    Object getProperty(String name);

    /**
     * Get a typed property.
     *
     * @param type the type of the property to retrieve
     * @return the value of the property or <code>null</code> if none has been set
     */
    <T> T getProperty(Class<T> type);

    /**
     * Returns a property associated with this exchange by name and specifying
     * the type required
     *
     * @param name the name of the property
     * @param type the type of the property
     * @return the value of the given header or null if there is no property for
     *         the given name or null if it cannot be converted to the given
     *         type
     */
    <T> T getProperty(String name, Class<T> type);

    /**
     * Return all the properties associated with this exchange
     *
     * @return all the properties
     */
    Map<String, Object> getProperties();

    /**
     * Set a property on this exchange.
     * Giving <code>null</code> will actually remove the property for the list.
     *
     * @param name the name of the property
     * @param value the value for this property or <code>null</code>
     */
    void setProperty(String name, Object value);

    /**
     * Set a typed property on this exchange.
     *
     * @param type the key
     * @param value the value
     */
    <T> void setProperty(Class<T> type, T value);

    /**
     * Obtains the input message, lazily creating one if none
     * has been associated with this exchange. If you want to inspect this property
     * but not force lazy creation then invoke the {@link #getIn(boolean)}
     * method passing in false
     *
     * @return the input message
     */
    Message getIn();

    /**
     * Returns the inbound message, optionally creating one if one has not already
     * been associated with this exchange.
     *
     * @param lazyCreate <code>true</code> if the message should be created
     * @return the input message
     */
    Message getIn(boolean lazyCreate);

    /**
     * Set the inbound message.
     *
     * @param message the new inbound message
     */
    void setIn(Message message);
    
    /**
     * Obtains the outbound message, lazily creating one if none
     * has been associated with this exchange and if this exchange
     * supports an out message. If you want to inspect this property
     * but not force lazy creation then invoke the {@link #getOut(boolean)}
     * method passing in false
     *
     * @return the output message
     */
    Message getOut();

    /**
     * Returns the outbound message, optionally creating one if one has not already
     * been associated with this exchange
     *
     * @return the out message
     */
    Message getOut(boolean lazyCreate);

    /**
     * Set the outbound message.
     *
     * @param message the new outbound message
     */
    void setOut(Message message);

    /**
     * Obtains the fault message, lazily creating one if none
     * has been associated with this exchange and if this exchange
     * supports a faut message. If you want to inspect this property
     * but not force lazy creation then invoke the {@link #getFault(boolean)}
     * method passing in false
     *
     * @return the fault message
     */
    Message getFault();

    /**
     * Returns the fault message, optionally creating one if one has not already
     * been associated with this exchange
     *
     * @return the fault message
     */
    Message getFault(boolean lazyCreate);

    /**
     * Set the fault message.
     *
     * @param message the new fault message
     */
    void setFault(Message message);

    /**
     * Obtains the given message, lazily creating one if none
     * has been associated with this exchange and if this exchange
     * supports a faut message. If you want to inspect this property
     * but not force lazy creation then invoke the {@link #getMessage(Type, boolean)}
     * method passing in false
     *
     * @param type the type of message to retrieve
     * @return the message or <code>null</code> if
     *         this pattern does not support this type of message
     */
    Message getMessage(Type type);

    /**
     * Returns the message of the given type, optionally creating one if one has not already
     * been associated with this exchange
     *
     * @param type the type of message to retrieve
     * @return the given message
     */
    Message getMessage(Type type, boolean lazyCreate);

    /**
     * Set the message.
     *
     * @param type the type of the message to set
     * @param message the new inbound message
     */
    void setMessage(Type type, Message message);

    /**
     * Obtains the error of this exchange
     *
     * @return the exception that caused the exchange to fail
     */
    Exception getError();

    /**
     * Set the error on this exchange
     *
     * @param error the exception that caused the exchange to fail
     */
    void setError(Exception error);

    /**
     * Copy the given exchange to this one
     * @param exchange the exchange to copy from
     */
    void copyFrom(Exchange exchange);

    /**
     * Duplicates this exchange and returns a new copy
     *
     * @return a copy of this exchange
     */
    Exchange copy();

    /**
     * Make sure that all streams contained in the content and in
     * attachments are transformed to re-readable sources.
     * This method will be called by the framework when persisting
     * the exchange or when displaying it
     *
     * TODO: do we really need that?
     */
    void ensureReReadable();

    /**
     * TODO: is toString() sufficient 
     *
     * @param displayContent
     * @return
     */
    String display(boolean displayContent);


}
