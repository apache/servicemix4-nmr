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
package org.apache.servicemix.jbi.management;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
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

    private static final Log LOG = LogFactory.getLog(ManagementSupport.class);

    private ManagementSupport() {
    }

    public static class Message {
        private boolean isCauseFramework;
        private String task;
        private String result;
        private Exception exception;
        private String type;
        private String message;
        private String component;
        private String locale;

        public Exception getException() {
            return exception;
        }
        public void setException(Exception exception) {
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

    public static Exception failure(String task, String info) throws Exception {
        return failure(task, info, null, null);
    }

    public static Exception failure(String task, List componentResults) throws Exception {
        return failure(task, null, null, componentResults);
    }

    public static Exception failure(String task, String info, Exception e) throws Exception {
        return failure(task, info, e, null);
    }

    public static Exception failure(String task, String info, Exception e, List componentResults) throws Exception {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult("FAILED");
        msg.setType("ERROR");
        msg.setException(e);
        msg.setMessage(info);
        return new Exception(ManagementSupport.createFrameworkMessage(msg, componentResults));
    }

    public static String createSuccessMessage(String task) {
        return createSuccessMessage(task, null, null);
    }

    public static String createSuccessMessage(String task, List componentResults) {
        return createSuccessMessage(task, null, componentResults);
    }

    public static String createSuccessMessage(String task, String info) {
        return createSuccessMessage(task, info, null);
    }

    public static String createSuccessMessage(String task, String info, List componentResults) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult("SUCCESS");
        msg.setMessage(info);
        return ManagementSupport.createFrameworkMessage(msg, componentResults);
    }

    public static String createWarningMessage(String task, String info, List componentResults) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult("SUCCESS");
        msg.setType("WARNING");
        msg.setMessage(info);
        return ManagementSupport.createFrameworkMessage(msg, componentResults);
    }

    public static String createFrameworkMessage(Message fmkMsg, List componentResults) {
        try {
            Document doc = createDocument();
            Element jbiTask = createChild(doc, "jbi-task");
            jbiTask.setAttribute("xmlns", "http://java.sun.com/xml/ns/jbi/management-message");
            jbiTask.setAttribute("version", "1.0");
            Element jbiTaskResult = createChild(jbiTask, "jbi-task-result");
            Element frmkTaskResult = createChild(jbiTaskResult, "frmwk-task-result");
            Element frmkTaskResultDetails = createChild(frmkTaskResult, "frmwk-task-result-details");
            appendTaskResultDetails(frmkTaskResultDetails, fmkMsg);
            if (fmkMsg.getLocale() != null) {
                createChild(frmkTaskResult, "locale", fmkMsg.getLocale());
            }
            if (componentResults != null) {
                for (Iterator iter = componentResults.iterator(); iter.hasNext();) {
                    Element element = (Element) iter.next();
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
        Element child = doc.createElementNS("http://java.sun.com/xml/ns/jbi/management-message", name);
        if (text != null) {
            child.appendChild(doc.createTextNode(text));
        }
        parent.appendChild(child);
        return child;
    }

    private static void appendTaskResultDetails(Element root, Message fmkMsg) {
        Element taskResultDetails = createChild(root, "task-result-details");
        createChild(taskResultDetails, "task-id", fmkMsg.getTask());
        createChild(taskResultDetails, "task-result", fmkMsg.getResult());
        if (fmkMsg.getType() != null) {
            createChild(taskResultDetails, "message-type", fmkMsg.getType());
        }
        // task-status-message
        if (fmkMsg.getMessage() != null) {
            Element taskStatusMessage = createChild(taskResultDetails, "task-status-msg");
            Element msgLocInfo = createChild(taskStatusMessage, "msg-loc-info");
            createChild(msgLocInfo, "loc-token");
            createChild(msgLocInfo, "loc-message", fmkMsg.getMessage());
        }
        // exception-info
        if (fmkMsg.getException() != null) {
            Element exceptionInfo = createChild(taskResultDetails, "exception-info");
            createChild(exceptionInfo, "nesting-level", "1");
            createChild(exceptionInfo, "loc-token");
            createChild(exceptionInfo, "loc-message", fmkMsg.getException().getMessage());
            Element stackTrace = createChild(exceptionInfo, "stack-trace");
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
        Element componentTaskResult = createChild(doc, "component-task-result");
        createChild(componentTaskResult, "component-name", msg.getComponent());
        Element componentTaskResultDetails = createChild(componentTaskResult, "component-task-result-details");
        appendTaskResultDetails(componentTaskResultDetails, msg);
        return componentTaskResult;
    }

    public static Element createComponentSuccess(String task, String component) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult("SUCCESS");
        msg.setComponent(component);
        return createComponentMessage(msg);
    }

    public static Element createComponentFailure(String task, String component, String info, Exception e) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult("FAILED");
        msg.setType("ERROR");
        msg.setException(e);
        msg.setMessage(info);
        msg.setComponent(component);
        return createComponentMessage(msg);
    }

    public static Element createComponentWarning(String task, String component, String info, Exception e) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setTask(task);
        msg.setResult("SUCCESS");
        msg.setType("WARNING");
        msg.setException(e);
        msg.setMessage(info);
        msg.setComponent(component);
        return createComponentMessage(msg);
    }

}
