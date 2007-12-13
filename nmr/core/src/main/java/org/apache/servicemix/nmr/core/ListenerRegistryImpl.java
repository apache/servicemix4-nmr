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
package org.apache.servicemix.nmr.core;

import org.apache.servicemix.nmr.api.event.Listener;
import org.apache.servicemix.nmr.api.event.ListenerRegistry;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 */
public class ListenerRegistryImpl extends ServiceRegistryImpl<Listener> implements ListenerRegistry {

    /**
     * Retrieve an iterator of listeners of a certain type
     *
     * @param type the type of listeners
     * @return an iterator over the registered listeners
     */
    public <T extends Listener> Iterable<T> getListeners(final Class<T> type) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return new FilterIterator(type, getServices().iterator());
            }
        };
    }

    /**
     * A filtered iterator that will only return elements of a certain type
     */
    private static class FilterIterator<U, T extends U> implements Iterator<T> {

        private Iterator<U> iter;
        private Class<T> type;
        private T next;

        public FilterIterator(Class<T> type, Iterator<U> iter) {
            this.iter = iter;
            this.type = type;
            advance();
        }

        private void advance() {
            while (iter.hasNext()) {
                U elt = iter.next();
                if (type.isInstance(elt)) {
                    next = (T) elt;
                    return;
                }
            }
            next = null;
        }

        public boolean hasNext() {
            return next != null;
        }

        public T next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            T o = next;
            advance();
            return o;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
