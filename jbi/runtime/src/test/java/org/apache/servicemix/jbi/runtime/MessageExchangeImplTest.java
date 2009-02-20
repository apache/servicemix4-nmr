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

import javax.jbi.messaging.MessageExchange;

import junit.framework.TestCase;
import org.apache.servicemix.jbi.runtime.impl.MessageExchangeImpl;
import org.apache.servicemix.nmr.core.ExchangeImpl;
import org.apache.servicemix.nmr.api.Pattern;

public class MessageExchangeImplTest extends TestCase {

    public void testMep() {
        MessageExchange me;

        me = new MessageExchangeImpl(new ExchangeImpl(Pattern.InOnly));
        assertEquals("http://www.w3.org/2004/08/wsdl/in-only", me.getPattern().toString());

        me = new MessageExchangeImpl(new ExchangeImpl(Pattern.InOut));
        assertEquals("http://www.w3.org/2004/08/wsdl/in-out", me.getPattern().toString());

        me = new MessageExchangeImpl(new ExchangeImpl(Pattern.InOptionalOut));
        assertEquals("http://www.w3.org/2004/08/wsdl/in-opt-out", me.getPattern().toString());

        me = new MessageExchangeImpl(new ExchangeImpl(Pattern.RobustInOnly));
        assertEquals("http://www.w3.org/2004/08/wsdl/robust-in-only", me.getPattern().toString());
    }
}
