package org.apache.servicemix.nmr.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Mar 10, 2009
 * Time: 5:57:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropertyMatchingReference implements CacheableReference, Serializable {

    private final Map<String, ?> properties;
    private transient volatile List<InternalEndpoint> matches;
    private transient EndpointRegistry registry;

    public PropertyMatchingReference(Map<String, ?> properties) {
        this.properties = properties;
    }

    public Iterable<InternalEndpoint> choose(EndpointRegistry registry) {
        if (this.matches == null || this.registry != registry) {
            List<InternalEndpoint> eps = new ArrayList<InternalEndpoint>();
            for (Endpoint ep : registry.query(null)) {
                InternalEndpoint iep = (InternalEndpoint) ep;
                if (match(registry, iep)) {
                    eps.add(iep);
                }
            }
            this.registry = registry;
            this.matches = eps;
        }
        return matches;
    }

    protected boolean match(EndpointRegistry registry, InternalEndpoint endpoint) {
        Map<String, ?> epProps = registry.getProperties(endpoint);
        for (Map.Entry<String, ?> name : properties.entrySet()) {
            if (!name.getValue().equals(epProps.get(name.getKey()))) {
                return false;
            }
        }
        return true;
    }

    public Document toXml() {
        // TODO
        return null;
    }

    public void setDirty() {
        matches = null;
    }

    @Override
    public String toString() {
        return "PropertyMatchingReference[" + properties + "]";
    }
}
