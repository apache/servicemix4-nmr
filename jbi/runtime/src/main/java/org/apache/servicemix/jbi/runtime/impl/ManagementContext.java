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
package org.apache.servicemix.jbi.runtime.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.runtime.Environment;

/**
 */
public class ManagementContext {

    private static final Log LOGGER = LogFactory.getLog(ComponentRegistryImpl.class);
    
    private String jmxDomainName;
    private Environment environment;

    public String getJmxDomainName() {
        return jmxDomainName;
    }

    public void setJmxDomainName(String jmxDomainName) {
        this.jmxDomainName = jmxDomainName;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public ObjectName createCustomComponentMBeanName(String type, String name) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        result.put("Type", "Component");
        result.put("Name", sanitizeString(name));
        result.put("SubType", sanitizeString(type));
        return createObjectName(result);
    }

    /**
     * Create an ObjectName
     *
     * @param props
     * @return the ObjectName
     */
    public ObjectName createObjectName(Map<String, String> props) {
        return createObjectName(getJmxDomainName(), props);
    }

    /**
     * The ':' and '/' characters are reserved in ObjectNames
     *
     * @param in
     * @return sanitized String
     */
    public static String sanitizeString(String in) {
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

    /**
     * Create an ObjectName
     *
     * @param domain
     *
     * @return the ObjectName
     */
    public static ObjectName createObjectName(String domain, Map<String, String> props) {
        StringBuffer sb = new StringBuffer();
        sb.append(domain).append(':');
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
            LOGGER.error(error, e);
            throw new RuntimeException(error);
        }
        return result;
    }

    public void registerMBean(ObjectName objectName, Object object, Class interfaceMBean, String description) {
        // TODO
    }

    public void unregisterMBean(Object object) {
        //To change body of created methods use File | Settings | File Templates.
    }
}
