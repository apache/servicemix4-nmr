package org.apache.servicemix.preferences;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import org.osgi.service.prefs.BackingStoreException;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 15, 2008
 * Time: 11:30:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class FilePreferencesImpl extends AbstractPreferencesImpl {

    private static final String PROPS_FILE = "node.properties";

    private File root;
    private Properties props;

    public FilePreferencesImpl(AbstractPreferencesImpl parent, String name, File path) {
        super(parent, name);
        this.root = path;
    }

    String doGetValue(String key) throws BackingStoreException {
        ensureLoaded();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    void doSetValue(String key, String value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    boolean doRemoveValue(String key) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    boolean doClear() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    List<String> doGetKeys() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    List<AbstractPreferencesImpl> doGetChildren() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    AbstractPreferencesImpl doCreateChild(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    void doRemoveNode(String node) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    void doFlush() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private void ensureLoaded() throws BackingStoreException {
        try {
            if (props == null) {
                Properties p = new Properties();
                p.load(new FileInputStream(new File(root, PROPS_FILE)));
                props = p;
            }
        } catch (Exception e) {
            throw new BackingStoreException("Unable to load node", e);
        }
    }

}
