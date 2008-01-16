package org.apache.servicemix.preferences;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 15, 2008
 * Time: 8:12:34 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Store {

    void setProperty(String key, String value);

    String getProperty(String key);

    void clear();

}
