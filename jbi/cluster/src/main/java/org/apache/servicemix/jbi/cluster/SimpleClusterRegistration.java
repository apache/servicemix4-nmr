package org.apache.servicemix.jbi.cluster;

import java.util.List;
import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.core.util.MapToDictionary;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Filter;

public class SimpleClusterRegistration implements ClusterRegistration {

    private Endpoint endpoint;
    private QName interfaceName;
    private QName serviceName;
    private String endpointName;
    private String name;
    private String filter;

    private transient Filter osgiFilter;

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public QName getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(QName interfaceName) {
        this.interfaceName = interfaceName;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public void init() throws Exception {
        if (endpoint == null) {
            List<String> filters = new ArrayList<String>();
            if (filter != null) {
                filters.add(filter);
            }
            if (interfaceName != null) {
                filters.add(Endpoint.INTERFACE_NAME + "=" + interfaceName.toString());
            }
            if (serviceName != null) {
                filters.add(Endpoint.SERVICE_NAME + "=" + serviceName.toString());
            }
            if (endpointName != null) {
                filters.add(Endpoint.SERVICE_NAME + "=" + endpointName);
            }
            if (name != null) {
                filters.add(Endpoint.NAME + "=" + name);
            }
            if (filters.isEmpty()) {
                throw new IllegalArgumentException("one field to match on must be set");
            }
            String f;
            if (filters.size() == 1) {
                f = filters.get(0);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("(&");
                for (String sf : filters) {
                    if (sf.startsWith("(")) {
                        sb.append(sf);
                    } else {
                        sb.append("(").append(sf).append(")");
                    }
                }
                sb.append(")");
                f = sb.toString();
            }
            osgiFilter = FrameworkUtil.createFilter(f);
        }
    }

    public boolean match(InternalEndpoint source) {
        if (endpoint != null) {
            return source.getEndpoint() == endpoint;
        } else {
            return osgiFilter.match(new MapToDictionary(source.getMetaData()));
        }
    }

}
