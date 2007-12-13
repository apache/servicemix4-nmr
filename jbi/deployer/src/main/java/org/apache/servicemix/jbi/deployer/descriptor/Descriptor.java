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

import org.apache.servicemix.jbi.deployer.descriptor.Component;

/**
 * @version $Revision: 426415 $
 */
public class Descriptor {
    private double version;
    private Component component;
    private SharedLibrary sharedLibrary;
    private ServiceAssembly serviceAssembly;
    private Services services;

    public double getVersion() {
        return version;
    }

    public void setVersion(double version) {
        this.version = version;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public SharedLibrary getSharedLibrary() {
        return sharedLibrary;
    }

    public void setSharedLibrary(SharedLibrary sharedLibrary) {
        this.sharedLibrary = sharedLibrary;
    }

    public ServiceAssembly getServiceAssembly() {
        return serviceAssembly;
    }

    public void setServiceAssembly(ServiceAssembly serviceAssembly) {
        this.serviceAssembly = serviceAssembly;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }
}

/*
default namespace this = "http://java.sun.com/xml/ns/jbi"
start =
  element jbi {
    attribute version { xsd:decimal },
    ( component | shared-library | service-assembly | services)
  }
component =
  element component {
    attribute type { "service-engine" | "binding-component" },
    attribute component-class-loader-delegation { "parent-first" | "self-first" }?,
    attribute bootstrap-class-loader-delegation { "parent-first" | "self-first" }?,
    identification,
    element component-class-name { attribute description { text }?, text },
    element component-class-path { class-path },
    element bootstrap-class-name { text },
    element bootstrap-class-path { class-path },
    shared-library-list*,
    element* -this:* { text }*
  }
shared-library =
  element shared-library {
    attribute class-loader-delegation { "parent-first" | "self-first" }?,
    attribute version { text }?,
    identification,
    element shared-library-class-path { class-path }
  }
shared-library-list =
  element shared-library {
    attribute version { text }?,
    text
  }
service-assembly =
  element service-assembly {
    identification,
    service-unit*,
    connections?,
    element* -this:* { text }*
  }
service-unit =
  element service-unit {
    identification,
    element target {
      element artifacts-zip { text },
      element component-name { xsd:NCName }
    },
    element* -this:* { text }*
  }
identification =
  element identification {
    element name { xsd:NCName },
    element description { text },
    element* -this:* { text }*
 }
class-path =
  (element path-element { text })+
services =
  element services {
    attribute binding-component { xsd:boolean },
    provides*,
    consumes*,
    element* -this:* { text }*
  }
connections =
  element connections {
    element connection {
      element consumer {
        ( attribute interface-name { xsd:QName } |
          (attribute service-name { xsd:QName }, attribute endpoint-name { text })
        )
      },
      element provider {
        attribute service-name { xsd:QName }, attribute endpoint-name { text }
      }
    }*,
    element* -this:* { text }*
  }
provides =
  element provides {
    attribute interface-name { xsd:QName },
    attribute service-name {xsd:QName }, attribute endpoint-name { text },
    element* -this:* { text }*
  }
consumes =
  element consumes {
    attribute interface-name { xsd:QName },
    ( attribute service-name {xsd:QName }, attribute endpoint-name { text },
      attribute link-type { "standard" | "hard" | "soft" }? )?,
    element* -this:* { text }*
  }
*/
