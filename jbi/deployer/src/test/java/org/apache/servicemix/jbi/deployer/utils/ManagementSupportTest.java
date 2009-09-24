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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import junit.framework.TestCase;

/**
 * Test cases for {@link ManagementSupport}
 */
public class ManagementSupportTest extends TestCase {
    
    /*
     * Test if wrap function returns valid xml 
     */
    public void testWrap() throws Exception {
        assertValidXml(ManagementSupport.wrap("<test/>"));
        assertValidXml(ManagementSupport.wrap("<?xml version='1.0'?><test/>"));
    }
    
    public void assertValidXml(String xml) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try {
            builder.parse(new ByteArrayInputStream(xml.getBytes()));
        } catch (SAXException e) {
            fail(xml + " is not valid XML: " + e.getMessage());
        } catch (IOException e) {
            fail(xml + " is not valid XML: " + e.getMessage());
        }
    }
}
