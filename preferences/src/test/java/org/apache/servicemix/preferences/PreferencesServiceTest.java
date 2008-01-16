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

import java.io.File;

import org.junit.Test;
import static org.junit.Assert.*;
import org.osgi.service.prefs.PreferencesService;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 16, 2008
 * Time: 10:46:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class PreferencesServiceTest {

    @Test
    public void test1() throws Exception {
        PreferencesService prefs = new PreferencesServiceImpl(new FilePreferencesImpl(null, "3", new File("target/prefs/3")));

        prefs.getSystemPreferences().put("key", "value");
        prefs.getUserPreferences("user1").put("key", "value2");

        assertNotNull(prefs.getUsers());
        assertEquals(1, prefs.getUsers().length);
        assertEquals("user1", prefs.getUsers()[0]);
        assertEquals("value2", prefs.getUserPreferences("user1").get("key", null));
        assertEquals("value", prefs.getSystemPreferences().get("key", null));

        prefs.getSystemPreferences().flush();
        prefs.getUserPreferences("user1").flush();

        prefs = new PreferencesServiceImpl(new FilePreferencesImpl(null, "3", new File("target/prefs/3")));
        assertNotNull(prefs.getUsers());
        assertEquals(1, prefs.getUsers().length);
        assertEquals("user1", prefs.getUsers()[0]);
        assertEquals("value2", prefs.getUserPreferences("user1").get("key", null));
        assertEquals("value", prefs.getSystemPreferences().get("key", null));
    }

}
