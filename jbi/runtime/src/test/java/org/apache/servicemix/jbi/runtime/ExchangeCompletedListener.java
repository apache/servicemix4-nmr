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
package org.apache.servicemix.jbi.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.event.ExchangeListener;


public class ExchangeCompletedListener extends Assert implements ExchangeListener {

    private Map<String, Exchange> exchanges = new HashMap<String, Exchange>();

    private long timeout;

    public ExchangeCompletedListener() {
        this(5000);
    }

    public ExchangeCompletedListener(long timeout) {
        this.timeout = timeout;
    }

    public void exchangeSent(Exchange exchange) {
        if (exchange.getStatus() == Status.Active) {
            synchronized (exchanges) {
                exchanges.put(exchange.getId(), exchange);
                exchanges.notifyAll();
            }
        }
    }

    public void exchangeDelivered(Exchange exchange) {
        if (exchange.getStatus() != Status.Active) {
            synchronized (exchanges) {
                exchanges.put(exchange.getId(), exchange);
                exchanges.notifyAll();
            }
        }
    }

    public void exchangeFailed(Exchange exchange) {
        synchronized (exchanges) {
            exchanges.put(exchange.getId(), exchange);
            exchanges.notifyAll();
        }
    }

    public void assertExchangeCompleted() throws Exception {
        long start = System.currentTimeMillis();
        Exchange active = null;
        while (true) {
            synchronized (exchanges) {
                for (Iterator<Exchange> it = exchanges.values().iterator(); it.hasNext();) {
                    active = null;
                    Exchange me = it.next();
                    if (me.getStatus() == Status.Active) {
                        active = me;
                        break;
                    }
                }
                if (active == null) {
                    break;
                }
                long remain = timeout - (System.currentTimeMillis() - start);
                if (remain <= 0) {
                    assertTrue("Exchange is ACTIVE: " + active, active.getStatus() != Status.Active);
                } else {
                    exchanges.wait(remain);
                }
            }
        }
    }

}
