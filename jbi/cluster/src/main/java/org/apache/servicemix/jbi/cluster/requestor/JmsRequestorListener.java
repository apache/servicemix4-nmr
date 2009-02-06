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
