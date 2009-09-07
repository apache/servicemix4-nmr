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
package org.apache.servicemix.jbi.osgi;

import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 * A simple converter to convert generics
 */
public class PassThroughConverter implements Converter {

    private Class target;

    public void setTarget(Class target) {
        this.target = target;
    }

    public boolean canConvert(Object sourceObject, ReifiedType targetType) {
        return target.isInstance(sourceObject) &&
               targetType.getRawClass().equals(target);
    }

    public Object convert(Object sourceObject, ReifiedType targetType) throws Exception {
        return sourceObject;
    }
}