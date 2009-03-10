package org.apache.servicemix.nmr.core;

import org.apache.servicemix.nmr.api.internal.InternalReference;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Mar 10, 2009
 * Time: 6:04:14 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CacheableReference extends InternalReference {

    void setDirty();
}
