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

Welcome to the ServiceMix NMR cluster example
=============================================

This example shows how to set up a cluster of two ServiceMix NMR and install
a simple example demonstrating the transparent remoting capability.

Description
===========

This example will use three ServiceMix instances:
  * the main one will contain an ActiveMQ broker
  * the second one will contain a quartz service assembly which will fire JBI exchanges
      at repeated intervals.  This quartz endpoint is registered as a clustered endpoint
      so that those exchanges will go through the cluster engine instead of looking for 
      the target locally
  * the last one will contain a camel route which will receive the exchanges coming from
      the previous instances and log them
      
The two custer engines deployed on the instances will use a single ActiveMQ broker.  It is
possible to set up two different brokers and create a cluster of broker by changing the 
configuration of the brokers so that they discover each other.  However, this is not the 
main topic of this example.

In order to set up this example, you will first create the ActiveMQ broker, then create two
new ServiceMix instances and deploy the required configurations on to them.  For each of 
these instances, three files will be put in the deploy folder:
  * jms.xml contains a definition of a JMS ConnectionFactory pointing to the ActiveMQ broker
     that will be registered in OSGi for the cluster engine to use it
  * cluster.xml contains the cluster engine definition.  This is not a spring configuration
     file, but a feature descriptor (see ServiceMix Kernel User's Guide for more informations).
     This descriptor will be user to automatically install the JBI cluster engine and the
     required JBI component for this particular instance
  * quartz.xml / camel.xml contain the JBI endpoints definition

Instructions
============

First, run "mvn install" in this examples folder, then copy the contents of the "target/instances" 
folder from this example into the installation directory of ServiceMix.
Note that the cluster.xml files refer to the release of ServiceMix Features project.  The default 
version used in this example is 4.0.0, but if this version has not been released yet, you need
to edit the pom.xml and replace the <properties><servicemix.features.version> text element with
a valid version, for example 4.0-m2-SNAPSHOT.

You should have the following tree:
  + apache-servicemix-nmr
  |-- ant
  |-- bin
  |-- etc
  |-- examples
  |-- instances
  | |-- smx1
  | | \-- deploy
  | |   |-- cluster.xml
  | |   |-- jms.xml
  | |   \-- quartz.xml
  | \-- smxx
  |   \-- deploy
  |     |-- camel.xml
  |     |-- cluter.xml
  |     \-- jms.xml
  |-- lib
  \-- system

The three commands below will install Apache ActiveMQ on the main instance and create a broker.
In this example, we will only use a single ActiveMQ broker but you can change this topology
to create a broker per JBI container if you want.  The required change would be to install
a broker on each of the instances and configure them to build a network of brokers.
See the ActiveMQ web site (http://activemq.apache.org/networks-of-brokers.html) for more
informations.

Note that the maven url below may need to be changed.  It is pointing to the released version
of Apache ServiceMix 4.x and the version may need to be modified (if the 4.0.0 version has not
been released yet, you need to change it to a snapshot version). Something like:
   mvn:org.apache.servicemix.features/apache-servicemix/4.0-m2-SNAPSHOT/xml/features

smx@root:/> features/addUrl mvn:org.apache.servicemix.features/apache-servicemix/4.0.0/xml/features
smx@root:/> features/install activemq
smx@root:/> activemq/create-broker

Then, create two new instances of ServiceMix Kernel and start then using the following commands:

smx@root:/> admin/create smx1
smx@root:/> admin/start smx1
smx@root:/> admin/create smx2
smx@root:/> admin/start smx2

Wait for the two new instances to be fully started.  This can be easily checked by running the
following command:

smx@root:/> admin/list

Both instances should be displayed as either "Starting" or "Started", so you just need to wait
a bit until both are displayed as "Started".



Now, we can make sure the example is working as designed by connecting to the "smx2" instance
and checking the log.

smx:root:/> admin/connect smx2
smx:smx2:/> ld | grep Exchange

You should see a list of log statements, one for each message received from the "smx1" instance.


