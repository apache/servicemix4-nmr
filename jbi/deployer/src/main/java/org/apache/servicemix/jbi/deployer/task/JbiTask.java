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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.servicemix.jbi.deployer.AdminCommandsService;
import org.apache.servicemix.jbi.deployer.impl.AdminService;
import org.apache.servicemix.jbi.deployer.impl.DefaultNamingStrategy;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * A bean for connecting to a remote JMX MBeanServer
 *
 * @version $Revision$
 */
public abstract class JbiTask extends Task {

    private String serverProtocol = "rmi";

    private String host = "localhost";

    private String containerName = AdminService.DEFAULT_NAME;

    private String jmxDomainName = AdminService.DEFAULT_DOMAIN;

    private int port = AdminService.DEFAULT_CONNECTOR_PORT;

    private String jndiPath = AdminService.DEFAULT_CONNECTOR_PATH;

    private String username;

    private String password;

    private boolean failOnError = true;

    private JMXConnector jmxConnector;


    /**
     * Get the JMXServiceURL - built from the protocol used and host names
     *
     * @return the url
     */
    public JMXServiceURL getServiceURL() throws MalformedURLException {
        JMXServiceURL url;
        url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + jndiPath);
        return url;
    }

    /**
     * Get a JMXConnector from a url
     *
     * @param url
     * @return the JMXConnector
     * @throws IOException
     */
    public JMXConnector getJMXConnector(JMXServiceURL url) throws IOException {
        String[] credentials = new String[]{getUsername(), getPassword()};
        Map<String, Object> environment = new HashMap<String, Object>();
        environment.put(JMXConnector.CREDENTIALS, credentials);
        return JMXConnectorFactory.connect(url, environment);
    }

    /**
     * initialize the connection
     *
     * @throws BuildException
     */
    public void connect() throws IOException {
        this.jmxConnector = getJMXConnector(getServiceURL());
    }

    /**
     * close any internal remote connections
     */
    public void close() {
        if (this.jmxConnector != null) {
            try {
                jmxConnector.close();
            } catch (IOException e) {
                log("Caught an error closing the jmxConnector" + e.getMessage(), Project.MSG_WARN);
            }
        }
    }

    /**
     * Get the AdminCommandsService
     *
     * @return the main administration service MBean
     * @throws IOException
     */
    public AdminCommandsService getAdminCommandsService() throws IOException {
        ObjectName objectName = DefaultNamingStrategy.getSystemObjectName(jmxDomainName, containerName,
                AdminCommandsService.class);
        return (AdminCommandsService) MBeanServerInvocationHandler.newProxyInstance(
                jmxConnector.getMBeanServerConnection(), objectName,
                AdminCommandsService.class, true);
    }

    /**
     * @return Returns the containerName.
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * @param containerName The containerName to set.
     */
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    /**
     * @return Returns the jmxDomainName.
     */
    public String getJmxDomainName() {
        return jmxDomainName;
    }

    /**
     * @param jmxDomainName The jmxDomainName to set.
     */
    public void setJmxDomainName(String jmxDomainName) {
        this.jmxDomainName = jmxDomainName;
    }

    /**
     * @return Returns the jndiPath.
     */
    public String getJndiPath() {
        return jndiPath;
    }

    /**
     * @param jndiPath The jndiPath to set.
     */
    public void setJndiPath(String jndiPath) {
        this.jndiPath = jndiPath;
    }

    /**
     * @return Returns the namingHost.
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host The namingHost to set.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return Returns the namingPort.
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port The namingPort to set.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return Returns the serverProtocol.
     */
    public String getServerProtocol() {
        return serverProtocol;
    }

    /**
     * @param serverProtocol The serverProtocol to set.
     */
    public void setServerProtocol(String serverProtocol) {
        this.serverProtocol = serverProtocol;
    }

    /**
     * @return Returns the passwd.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password The passwd to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return Returns the username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username The username to set.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return Returns the failOnError.
     */
    public boolean isFailOnError() {
        return failOnError;
    }

    /**
     * @param failOnError The failOnError to set.
     */
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * execute the task
     *
     * @throws BuildException
     */
    public void execute() throws BuildException {
        AdminCommandsService acs;
        try {
            log("Retrieving remote admin interface", Project.MSG_DEBUG);
            connect();
            acs = getAdminCommandsService();
        } catch (Throwable e) {
            log("Error accessing ServiceMix administration: " + e.getMessage(), Project.MSG_WARN);
            if (isFailOnError()) {
                throw new BuildException("Error accessing ServiceMix administration", e);
            } else {
                return;
            }
        }
        try {
            log("Executing command", Project.MSG_DEBUG);
            doExecute(acs);
        } catch (Throwable e) {
            log("Error executing command: " + e.getMessage(), Project.MSG_WARN);
            if (isFailOnError()) {
                throw new BuildException("Error accessing ServiceMix administration", e);
            } else {
                return;
            }
        }
    }

    protected abstract void doExecute(AdminCommandsService acs) throws Exception;

}