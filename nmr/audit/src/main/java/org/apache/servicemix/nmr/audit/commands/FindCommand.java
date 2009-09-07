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

import org.apache.felix.gogo.commands.Argument;
import org.apache.servicemix.nmr.audit.AuditorMBean;
import org.apache.servicemix.nmr.audit.AuditorQueryMBean;

/**
 * Retrieve exchange ids
 */
public class FindCommand extends AuditCommandSupport {

    @Argument(required = true)
    String query;

    protected Object doExecute(AuditorMBean auditor) throws Exception {
        if (!(auditor instanceof AuditorQueryMBean)) {
            System.err.println("Auditor does not support search.  The auditor should be wrapped within a lucene auditor");
            return 1;
        }
        return doExecute((AuditorQueryMBean) auditor);
    }

    protected Object doExecute(AuditorQueryMBean auditor) throws Exception {
        String[] ids = auditor.findExchangesIdsByQuery(query);
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
