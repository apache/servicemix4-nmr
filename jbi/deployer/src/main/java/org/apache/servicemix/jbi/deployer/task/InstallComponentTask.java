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
package org.apache.servicemix.jbi.deployer.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.servicemix.jbi.deployer.AdminCommandsService;
import org.apache.tools.ant.BuildException;

/**
 * Install a Component
 *
 * @version $Revision$
 */
public class InstallComponentTask extends JbiTask {

    private String file; // file to install

    private String paramsFile;

    private List nestedParams;

    private boolean deferExceptions;

    public boolean isDeferExceptions() {
        return deferExceptions;
    }

    public void setDeferExceptions(boolean deferExceptions) {
        this.deferExceptions = deferExceptions;
    }

    /**
     * @return Returns the file.
     */
    public String getFile() {
        return file;
    }

    /**
     * @param file The file to set.
     */
    public void setFile(String file) {
        this.file = file;
    }

    public String getParams() {
        return paramsFile;
    }

    public void setParams(String params) {
        this.paramsFile = params;
    }

    public Param createParam() {
        Param p = new Param();
        if (nestedParams == null) {
            nestedParams = new ArrayList();
        }
        nestedParams.add(p);
        return p;
    }

    /**
     * execute the task
     *
     * @throws BuildException
     */
    public void doExecute(AdminCommandsService acs) throws Exception {
        if (file == null) {
            throw new BuildException("null file - file should be an archive");
        }
        if (!file.endsWith(".zip") && !file.endsWith(".jar")) {
            throw new BuildException("file: " + file + " is not an archive");
        }
        File archive = new File(file);
        String location = archive.getAbsolutePath();
        if (!archive.isFile()) {
            // if it's not a file, assume it's a url and pass it along
            location = file;
        }
        Properties props = getProperties();
        acs.installComponent(location, props, deferExceptions);
    }

    private Properties getProperties() throws IOException {
        Properties props = new Properties();
        if (paramsFile != null) {
            props.load(new FileInputStream(paramsFile));
        }
        if (nestedParams != null) {
            for (Iterator iter = nestedParams.iterator(); iter.hasNext();) {
                Param p = (Param) iter.next();
                props.setProperty(p.getName(), p.getValue());
            }
        }
        return props;
    }

    public static class Param {
        private String name;

        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}