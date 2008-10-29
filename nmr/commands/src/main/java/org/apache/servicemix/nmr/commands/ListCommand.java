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
package org.apache.servicemix.nmr.commands;

import org.apache.servicemix.nmr.api.Endpoint;
import org.osgi.framework.ServiceReference;

/**
 * Displays the name of existing NMR endpoints
 */
public class ListCommand extends NmrCommandSupport {

    protected Object doExecute() throws Exception {
        io.out.println("Endpoints");
        io.out.println("---------");
        ServiceReference[] references = getBundleContext().getAllServiceReferences(Endpoint.class.getName(), null);
        if (references != null) {
            for (ServiceReference ref : references) {
                String name = (String) ref.getProperty(Endpoint.NAME);
                io.out.println(name);
            }
        }
        io.out.println();
        return null;
    }
}
