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

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

/**
 * Default Lucene Callback implementation. Used on LuceneAuditor
 * 
 * @author George Gastaldi (gastaldi)
 * @since 2.1
 * @version $Revision: 550578 $
 */
public class DefaultLuceneCallback implements LuceneCallback {

    public static final int SEARCH_SIZE = 100;

    private String field;

    private String query;

    public DefaultLuceneCallback(String field, String query) {
        this.field = field;
        this.query = query;
    }

    public Object doCallback(IndexSearcher is) throws IOException {
        try {
            QueryParser qp = new QueryParser(field, new StandardAnalyzer());
            Query queryObj = qp.parse(query);
            TopDocs topdocs = is.search(queryObj, SEARCH_SIZE);
            int total = topdocs.totalHits;
            String[] ids = new String[total];
            for (int i = 0; i < total; i++) {
                ScoreDoc d = topdocs.scoreDocs[i];
                ids[i] = is.doc(d.doc).get(LuceneAuditor.FIELD_ID);
            }
            return ids;
        } catch (ParseException pe) {
            throw (IOException) new IOException("Error parsing query").initCause(pe);
        }
    }

}
