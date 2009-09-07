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

Welcome to the ServiceMix Custom Endpoint Listener Example
==========================================================

Quick steps to install this example
-------------------------------------

Launch the ServiceMix Kernel by running
  bin/karaf
in the root dir of this distribution.

run:
  mvn install

Deploy the example on ServiceMix 4:

- using the ServiceMix console:
   osgi:install -s mvn:org.apache.servicemix.nmr.examples.interceptors/endpoint/${version}/jar

Once the bundle is installed it will capture and report to the servicemix 
log the endpoint register and unregister events in the NMR.

Starting and Stopping the Listener:

- using the ServiceMix console:
   osgi:start BundleID
   osgi:stop BundleID

Note: Upon installation the Listener will automatically be started.
