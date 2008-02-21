package org.apache.servicemix.nmr.core;

/**
 * Created by IntelliJ IDEA.
* User: gnodet
* Date: Feb 21, 2008
* Time: 10:42:57 PM
* To change this template use File | Settings | File Templates.
*/
class CamelConverter implements Converter {
    final org.apache.camel.impl.converter.DefaultTypeConverter tc =
            new org.apache.camel.impl.converter.DefaultTypeConverter(
                    new org.apache.camel.impl.ReflectionInjector());
    public <T> T convert(Object body, Class<T> type) {
        return tc.convertTo(type, body);
    }
}
