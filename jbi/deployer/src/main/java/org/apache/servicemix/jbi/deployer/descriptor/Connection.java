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
package org.apache.servicemix.jbi.deployer.descriptor;

import org.apache.servicemix.nmr.api.Wire;
import org.apache.servicemix.nmr.api.service.ServiceHelper;

/**
 * The <code>&lt;jbi:connection&gt;</code> element.
 */
public class Connection {
    private Consumer consumer;
    private Provider provider;
    private Wire wire;

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Wire getWire() {
        if (wire == null) {
            wire = ServiceHelper.createWire(consumer.toMap(), provider.toMap());
        } 
        return wire;
    }
}
