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
package org.apache.servicemix.nmr.audit.commands;

import org.apache.felix.gogo.commands.Option;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.audit.AuditorMBean;

/**
 * Retrieve exchanges
 */
public class ExchangesCommand extends AuditCommandSupport {

    @Option(name = "--index", required = true)
    int index = -1;

    @Option(name = "--from", required = true)
    int from = -1;

    @Option(name = "--to", required = true)
    int to = -1;

    @Option(name = "--id", required = true)
    String id;

    @Option(name="--all")
    boolean all;

    protected Object doExecute(AuditorMBean auditor) throws Exception {
        Exchange[] exchanges;
        if (index >= 0) {
            exchanges = new Exchange[] { auditor.getExchangeByIndex(index) };
        } else if (from >= 0 && to >= 0) {
            exchanges = auditor.getExchangesByRange(from, to);
        } else if (id != null) {
            exchanges = new Exchange[] { auditor.getExchangeById(id) };
        } else if (all) {
            exchanges = auditor.getAllExchanges();
        } else {
            System.err.println("One of [--index, --id, --all] option must be specified");
            return 1;
        }
        if (exchanges != null) {
            for (Exchange e : exchanges) {
                if (e != null) {
                    System.out.println(e.display(true));
                }
            }
        }
        return 0;
    }
}
