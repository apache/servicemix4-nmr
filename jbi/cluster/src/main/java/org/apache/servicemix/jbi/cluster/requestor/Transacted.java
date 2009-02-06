package org.apache.servicemix.jbi.cluster.requestor;

/**
 * Type of transactions used
 */
public enum Transacted {
    /**
     * No transactions are used
     */
    None,

    /**
     * Use client acknowledgement
     */
    ClientAck,

    /**
     * Use XA transactions
     */
    Jms,

    /**
     * Use JMS local transactions
     */
    Xa
}
