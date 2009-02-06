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
package org.apache.servicemix.jbi.management;

import javax.jbi.JBIException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class SharedLibInstaller extends AbstractInstaller {

	private static final Log LOGGER = LogFactory.getLog(SharedLibInstaller.class);
	
	private String name;
    private AdminService adminService;
	
	public SharedLibInstaller(String name, AdminService adminService) {
		this.name = name;
		this.adminService = adminService;
		setBundleContext(adminService.getBundleContext());
	}
	
	public void install(String filename) {
		deployFile(filename);
	}
	
	public void uninstall() throws javax.jbi.JBIException {
		try {
        	Bundle bundle = getBundle();

            if (bundle == null) {
                LOGGER.warn("Could not find Bundle for shared lib: " + name);
            }
            else {
                bundle.stop();
                bundle.uninstall();
            }
        } catch (BundleException e) {
        	LOGGER.error("failed to uninstall shared lib: " + name, e);
        	throw new JBIException(e);
		} 
    }
}
