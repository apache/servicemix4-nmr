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

/**
 * @version $Revision: 426415 $
 */
public class Services {
    private boolean bindingComponent;
    private Provides[] provides;
    private Consumes[] consumes;

    public boolean isBindingComponent() {
        return bindingComponent;
    }

    public void setBindingComponent(boolean bindingComponent) {
        this.bindingComponent = bindingComponent;
    }

    public Provides[] getProvides() {
        return provides;
    }

    public void setProvides(Provides[] provides) {
        this.provides = provides;
    }

    public Consumes[] getConsumes() {
        return consumes;
    }

    public void setConsumes(Consumes[] consumes) {
        this.consumes = consumes;
    }
}
