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
package org.apache.servicemix.jbi.deployer.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.AdminCommandsService;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.NamingStrategy;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.SharedLibrary;
import org.apache.servicemix.jbi.runtime.impl.ManagementContext;

/**
 */
public class DefaultNamingStrategy implements NamingStrategy {

    private static final Log LOG = LogFactory.getLog(DefaultNamingStrategy.class);

    private String jmxDomainName;

    public String getJmxDomainName() {
        return jmxDomainName;
    }

    public void setJmxDomainName(String jmxDomainName) {
        this.jmxDomainName = jmxDomainName;
    }

    public ObjectName getObjectName(SharedLibrary sharedLibrary) throws MalformedObjectNameException {
        return new ObjectName(jmxDomainName + ":" +
                "Type=SharedLibrary," +
                "Name=" + sanitize(sharedLibrary.getName()) + "," +
                "Version=" + sanitize(sharedLibrary.getVersion()));
    }

    public ObjectName getObjectName(Component component) throws MalformedObjectNameException {
        return new ObjectName(jmxDomainName + ":" +
                "Type=Component," +
                "Name=" + sanitize(component.getName()) + "," +
                "SubType=LifeCycle");
    }

    public ObjectName getObjectName(ServiceAssembly serviceAssembly) throws MalformedObjectNameException {
        return new ObjectName(jmxDomainName + ":" +
                "Type=ServiceAssembly," +
                "Name=" + sanitize(serviceAssembly.getName()));
    }

    public ObjectName getObjectName(AdminCommandsService adminCommandsService) throws MalformedObjectNameException {
        return getSystemObjectName(jmxDomainName, AdminService.DEFAULT_NAME, AdminCommandsService.class);
    }


    private String sanitize(String in) {
        String result = null;
        if (in != null) {
            result = in.replace(':', '_');
            result = result.replace('/', '_');
            result = result.replace('\\', '_');
            result = result.replace('?', '_');
            result = result.replace('=', '_');
            result = result.replace(',', '_');
        }
        return result;
    }

    public ObjectName createCustomComponentMBeanName(String type, String name) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        result.put("Type", "Component");
        result.put("Name", sanitize(name));
        result.put("SubType", sanitize(type));
        return createObjectName(result);
    }

    public ObjectName createObjectName(Map<String, String> props) {
        return ManagementContext.createObjectName(getJmxDomainName(), props);
    }

    public static ObjectName getSystemObjectName(String domainName, String containerName, Class interfaceType) {
        String tmp = domainName + ":ContainerName=" + containerName + ",Type=SystemService,Name=" + getSystemServiceName(interfaceType);
        ObjectName result = null;
        try {
            result = new ObjectName(tmp);
        } catch (MalformedObjectNameException e) {
            LOG.error("Failed to build ObjectName:", e);
        } catch (NullPointerException e) {
            LOG.error("Failed to build ObjectName:", e);
        }
        return result;
    }

    public static String getSystemServiceName(Class interfaceType) {
        String name = interfaceType.getName();
        name = name.substring(name.lastIndexOf('.') + 1);
        if (name.endsWith("MBean")) {
            name = name.substring(0, name.length() - 5);
        }
        return name;
    }

}

