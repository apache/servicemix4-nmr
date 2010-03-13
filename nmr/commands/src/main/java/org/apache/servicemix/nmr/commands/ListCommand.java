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

import java.util.Set;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.NMR;
import org.apache.felix.gogo.commands.Command;
import org.osgi.framework.ServiceReference;

/**
 * Displays the name of existing NMR endpoints
 */
@Command(scope = "nmr", name = "list", description = "List NMR endpoints")
public class ListCommand extends NmrCommandSupport {

    protected Object doExecute() throws Exception {
        System.out.println("Endpoints");
        System.out.println("---------");
        ServiceReference reference = getBundleContext().getServiceReference(NMR.class.getName());
        if (reference != null) {
            NMR nmr = (NMR)getBundleContext().getService(reference);
            if (nmr != null) {
            	Set<Endpoint> endpoints = nmr.getEndpointRegistry().getServices();
            	for (Endpoint endpoint : endpoints) {
            		String name = (String)nmr.getEndpointRegistry().getProperties(endpoint).get(Endpoint.NAME);
            		System.out.println(name);
            	}
            }
        }
        System.out.println();
        return null;
    }
}
