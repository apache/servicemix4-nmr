package org.apache.servicemix.jbi.cluster.requestor;

import javax.jms.JMSException;

import org.springframework.context.Lifecycle;

public interface JmsRequestorPool extends Lifecycle {

    Transacted getTransacted();

    void setListener(JmsRequestorListener listener);

    void setMessageSelector(String selector);

    JmsRequestor newRequestor() throws JMSException;

    JmsRequestor resume(String id);

}
