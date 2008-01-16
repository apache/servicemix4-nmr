package org.apache.servicemix.preferences;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 15, 2008
 * Time: 2:42:49 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractPreferencesImpl implements Preferences {

    private AbstractPreferencesImpl parent;
    private String name;
    private boolean removed;
    private boolean dirty;

    protected AbstractPreferencesImpl(AbstractPreferencesImpl parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public void put(String key, String value) {
        internalSetValue(key, value);
    }

    public String get(String key, String defaultValue) {
        String val = internalGetValue(key);
        return (val != null) ? val : defaultValue;
    }

    public void remove(String key) {
        check(key);
        checkStatus();
        if (doRemoveValue(key)) {
            makeDirty();
        }
    }

    public void clear() throws BackingStoreException {
        checkStatus();
        if (doClear()) {
            makeDirty();
        }
    }

    public void putInt(String key, int value) {
        internalSetValue(key, Integer.toString(value));
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(internalGetValue(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void putLong(String key, long value) {
        internalSetValue(key, Long.toString(value));
    }

    public long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(internalGetValue(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void putBoolean(String key, boolean value) {
        internalSetValue(key, Boolean.toString(value));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = internalGetValue(key);
        if (val != null) {
            if (val.equalsIgnoreCase("true")) {
                return true;
            }
            if (val.equalsIgnoreCase("false")) {
                return false;
            }
        }
        return defaultValue;
    }

    public void putFloat(String key, float value) {
        internalSetValue(key, Float.toString(value));
    }

    public float getFloat(String key, float defaultValue) {
        try {
            return Float.parseFloat(internalGetValue(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void putDouble(String key, double value) {
        internalSetValue(key, Double.toString(value));
    }

    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(internalGetValue(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void putByteArray(String key, byte[] value) {
        internalSetValue(key, Base64.encode(value));
    }

    public byte[] getByteArray(String key, byte[] defaultValue) {
        byte[] value = Base64.decode(internalGetValue(key));
        return value != null ? value : defaultValue;
    }

    public String[] keys() throws BackingStoreException {
        checkStatus();
        List<String> names = doGetKeys();
        return names.toArray(new String[names.size()]);
    }

    public String[] childrenNames() throws BackingStoreException {
        checkStatus();
        List<String> names = new ArrayList<String>();
        for (AbstractPreferencesImpl p : doGetChildren()) {
            names.add(p.name());
        }
        return names.toArray(new String[names.size()]);
    }

    public Preferences parent() {
        checkStatus();
        return parent;
    }

    public Preferences node(String path) {
        if (path == null) {
            throw new NullPointerException("path is null");
        }
        checkStatus();
        if (path.equals("")) {
            return this;
        }
        if (path.indexOf("//") >= 0) {
            throw new IllegalArgumentException();
        }
        if (path.endsWith("/") && !path.equals("/")) {
            throw new IllegalArgumentException();
        }
        AbstractPreferencesImpl node = this;
        String[] paths = path.split("/");
        if (path.startsWith("/")) {
            while (node.parent != null) {
                node = node.parent;
            }
        }
        for (String p : paths) {
            AbstractPreferencesImpl tc = null;
            for (AbstractPreferencesImpl c : node.doGetChildren()) {
                if (p.equals(c.name)) {
                    tc = c;
                    break;
                }
            }
            if (tc == null) {
                tc = doCreateChild(p);
            }
            node = tc;
        }
        return node;
    }

    public boolean nodeExists(String path) throws BackingStoreException {
        if (path == null) {
            throw new NullPointerException("path is null");
        }
        boolean r = false;
        for (AbstractPreferencesImpl p = this; p.parent != null; p = p.parent) {
            r |= p.removed;
        }
        if (path.equals("")) {
            return r;
        }
        if (path.indexOf("//") >= 0) {
            throw new IllegalArgumentException();
        }
        if (path.endsWith("/") && !path.equals("/")) {
            throw new IllegalArgumentException();
        }
        if (r) {
            throw new IllegalStateException("Node has been removed");
        }
        AbstractPreferencesImpl node = this;
        String[] paths = path.split("/");
        if (path.startsWith("/")) {
            while (node.parent != null) {
                node = node.parent;
            }
        }
        for (String p : paths) {
            AbstractPreferencesImpl tc = null;
            for (AbstractPreferencesImpl c : node.doGetChildren()) {
                if (p.equals(c.name)) {
                    tc = c;
                    break;
                }
            }
            if (tc == null) {
                return false;
            }
            node = tc;
        }
        return true;
    }

    public void removeNode() throws BackingStoreException {
        checkStatus();
        parent.doRemoveNode(name);
        removed = true;
        makeDirty();
    }

    public String name() {
        return name;
    }

    public String absolutePath() {
        if (parent != null) {
            return parent.absolutePath() + "/" + name;
        } else {
            return "/" + name;
        }
    }

    public void flush() throws BackingStoreException {
        checkStatus();
        parent.flush();
        if (dirty) {
            doFlush();
            dirty = false;
        }
    }

    public void sync() throws BackingStoreException {
        flush();
    }

    private String internalGetValue(String key) throws BackingStoreException {
        check(key);
        checkStatus();
        return doGetValue(key);
    }

    private void internalSetValue(String key, String value) throws BackingStoreException {
        check(key);
        checkStatus();
        String v = doGetValue(key);
        if (v == value) {
            return;
        }
        if (value == null || v == null || value.equals(v)) {
            doSetValue(key, value);
            makeDirty();
        }
    }

    private void check(String key) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
    }

    private void checkStatus() {
        for (AbstractPreferencesImpl p = this; p.parent != null; p = p.parent) {
            if (p.removed) {
                throw new IllegalStateException("Node has been removed");
            }
        }
    }

    private void makeDirty() {
        for (AbstractPreferencesImpl p = this; p.parent != null; p = p.parent) {
            p.dirty = true;
        }
    }

    abstract String doGetValue(String key) throws BackingStoreException;

    abstract void doSetValue(String key, String value) throws BackingStoreException;

    abstract boolean doRemoveValue(String key) throws BackingStoreException;

    abstract boolean doClear() throws BackingStoreException;

    abstract List<String> doGetKeys() throws BackingStoreException;

    abstract List<AbstractPreferencesImpl> doGetChildren() throws BackingStoreException;

    abstract AbstractPreferencesImpl doCreateChild(String name) throws BackingStoreException;

    abstract void doRemoveNode(String node) throws BackingStoreException;

    abstract void doFlush() throws BackingStoreException;

}
