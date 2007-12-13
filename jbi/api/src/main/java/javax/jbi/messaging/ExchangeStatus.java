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
package javax.jbi.messaging;

public final class ExchangeStatus {

    public static final ExchangeStatus ACTIVE = new ExchangeStatus("Active");

    public static final ExchangeStatus ERROR = new ExchangeStatus("Error");

    public static final ExchangeStatus DONE = new ExchangeStatus("Done");

    private String mStatus;

    private ExchangeStatus(String status) {
        mStatus = status;
    }

    public String toString() {
        return mStatus;
    }

    public static ExchangeStatus valueOf(String status) {
        ExchangeStatus instance;

        //
        //  Convert symbolic name to object reference.
        //
        if (status.equals(DONE.toString())) {
            instance = DONE;
        } else if (status.equals(ERROR.toString())) {
            instance = ERROR;
        } else if (status.equals(ACTIVE.toString())) {
            instance = ACTIVE;

        } else {
            //
            //  Someone has a problem.
            //
            throw new java.lang.IllegalArgumentException(status);
        }

        return instance;
    }

    public int hashCode() {
        return mStatus.hashCode();
    }
}
