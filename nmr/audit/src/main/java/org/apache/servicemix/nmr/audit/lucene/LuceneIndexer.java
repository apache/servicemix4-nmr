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
package org.apache.servicemix.nmr.audit.lucene;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


/**
 * Utility class for Lucene API.
 * @author george
 * @since 2.1
 * @version $Revision: 550578 $
 */
public class LuceneIndexer {
    protected Directory directory;

    private File segmentFile;

    public LuceneIndexer() {
    }

    public Directory getDirectory() {
        return directory;
    }

    public void setDirectory(Directory directory) {
        this.directory = directory;
    }

    public void setDirectoryName(File directoryName) throws IOException {
        this.segmentFile = new File(directoryName, "segments");
        this.directory = FSDirectory.getDirectory(directoryName.toString(), !this.segmentFile.exists());
    }

    /**
     * Drop object from Lucene index
     */
    protected void remove(String id) throws IOException {
        synchronized (directory) {
            IndexReader ir = IndexReader.open(directory);
            try {
                ir.deleteDocuments(new Term("org.apache.servicemix.id", id));
            } finally {
                ir.close();
            }
        }
    }

    protected void remove(String[] ids) throws IOException {
        if (ids != null && ids.length > 0) {
            synchronized (directory) {
                IndexReader ir = IndexReader.open(directory);
                try {
                    for (int i = 0; i < ids.length; i++) {
                        ir.deleteDocuments(new Term("org.apache.servicemix.id", ids[i]));
                    }
                } finally {
                    ir.close();
                }
            }
        }
    }

    /**
     * Add object to Lucene index
     */
    public void add(Document lucDoc, String id) throws IOException {
        synchronized (directory) {
            IndexWriter writer = new IndexWriter(directory, new SimpleAnalyzer(), !segmentFile.exists());
            try {
                writer.addDocument(lucDoc);
            } finally {
                writer.close();
            }
        }
    }

    /**
     * called when an existing document is updated.
     */
    public void update(Document lucDoc, String id) throws IOException {
        remove(id);
        add(lucDoc, id);
    }

    public Object search(LuceneCallback lc) throws IOException {
        synchronized (directory) {
            IndexReader ir = IndexReader.open(directory);
            IndexSearcher is = new IndexSearcher(ir);
            try {
                return lc.doCallback(is);
            } finally {
                is.close();
                ir.close();
            }
        }
    }
}