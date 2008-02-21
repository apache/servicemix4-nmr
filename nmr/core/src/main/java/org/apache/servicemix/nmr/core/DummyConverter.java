package org.apache.servicemix.nmr.core;

/**
 * Created by IntelliJ IDEA.
* User: gnodet
* Date: Feb 21, 2008
* Time: 10:43:10 PM
* To change this template use File | Settings | File Templates.
*/
class DummyConverter implements Converter {
    public <T> T convert(Object body, Class<T> type) {
        return null;
    }
}
