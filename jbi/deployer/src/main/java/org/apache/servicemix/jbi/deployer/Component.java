package org.apache.servicemix.jbi.deployer;

import javax.jbi.management.ComponentLifeCycleMBean;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 3, 2008
 * Time: 3:44:17 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Component extends ComponentLifeCycleMBean {

    /**
     * Retrieves the name of this service assembly
     * @return the name
     */
    String getName();

    /**
     * Retrieves the description of this service assembly
     * @return the description
     */
    String getDescription();

}
