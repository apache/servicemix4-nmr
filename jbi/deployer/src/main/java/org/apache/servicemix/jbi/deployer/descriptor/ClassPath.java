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
package org.apache.servicemix.jbi.deployer.descriptor;

import java.util.Arrays;
import java.util.List;

/**
 * @version $Revision: 426415 $
 */
public class ClassPath {
    
    private String[] pathElements = new String[] {};

    public ClassPath() {
    }

    public ClassPath(String[] pathElements) {
        this.pathElements = pathElements;
    }

    public String[] getPathElements() {
        return pathElements;
    }

    public void setPathElements(String[] pathElements) {
        this.pathElements = pathElements;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClassPath)) {
            return false;
        }
        final ClassPath classPath = (ClassPath) o;
        if (!Arrays.equals(pathElements, classPath.pathElements)) {
            return false;
        }
        return true;
    }
    
    //SM-199: Hashcode method added
    public int hashCode() {
        if (pathElements == null) {
            return 0;
        }
        int result = 1;
        for (int i=0;i < pathElements.length;i++) {
            result = 31 * result + (pathElements[i] == null ? 0 : pathElements[i].hashCode());
        }
        return result;    	
    }
    
    
    public String toString() {
        StringBuffer buffer = new StringBuffer("ClassPath[");
        for (int i = 0; i < pathElements.length; i++) {
            String pathElement = pathElements[i];
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(pathElement);
        }
        return buffer.toString();
    }

    public List<String> getPathList() {
        return Arrays.asList(pathElements);
    }

    public void setPathList(List<String> list) {
        pathElements = new String[list.size()];
        list.toArray(pathElements);
    }
}
