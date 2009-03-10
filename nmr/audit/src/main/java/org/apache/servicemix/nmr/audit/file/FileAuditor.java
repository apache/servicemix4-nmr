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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.audit.AbstractAuditor;
import org.apache.servicemix.nmr.audit.AuditorException;
import org.apache.servicemix.util.FileUtil;

/**
 * Simple implementation of a ServiceMix auditor that stores messages in files in a directory.
 *
 * Currently, the file auditor will only store the message body for ACTIVE exchanges.
 * 
 * @org.apache.xbean.XBean element="fileAuditor" description="The Auditor of message exchanges to a directory"
 */
public class FileAuditor extends AbstractAuditor {

    private static final Log LOG = LogFactory.getLog(FileAuditor.class);
    private File directory;
    private FileAuditorStrategy strategy = new FileAuditorStrategyImpl();

    /**
     * The directory used for storing the audited messages
     * 
     * @param directory
     *            the directory
     */
    public void setDirectory(File directory) {
        if (!directory.exists()) {
            LOG.info("Creating directory " + directory);
            directory.mkdirs();
        }
        this.directory = directory;
    }

    /**
     * {@inheritDoc}
     */
    public void exchangeSent(Exchange exchange) {
        try {
            if (exchange.getStatus() == Status.Active) {
                OutputStream os = getOutputStream(exchange);
                os.write(exchange.display(true).getBytes());
                os.close();
            }
        } catch (Exception e) {
            LOG.error(String.format("Error occurred while storing message %s", exchange.getId()), e);
        }
    }

    /*
     * Get the outputstream for writing the message content
     */
    private OutputStream getOutputStream(Exchange exchange) throws FileNotFoundException {
        File file = new File(directory, strategy.getFileName(exchange));
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return new BufferedOutputStream(new FileOutputStream(file));
    }

    @Override
    public int deleteExchangesByIds(String[] ids) throws AuditorException {
        throw new AuditorException("deleteExchangesById(s) currently unsupported by FileAuditor");
    }

    @Override
    public int getExchangeCount() throws AuditorException {
        return FileUtil.countFilesInDirectory(directory);
    }

    @Override
    public String[] getExchangeIdsByRange(int fromIndex, int toIndex) throws AuditorException {
        throw new AuditorException("getExchangeIdsByRange currently unsupported by FileAuditor");
    }

    @Override
    public Exchange[] getExchangesByIds(String[] ids) throws AuditorException {
        throw new AuditorException("getExchangeByIds currently unsupported by FileAuditor");
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "File-based auditing service";
    }

    /*
     * Default FileAuditorStrategy implementation, writing audit files in a folder per day
     */
    private class FileAuditorStrategyImpl implements FileAuditorStrategy {
        
        private final DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
        
        public String getFileName(Exchange exchange) {
            return dateformat.format(new Date()) + File.separatorChar + exchange.getId().replaceAll("[:\\.]", "_");
        }
    }
}
