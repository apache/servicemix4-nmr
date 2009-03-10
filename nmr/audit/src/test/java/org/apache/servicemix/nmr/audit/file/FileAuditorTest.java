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
package org.apache.servicemix.nmr.audit.file;

import java.io.File;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.nmr.audit.AbstractAuditorTest;
import org.apache.servicemix.util.FileUtil;

public class FileAuditorTest extends AbstractAuditorTest {

    private static final File DIRECTORY = new File("target/tests/FileAuditor");
    private final SourceTransformer transformer = new SourceTransformer();

    protected void setUp() throws Exception {
        super.setUp();
        FileUtil.deleteFile(DIRECTORY);
        DIRECTORY.mkdirs();
    }

    public void testFileAuditor() throws Exception {
        ReceiverEndpoint receiver = createReceiver(nmr, false, false);

        FileAuditor auditor = new FileAuditor();
        auditor.setDirectory(DIRECTORY);
        nmr.getListenerRegistry().register(auditor, null);

        sendExchange(new StringSource("<hello>world</hello>"));

        //check if the message has been audited
        assertEquals(1, auditor.getExchangeCount());
        
        sendExchange(transformer.toDOMSource(new StringSource("<hello>world</hello>")));
        
        //check if the message has been audited
        assertEquals(2, auditor.getExchangeCount());
    }

}
