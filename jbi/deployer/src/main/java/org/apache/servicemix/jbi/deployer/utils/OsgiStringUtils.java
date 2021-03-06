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
package org.apache.servicemix.jbi.deployer.utils;

import java.util.Dictionary;

import org.osgi.framework.Bundle;

public class OsgiStringUtils {

    private static final String NULL_STRING = "null";

    /**
     * Returns the bundle name and symbolic name - useful when logging bundle
     * info.
     *
     * @param bundle OSGi bundle (can be null)
     * @return the bundle name and symbolic name
     */
    public static String nullSafeNameAndSymName(Bundle bundle) {
        if (bundle == null)
            return NULL_STRING;

        Dictionary dict = bundle.getHeaders();

        if (dict == null)
            return NULL_STRING;

        StringBuffer buf = new StringBuffer();
        String name = (String) dict.get(org.osgi.framework.Constants.BUNDLE_NAME);
        if (name == null)
            buf.append(NULL_STRING);
        else
            buf.append(name);
        buf.append(" (");
        String sname = (String) dict.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME);

        if (sname == null)
            buf.append(NULL_STRING);
        else
            buf.append(sname);

        buf.append(")");

        return buf.toString();
    }

}
