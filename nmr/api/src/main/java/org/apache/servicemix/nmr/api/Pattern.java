/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.nmr.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Pattern of the exchange
 * 
 * @version $Revision: $
 * @since 4.0
 */
public enum Pattern {

    InOnly,
    RobustInOnly,
    InOut,
    InOptionalOut;


    protected static final Map<String, Pattern> map;

    /**
     * Returns the WSDL URI for this message exchange pattern
     *
     * @return the WSDL URI for this message exchange pattern
     */
    public String getWsdlUri() {
        switch (this) {
            case InOnly:
                return "http://www.w3.org/ns/wsdl/in-only";
            case InOptionalOut:
                return "http://www.w3.org/ns/wsdl/in-optional-out";
            case InOut:
                return "http://www.w3.org/ns/wsdl/in-out";
            case RobustInOnly:
                return "http://www.w3.org/ns/wsdl/robust-in-only";
            default:
                throw new IllegalArgumentException("Unknown message exchange pattern: " + this);
        }
    }

    /**
     * Converts the WSDL URI into a {@link Pattern} instance
     */
    public static Pattern fromWsdlUri(String wsdlUri) {
        return map.get(wsdlUri);
    }

    static {
        map = new HashMap<String, Pattern>();
        for (Pattern mep : values()) {
            String uri = mep.getWsdlUri();
            map.put(uri, mep);
            String name = uri.substring(uri.lastIndexOf('/') + 1);
            map.put("http://www.w3.org/2004/08/wsdl/" + name, mep);
            map.put("http://www.w3.org/2006/01/wsdl/" + name, mep);
        }
    }
}