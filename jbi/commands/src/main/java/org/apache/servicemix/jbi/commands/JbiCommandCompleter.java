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
package org.apache.servicemix.jbi.commands;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.karaf.gshell.console.Completer;
import org.apache.felix.karaf.gshell.console.completer.StringsCompleter;
import org.apache.servicemix.jbi.deployer.Component;
import org.apache.servicemix.jbi.deployer.ServiceAssembly;
import org.apache.servicemix.jbi.deployer.impl.Deployer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.BundleContextAware;

/**
 * {@link org.apache.felix.karaf.gshell.console.Completer} for JBI artifacts.
 */
public class JbiCommandCompleter implements Completer, BundleContextAware {

    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public int complete(final String buffer, final int cursor, final List candidates) {
        Collection<String> artifacts = getComponentsAndAssemblies();
        StringsCompleter delegate = new StringsCompleter(artifacts);
        return delegate.complete(buffer, cursor, candidates);
    }

    protected Set<String> getComponentsAndAssemblies() {
        try {
            Set<String> artifacts = new HashSet<String>();
            ServiceReference[] references = bundleContext.getAllServiceReferences(Component.class.getName(), null);
            if (references != null) {
                for (ServiceReference ref : references) {
                    String name = (String) ref.getProperty(Deployer.NAME);
                    if (name != null) {
                        artifacts.add(name);
                    }
                }
            }
            references = bundleContext.getAllServiceReferences(ServiceAssembly.class.getName(), null);
            if (references != null) {
                for (ServiceReference ref : references) {
                    String name = (String) ref.getProperty(Deployer.NAME);
                    if (name != null) {
                        artifacts.add(name);
                    }
                }
            }
            return artifacts;
        } catch (Exception e) {
            return null;
        }
    }

}
