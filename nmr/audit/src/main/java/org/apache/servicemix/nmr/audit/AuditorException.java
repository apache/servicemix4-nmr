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
package org.apache.servicemix.nmr.audit;

import org.apache.servicemix.nmr.api.ServiceMixException;

/**
 * AuditorException is the exception that can be thrown by the {@link AuditorMBean}
 * when an error occurs accessing the data store when performing an operation.
 * 
 * @author Guillaume Nodet (gnt)
 * @since 1.0.0
 * @version $Revision: 426415 $
 */
public class AuditorException extends ServiceMixException {

    /** Serial Version UID */
    private static final long serialVersionUID = -1259059806617598480L;

    /**
     * Constructs a new AuditorException with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param   aMessage   the detail message. The detail message is saved for 
     *          later retrieval by the {@link #getMessage()} method.
     */
    public AuditorException(String aMessage) {
        super(aMessage);
    }

    /**
     * Constructs a new AuditorException with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param  aMessage the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  aCause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public AuditorException(String aMessage, Throwable aCause) {
        super(aMessage, aCause);
    }

    /**
     * Constructs a new AuditorException with the specified cause and a detail
     * message of <tt>(cause==null ? null : cause.toString())</tt> (which
     * typically contains the class and detail message of <tt>cause</tt>).
     * This constructor is useful for exceptions that are little more than
     * wrappers for other throwables (for example, {@link
     * java.security.PrivilegedActionException}).
     *
     * @param  aCause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public AuditorException(Throwable aCause) {
        super(aCause);
    }
}
