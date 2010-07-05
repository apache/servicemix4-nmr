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
package org.apache.servicemix.document.factory;

import java.util.ArrayList;
import java.util.List;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ComponentDefinitionRegistryProcessor;
import org.apache.aries.blueprint.mutable.*;
import org.apache.servicemix.document.DocumentRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;

public class BlueprintDocumentFactory implements ComponentDefinitionRegistryProcessor {

    private BundleContext bundleContext;
    private String document;
    private String documentId;
    private BeanFactory beanFactory;
    private String beanName;
    private DocumentRepository repository;

    public void setDocument(String document) {
        this.document = document;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public void setRepository(DocumentRepository repository) {
        this.repository = repository;
    }

    public void afterPropertiesSet() throws Exception {
        if (document == null) {
            throw new IllegalStateException("document must be set");
        }
        if (repository != null) {
            this.documentId = repository.register(document.getBytes());
        } else if (bundleContext != null) {
            ServiceReference ref = bundleContext.getServiceReference(DocumentRepository.class.getName());
            if (ref == null) {
                throw new IllegalStateException("Can not get a reference to the DocumentRepository");
            }
            try {
                DocumentRepository rep = (DocumentRepository) bundleContext.getService(ref);
                this.documentId = rep.register(document.getBytes());
            } finally {
                if (ref != null) {
                    bundleContext.ungetService(ref);
                }
            }
        } else {
            throw new IllegalStateException("repoitory or bundleContext must be set");
        }
    }

    public void process(ComponentDefinitionRegistry registry) throws ComponentDefinitionException {
        for (String name : registry.getComponentDefinitionNames()) {
            processMetadata(registry.getComponentDefinition(name));
        }
    }

    protected Metadata processMetadata(Metadata metadata) {
        if (metadata instanceof BeanMetadata) {
            return processBeanMetadata((BeanMetadata) metadata);
        } else if (metadata instanceof ReferenceListMetadata) {
            return processRefCollectionMetadata((ReferenceListMetadata) metadata);
        } else if (metadata instanceof ReferenceMetadata) {
            return processReferenceMetadata((ReferenceMetadata) metadata);
        } else if (metadata instanceof ServiceMetadata) {
            return processServiceMetadata((ServiceMetadata) metadata);
        } else if (metadata instanceof CollectionMetadata) {
            return processCollectionMetadata((CollectionMetadata) metadata);
        } else if (metadata instanceof MapMetadata) {
            return processMapMetadata((MapMetadata) metadata);
        } else if (metadata instanceof PropsMetadata) {
            return processPropsMetadata((PropsMetadata) metadata);
        } else if (metadata instanceof ValueMetadata) {
            return processValueMetadata((ValueMetadata) metadata);
        } else {
            return metadata;
        }
    }

    protected Metadata processBeanMetadata(BeanMetadata component) {
        for (BeanArgument arg :  component.getArguments()) {
            ((MutableBeanArgument) arg).setValue(processMetadata(arg.getValue()));
        }
        for (BeanProperty prop : component.getProperties()) {
            ((MutableBeanProperty) prop).setValue(processMetadata(prop.getValue()));
        }
        ((MutableBeanMetadata) component).setFactoryComponent((Target) processMetadata(component.getFactoryComponent()));
        return component;
    }

    protected Metadata processServiceMetadata(ServiceMetadata component) {
        ((MutableServiceMetadata) component).setServiceComponent((Target) processMetadata(component.getServiceComponent()));
        List<MapEntry> entries = new ArrayList<MapEntry>(component.getServiceProperties());
        for (MapEntry entry : entries) {
            ((MutableServiceMetadata) component).removeServiceProperty(entry);
        }
        for (MapEntry entry : processMapEntries(entries)) {
            ((MutableServiceMetadata) component).addServiceProperty(entry);
        }
        for (RegistrationListener listener : component.getRegistrationListeners()) {
            ((MutableRegistrationListener) listener).setListenerComponent((Target) processMetadata(listener.getListenerComponent()));
        }
        return component;
    }

    protected Metadata processReferenceMetadata(ReferenceMetadata component) {
        for (ReferenceListener listener : component.getReferenceListeners()) {
            ((MutableReferenceListener) listener).setListenerComponent((Target) processMetadata(listener.getListenerComponent()));
        }
        return component;
    }

    protected Metadata processRefCollectionMetadata(ReferenceListMetadata component) {
        for (ReferenceListener listener : component.getReferenceListeners()) {
            ((MutableReferenceListener) listener).setListenerComponent((Target) processMetadata(listener.getListenerComponent()));
        }
        return component;
    }

    protected Metadata processPropsMetadata(PropsMetadata metadata) {
        List<MapEntry> entries = new ArrayList<MapEntry>(metadata.getEntries());
        for (MapEntry entry : entries) {
            ((MutablePropsMetadata) metadata).removeEntry(entry);
        }
        for (MapEntry entry : processMapEntries(entries)) {
            ((MutablePropsMetadata) metadata).addEntry(entry);
        }
        return metadata;
    }

    protected Metadata processMapMetadata(MapMetadata metadata) {
        List<MapEntry> entries = new ArrayList<MapEntry>(metadata.getEntries());
        for (MapEntry entry : entries) {
            ((MutableMapMetadata) metadata).removeEntry(entry);
        }
        for (MapEntry entry : processMapEntries(entries)) {
            ((MutableMapMetadata) metadata).addEntry(entry);
        }
        return metadata;
    }

    protected List<MapEntry> processMapEntries(List<MapEntry> entries) {
        for (MapEntry entry : entries) {
            ((MutableMapEntry) entry).setKey((NonNullMetadata) processMetadata(entry.getKey()));
            ((MutableMapEntry) entry).setValue(processMetadata(entry.getValue()));
        }
        return entries;
    }

    protected Metadata processCollectionMetadata(CollectionMetadata metadata) {
        List<Metadata> values = new ArrayList<Metadata>(metadata.getValues());
        for (Metadata value : values) {
            ((MutableCollectionMetadata) metadata).removeValue(value);
        }
        for (Metadata value : values) {
            ((MutableCollectionMetadata) metadata).addValue(processMetadata(value));
        }
        return metadata;
    }

    protected Metadata processValueMetadata(ValueMetadata metadata) {
        return new LateBindingValueMetadata(metadata);
    }

    public class LateBindingValueMetadata implements ValueMetadata {

        private final ValueMetadata metadata;
        private boolean retrieved;
        private String retrievedValue;

        public LateBindingValueMetadata(ValueMetadata metadata) {
            this.metadata = metadata;
        }

        public String getStringValue() {
            if (!retrieved) {
                retrieved = true;
                retrievedValue = processString(metadata.getStringValue());
            }
            return retrievedValue;
        }

        public String getType() {
            return metadata.getType();
        }

    }

    protected String processString(String str) {
        if (str.equals("document-name:" + beanName)) {
            str = documentId;
        }
        return str;
    }

}
