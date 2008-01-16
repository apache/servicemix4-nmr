package org.apache.servicemix.preferences;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Jan 15, 2008
 * Time: 3:05:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class PreferencesServiceFactoryImpl implements ServiceFactory, BundleListener {

    private BundleContext bundleContext;
    private AbstractPreferencesImpl preferences;

    public PreferencesServiceFactoryImpl() {
    }

    public PreferencesServiceFactoryImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        init();
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public synchronized Object getService(Bundle bundle, ServiceRegistration serviceRegistration) {
        checkInit();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public synchronized void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object o) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.UNINSTALLED) {
            // TODO: remove backing bundle
        }
    }

    public void init() {
        if (bundleContext == null) {
            throw new NullPointerException("bundleContext is null");
        }
        bundleContext.addBundleListener(this);
        preferences = null;
    }

    public void destroy() throws BackingStoreException {
        if (bundleContext != null) {
            bundleContext.removeBundleListener(this);
        }
        preferences.flush();
    }

    protected void checkInit() {
        if (preferences == null) {
            throw new IllegalStateException("Preferences Service factory must be initialized");
        }
    }
}
