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
import org.apache.servicemix.nmr.audit.AuditorMBean;

/**
 * Delete all exchanges
 */
public class DeleteCommand extends AuditCommandSupport {

    @Option(name = "--index", required = true)
    int index = -1;

    @Option(name = "--id", required = true)
    String id;

    @Option(name="--all")
    boolean all;

    protected Object doExecute(AuditorMBean auditor) throws Exception {
        int nb = 0;
        if (index >= 0) {
            nb = auditor.deleteExchangeByIndex(index) ? 1 : 0;
        } else if (id != null) {
            nb = auditor.deleteExchangeById(id) ? 1 : 0;
        } else if (all) {
            nb = auditor.deleteAllExchanges();
        } else {
            System.err.println("One of [--index, --id, --all] option must be specified");
            return 1;
        }
        System.out.println(nb + " exchanges deleted");
        return 0;
    }
}
