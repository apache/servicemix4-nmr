package org.apache.servicemix.jbi.runtime.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 22, 2008
 * Time: 10:15:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class ManagementContext {

    private static final Log LOGGER = LogFactory.getLog(ComponentRegistryImpl.class);
    
    private String jmxDomainName;
    private MBeanServer mbeanServer;
    private List<MBeanServer> mbeanServers;

    public MBeanServer getMbeanServer() {
        if (mbeanServer != null) {
            return mbeanServer;
        }
        if (mbeanServers != null && !mbeanServers.isEmpty()) {
            return mbeanServers.get(0);
        }
        return null;
    }

    public void setMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public List<MBeanServer> getMbeanServers() {
        return mbeanServers;
    }

    public void setMbeanServers(List<MBeanServer> mbeanServers) {
        this.mbeanServers = mbeanServers;
    }

    public String getJmxDomainName() {
        return jmxDomainName;
    }

    public void setJmxDomainName(String jmxDomainName) {
        this.jmxDomainName = jmxDomainName;
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

}
