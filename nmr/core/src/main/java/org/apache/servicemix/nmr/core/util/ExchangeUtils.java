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
package org.apache.servicemix.nmr.core.util;

import java.util.Arrays;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import org.apache.servicemix.nmr.api.Exchange;
import org.apache.servicemix.nmr.api.Message;
import org.apache.servicemix.nmr.api.Type;
import org.apache.servicemix.nmr.core.NmrRuntimeException;

public class ExchangeUtils {

    public static final int MAX_MSG_DISPLAY_SIZE = 1500;

    public static String display(Exchange exchange, boolean displayContent) {
        if (displayContent) {
            ensureReReadable(exchange);
        }
        StringBuffer sb = new StringBuffer();
        sb.append("[\n");
        sb.append("  id:        ").append(exchange.getId()).append('\n');
        sb.append("  mep:       ").append(exchange.getPattern()).append('\n');
        sb.append("  status:    ").append(exchange.getStatus()).append('\n');
        sb.append("  role:      ").append(exchange.getRole()).append('\n');
        if (exchange.getTarget() != null) {
            sb.append("  target:    ").append(exchange.getTarget()).append('\n');
        }
        if (exchange.getOperation() != null) {
            sb.append("  operation: ").append(exchange.getOperation()).append('\n');
        }
        if (exchange.getProperties().size() > 0) {
            sb.append("  properties: [").append('\n');
            for (String key : exchange.getProperties().keySet()) {
                sb.append("      ").append(key).append(" = ");
                Object contents = exchange.getProperty(key);
                sb.append(convertDisplay(contents));
                sb.append('\n');
            }
            sb.append("  ]").append('\n');
        }
        display(exchange, Type.In, sb);
        display(exchange, Type.Out, sb);
        display(exchange, Type.Fault, sb);
        if (exchange.getError() != null) {
            sb.append("  error: [").append('\n');
            StringWriter sw = new StringWriter();
            exchange.getError().printStackTrace(new PrintWriter(sw));
            sb.append("    ").append(sw.toString().replace("\n", "\n    ").replace("\t", "  ").trim()).append('\n');
            sb.append("  ]").append('\n');
        }
        sb.append("]\n");
        return sb.toString();
    }

    public static void display(Exchange exchange, Type type, StringBuffer sb) {
        Message message = exchange.getMessage(type, false);
        if (message != null) {
            sb.append("  ").append(type).append(": [").append('\n');
            sb.append("    content: ");
            try {
                if (message.getBody() != null) {
                    Object contents = message.getBody();
                    sb.append(convertDisplay(contents));
                } else {
                    sb.append("null");
                }
            } catch (Exception e) {
                sb.append("Unable to display: ").append(e);
            }
            sb.append('\n');
            if (message.getAttachments().size() > 0) {
                sb.append("    attachments: [").append('\n');
                for (String key : message.getAttachments().keySet()) {
                    Object contents = message.getAttachment(key);
                    sb.append("      ").append(key).append(" = ").append(convertDisplay(contents)).append('\n');
                }
                sb.append("    ]").append('\n');
            }
            if (message.getHeaders().size() > 0) {
                sb.append("    properties: [").append('\n');
                for (String key : message.getHeaders().keySet()) {
                    sb.append("      ").append(key).append(" = ");
                    Object contents = message.getHeader(key);
                    sb.append(convertDisplay(contents));
                    sb.append('\n');
                }
                sb.append("    ]").append('\n');
            }
            sb.append("  ]").append('\n');
        }
    }

    private static String convertDisplay(Object object) {
        try {
            String result;
            if (object instanceof ByteArrayInputStream) {
                InputStream is = (InputStream) object;
                byte[] data = new byte[is.available()];
                is.mark(0);
                is.read(data);
                is.reset();
                // Heuristic to check if this is a string
                if (isBinary(data)) {
                    result = Arrays.toString(data);
                } else {
                    result = new String(data);
                }
            } else if (object instanceof DOMSource) {
                StringWriter buffer = new StringWriter();
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.transform((DOMSource) object, new StreamResult(buffer));
                result = buffer.toString();
            } else if (object != null) {
                result = object.toString();
            } else {
                result = "<null>";
            }
            // trim if too long
            if (result.length() > MAX_MSG_DISPLAY_SIZE) {
                result = result.substring(0, MAX_MSG_DISPLAY_SIZE) + "...";
            }
            return result;
        } catch (Throwable t) {
            return "Error display value (" + t.toString() + ")";
        }
    }

    private static boolean isBinary(byte[] data) {
        if (data.length == 0) {
            return true;
        }
        double prob_bin = 0;
        for (int i = 0; i < data.length; i++) {
            int j = (int) data[i];
            if (j < 32 || j > 127) {
                prob_bin++;
            }
        }
        double pb = prob_bin / data.length;
        return pb > 0.5;
    }

    public static void ensureReReadable(Exchange exchange) throws NmrRuntimeException {
        try {
            if (exchange != null) {
                for (String prop : exchange.getProperties().keySet()) {
                    exchange.setProperty(prop, convert(exchange.getProperty(prop)));
                }
                ensureReReadable(exchange.getIn(false));
                ensureReReadable(exchange.getOut(false));
                ensureReReadable(exchange.getFault(false));
            }
        } catch (IOException e) {
            throw new NmrRuntimeException(e);
        } catch (TransformerException e) {
            throw new NmrRuntimeException(e);
        }
    }

    public static void ensureReReadable(Message message) throws NmrRuntimeException {
        try {
            if (message != null) {
                message.setBody(convert(message.getBody()));
                for (String hdr : message.getHeaders().keySet()) {
                    message.setHeader(hdr, convert(message.getHeader(hdr)));
                }
                for (String id : message.getAttachments().keySet()) {
                    message.addAttachment(id, convert(message.getAttachment(id)));
                }
            }
        } catch (IOException e) {
            throw new NmrRuntimeException(e);
        } catch (TransformerException e) {
            throw new NmrRuntimeException(e);
        }
    }

    private static Object convert(Object object) throws IOException, TransformerException {
        if (object instanceof InputStream) {
            object = convertInputStream((InputStream) object);
        } else if (object instanceof Source) {
            object = convertSource((Source) object);
        }
        return object;
    }

    private static InputStream convertInputStream(InputStream is) throws IOException {
        if (!(is instanceof ByteArrayInputStream)) {
            if (!(is instanceof BufferedInputStream)) {
                is = new BufferedInputStream(is);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            is.close();
            is = new ByteArrayInputStream(baos.toByteArray());
        }
        return is;
    }

    private static TransformerFactory transformerFactory;

    private static TransformerFactory getTransformerFactory() {
        if (transformerFactory == null) {
            transformerFactory = TransformerFactory.newInstance();
        }
        return transformerFactory;
    }

    private static Source convertSource(Source src) throws TransformerException {
        if (!(src instanceof StringSource)) {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            getTransformerFactory().newTransformer().transform(src, result);
            return new StringSource(writer.toString());
        }
        return src;
    }
}
