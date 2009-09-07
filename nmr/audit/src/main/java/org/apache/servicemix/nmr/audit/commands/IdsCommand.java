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
 * Retrieve exchange ids
 */
public class IdsCommand extends AuditCommandSupport {

    @Option(name = "--index")
    private int index = -1;

    @Option(name = "--from")
    private int from = -1;

    @Option(name = "--to")
    private int to = -1;

    @Option(name = "--all")
    private boolean all;

    protected Object doExecute(AuditorMBean auditor) throws Exception {
        String[] ids;
        if (index >= 0) {
            ids = new String[] { auditor.getExchangeIdByIndex(index) };
        } else if (from >= 0 && to >= 0) {
            ids = auditor.getExchangeIdsByRange(from, to);
        } else if (all) {
            ids = auditor.getAllExchangeIds();
        } else {
            System.err.println("One of [--index, --id, --all] option must be specified");
            return 1;
        }
        if (ids == null || ids.length == 0) {
            System.out.println("No matching exchanges");
        } else {
            for (String id : ids) {
                if (id != null) {
                    System.out.println(id);
                }
            }
        }
        return 0;
    }
}
