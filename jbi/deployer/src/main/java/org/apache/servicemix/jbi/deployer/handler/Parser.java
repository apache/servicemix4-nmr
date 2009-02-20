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
package org.apache.servicemix.jbi.deployer.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for jbi: protocol URL.
 */
public class Parser {

    /**
     * Syntax for the url; to be shown on exception messages.
     */
    private static final String SYNTAX = "jbi:jbi-jar-uri[,jbi-instr-uri][$jbi-instructions]";
    /**
     * Separator between wrapped jar url and instructions.
     */
    private static final String INSTRUCTIONS_SEPARATOR = "$";
    /**
     * Separator between wrapped jar url and instructions file url.
     */
    private static final String INSTRUCTIONS_FILE_SEPARATOR = ",";
    /**
     * Regex pattern for matching jar, wrapping file and instructions.
     */
    private static final Pattern SYNTAX_JAR_BND_INSTR =
            Pattern.compile("(.+?)" + INSTRUCTIONS_FILE_SEPARATOR + "(.+?)\\" + INSTRUCTIONS_SEPARATOR + "(.+?)");
    /**
     * Regex pattern for matching jar and instructions.
     */
    private static final Pattern SYNTAX_JAR_INSTR =
            Pattern.compile("(.+?)\\" + INSTRUCTIONS_SEPARATOR + "(.+?)");
    /**
     * Regex pattern for matching jar and wrapping file.
     */
    private static final Pattern SYNTAX_JAR_BND =
            Pattern.compile("(.+?)" + INSTRUCTIONS_FILE_SEPARATOR + "(.+?)");

    /**
     * Regex pattern for matching instructions when specified in url.
     */
    private static final Pattern INSTRUCTIONS_PATTERN =
        Pattern.compile( "([a-zA-Z_0-9-]+)=([-!\"'()*+,.0-9A-Z_a-z%;=]+)" );

    /**
     * JBI jar URL.
     */
    private URL jbiJarURL;
    /**
     * JBI instructions URL.
     */
    private Properties jbiProperties;

    /**
     * Creates a new protocol parser.
     *
     * @param path the path part of the url (without starting jbi:)
     * @throws MalformedURLException if provided path does not comply to expected syntax or has malformed urls
     */
    public Parser(final String path) throws MalformedURLException {
        if (path == null || path.trim().length() == 0) {
            throw new MalformedURLException("Path cannot be null or empty. Syntax " + SYNTAX);
        }
        if (path.startsWith(INSTRUCTIONS_SEPARATOR) || path.endsWith(INSTRUCTIONS_SEPARATOR)) {
            throw new MalformedURLException(
                    "Path cannot start or end with " + INSTRUCTIONS_SEPARATOR + ". Syntax " + SYNTAX
            );
        }
        jbiProperties = new Properties();
        Matcher matcher = SYNTAX_JAR_BND_INSTR.matcher(path);
        if (matcher.matches()) {
            // we have all the parts
            jbiJarURL = new URL(matcher.group(1));
            parseInstructionsFile(new URL(matcher.group(2)));
            jbiProperties.putAll(parseInstructions(matcher.group(3)));
        } else if ((matcher = SYNTAX_JAR_INSTR.matcher(path)).matches()) {
            // we have a wrapped jar and instructions
            jbiJarURL = new URL(matcher.group(1));
            jbiProperties.putAll(parseInstructions(matcher.group(2)));
        } else if ((matcher = SYNTAX_JAR_BND.matcher(path)).matches()) {
            // we have a wraped jar and a wrapping instructions file
            jbiJarURL = new URL(matcher.group(1));
            parseInstructionsFile(new URL(matcher.group(2)));
        } else {
            //we have only a wrapped jar
            jbiJarURL = new URL(path);
        }
    }

    /**
     * Loeads the propertis out of an url.
     *
     * @param bndFileURL url of the file containing the instructions
     * @throws MalformedURLException if the file could not be read
     */
    private void parseInstructionsFile(final URL bndFileURL) throws MalformedURLException {
        try {
            InputStream is = null;
            try {
                is = bndFileURL.openStream();
                jbiProperties.load(is);
            }
            finally {
                if (is != null) {
                    is.close();
                }
            }
        }
        catch (IOException e) {
            throwAsMalformedURLException("Could not retrieve the instructions from [" + bndFileURL + "]", e);
        }
    }

    /**
     * Returns the JBI URL if present, null otherwise
     *
     * @return wraped jar URL
     */
    public URL getJbiJarURL() {
        return jbiJarURL;
    }

    /**
     * Returns the JBI instructions as Properties.
     *
     * @return wrapping instructions as Properties
     */
    public Properties getJbiProperties() {
        return jbiProperties;
    }

    /**
     * Parses bnd instructions out of an url query string.
     *
     * @param query query part of an url.
     *
     * @return parsed instructions as properties
     *
     * @throws java.net.MalformedURLException if provided path does not comply to syntax.
     */
    public static Properties parseInstructions( final String query )
        throws MalformedURLException
    {
        final Properties instructions = new Properties();
        if( query != null )
        {
            try
            {
                // just ignore for the moment and try out if we have valid properties separated by "&"
                final String segments[] = query.split( "&" );
                for( String segment : segments )
                {
                    // do not parse empty strings
                    if( segment.trim().length() > 0 )
                    {
                        final Matcher matcher = INSTRUCTIONS_PATTERN.matcher( segment );
                        if( matcher.matches() )
                        {
                            instructions.setProperty(
                                matcher.group( 1 ),
                                URLDecoder.decode( matcher.group( 2 ), "UTF-8" )
                            );
                        }
                        else
                        {
                            throw new MalformedURLException( "Invalid syntax for instruction [" + segment
                                                             + "]. Take a look at http://www.aqute.biz/Code/Bnd."
                            );
                        }
                    }
                }
            }
            catch( UnsupportedEncodingException e )
            {
                // thrown by URLDecoder but it should never happen
                throwAsMalformedURLException( "Could not retrieve the instructions from [" + query + "]", e );
            }
        }
        return instructions;
    }

    /**
     * Creates an MalformedURLException with a message and a cause.
     *
     * @param message exception message
     * @param cause   exception cause
     * @throws MalformedURLException the created MalformedURLException
     */
    private static void throwAsMalformedURLException(final String message, final Exception cause)
                        throws MalformedURLException {
        final MalformedURLException exception = new MalformedURLException(message);
        exception.initCause(cause);
        throw exception;
    }

}
