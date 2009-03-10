package org.apache.servicemix.nmr.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

import org.w3c.dom.Document;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.EndpointRegistry;
import org.apache.servicemix.nmr.api.internal.InternalEndpoint;
import org.apache.servicemix.nmr.core.util.MapToDictionary;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Mar 10, 2009
 * Time: 6:07:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class FilterMatchingReference implements CacheableReference, Serializable {

    private final String filter;
    private transient Filter osgiFilter;
    private transient volatile List<InternalEndpoint> matches;
    private transient EndpointRegistry registry;

    public FilterMatchingReference(String filter) throws InvalidSyntaxException {
        this.filter = filter;
        this.osgiFilter = org.osgi.framework.FrameworkUtil.createFilter(filter);
    }

    public Iterable<InternalEndpoint> choose(EndpointRegistry registry) {
        List<InternalEndpoint> result = matches;
        if (result == null || this.registry != registry) {
            result = new ArrayList<InternalEndpoint>();
            for (Endpoint ep : registry.query(null)) {
                InternalEndpoint iep = (InternalEndpoint) ep;
                if (match(registry, iep)) {
                    result.add(iep);
                }
            }
            this.registry = registry;
            this.matches = result;
        }
        return result;
    }

    protected boolean match(EndpointRegistry registry, InternalEndpoint endpoint) {
        Map<String, ?> epProps = registry.getProperties(endpoint);
        if (osgiFilter == null) {
            synchronized (this) {
                if (osgiFilter == null) {
                    try {
                        this.osgiFilter = org.osgi.framework.FrameworkUtil.createFilter(filter);
                    } catch (InvalidSyntaxException e) {
                        // should not happen as this has been checked in the constructor
                    }
                }
            }
        }
        return osgiFilter.match(new MapToDictionary(epProps));
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
        return "FilterMatchingReference[" + filter + "]";
    }
}
