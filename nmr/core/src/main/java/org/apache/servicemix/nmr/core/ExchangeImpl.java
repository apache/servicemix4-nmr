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

import org.apache.servicemix.nmr.api.*;
import org.apache.servicemix.nmr.api.internal.InternalExchange;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/**
 * @version $Revision: $
 * @since 4.0
 */
public class ExchangeImpl implements InternalExchange {

	/**
	 * Generated serial version UID 
	 */
	private static final long serialVersionUID = 5453128544624717320L;
	
	private String id;
	private Status status;
    private Role role;
    private Pattern pattern;
    private Reference target;
    private QName operation;
    private Map<String, Object> properties;
    private Message in;
    private Message out;
    private Message fault;
    private Exception error;
    private InternalEndpoint source;
    private InternalEndpoint destination;
    private Semaphore consumerLock;
    private Semaphore providerLock;

    /**
     * Creates and exchange of the given pattern
     * @param pattern the pattern of this exchange
     */
    public ExchangeImpl(Pattern pattern) {
        this.id = UUID.randomUUID().toString();
        this.status = Status.Active;
        this.role = Role.Consumer;
        this.pattern = pattern;
    }
    
    private ExchangeImpl() {
    }

    /**
     * The unique id of the exchange
     *
     * @return the unique id
     */
    public String getId() {
    	return id;
    }

    /**
     * The role of the exchange.
     *
     * @return the role
     */
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
    
    /**
     * The status of the exchange
     * 
     * @return the status
     */
    public Status getStatus() {
    	return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * The exchange pattern
     *
     * @return the pattern
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * The target reference
     * 
     * @return the target
     */
	public Reference getTarget() {
		return target;
	}

	/**
	 * Set the target reference
	 * 
	 * @param target the new target
	 */
	public void setTarget(Reference target) {
		this.target = target;
	}

    /**
     * The operation
     *
     * @return the operation
     */
    public QName getOperation() {
        return operation;
    }

    /**
     * Set the operation
     *
     * @param operation the operation
     */
    public void setOperation(QName operation) {
        this.operation = operation;
    }

    /**
     * Get a given property by its name.
     *
     * @param name the name of the property to retrieve
     * @return the value of the property or <code>null</code> if none has been set
     */
    public Object getProperty(String name) {
    	if (properties == null) {
    		return null;
    	}
        return properties.get(name);
    }

    /**
     * Get a given property by its name.
     *
     * @param name the name of the property to retrieve
     * @return the value of the property or <code>null</code> if none has been set
     */
    public <T> T getProperty(String name, Class<T> type) {
    	if (properties == null) {
    		return null;
    	}
        // TODO: use converters
        return (T) properties.get(name);
    }

    /**
     * Get a typed property
     *
     * @param type the type
     * @return the value
     */
    public <T> T getProperty(Class<T> type) {
        if (properties == null) {
            return null;
        }
        return (T) properties.get(type.getName());
    }

    /**
     * Set a property on this exchange.
     * Giving <code>null</code> will actually remove the property for the list.
     *
     * @param name  the name of the property
     * @param value the value for this property or <code>null</code>
     */
    public void setProperty(String name, Object value) {
    	if (properties == null) {
    		properties = new HashMap<String, Object>();
    	}
        properties.put(name, value);
    }

    public <T> void setProperty(Class<T> type, T value) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        properties.put(type.getName(), value);
    }

    public Map<String, Object> getProperties() {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Obtains the input message
     *
     * @return the input message or <code>null</code> if
     *         this pattern do not have any
     */
    public Message getIn() {
        return getIn(true);
    }
    
    public Message getIn(boolean lazyCreate) {
        if (this.in == null) {
            this.in = createMessage();
        }
        return this.in;
    }

    public void setIn(Message message) {
        this.in = message;
    }

    /**
     * Obtains the output message
     *
     * @return the output message or <code>null</code> if
     *         this pattern does not have any
     */
    public Message getOut() {
        return getOut(true);
    }

    public Message getOut(boolean lazyCreate) {
        if (this.out == null) {
            if (this.pattern != Pattern.InOnly && this.pattern != Pattern.RobustInOnly) {
                this.out = createMessage();
            }
        }
        return this.out;
    }

    public void setOut(Message message) {
        this.out = message;
    }

    /**
     * Obtains the fault message
     *
     * @return the fault message
     */
    public Message getFault() {
        return getFault(true);
    }

    public Message getFault(boolean lazyCreate) {
        if (this.fault == null) {
            if (this.pattern != Pattern.InOnly) {
                this.fault = createMessage();
            }
        }
        return this.fault;
    }

    public void setFault(Message message) {
        this.fault = message;
    }

    /**
     * Obtains the message of the given type
     *
     * @return the message or <code>null</code> if
     *         this pattern does not support this type of message
     */
    public Message getMessage(Type type) {
        return getMessage(type, true);
    }
    
    public Message getMessage(Type type, boolean lazyCreate) {
        switch (type) {
            case In: return getIn(lazyCreate);
            case Out: return getOut(lazyCreate);
            case Fault: return getFault(lazyCreate);
            default: throw new IllegalArgumentException();
        }
    }

    public void setMessage(Type type, Message message) {
        switch (type) {
            case In: setIn(message);
            case Out: setOut(message);
            case Fault: setFault(message);
            default: throw new IllegalArgumentException();
        }
    }
    
    protected Message createMessage() {
    	return new MessageImpl();
    }

    /**
     * Obtains the error of this exchange
     *
     * @return the exception that caused the exchange to fail
     */
    public Exception getError() {
        return error;
    }
    
    public void setError(Exception error) {
    	this.error = error;
    }

	public void ensureReReadable() {
		if (in != null) {
			in.ensureReReadable();
		}
		if (out != null) {
			out.ensureReReadable();
		}
		if (fault != null) {
			fault.ensureReReadable();
		}
	}

    /**
     * Copy this exchange
     */
	public void copyFrom(Exchange exchange) {
		this.error = exchange.getError();
		if (exchange.getFault() != null) {
			this.fault = exchange.getFault().copy();
		}
		if (exchange.getIn() != null) {
			this.in = exchange.getIn().copy();
		}
		if (exchange.getOut() != null) {
			this.out = exchange.getOut().copy();
		}
		this.pattern = exchange.getPattern();
        this.properties = new HashMap<String, Object>(exchange.getProperties());
		this.role = exchange.getRole();
		this.target = exchange.getTarget();
	}
	
    public Exchange copy() {
    	ExchangeImpl copy = new ExchangeImpl();
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
		return "Exchange []";
	}

	public String toString() {
		return display(true);
	}

    public InternalEndpoint getSource() {
        return source;
    }

    public void setSource(InternalEndpoint source) {
        this.source = source;
    }

    public InternalEndpoint getDestination() {
        return destination;
    }

    public void setDestination(InternalEndpoint destination) {
        this.destination = destination;
    }

    public Semaphore getConsumerLock(boolean create) {
        if (create) {
            consumerLock = new Semaphore(0);
        }
        return consumerLock;
    }

    public Semaphore getProviderLock(boolean create) {
        if (create) {
            providerLock = new Semaphore(0);
        }
        return providerLock;
    }

}
