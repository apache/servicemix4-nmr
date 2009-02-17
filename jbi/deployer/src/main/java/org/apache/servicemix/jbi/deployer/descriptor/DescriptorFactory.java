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
package org.apache.servicemix.jbi.deployer.descriptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Factory to read a JBI descriptor from a file, url or stream.
 */
public class DescriptorFactory {

    /**
     * JAXP attribute value indicating the XSD schema language.
     */
    private static final String XSD_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";

    /**
     * The location of the JBI descriptor in a JBI artifact.
     */
    public static final String DESCRIPTOR_FILE = "META-INF/jbi.xml";


    private static final String VERSION = "version";
    private static final String TYPE = "type";
    private static final String COMPONENT = "component";
    private static final String COMPONENT_CLASS_LOADER_DELEGATION = "component-class-loader-delegation";
    private static final String BOOTSTRAP_CLASS_LOADER_DELEGATION = "bootstrap-class-loader-delegation";
    private static final String DESCRIPTION = "description";
    private static final String IDENTIFICATION = "identification";
    private static final String COMPONENT_CLASS_NAME = "component-class-name";
    private static final String COMPONENT_CLASS_PATH = "component-class-path";
    private static final String PATH_ELEMENT = "path-element";
    private static final String SHARED_LIBRARY = "shared-library";
    private static final String BOOTSTRAP_CLASS_PATH = "bootstrap-class-path";
    private static final String BOOTSTRAP_CLASS_NAME = "bootstrap-class-name";
    private static final String CLASS_LOADER_DELEGATION = "class-loader-delegation";
    private static final String SERVICE_ASSEMBLY = "service-assembly";
    private static final String SERVICE_UNIT = "service-unit";
    private static final String TARGET = "target";
    private static final String ARTIFACTS_ZIP = "artifacts-zip";
    private static final String COMPONENT_NAME = "component-name";
    private static final String CONNECTIONS = "connections";
    private static final String CONNECTION = "connection";
    private static final String CONSUMER = "consumer";
    private static final String PROVIDER = "provider";
    private static final String INTERFACE_NAME = "interface-name";
    private static final String SERVICE_NAME = "service-name";
    private static final String ENDPOINT_NAME = "endpoint-name";
    private static final String BINDING_COMPONENT = "binding-component";
    private static final String SERVICES = "services";
    private static final String PROVIDES = "provides";
    private static final String CONSUMES = "consumes";
    private static final String LINK_TYPE = "link-type";
    private static final String NAME = "name";
    private static final String JBI_DESCRIPTOR_XSD = "jbi-descriptor.xsd";
    private static final String SHARED_LIBRARY_CLASS_PATH = "shared-library-class-path";

    /**
     * Build a jbi descriptor from a file archive.
     *
     * @param descriptorFile path to the jbi descriptor, or to the root directory
     * @return the Descriptor object
     */
    public static Descriptor buildDescriptor(File descriptorFile) {
        if (descriptorFile.isDirectory()) {
            descriptorFile = new File(descriptorFile, DESCRIPTOR_FILE);
        }
        if (descriptorFile.isFile()) {
            try {
                return buildDescriptor(descriptorFile.toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException("There is a bug here...", e);
            }
        }
        return null;
    }

    /**
     * Build a jbi descriptor from the specified URL
     *
     * @param url url to the jbi descriptor
     * @return the Descriptor object
     */
    public static Descriptor buildDescriptor(final URL url) {
        try {
            return buildDescriptor(url.openStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Build a jbi descriptor from the specified stream
     *
     * @param stream input stream to the jbi descriptor
     * @return the Descriptor object
     */
    public static Descriptor buildDescriptor(final InputStream stream) {
        try {
            // Read descriptor
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            copyInputStream(stream, baos);
            return buildDescriptor(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Build a jbi descriptor from the specified binary data.
     * The descriptor is validated against the schema, but no
     * semantic validation is performed.
     *
     * @param bytes hold the content of the JBI descriptor xml document
     * @return the Descriptor object
     */
    public static Descriptor buildDescriptor(final byte[] bytes) {
        try {
            // Validate descriptor
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XSD_SCHEMA_LANGUAGE);
            Schema schema = schemaFactory.newSchema(DescriptorFactory.class.getResource(JBI_DESCRIPTOR_XSD));
            Validator validator = schema.newValidator();
            validator.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException exception) throws SAXException {
                    //log.debug("Validation warning on " + url + ": " + exception);
                }

                public void error(SAXParseException exception) throws SAXException {
                    //log.info("Validation error on " + url + ": " + exception);
                }

                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            validator.validate(new StreamSource(new ByteArrayInputStream(bytes)));
            // Parse descriptor
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(new ByteArrayInputStream(bytes));
            Element jbi = doc.getDocumentElement();
            Descriptor desc = new Descriptor();
            desc.setVersion(Double.parseDouble(getAttribute(jbi, VERSION)));
            Element child = getFirstChildElement(jbi);
            if (COMPONENT.equals(child.getLocalName())) {
                ComponentDesc component = new ComponentDesc();
                component.setType(child.getAttribute(TYPE));
                component.setComponentClassLoaderDelegation(getAttribute(child, COMPONENT_CLASS_LOADER_DELEGATION));
                component.setBootstrapClassLoaderDelegation(getAttribute(child, BOOTSTRAP_CLASS_LOADER_DELEGATION));
                List<SharedLibraryList> sls = new ArrayList<SharedLibraryList>();
                DocumentFragment ext = null;
                for (Element e = getFirstChildElement(child); e != null; e = getNextSiblingElement(e)) {
                    if (IDENTIFICATION.equals(e.getLocalName())) {
                        component.setIdentification(readIdentification(e));
                    } else if (COMPONENT_CLASS_NAME.equals(e.getLocalName())) {
                        component.setComponentClassName(getText(e));
                        component.setDescription(getAttribute(e, DESCRIPTION));
                    } else if (COMPONENT_CLASS_PATH.equals(e.getLocalName())) {
                        ClassPath componentClassPath = new ClassPath();
                        ArrayList<String> l = new ArrayList<String>();
                        for (Element e2 = getFirstChildElement(e); e2 != null; e2 = getNextSiblingElement(e2)) {
                            if (PATH_ELEMENT.equals(e2.getLocalName())) {
                                l.add(getText(e2));
                            }
                        }
                        componentClassPath.setPathList(l);
                        component.setComponentClassPath(componentClassPath);
                    } else if (BOOTSTRAP_CLASS_NAME.equals(e.getLocalName())) {
                        component.setBootstrapClassName(getText(e));
                    } else if (BOOTSTRAP_CLASS_PATH.equals(e.getLocalName())) {
                        ClassPath bootstrapClassPath = new ClassPath();
                        ArrayList<String> l = new ArrayList<String>();
                        for (Element e2 = getFirstChildElement(e); e2 != null; e2 = getNextSiblingElement(e2)) {
                            if (PATH_ELEMENT.equals(e2.getLocalName())) {
                                l.add(getText(e2));
                            }
                        }
                        bootstrapClassPath.setPathList(l);
                        component.setBootstrapClassPath(bootstrapClassPath);
                    } else if (SHARED_LIBRARY.equals(e.getLocalName())) {
                        SharedLibraryList sl = new SharedLibraryList();
                        sl.setName(getText(e));
                        sl.setVersion(getAttribute(e, VERSION));
                        sls.add(sl);
                    } else {
                        if (ext == null) {
                            ext = doc.createDocumentFragment();
                        }
                        ext.appendChild(e);
                    }
                }
                component.setSharedLibraries(sls.toArray(new SharedLibraryList[sls.size()]));
                if (ext != null) {
                    InstallationDescriptorExtension descriptorExtension = new InstallationDescriptorExtension();
                    descriptorExtension.setDescriptorExtension(ext);
                    component.setDescriptorExtension(descriptorExtension);
                }
                desc.setComponent(component);
            } else if (SHARED_LIBRARY.equals(child.getLocalName())) {
                SharedLibraryDesc sharedLibrary = new SharedLibraryDesc();
                sharedLibrary.setClassLoaderDelegation(getAttribute(child, CLASS_LOADER_DELEGATION));
                sharedLibrary.setVersion(getAttribute(child, VERSION));
                for (Element e = getFirstChildElement(child); e != null; e = getNextSiblingElement(e)) {
                    if (IDENTIFICATION.equals(e.getLocalName())) {
                        sharedLibrary.setIdentification(readIdentification(e));
                    } else if (SHARED_LIBRARY_CLASS_PATH.equals(e.getLocalName())) {
                        ClassPath sharedLibraryClassPath = new ClassPath();
                        ArrayList<String> l = new ArrayList<String>();
                        for (Element e2 = getFirstChildElement(e); e2 != null; e2 = getNextSiblingElement(e2)) {
                            if (PATH_ELEMENT.equals(e2.getLocalName())) {
                                l.add(getText(e2));
                            }
                        }
                        sharedLibraryClassPath.setPathList(l);
                        sharedLibrary.setSharedLibraryClassPath(sharedLibraryClassPath);
                    }
                }
                desc.setSharedLibrary(sharedLibrary);
            } else if (SERVICE_ASSEMBLY.equals(child.getLocalName())) {
                ServiceAssemblyDesc serviceAssembly = new ServiceAssemblyDesc();
                ArrayList<ServiceUnitDesc> sus = new ArrayList<ServiceUnitDesc>();
                for (Element e = getFirstChildElement(child); e != null; e = getNextSiblingElement(e)) {
                    if (IDENTIFICATION.equals(e.getLocalName())) {
                        serviceAssembly.setIdentification(readIdentification(e));
                    } else if (SERVICE_UNIT.equals(e.getLocalName())) {
                        ServiceUnitDesc su = new ServiceUnitDesc();
                        for (Element e2 = getFirstChildElement(e); e2 != null; e2 = getNextSiblingElement(e2)) {
                            if (IDENTIFICATION.equals(e2.getLocalName())) {
                                su.setIdentification(readIdentification(e2));
                            } else if (TARGET.equals(e2.getLocalName())) {
                                Target target = new Target();
                                for (Element e3 = getFirstChildElement(e2); e3 != null; e3 = getNextSiblingElement(e3)) {
                                    if (ARTIFACTS_ZIP.equals(e3.getLocalName())) {
                                        target.setArtifactsZip(getText(e3));
                                    } else if (COMPONENT_NAME.equals(e3.getLocalName())) {
                                        target.setComponentName(getText(e3));
                                    }
                                }
                                su.setTarget(target);
                            }
                        }
                        sus.add(su);
                    } else if (CONNECTIONS.equals(e.getLocalName())) {
                        Connections connections = new Connections();
                        ArrayList<Connection> cns = new ArrayList<Connection>();
                        for (Element e2 = getFirstChildElement(e); e2 != null; e2 = getNextSiblingElement(e2)) {
                            if (CONNECTION.equals(e2.getLocalName())) {
                                Connection cn = new Connection();
                                for (Element e3 = getFirstChildElement(e2); e3 != null; e3 = getNextSiblingElement(e3)) {
                                    if (CONSUMER.equals(e3.getLocalName())) {
                                        Consumer consumer = new Consumer();
                                        consumer.setInterfaceName(readAttributeQName(e3, INTERFACE_NAME));
                                        consumer.setServiceName(readAttributeQName(e3, SERVICE_NAME));
                                        consumer.setEndpointName(getAttribute(e3, ENDPOINT_NAME));
                                        cn.setConsumer(consumer);
                                    } else if (PROVIDER.equals(e3.getLocalName())) {
                                        Provider provider = new Provider();
                                        provider.setServiceName(readAttributeQName(e3, SERVICE_NAME));
                                        provider.setEndpointName(getAttribute(e3, ENDPOINT_NAME));
                                        cn.setProvider(provider);
                                    }
                                }
                                cns.add(cn);
                            }
                        }
                        connections.setConnections(cns.toArray(new Connection[cns.size()]));
                        serviceAssembly.setConnections(connections);
                    }
                }
                serviceAssembly.setServiceUnits(sus.toArray(new ServiceUnitDesc[sus.size()]));
                desc.setServiceAssembly(serviceAssembly);
            } else if (SERVICES.equals(child.getLocalName())) {
                Services services = new Services();
                services.setBindingComponent(Boolean.valueOf(getAttribute(child, BINDING_COMPONENT)).booleanValue());
                ArrayList<Provides> provides = new ArrayList<Provides>();
                ArrayList<Consumes> consumes = new ArrayList<Consumes>();
                for (Element e = getFirstChildElement(child); e != null; e = getNextSiblingElement(e)) {
                    if (PROVIDES.equals(e.getLocalName())) {
                        Provides p = new Provides();
                        p.setInterfaceName(readAttributeQName(e, INTERFACE_NAME));
                        p.setServiceName(readAttributeQName(e, SERVICE_NAME));
                        p.setEndpointName(getAttribute(e, ENDPOINT_NAME));
                        provides.add(p);
                    } else if (CONSUMES.equals(e.getLocalName())) {
                        Consumes c = new Consumes();
                        c.setInterfaceName(readAttributeQName(e, INTERFACE_NAME));
                        c.setServiceName(readAttributeQName(e, SERVICE_NAME));
                        c.setEndpointName(getAttribute(e, ENDPOINT_NAME));
                        c.setLinkType(getAttribute(e, LINK_TYPE));
                        consumes.add(c);
                    }
                }
                services.setProvides(provides.toArray(new Provides[provides.size()]));
                services.setConsumes(consumes.toArray(new Consumes[consumes.size()]));
                desc.setServices(services);
            }
            checkDescriptor(desc);
            return desc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getAttribute(Element e, String name) {
        if (e.hasAttribute(name)) {
            return e.getAttribute(name);
        } else {
            return null;
        }
    }

    private static QName readAttributeQName(Element e, String name) {
        String attr = getAttribute(e, name);
        if (attr != null) {
            return createQName(e, attr);
        } else {
            return null;
        }
    }

    private static String getText(Element e) {
        return getElementText(e).trim();
    }

    private static Identification readIdentification(Element e) {
        Identification ident = new Identification();
        for (Element e2 = getFirstChildElement(e); e2 != null; e2 = getNextSiblingElement(e2)) {
            if (NAME.equals(e2.getLocalName())) {
                ident.setName(getElementText(e2));
            } else if (DESCRIPTION.equals(e2.getLocalName())) {
                ident.setDescription(getElementText(e2));
            }
        }
        return ident;
    }

    /**
     * Check validity of the JBI descriptor.
     *
     * @param descriptor the descriptor to check
     * @throws Exception if the descriptor is not valid
     */
    public static void checkDescriptor(Descriptor descriptor) {
        List<String> violations = new ArrayList<String>();

        if (descriptor.getVersion() != 1.0) {
            violations.add("JBI descriptor version should be set to '1.0' but is " + descriptor.getVersion());
        }

        if (descriptor.getComponent() != null) {
            checkComponent(violations, descriptor.getComponent());
        } else if (descriptor.getServiceAssembly() != null) {
            checkServiceAssembly(violations, descriptor.getServiceAssembly());
        } else if (descriptor.getServices() != null) {
            checkServiceUnit(violations, descriptor.getServices());
        } else if (descriptor.getSharedLibrary() != null) {
            checkSharedLibrary(violations, descriptor.getSharedLibrary());
        } else {
            violations.add("The jbi descriptor does not contain any informations");
        }

        if (violations.size() > 0) {
            throw new RuntimeException("The JBI descriptor is not valid, please correct these violations "
                    + violations.toString());
        }
    }

    /**
     * Checks that the component is valid
     *
     * @param violations A list of violations that the check can add to
     * @param component  The component descriptor that is being checked
     */
    private static void checkComponent(List<String> violations, ComponentDesc component) {
        if (component.getIdentification() == null) {
            violations.add("The component has not identification");
        } else {
            if (isBlank(component.getIdentification().getName())) {
                violations.add("The component name is not set");
            }
        }
        if (component.getBootstrapClassName() == null) {
            violations.add("The component has not defined a boot-strap class name");
        }
        if (component.getBootstrapClassPath() == null || component.getBootstrapClassPath().getPathElements() == null) {
            violations.add("The component has not defined any boot-strap class path elements");
        }
    }

    /**
     * Checks that the service assembly is valid
     *
     * @param violations      A list of violations that the check can add to
     * @param serviceAssembly The service assembly descriptor that is being checked
     */
    private static void checkServiceAssembly(List<String> violations, ServiceAssemblyDesc serviceAssembly) {
        if (serviceAssembly.getIdentification() == null) {
            violations.add("The service assembly has not identification");
        } else {
            if (isBlank(serviceAssembly.getIdentification().getName())) {
                violations.add("The service assembly name is not set");
            }
        }
    }

    /**
     * Checks that the service unit is valid
     *
     * @param violations A list of violations that the check can add to
     * @param services   The service unit descriptor that is being checked
     */
    private static void checkServiceUnit(List<String> violations, Services services) {
        // TODO validate service unit

    }

    /**
     * Checks that the shared library is valid
     *
     * @param violations    A list of violations that the check can add to
     * @param sharedLibrary The shared library descriptor that is being checked
     */
    private static void checkSharedLibrary(List<String> violations, SharedLibraryDesc sharedLibrary) {
        if (sharedLibrary.getIdentification() == null) {
            violations.add("The shared library has not identification");
        } else {
            if (isBlank(sharedLibrary.getIdentification().getName())) {
                violations.add("The shared library name is not set");
            }
        }
    }

    /**
     * Retrieves the jbi descriptor as a string
     *
     * @param descriptorFile path to the jbi descriptor, or to the root directory
     * @return the contents of the jbi descriptor
     */
    public static String getDescriptorAsText(File descriptorFile) {
        if (descriptorFile.isDirectory()) {
            descriptorFile = new File(descriptorFile, DESCRIPTOR_FILE);
        }
        if (descriptorFile.isFile()) {
            try {
                return getDescriptorAsText(descriptorFile.toURL());
            } catch (MalformedURLException e) {
                //log.debug("Error reading jbi descritor: " + descriptorFile, e);
            }
        }
        return null;
    }

    /**
     * Retrieves the jbi descriptor as a string
     *
     * @param descriptorURL URL pointing to the JBI descriptor
     * @return the contents of the jbi descriptor
     */
    public static String getDescriptorAsText(URL descriptorURL) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream is = descriptorURL.openStream();
            copyInputStream(is, os);
            return os.toString();
        } catch (Exception e) {
            //log.debug("Error reading jbi descritor: " + descriptorFile, e);
        }
        return null;
    }

    /**
     * <p>Checks if a String is whitespace, empty ("") or null.</p>
     * <p/>
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is null, empty or whitespace
     *         <p/>
     *         Copied from org.apache.commons.lang.StringUtils#isBlanck
     */
    private static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Copy in stream to an out stream
     *
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.close();
    }

    /**
     * Creates a QName instance from the given namespace context for the given qualifiedName
     *
     * @param element       the element to use as the namespace context
     * @param qualifiedName the fully qualified name
     * @return the QName which matches the qualifiedName
     */
    public static QName createQName(Element element, String qualifiedName) {
        int index = qualifiedName.indexOf(':');
        if (index >= 0) {
            String prefix = qualifiedName.substring(0, index);
            String localName = qualifiedName.substring(index + 1);
            String uri = recursiveGetAttributeValue(element, "xmlns:" + prefix);
            return new QName(uri, localName, prefix);
        } else {
            String uri = recursiveGetAttributeValue(element, "xmlns");
            if (uri != null) {
                return new QName(uri, qualifiedName);
            }
            return new QName(qualifiedName);
        }
    }

    /**
     * Recursive method to find a given attribute value
     */
    public static String recursiveGetAttributeValue(Element element, String attributeName) {
        String answer = null;
        try {
            answer = element.getAttribute(attributeName);
        }
        catch (Exception e) {
            //if (log.isTraceEnabled()) {
            //    log.trace("Caught exception looking up attribute: " + attributeName + " on element: " + element + ". Cause: " + e, e);
            //}
        }
        if (answer == null || answer.length() == 0) {
            Node parentNode = element.getParentNode();
            if (parentNode instanceof Element) {
                return recursiveGetAttributeValue((Element) parentNode, attributeName);
            }
        }
        return answer;
    }

    /**
     * Returns the text of the element
     */
    public static String getElementText(Element element) {
        StringBuffer buffer = new StringBuffer();
        NodeList nodeList = element.getChildNodes();
        for (int i = 0, size = nodeList.getLength(); i < size; i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                buffer.append(node.getNodeValue());
            }
        }
        return buffer.toString();
    }

    /**
     * Get the first child element
     *
     * @param parent
     * @return
     */
    public static Element getFirstChildElement(Node parent) {
        NodeList childs = parent.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node child = childs.item(i);
            if (child instanceof Element) {
                return (Element) child;
            }
        }
        return null;
    }

    /**
     * Get the next sibling element
     *
     * @param el
     * @return
     */
    public static Element getNextSiblingElement(Element el) {
        for (Node n = el.getNextSibling(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                return (Element) n;
            }
        }
        return null;
    }

}
