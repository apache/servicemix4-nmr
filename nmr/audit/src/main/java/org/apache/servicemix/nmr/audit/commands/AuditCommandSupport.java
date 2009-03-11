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

import org.apache.servicemix.kernel.gshell.core.OsgiCommandSupport;
import org.apache.servicemix.nmr.audit.AuditorMBean;
import org.osgi.framework.ServiceReference;

/**
 * Base class for audit commands
 */
public abstract class AuditCommandSupport extends OsgiCommandSupport {

    protected AuditorMBean getAuditor() {
        ServiceReference ref = getBundleContext().getServiceReference(AuditorMBean.class.getName());
        if (ref != null) {
            return getService(AuditorMBean.class, ref);
        }
        return null;
    }

    protected Object doExecute() throws Exception {
        AuditorMBean auditor = getAuditor();
        if (auditor == null) {
            io.err.println("No NMR auditor has been registered. Aborting");
            return Result.FAILURE;
        }
        return doExecute(auditor);
    }

    protected abstract Object doExecute(AuditorMBean auditor) throws Exception;
}
