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
import org.apache.maven.model.Dependency

import scala.collection.JavaConversions._

def versionOf(group: String, artifact: String) = {
  project.getDependencyManagement().getDependencies().find { item =>
    val dependency = item.asInstanceOf[Dependency]
    group == dependency.getGroupId() && artifact == dependency.getArtifactId()
  } match {
    case Some(value) => value.asInstanceOf[Dependency].getVersion()
    case None => throw new RuntimeException("Version for artifact %s:%s could not be found".format(group, artifact))
  }
}

def set(key: String, value: String) = {
  log.info("- %s = %s".format(key, value))
  project.getProperties().put(key, value)
}

log.info("----------------------------------------------------------------------")
log.info("Setting version properties based on dependency management information:")

set("aries.blueprint.version", versionOf("org.apache.aries.blueprint", "org.apache.aries.blueprint"))
set("aries.jmx.version", versionOf("org.apache.aries.jmx", "org.apache.aries.jmx"))
set("aries.proxy.version", versionOf("org.apache.aries.proxy", "org.apache.aries.proxy"))
set("aries.util.version", versionOf("org.apache.aries", "org.apache.aries.util"))

set("felix.configadmin.version", versionOf("org.apache.felix", "org.apache.felix.configadmin"))
set("felix.bundlerepository.version", versionOf("org.apache.felix", "org.apache.felix.bundlerepository"))
set("felix.fileinstall.version", versionOf("org.apache.felix", "org.apache.felix.fileinstall"))

set("mina.version", versionOf("org.apache.mina", "mina-core"))

set("pax.logging.version", versionOf("org.ops4j.pax.logging", "pax-logging-api"))
set("pax.url.version", versionOf("org.ops4j.pax.url", "pax-url-mvn"))

set("sshd.version", versionOf("org.apache.sshd", "sshd-core"))

log.info("----------------------------------------------------------------------")