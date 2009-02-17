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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * File utilities
 *
 * @version $Revision: 564900 $
 */
public final class FileUtil {

    /**
     * Buffer size used when copying the content of an input stream to
     * an output stream.
     */
    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private FileUtil() {
    }

    /**
     * Build a directory path - creating directories if neccesary
     *
     * @param file
     * @return true if the directory exists, or making it was successful
     */
    public static boolean buildDirectory(File file) {
        return file.exists() || file.mkdirs();
    }

    /**
     * Copy in stream to an out stream
     *
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int len = in.read(buffer);
        while (len >= 0) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
    }

    /**
     * Unpack a zip file
     *
     * @param theFile
     * @param targetDir
     * @throws IOException
     */
    public static void unpackArchive(File theFile, File targetDir) throws IOException {
        if (!theFile.exists()) {
            throw new IOException(theFile.getAbsolutePath() + " does not exist");
        }
        InputStream is = new FileInputStream(theFile);
        try {
            unpackArchive(is, targetDir);
        } finally {
            is.close();
        }
    }

    /**
     * Unpack an archive from a URL
     *
     * @param url
     * @param targetDir
     * @return the file to the url
     * @throws IOException
     */
    public static void unpackArchive(URL url, File targetDir) throws IOException {
        InputStream is = url.openStream();
        try {
            unpackArchive(is, targetDir);
        } finally {
            try {
                is.close();
            } catch (Throwable t) {
            }
        }
    }

    /**
     * Unpack an archive from an input stream
     *
     * @param is
     * @param targetDir
     * @throws IOException
     */
    public static void unpackArchive(InputStream is, File targetDir) throws IOException {
        if (!buildDirectory(targetDir)) {
            throw new IOException("Could not create directory: " + targetDir);
        }
        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(is));
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            File file = new File(targetDir, File.separator + entry.getName());
            // Take the sledgehammer approach to creating directories
            // to work around ZIP's that incorrectly miss directories
            if (!buildDirectory(file.getParentFile())) {
                throw new IOException("Could not create directory: " + file.getParentFile());
            }
            if (!entry.isDirectory()) {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                copyInputStream(zip, bos);
                bos.close();
            } else {
                if (!buildDirectory(file)) {
                    throw new IOException("Could not create directory: " + file);
                }
            }
        }
    }

    /**
     * Delete a file
     *
     * @param fileToDelete
     * @return true if the File is deleted
     */
    public static boolean deleteFile(File fileToDelete) {
        if (fileToDelete == null || !fileToDelete.exists()) {
            return true;
        }
        boolean result = true;
        if (fileToDelete.isDirectory()) {
            File[] files = fileToDelete.listFiles();
            if (files == null) {
                result = false;
            } else {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (file.getName().equals(".") || file.getName().equals("..")) {
                        continue;
                    }
                    if (file.isDirectory()) {
                        result &= deleteFile(file);
                    } else {
                        result &= file.delete();
                    }
                }
            }
        }
        result &= fileToDelete.delete();
        return result;
    }

}