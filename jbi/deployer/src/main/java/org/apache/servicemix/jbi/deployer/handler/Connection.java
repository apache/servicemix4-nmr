/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jbi.deployer.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.deployer.utils.FileUtil;

/**
 * A URL connection to handle the JBI to OSGi transformation
 * using a URL.
 */
public class Connection extends URLConnection {

    private static Log logger = LogFactory.getLog(Connection.class);

    private final Parser parser;

    public Connection(URL url) throws MalformedURLException {
        super(url);
        this.parser = new Parser(url.getPath());
    }


    /**
     * Connect method.  Nothing to do in our case.
     */
    @Override
    public void connect() {
    }

    /**
     * Retrieve an InputStream on the OSGi bundle.
     *
     * @return an InputStream used to read the transformation output.
     * @throws IOException if an error occurs when transforming the JBI artifact.
     */
    @Override
    public InputStream getInputStream() throws IOException {
        try {
            InputStream targetInputStream = parser.getJbiJarURL().openConnection().getInputStream();
            File jbiZipFile = File.createTempFile("jbi", ".zip");
            FileOutputStream jbiZip = new FileOutputStream(jbiZipFile);

            FileUtil.copyInputStream(targetInputStream, jbiZip);
            jbiZip.close();
            targetInputStream.close();

            File jbiBundle = File.createTempFile("jbi", ".jar");
            Transformer.transformToOSGiBundle(jbiZipFile, jbiBundle, parser.getJbiProperties());
            return new FileInputStream(jbiBundle);
        } catch (Exception e) {
            logger.error("Error opening jbi protocol artifact", e);
            throw (IOException) new IOException("Error opening jbi protocol artifact").initCause(e);
        }
    }


}
