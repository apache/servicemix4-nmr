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
package org.apache.servicemix.naming;

import java.util.Hashtable;

import javax.naming.*;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.spi.NamingManager;

/**
 * InitialContext wrapper allowing the use of the default context provided, the OSGi context
 * or any URL context supported by the JVM.
 */
public class InitialContextWrapper implements DirContext {

    public static final String OSGI_SCHEME = "osgi";

    private final Context osgiContext;

    private Hashtable<Object, Object> env = new Hashtable<Object, Object>();
    private Context delegate;

    public InitialContextWrapper(Context delegate, Context osgiContext, Hashtable<?, ?> environment) throws NamingException {
        this.delegate = delegate;
        this.osgiContext = osgiContext;
    }

    protected Context getURLOrDefaultInitCtx(String name) throws NamingException {
        String scheme = getURLScheme(name);
        if (OSGI_SCHEME.equals(scheme)) {
            return osgiContext;
        } else if (scheme != null) {
            Context ctx = NamingManager.getURLContext(scheme, delegate.getEnvironment());
            if (ctx != null) {
                return ctx;
            }
        }
        return delegate;
    }

    private static String getURLScheme(String str) {
        int colon_posn = str.indexOf(':');
        int slash_posn = str.indexOf('/');
        if (colon_posn > 0 && (slash_posn == -1 || colon_posn < slash_posn)) {
            return str.substring(0, colon_posn);
        }
        return null;
    }

    public Object addToEnvironment(String propName, Object propVal) throws NamingException
    {
        Context ctx = getDelegate();

        if (ctx != null) ctx.addToEnvironment(propName, propVal);

        return env.put(propName, propVal);
    }

    public void bind(Name name, Object obj) throws NamingException
    {
        findContext(name).bind(name, obj);
    }

    public void bind(String name, Object obj) throws NamingException
    {
        findContext(name).bind(name, obj);
    }

    public void close() throws NamingException
    {
        if (delegate != null) delegate.close();
        env.clear();
    }

    public Name composeName(Name name, Name prefix) throws NamingException
    {
        return findContext(name).composeName(name, prefix);
    }

    public String composeName(String name, String prefix) throws NamingException
    {
        return findContext(name).composeName(name, prefix);
    }

    public Context createSubcontext(Name name) throws NamingException
    {
        return findContext(name).createSubcontext(name);
    }

    public Context createSubcontext(String name) throws NamingException
    {
        return findContext(name).createSubcontext(name);
    }

    public void destroySubcontext(Name name) throws NamingException
    {
        findContext(name).destroySubcontext(name);
    }

    public void destroySubcontext(String name) throws NamingException
    {
        findContext(name).destroySubcontext(name);
    }

    public Hashtable<?, ?> getEnvironment() throws NamingException
    {
        Hashtable<Object, Object> theEnv = new Hashtable<Object, Object>();
        theEnv.putAll(env);
        return theEnv;
    }

    public String getNameInNamespace() throws NamingException
    {
        return getDelegate().getNameInNamespace();
    }

    public NameParser getNameParser(Name name) throws NamingException
    {
        return findContext(name).getNameParser(name);
    }

    public NameParser getNameParser(String name) throws NamingException
    {
        return findContext(name).getNameParser(name);
    }

    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException
    {
        return findContext(name).list(name);
    }

    public NamingEnumeration<NameClassPair> list(String name) throws NamingException
    {
        return findContext(name).list(name);
    }

    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException
    {
        return findContext(name).listBindings(name);
    }

    public NamingEnumeration<Binding> listBindings(String name) throws NamingException
    {
        return findContext(name).listBindings(name);
    }

    public Object lookup(Name name) throws NamingException
    {
        return findContext(name).lookup(name);
    }

    public Object lookup(String name) throws NamingException
    {
        return findContext(name).lookup(name);
    }

    public Object lookupLink(Name name) throws NamingException
    {
        return findContext(name).lookupLink(name);
    }

    public Object lookupLink(String name) throws NamingException
    {
        return findContext(name).lookupLink(name);
    }

    public void rebind(Name name, Object obj) throws NamingException
    {
        findContext(name).rebind(name, obj);
    }

    public void rebind(String name, Object obj) throws NamingException
    {
        findContext(name).rebind(name, obj);
    }

    public Object removeFromEnvironment(String propName) throws NamingException
    {
        Context ctx = getDelegate();

        if (ctx != null) ctx.removeFromEnvironment(propName);

        return env.remove(propName);
    }

    public void rename(Name oldName, Name newName) throws NamingException
    {
        findContext(oldName).rename(oldName, newName);
    }

    public void rename(String oldName, String newName) throws NamingException
    {
        findContext(oldName).rename(oldName, newName);
    }

    public void unbind(Name name) throws NamingException
    {
        findContext(name).unbind(name);
    }

    public void unbind(String name) throws NamingException
    {
        findContext(name).unbind(name);
    }

    protected Context findContext(Name name) throws NamingException
    {
        return findContext(name.toString());
    }


    protected Context findContext(String name) throws NamingException
    {
        Context toReturn = null;

        if (name.contains(":")) {
            toReturn = getURLOrDefaultInitCtx(name);
        } else {
            toReturn =  getDelegate();
        }

        if (toReturn != null)
        {
            String packages = System.getProperty(Context.URL_PKG_PREFIXES, null);

            if (packages != null)
            {
                toReturn.addToEnvironment(Context.URL_PKG_PREFIXES, packages);
            }
        }

        return toReturn;
    }

    private Context getDelegate() throws NamingException
    {
        return delegate;
    }

    public Attributes getAttributes(Name name) throws NamingException {
        return ((DirContext)findContext(name)).getAttributes(name);
    }

    public Attributes getAttributes(String name) throws NamingException {
        return ((DirContext)findContext(name)).getAttributes(name);
    }

    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        return ((DirContext)findContext(name)).getAttributes(name, attrIds);
    }

    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return ((DirContext)findContext(name)).getAttributes(name, attrIds);
    }

    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
        ((DirContext)findContext(name)).modifyAttributes(name, mod_op, attrs);
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        ((DirContext)findContext(name)).modifyAttributes(name, mod_op, attrs);
    }

    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        ((DirContext)findContext(name)).modifyAttributes(name, mods);
    }

    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        ((DirContext)findContext(name)).modifyAttributes(name, mods);
    }

    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        ((DirContext)findContext(name)).bind(name, obj, attrs);
    }

    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        ((DirContext)findContext(name)).bind(name, obj, attrs);
    }

    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        ((DirContext)findContext(name)).rebind(name, obj, attrs);
    }

    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        ((DirContext)findContext(name)).rebind(name, obj, attrs);
    }

    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        return ((DirContext)findContext(name)).createSubcontext(name, attrs);
    }

    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return ((DirContext)findContext(name)).createSubcontext(name, attrs);
    }

    public DirContext getSchema(Name name) throws NamingException {
        return ((DirContext)findContext(name)).getSchema(name);
    }

    public DirContext getSchema(String name) throws NamingException {
        return ((DirContext)findContext(name)).getSchema(name);
    }

    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        return ((DirContext)findContext(name)).getSchemaClassDefinition(name);
    }

    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return ((DirContext)findContext(name)).getSchemaClassDefinition(name);
    }

    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return ((DirContext)findContext(name)).search(name, matchingAttributes, attributesToReturn);
    }

    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return ((DirContext)findContext(name)).search(name, matchingAttributes, attributesToReturn);
    }

    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
        return ((DirContext)findContext(name)).search(name, matchingAttributes);
    }

    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        return ((DirContext)findContext(name)).search(name, matchingAttributes);
    }

    public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
        return ((DirContext)findContext(name)).search(name, filter, cons);
    }

    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
        return ((DirContext)findContext(name)).search(name, filter, cons);
    }

    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return ((DirContext)findContext(name)).search(name, filterExpr, filterArgs, cons);
    }

    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return ((DirContext)findContext(name)).search(name, filterExpr, filterArgs, cons);
    }
}
