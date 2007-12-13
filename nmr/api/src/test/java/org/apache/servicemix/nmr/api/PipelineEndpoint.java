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

/**
 * @version $Revision: $
 */
public class PipelineEndpoint implements Endpoint {

    private Channel channel;

    private Reference transformer;
    private Reference target;

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setTransformer(Reference transformer) {
        this.transformer = transformer;
    }

    public void setTarget(Reference target) {
        this.target = target;
    }

    public void process(Exchange exchange) {
        if (exchange.getPattern() != Pattern.InOnly && exchange.getPattern() != Pattern.RobustInOnly) {
            fail(exchange, new UnsupportedOperationException("Use an InOnly or RobustInOnly MEP"));
            return;
        }
        Exchange tme = channel.createExchange(Pattern.InOut);
        tme.setTarget(target);
        tme.getIn().copyFrom(exchange.getIn());
        if (!channel.sendSync(tme)) {
            fail(exchange, new InterruptedException("Timeout"));
            return;
        }
        if (tme.getStatus() == Status.Error) {
            fail(exchange, tme.getError());
        } else if (tme.getStatus() == Status.Active && tme.getFault(false) != null) {
            // TODO: send fault back to the consumer or send it to target or another target?
            if (exchange.getPattern() == Pattern.InOnly) {
                // TODO: send fault back as an error or just log them?
            } else  {
                exchange.getFault().copyFrom(tme.getFault());
                done(tme);
                channel.sendSync(exchange);
            }
        } else if (tme.getStatus() == Status.Active && tme.getOut(false) != null) {
            Exchange me = channel.createExchange(Pattern.InOnly);
            me.getIn().copyFrom(tme.getOut());
            boolean res = channel.sendSync(me);
            done(tme);
            if (!res) {
                fail(exchange, new InterruptedException("Timeout"));
            } else if (me.getStatus() == Status.Done) {
                done(exchange);
            } else if (me.getStatus() == Status.Error) {
                fail(exchange, me.getError());
            } else if (me.getFault(false) != null) {
                if (exchange.getPattern() == Pattern.InOnly) {
                    // TODO: send fault back as an error or just log them?
                } else  {
                    exchange.getFault().copyFrom(me.getFault());
                    done(me);
                    channel.sendSync(exchange);
                }
            } else {
                fail(exchange, new IllegalStateException()); // This should never happen
            }
        } else {
            fail(exchange, new IllegalStateException()); // This should never happen
        }
    }

    private void done(Exchange exchange) {
        exchange.setStatus(Status.Done);
        channel.send(exchange);
    }


    private void fail(Exchange exchange, Exception e) {
        exchange.setError(e);
        channel.send(exchange);
    }
}
