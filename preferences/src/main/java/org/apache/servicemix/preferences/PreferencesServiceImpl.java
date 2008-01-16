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
package org.apache.servicemix.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 15, 2008
 * Time: 2:40:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class PreferencesServiceImpl implements PreferencesService {

    private static final String SYSTEM_NODE = "system";

    private AbstractPreferencesImpl preferences;

    public PreferencesServiceImpl(AbstractPreferencesImpl preferences) {
        this.preferences = preferences;
    }

    public Preferences getSystemPreferences() {
        return preferences.node(SYSTEM_NODE);
    }

    public Preferences getUserPreferences(String s) {
        return preferences.node(s);
    }

    public String[] getUsers() {
        try {
            List<String> children = new ArrayList(Arrays.asList(preferences.childrenNames()));
            children.remove(SYSTEM_NODE);
            return children.toArray(new String[children.size()]);
        } catch (BackingStoreException e) {
            throw new IllegalStateException(e);
        }
    }
}
