package org.apache.servicemix.nmr.core;

/**
 * Created by IntelliJ IDEA.
* User: gnodet
* Date: Feb 21, 2008
* Time: 10:42:38 PM
* To change this template use File | Settings | File Templates.
*/
interface Converter {
    <T> T convert(Object body, Class<T> type);
}
