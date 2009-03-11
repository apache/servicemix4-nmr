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
import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Status;
import org.apache.servicemix.nmr.audit.AbstractAuditorTest;
import org.apache.servicemix.nmr.audit.jdbc.JdbcAuditor;
import org.apache.servicemix.util.FileUtil;
import org.hsqldb.jdbc.jdbcDataSource;

public class LuceneAuditorTest extends AbstractAuditorTest {

    private DataSource dataSource;

    private Connection connection;

    private File index;

    protected void setUp() throws Exception {
        super.setUp();
        jdbcDataSource ds = new jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:aname");
        ds.setUser("sa");
        dataSource = ds;
        connection = dataSource.getConnection();

        index = new File("target/data/lucene");
        FileUtil.deleteFile(index);
    }

    protected void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    public void testInsertUpdate() throws Exception {
        AbstractAuditorTest.ReceiverEndpoint receiver = createReceiver(nmr, false, false);

        JdbcAuditor jdbcAuditor = new JdbcAuditor();
        jdbcAuditor.setDataSource(dataSource);
        jdbcAuditor.afterPropertiesSet();
        LuceneAuditor auditor = new LuceneAuditor();
        auditor.setDelegatedAuditor(jdbcAuditor);
        LuceneIndexer indexer = new LuceneIndexer();
        indexer.setDirectoryName(index);
        auditor.setLuceneIndexer(indexer);
        nmr.getListenerRegistry().register(auditor, null);

        auditor.deleteAllExchanges();

        sendExchange(new StringSource("<hello>world</hello>"));

        int nbMessages = auditor.getExchangeCount();
        assertEquals(1, nbMessages);
        Exchange[] exchanges = auditor.getExchangesByRange(0, 1);
        assertNotNull(exchanges);
        assertEquals(1, exchanges.length);
        assertEquals(Status.Done, exchanges[0].getStatus());

        System.err.println(exchanges[0].display(true));

        String[] ids = auditor.findExchangesIdsByMessageContent("in", "world");
        assertNotNull(ids);
        assertEquals(1, ids.length);
        exchanges = auditor.getExchangesByIds(ids);
        assertNotNull(exchanges);
        assertEquals(1, exchanges.length);
        assertEquals(Status.Done, exchanges[0].getStatus());

        ids = auditor.findExchangesIdsByMessageHeader("in", "prop1", "val*");
        assertNotNull(ids);
        assertEquals(1, ids.length);

        ids = auditor.findExchangesIdsByProperty("prop1", "val*");
        assertNotNull(ids);
        assertEquals(1, ids.length);

        ids = auditor.findExchangesIdsByQuery("(properties.prop1: val*) AND (in.content: hello)");
        assertNotNull(ids);
        assertEquals(1, ids.length);

        ids = auditor.findExchangesIdsByQuery("id: " + ids[0]);
        assertNotNull(ids);
        assertEquals(1, ids.length);

        ids = auditor.findExchangesIdsByQuery("mep: InOnly");
        assertNotNull(ids);
        assertEquals(1, ids.length);

        ids = auditor.findExchangesIdsByQuery("status: Done");
        assertNotNull(ids);
        assertEquals(1, ids.length);

        // TODO: reenable this when resendExchange is implemented

//        auditor.resendExchange(exchanges[0]);
//
//        nbMessages = auditor.getExchangeCount();
//        assertEquals(2, nbMessages);
//        Exchange exchange = auditor.getExchangeByIndex(1);
//        assertNotNull(exchange);
//        assertEquals(Status.Done, exchange.getStatus());
    }

}
