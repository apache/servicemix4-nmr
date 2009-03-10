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
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Message;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.api.Type;
import org.apache.servicemix.nmr.api.event.ExchangeListener;
import org.apache.servicemix.nmr.audit.AbstractAuditor;
import org.apache.servicemix.nmr.audit.AuditorException;
import org.apache.servicemix.nmr.audit.AuditorMBean;
import org.apache.servicemix.nmr.audit.AuditorQueryMBean;
import org.apache.servicemix.nmr.core.util.StringSource;

/**
 * Lucene AuditorQuery implementation. It uses Lucene as the indexing mechanism
 * for searching Exchanges and needs a delegated AuditorMBean to persist
 * Exchanges.
 * 
 * The Content of messages are stored as: 
 *  - org.apache.servicemix.in.contents
 *  - org.apache.servicemix.out.contents, if exists
 *  - org.apache.servicemix.fault.contents, if exists
 * 
 * Properties for IN Messages are stored as: 
 *  - org.apache.servicemix.in.propertyname
 *  - org.apache.servicemix.out.propertyname, if exists
 *  - org.apache.servicemix.fault.propertyname, if exists
 * 
 * @author George Gastaldi
 * @since 2.1
 * @version $Revision: 550578 $
 */
public class LuceneAuditor extends AbstractAuditor implements AuditorQueryMBean {

    private AuditorMBean delegatedAuditor;

    private LuceneIndexer luceneIndexer = new LuceneIndexer();

    /**
     * @return Returns the luceneIndexer.
     */
    public LuceneIndexer getLuceneIndexer() {
        return luceneIndexer;
    }

    /**
     * @param luceneIndexer
     *            The luceneIndexer to set.
     */
    public void setLuceneIndexer(LuceneIndexer luceneIndexer) {
        this.luceneIndexer = luceneIndexer;
    }

    /**
     * @return Returns the delegatedAuditor.
     */
    public AuditorMBean getDelegatedAuditor() {
        return delegatedAuditor;
    }

    /**
     * @param delegatedAuditor
     *            The delegatedAuditor to set.
     */
    public void setDelegatedAuditor(AuditorMBean delegatedAuditor) {
        this.delegatedAuditor = delegatedAuditor;
    }

    public int getExchangeCount() throws AuditorException {
        return this.delegatedAuditor.getExchangeCount();
    }

    public String[] getExchangeIdsByRange(int fromIndex, int toIndex) throws AuditorException {
        return this.delegatedAuditor.getExchangeIdsByRange(fromIndex, toIndex);
    }

    public Exchange[] getExchangesByIds(String[] ids) throws AuditorException {
        return this.delegatedAuditor.getExchangesByIds(ids);
    }

    public int deleteExchangesByRange(int fromIndex, int toIndex) throws AuditorException {
        // TODO: Remove ids from Lucene Index
        return this.delegatedAuditor.deleteExchangesByRange(fromIndex, toIndex);
    }

    public int deleteExchangesByIds(String[] ids) throws AuditorException {
        try {
            this.luceneIndexer.remove(ids);
        } catch (IOException io) {
            throw new AuditorException(io);
        }
        return this.delegatedAuditor.deleteExchangesByIds(ids);
    }

    public void exchangeSent(Exchange exchange) {
        try {
            Document doc = createDocument(exchange);
            this.luceneIndexer.add(doc, exchange.getId());
            if (delegatedAuditor instanceof ExchangeListener) {
                ((ExchangeListener) delegatedAuditor).exchangeSent(exchange);
            }
        } catch (Exception e) {
            log.error("Error while adding to lucene", e);
        }
    }

    public String getDescription() {
        return "Lucene Auditor";
    }

    public String[] findExchangesIDsByStatus(Status status) throws AuditorException {
        String field = "org.apache.servicemix.status";
        return getExchangeIds(field, String.valueOf(status));
    }

    public String[] findExchangesIDsByMessageContent(String type, String content) throws AuditorException {
        String field = "org.apache.servicemix." + type + ".content";
        return getExchangeIds(field, content);
    }

    public String[] findExchangesIDsByMessageProperty(String type, 
                                                      String property, 
                                                      String value) throws AuditorException {
        if (property != null && !property.startsWith("org.apache.servicemix")) {
            property = "org.apache.servicemix." + type + ".headers." + property;
        }
        return getExchangeIds(property, value);
    }

    protected Document createDocument(Exchange exchange) throws AuditorException {
        try {
            exchange.ensureReReadable();
            // This could be in a separated class (a LuceneDocumentProvider)
            Document d = new Document();
            d.add(new Field("org.apache.servicemix.id", exchange.getId(), Field.Store.YES, Field.Index.ANALYZED));
            d.add(new Field("org.apache.servicemix.status", String.valueOf(exchange.getStatus()), Field.Store.YES, Field.Index.ANALYZED));

            Type[] types = { Type.In, Type.Out, Type.Fault };
            for (int i = 0; i < types.length; i++) {
                Message message = exchange.getMessage(types[i], false);
                if (message != null) {
                    StringSource src = message.getBody(StringSource.class);
                    d.add(new Field("org.apache.servicemix." + types[i].toString().toLowerCase() + ".content", src.getText(), Field.Store.NO, Field.Index.ANALYZED));
                    addMessagePropertiesToDocument(message, d, types[i]);
                }
            }
            return d;
        } catch (Exception ex) {
            throw new AuditorException("Error while creating Lucene Document", ex);
        }
    }

    protected void addMessagePropertiesToDocument(Message message,
                                                  Document document, 
                                                  Type type) {
        for (Map.Entry<String,Object> entry : message.getHeaders().entrySet()) {
            if (entry.getValue() instanceof String) {
                //org.apache.servicemix.out.myproperty
                document.add(new Field("org.apache.servicemix." + type.toString().toLowerCase() + ".headers." + entry.getKey(), (String) entry.getValue(), Field.Store.NO, Field.Index.ANALYZED));
            }
        }
    }

    public String[] getExchangeIds(String queryContent, String field) throws AuditorException {
        DefaultLuceneCallback dfc = new DefaultLuceneCallback(queryContent, field);
        try {
            return (String[]) luceneIndexer.search(dfc);
        } catch (IOException e) {
            throw new AuditorException("Error while getting Exchange IDs", e);
        }
    }
}
