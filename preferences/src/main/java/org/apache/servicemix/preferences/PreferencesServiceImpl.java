package org.apache.servicemix.preferences;

import java.util.Arrays;
import java.util.List;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 15, 2008
 * Time: 2:40:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class PreferencesServiceImpl implements PreferencesService {

    private static final String SYSTEM_NODE = "system";

    private AbstractPreferencesImpl preferences;

    public PreferencesServiceImpl(AbstractPreferencesImpl preferences) {
        this.preferences = preferences;
    }

    public Preferences getSystemPreferences() {
        return preferences.node(SYSTEM_NODE);
    }

    public Preferences getUserPreferences(String s) {
        return preferences.node(s);
    }

    public String[] getUsers() {
        try {
            List<String> children = Arrays.asList(preferences.childrenNames());
            children.remove(SYSTEM_NODE);
            return children.toArray(new String[children.size()]);
        } catch (BackingStoreException e) {
            throw new IllegalStateException(e);
        }
    }
}
