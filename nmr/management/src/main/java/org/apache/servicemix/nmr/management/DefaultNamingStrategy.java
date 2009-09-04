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
package org.apache.servicemix.nmr.management;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    public ObjectName getObjectName(ManagedEndpoint endpoint) throws MalformedObjectNameException {
        return new ObjectName(jmxDomainName + ":Type=Endpoint,Id=" + sanitize(getId(endpoint)));
    }
    
    public ObjectName getObjectName(Nameable nameable) throws MalformedObjectNameException {
        String name = jmxDomainName + ":" +
                (nameable.getParent() != null ? "ContainerName=" + sanitize(nameable.getParent()) + "," : "") +
                "Type=" + sanitize(nameable.getMainType()) +
                ",Name=" + sanitize(nameable.getName()) +
                (nameable.getVersion() != null ? ",Version=" + sanitize(nameable.getVersion()) : "") +
                (nameable.getSubType() != null ? ",SubType=" + sanitize(nameable.getSubType()) : "");
        return new ObjectName(name);
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

    public ObjectName getCustomObjectName(String type, String name) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        result.put("Type", "Component");
        result.put("Name", sanitize(name));
        result.put("SubType", sanitize(type));
        return createObjectName(result);
    }

    public ObjectName createObjectName(Map<String, String> props) {
        StringBuffer sb = new StringBuffer();
        sb.append(getJmxDomainName()).append(':');
        int i = 0;
        for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            if (i++ > 0) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        ObjectName result = null;
        try {
            result = new ObjectName(sb.toString());
        } catch (MalformedObjectNameException e) {
            // shouldn't happen
            String error = "Could not create ObjectName for " + props;
            LOG.error(error, e);
            throw new RuntimeException(error);
        }
        return result;
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
    
    private String getId(ManagedEndpoint endpoint) {
        return endpoint.getEndpoint().getId();
    }


}

