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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.BeansException;
import org.springframework.osgi.context.BundleContextAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.apache.servicemix.document.DocumentRepository;

/**
 * A spring document factory allowing documents to be registered in the DocumentRegistry
 * and also post processes the document-name:xxx url to transform it into a
 * document: url handled by the repository.
 */
public class DocumentFactory implements FactoryBean, BundleContextAware, InitializingBean, BeanFactoryPostProcessor, BeanNameAware, BeanFactoryAware {

    private BundleContext bundleContext;
    private String document;
    private String documentId;
    private BeanFactory beanFactory;
    private String beanName;
    private DocumentRepository repository;

    public Object getObject() throws Exception {
        return documentId;
    }

    public Class getObjectType() {
        return String.class;
    }

    public boolean isSingleton() {
        return true;
    }

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

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactoryToProcess) throws BeansException {
        BeanDefinitionVisitor visitor = new BundleExtUrlBeanDefinitionVisitor();
        String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
        for (int i = 0; i < beanNames.length; i++) {
            // Check that we're not parsing our own bean definition,
            // to avoid failing on unresolvable placeholders in properties file locations.
            if (!(beanNames[i].equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
                BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(beanNames[i]);
                try {
                    visitor.visitBeanDefinition(bd);
                } catch (BeanDefinitionStoreException ex) {
                    throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanNames[i], ex.getMessage());
                }
            }
        }
    }

    private class BundleExtUrlBeanDefinitionVisitor extends BeanDefinitionVisitor {

        protected String resolveStringValue(String string) {
            if (string.equals("document-name:" + beanName)) {
                string = documentId;
            }
            return string;
        }
    }

}
