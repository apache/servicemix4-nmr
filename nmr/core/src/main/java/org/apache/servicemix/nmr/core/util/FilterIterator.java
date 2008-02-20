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
package org.apache.servicemix.nmr.core.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A filtering iterator
 */
public class FilterIterator<T> implements Iterator<T> {

    private Iterator<T> iterator;
    private Filter<T> filter;
    private T next;

    public FilterIterator(Iterator<T> iterator, Filter<T> filter) {
        this.iterator = iterator;
        this.filter = filter;
        this.next = checkNext();
    }

    public boolean hasNext() {
        return next != null;
    }

    public T next() {
        if (next != null) {
            T ep = next;
            next = checkNext();
            return ep;
        }
        throw new NoSuchElementException();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    protected T checkNext() {
        while (iterator.hasNext()) {
            T ep = iterator.next();
            if (filter.match(ep)) {
                return ep;
            }
        }
        return null;
    }
}
