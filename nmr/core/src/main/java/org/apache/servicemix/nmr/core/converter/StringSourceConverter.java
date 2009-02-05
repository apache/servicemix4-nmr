package org.apache.servicemix.nmr.core.converter;

import org.apache.camel.Converter;
import org.apache.servicemix.nmr.core.util.StringSource;

@Converter
public class StringSourceConverter {

    @Converter
    public static String toString(StringSource source) {
        return source.getText();
    }
}
