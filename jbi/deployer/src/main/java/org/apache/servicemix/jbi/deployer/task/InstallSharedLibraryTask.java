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

import org.apache.servicemix.jbi.deployer.AdminCommandsService;
import org.apache.tools.ant.BuildException;

/**
 * Install a shared library
 *
 * @version $Revision$
 */
public class InstallSharedLibraryTask extends JbiTask {

    private String file; // shared library URI to install

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
     * @param file The shared library URI to set.
     */
    public void setFile(String file) {
        this.file = file;
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
        acs.installSharedLibrary(location, deferExceptions);
    }

}