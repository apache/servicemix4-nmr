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
package org.apache.servicemix.nmr.audit.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javax.sql.DataSource;

import org.apache.servicemix.jdbc.JDBCAdapter;
import org.apache.servicemix.jdbc.JDBCAdapterFactory;
import org.apache.servicemix.jdbc.Statements;
import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Type;
import org.apache.servicemix.nmr.api.Message;
import org.apache.servicemix.nmr.audit.AbstractAuditor;
import org.apache.servicemix.nmr.audit.AuditorException;
import org.springframework.beans.factory.InitializingBean;

/**
 * Basic implementation of ServiceMix auditor on a jdbc store.
 * This implementation, for performance purposes, only relies
 * on one table SM_AUDIT with two columns:
 * <ul>
 *   <li><b>ID</b> the exchange id (varchar)</li>
 *   <li><b>EXCHANGE</b> the serialized exchange (blob)</li>
 * </ul>
 *
 * @org.apache.xbean.XBean element="jdbcAuditor" description="The Auditor of message exchanges to a JDBC database"
 * 
 * @author Guillaume Nodet (gnt)
 * @version $Revision: 550578 $
 * @since 2.1
 */
public class JdbcAuditor extends AbstractAuditor implements InitializingBean {

    private DataSource dataSource;
    private boolean autoStart = true;
    private Statements statements;
    private String tableName = "SM_AUDIT";
    private JDBCAdapter adapter;
    private boolean createDataBase = true;
    private Set<String> nonSerializableClasses = new HashSet<String>();
    private ClassLoader tccl;
    
    public String getDescription() {
        return "JDBC Auditing Service";
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.dataSource == null) {
            throw new IllegalArgumentException("dataSource should not be null");
        }
        if (statements == null) {
            statements = new Statements();
            statements.setStoreTableName(tableName);
        }
        Connection connection = null;
        boolean restoreAutoCommit = false;
        try {
            connection = getDataSource().getConnection();
            if (connection.getAutoCommit()) {
                connection.setAutoCommit(false);
                restoreAutoCommit = true;
            }
            adapter = JDBCAdapterFactory.getAdapter(connection);
            if (statements == null) {
                statements = new Statements();
                statements.setStoreTableName(tableName);
            }
            adapter.setStatements(statements);
            if (createDataBase) {
                adapter.doCreateTables(connection);
            }
            connection.commit();
        } catch (SQLException e) {
            throw (IOException) new IOException("Exception while creating database").initCause(e); 
        } finally {
            close(connection, restoreAutoCommit);
        }
        this.tccl = Thread.currentThread().getContextClassLoader();
    }
    
    public void exchangeSent(Exchange exchange) {
        try {
            String id = exchange.getId();
            Connection connection = null;
            boolean restoreAutoCommit = false;
            try {
                connection = dataSource.getConnection();
                if (connection.getAutoCommit()) {
                    connection.setAutoCommit(false);
                    restoreAutoCommit = true;
                }
                store(connection, id, getDataForExchange(exchange));
                connection.commit();
            } finally {
                close(connection, restoreAutoCommit);
            }
        } catch (Exception e) {
            log.error("Could not persist exchange", e);
        }
    }

    protected byte[] getDataForExchange(Exchange exchange) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(checkSerializable(exchange));
        os.close();
        return baos.toByteArray();
    }

    protected Exchange checkSerializable(Exchange exchange) {
        boolean isSerializable = isMapSerializable(exchange.getProperties());
        if (isSerializable) {
            for (Type t : Type.values()) {
                Message m = exchange.getMessage(t, false);
                if (m != null) {
                    if (!isMapSerializable(m.getHeaders())) {
                        isSerializable = false;
                        break;
                    }
                }
            }
        }
        if (!isSerializable) {
            exchange = exchange.copy();
            makeMapSerializable(exchange.getProperties());
            for (Type t : Type.values()) {
                Message m = exchange.getMessage(t, false);
                if (m != null) {
                    makeMapSerializable(m.getHeaders());
                }
            }
        }
        return exchange;
    }

    protected boolean isMapSerializable(Map<String,Object> map) {
        for (Object o : map.values()) {
            if (o != null && !(o instanceof Serializable)) {
                return false;
            }
        }
        return true;
    }

    protected void makeMapSerializable(Map<String,Object> map) {
        List<String> badEntries = new ArrayList<String>();
        for (Map.Entry<String,Object> entry : map.entrySet()) {
            if (entry.getValue() != null && !(entry.getValue() instanceof Serializable)) {
                warnAboutNonSerializableClass(entry.getValue());
                badEntries.add(entry.getKey());
            }
        }
        for (String key : badEntries) {
            map.remove(key);
        }
    }

    protected void warnAboutNonSerializableClass(Object o) {
        boolean added;
        synchronized (nonSerializableClasses) {
            added = nonSerializableClasses.add(o.getClass().getName());
        }
        if (added) {
            log.warn("Properties of types '" + o.getClass().getName() + "' will be removed from the audit log as they are not serializable");
        }
    }

    protected void store(Connection connection, String id, byte[] data) throws Exception {
        if (adapter.doLoadData(connection, id) != null) {
            adapter.doUpdateData(connection, id, data);
        } else {
            adapter.doStoreData(connection, id, data);
        }
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchangeCount()
     */
    public int getExchangeCount() throws AuditorException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            return adapter.doGetCount(connection);
        } catch (Exception e) {
            throw new AuditorException("Could not retrieve exchange count", e);
        } finally {
            close(connection, false);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchangeIds(int, int)
     */
    public String[] getExchangeIdsByRange(int fromIndex, int toIndex) throws AuditorException {
        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex should be greater or equal to zero");
        }
        if (toIndex < fromIndex) {
            throw new IllegalArgumentException("toIndex should be greater or equal to fromIndex");
        }
        // Do not hit the database if no ids are requested
        if (fromIndex == toIndex) {
            return new String[0];
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            return adapter.doGetIds(connection, fromIndex, toIndex);
        } catch (Exception e) {
            throw new AuditorException("Could not retrieve exchange ids", e);
        } finally {
            close(connection, false);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#getExchanges(java.lang.String[])
     */
    public Exchange[] getExchangesByIds(String[] ids) throws AuditorException {
        Exchange[] exchanges = new Exchange[ids.length];
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            for (int row = 0; row < ids.length; row++) {
                exchanges[row] = getExchange(adapter.doLoadData(connection, ids[row]));
            }
            return exchanges;
        } catch (Exception e) {
            throw new AuditorException("Could not retrieve exchanges", e);
        } finally {
            close(connection, false);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.nmr.audit.AuditorMBean#deleteExchanges(java.lang.String[])
     */
    public int deleteExchangesByIds(String[] ids) throws AuditorException {
        Connection connection = null;
        boolean restoreAutoCommit = false;
        try {
            connection = dataSource.getConnection();
            if (connection.getAutoCommit()) {
                connection.setAutoCommit(false);
                restoreAutoCommit = true;
            }
            for (int row = 0; row < ids.length; row++) {
                adapter.doRemoveData(connection, ids[row]);
            }
            connection.commit();
            return ids.length;
        } catch (Exception e) {
            throw new AuditorException("Could not delete exchanges", e);
        } finally {
            close(connection, restoreAutoCommit);
        }
    }
    
    protected Exchange getExchange(byte[] data) throws AuditorException {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(tccl);
            ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(data));
            return (Exchange) is.readObject();
        } catch (Exception e) {
            throw new AuditorException("Unable to reconstruct exchange", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }
    
    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    private static void close(Connection connection, boolean restoreAutoCommit) {
        if (connection != null) {
            try {
                if (restoreAutoCommit) {
                    connection.setAutoCommit(true);
                }
                connection.close();
            } catch (SQLException e) {
                // Do nothing
            }
        }
    }

}
