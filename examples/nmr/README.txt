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

Welcome to the ServiceMix NMR nmr example
==========================================

This example include two bundles. 
The endpoint bundle shows how to write a
simple NMR endpoint, a osgi spring configuration file appended with this
bundle to demostrate how to expose the Endpoint service in this bundle. The
endpoint bundle include one java class EchoEndpoint, it simply print out the
received exchange and add "Echo" prefix then send the exchange back.
The client bundle shows how to access the NMR and send exhanges, also there is
an osgi spring configuration file with this bundle to show how NMR get injected
from OSGI service.

Quick steps to install the sample
---------------------------------
cd this example folder
mvn install
Launch the ServiceMix Kernel by running
  bin/servicemix
in the root dir of this distribution.

When inside the console, just run the following commands to install the
example:

  features/install examples-nmr

If you have all the bundles available in your local repo, the installation
of the example will be very fast, otherwise it may take some time to
download everything needed.

Testing the example
-------------------

When the feature is installed, you should be output from the log file every 5
seconds
15:50:37,240 | INFO  | pool-8-thread-1  | EchoEndpoint                     |
amples.nmr.endpoint.EchoEndpoint   30 | Receiced in EchoEndpoint: Hello
15:50:37,241 | INFO  | Thread-11        | Client                           |
.client.Client$SendRequestThread   69 | Response from Endpoint EchoHello




