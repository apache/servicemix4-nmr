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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.jbi.management.DeploymentException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.runtime.impl.utils.DOMUtil;

/**
 * ManagementMessageHelper is a class that ease the building of management messages.
 */
public final class ManagementSupport {

    public static final String FAILED = "FAILED";
    public static final String ERROR = "ERROR";
    public static final String SUCCESS = "SUCCESS";
    public static final String WARNING = "WARNING";

    public static final String COMPONENT_NAME = "component-name";
    public static final String COMPONENT_TASK_RESULT = "component-task-result";
    public static final String COMPONENT_TASK_RESULT_DETAILS = "component-task-result-details";
    public static final String DEFAULT_VERSION = "1.0";
    public static final String EXCEPTION_INFO = "exception-info";
    public static final String FRMWK_TASK_RESULT = "frmwk-task-result";
    public static final String FRMWK_TASK_RESULT_DETAILS = "frmwk-task-result-details";
    public static final String HTTP_JAVA_SUN_COM_XML_NS_JBI_MANAGEMENT_MESSAGE = "http://java.sun.com/xml/ns/jbi/management-message";
    public static final String JBI_TASK = "jbi-task";
    public static final String JBI_TASK_RESULT = "jbi-task-result";
    public static final String LOC_MESSAGE = "loc-message";
    public static final String LOC_TOKEN = "loc-token";
    public static final String LOCALE = "locale";
    public static final String MESSAGE_TYPE = "message-type";
    public static final String MSG_LOC_INFO = "msg-loc-info";
    public static final String NESTING_LEVEL = "nesting-level";
    public static final String STACK_TRACE = "stack-trace";
    public static final String TASK_ID = "task-id";
    public static final String TASK_RESULT = "task-result";
    public static final String TASK_RESULT_DETAILS = "task-result-details";
    public static final String TASK_STATUS_MSG = "task-status-msg";
    public static final String VERSION = "version";
    public static final String XMLNS = "xmlns";

    private static final Log LOG = LogFactory.getLog(ManagementSupport.class);

    private ManagementSupport() {
    }

    public static class Message {
        private boolean isCauseFramework;
        private String task;
        private String result;
        private Throwable exception;
        private String type;
        private String message;
        private String component;
        private String locale;

        public Throwable getException() {
            return exception;
        }

        public void setException(Throwable exception) {
            this.exception = exception;
        }

        public boolean isCauseFramework() {
            return isCauseFramework;
        }

        public void setCauseFramework(boolean value) {
            this.isCauseFramework = value;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getTask() {
            return task;
        }

        public void setTask(String task) {
            this.task = task;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getComponent() {
            return component;
        }

        public void setComponent(String component) {
            this.component = component;
        }

        public String getLocale() {
            return locale;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }
    }

    public static RuntimeException failure(String task, String info) {
        return failure(task, info, null, null);
    }

    public static RuntimeException failure(String task, List componentResults) {
        return failure(task, null, null, componentResults);
    }

    public static RuntimeException failure(String task, String info, Throwable t) {
        return failure(task, info, t, null);
    }

    public static RuntimeException failure(String task, String info, Throwable t, List<Element> componentResults) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult(FAILED);
        msg.setType(ERROR);
        msg.setException(t);
        msg.setMessage(info);
        return new RuntimeException(ManagementSupport.createFrameworkMessage(msg, componentResults));
    }

    public static String createSuccessMessage(String task) {
        return createSuccessMessage(task, null, null);
    }

    public static String createSuccessMessage(String task, List<Element> componentResults) {
        return createSuccessMessage(task, null, componentResults);
    }

    public static String createSuccessMessage(String task, String info) {
        return createSuccessMessage(task, info, null);
    }

    public static String createSuccessMessage(String task, String info, List<Element> componentResults) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult(SUCCESS);
        msg.setMessage(info);
        return ManagementSupport.createFrameworkMessage(msg, componentResults);
    }

    public static String createWarningMessage(String task, String info, List<Element> componentResults) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult(SUCCESS);
        msg.setType(WARNING);
        msg.setMessage(info);
        return ManagementSupport.createFrameworkMessage(msg, componentResults);
    }

    public static String createFrameworkMessage(Message fmkMsg, List<Element> componentResults) {
        try {
            Document doc = createDocument();
            Element jbiTask = createChild(doc, JBI_TASK);
            jbiTask.setAttribute(XMLNS, HTTP_JAVA_SUN_COM_XML_NS_JBI_MANAGEMENT_MESSAGE);
            jbiTask.setAttribute(VERSION, DEFAULT_VERSION);
            Element jbiTaskResult = createChild(jbiTask, JBI_TASK_RESULT);
            Element frmkTaskResult = createChild(jbiTaskResult, FRMWK_TASK_RESULT);
            Element frmkTaskResultDetails = createChild(frmkTaskResult, FRMWK_TASK_RESULT_DETAILS);
            appendTaskResultDetails(frmkTaskResultDetails, fmkMsg);
            if (fmkMsg.getLocale() != null) {
                createChild(frmkTaskResult, LOCALE, fmkMsg.getLocale());
            }
            if (componentResults != null) {
                for (Element element : componentResults) {
                    jbiTaskResult.appendChild(doc.importNode(element, true));
                }
            }
            return DOMUtil.asIndentedXML(doc);
        } catch (Exception e) {
            LOG.error("Error", e);
            return null;
        }
    }

    private static Document createDocument() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.newDocument();
        } catch (Exception e) {
            throw new RuntimeException("Could not create DOM document", e);
        }
    }

    private static Element createChild(Node parent, String name) {
        return createChild(parent, name, null);
    }

    private static Element createChild(Node parent, String name, String text) {
        Document doc = parent instanceof Document ? (Document) parent : parent.getOwnerDocument();
        Element child = doc.createElementNS(HTTP_JAVA_SUN_COM_XML_NS_JBI_MANAGEMENT_MESSAGE, name);
        if (text != null) {
            child.appendChild(doc.createTextNode(text));
        }
        parent.appendChild(child);
        return child;
    }

    private static void appendTaskResultDetails(Element root, Message fmkMsg) {
        Element taskResultDetails = createChild(root, TASK_RESULT_DETAILS);
        createChild(taskResultDetails, TASK_ID, fmkMsg.getTask());
        createChild(taskResultDetails, TASK_RESULT, fmkMsg.getResult());
        if (fmkMsg.getType() != null) {
            createChild(taskResultDetails, MESSAGE_TYPE, fmkMsg.getType());
        }
        // task-status-message
        if (fmkMsg.getMessage() != null) {
            Element taskStatusMessage = createChild(taskResultDetails, TASK_STATUS_MSG);
            Element msgLocInfo = createChild(taskStatusMessage, MSG_LOC_INFO);
            createChild(msgLocInfo, LOC_TOKEN);
            createChild(msgLocInfo, LOC_MESSAGE, fmkMsg.getMessage());
        }
        // exception-info
        if (fmkMsg.getException() != null) {
            Element exceptionInfo = createChild(taskResultDetails, EXCEPTION_INFO);
            createChild(exceptionInfo, NESTING_LEVEL, "1");
            createChild(exceptionInfo, LOC_TOKEN);
            createChild(exceptionInfo, LOC_MESSAGE, fmkMsg.getException().getMessage());
            Element stackTrace = createChild(exceptionInfo, STACK_TRACE);
            StringWriter sw2 = new StringWriter();
            PrintWriter pw = new PrintWriter(sw2);
            fmkMsg.getException().printStackTrace(pw);
            pw.close();
            stackTrace.appendChild(root.getOwnerDocument().createCDATASection(sw2.toString()));
        }
    }

    public static DeploymentException componentFailure(String task, String component, String info) {
        try {
            Element e = createComponentFailure(task, component, info, null);
            return new DeploymentException(DOMUtil.asXML(e));
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error creating management message", e);
            }
            return new DeploymentException(info);
        }
    }

    public static Element createComponentMessage(Message msg) {
        Document doc = createDocument();
        Element componentTaskResult = createChild(doc, COMPONENT_TASK_RESULT);
        createChild(componentTaskResult, COMPONENT_NAME, msg.getComponent());
        Element componentTaskResultDetails = createChild(componentTaskResult, COMPONENT_TASK_RESULT_DETAILS);
        appendTaskResultDetails(componentTaskResultDetails, msg);
        return componentTaskResult;
    }

    public static Element createComponentSuccess(String task, String component) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult(SUCCESS);
        msg.setComponent(component);
        return createComponentMessage(msg);
    }

    public static Element createComponentFailure(String task, String component, String info, Throwable t) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult(FAILED);
        msg.setType(ERROR);
        msg.setException(t);
        msg.setMessage(info);
        msg.setComponent(component);
        return createComponentMessage(msg);
    }

    public static Element createComponentWarning(String task, String component, String info, Throwable t) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult(SUCCESS);
        msg.setType(WARNING);
        msg.setException(t);
        msg.setMessage(info);
        msg.setComponent(component);
        return createComponentMessage(msg);
    }

}
