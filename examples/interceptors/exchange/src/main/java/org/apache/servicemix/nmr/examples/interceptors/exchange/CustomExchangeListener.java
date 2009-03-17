package org.apache.servicemix.nmr.examples.interceptors.exchange;

import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Role;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.event.ExchangeListener;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.api.internal.InternalExchange;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This listener will be called each time an exchange is sent
 * or delivered to an endpoint on NMR.
 */
public class CustomExchangeListener implements ExchangeListener {

    private static final transient Log LOG = LogFactory.getLog(CustomExchangeListener.class);

    /**
     * Method called each time an exchange is sent
     *
     * @param exchange the exchange sent
     */
    public void exchangeSent(Exchange exchange) {
         try {
             LOG.info("Sending exchange: " + exchange);
             // Intercept exchanges
             if (exchange instanceof InternalExchange &&
                 exchange.getStatus() == Status.Active &&
                 exchange.getRole() == Role.Consumer &&
                 exchange.getOut(false) == null &&
                 exchange.getFault(false) == null) {
                 String id = ((InternalExchange) exchange).getSource().getId();
                 LOG.info("Source endpoint: " + id);
             }
         } catch (Throwable t) {
             LOG.warn("Caught exception while processing exchange: " + t, t);
         }
    }

    /**
     * Method called each time an exchange is delivered
     *
     * @param exchange the delivered exchange
     */
    public void exchangeDelivered(Exchange exchange) {
        try {
            LOG.info("Receiving exchange: " + exchange);
            if (exchange.getStatus() == Status.Active &&
                exchange.getRole() == Role.Provider &&
                exchange.getOut(false) == null &&
                exchange.getFault(false) == null &&
                exchange instanceof InternalExchange) {
                String id = ((InternalExchange) exchange).getDestination().getId();
                LOG.info("Dest endpoint: " + id);
            }
        } catch (Throwable t) {
            LOG.warn("Caught exception while processing exchange: " + t, t);
        }
    }

    /**
     * Method called when an exchange resulted in an exception to be
     * thrown and the exchange not delivered.  This can happen if no
     * endpoint can be found for the target or if something else bad
     * happened.
     *
     * @param exchange the exchange that failed
     */
    public void exchangeFailed(Exchange exchange) {
        LOG.info("Exchange Failed: " + exchange);
    }

}
