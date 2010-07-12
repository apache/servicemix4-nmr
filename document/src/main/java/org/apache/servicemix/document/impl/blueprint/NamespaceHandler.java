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
package org.apache.servicemix.document.impl.blueprint;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NamespaceHandler implements org.apache.aries.blueprint.NamespaceHandler {

    public static final String DOCUMENT = "document";
    public static final String ID = "id";
    public static final String REPOSITORY = "repository";

    public static ServiceRegistration register(BundleContext context) {
        Properties props = new Properties();
        props.put("osgi.service.blueprint.namespace", "http://servicemix.apache.org/schema/document");
        return context.registerService(
                new String[] { org.apache.aries.blueprint.NamespaceHandler.class.getName() },
                new NamespaceHandler(),
                props
        );
    }

    public URL getSchemaLocation(String s) {
        return getClass().getResource("/org/apache/servicemix/document/document.xsd");
    }

    public Set<Class> getManagedClasses() {
        return new HashSet<Class>(Collections.singletonList(BlueprintDocumentFactory.class));
    }

    public Metadata parse(Element element, ParserContext context) {
        String name = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
        if (DOCUMENT.equals(name)) {
            return parseDocument(element, context);
        }
        throw new ComponentDefinitionException("Bad xml syntax: unknown element '" + name + "'");
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata componentMetadata, ParserContext parserContext) {
        throw new ComponentDefinitionException("Unsupported node: " + node.getNodeName());
    }

    private Metadata parseDocument(Element element, ParserContext context) {
        MutableBeanMetadata bean = context.createMetadata(MutableBeanMetadata.class);
        bean.setRuntimeClass(BlueprintDocumentFactory.class);
        String id = element.getAttribute(ID);
        bean.setId(id);
        bean.addProperty("beanName", createValue(context, id));
        bean.addProperty("bundleContext", createRef(context, "blueprintBundleContext"));
        if (element.hasAttribute(REPOSITORY)) {
            bean.addProperty(REPOSITORY, createRef(context, element.getAttribute(REPOSITORY)));
        }
        bean.addProperty(DOCUMENT, createValue(context, getTextValue(element)));
        bean.setInitMethod("afterPropertiesSet");
        bean.setProcessor(true);
        return bean;
    }

    private ValueMetadata createValue(ParserContext context, String value) {
        MutableValueMetadata v = context.createMetadata(MutableValueMetadata.class);
        v.setStringValue(value);
        return v;
    }

    private RefMetadata createRef(ParserContext context, String value) {
        MutableRefMetadata r = context.createMetadata(MutableRefMetadata.class);
        r.setComponentId(value);
        return r;
    }

    private static String getTextValue(Element element) {
        StringBuffer value = new StringBuffer();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
                value.append(item.getNodeValue());
            }
        }
        return value.toString();
    }

}
