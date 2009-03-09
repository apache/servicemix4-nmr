package org.apache.servicemix.nmr.osgi;

import java.util.Map;

import org.apache.servicemix.nmr.api.service.ServiceRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Mar 9, 2009
 * Time: 3:05:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class OsgiServiceRegistryTracker<T> implements BundleContextAware, InitializingBean, DisposableBean, ServiceTrackerCustomizer {

    private BundleContext bundleContext;
    private ServiceRegistry<T> registry;
    private Class clazz;
    private ServiceTracker tracker;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public Class getInterface() {
        return clazz;
    }

    public void setInterface(Class clazz) {
        this.clazz = clazz;
    }

    public ServiceRegistry<T> getRegistry() {
        return registry;
    }

    public void setRegistry(ServiceRegistry<T> registry) {
        this.registry = registry;
    }

    public void afterPropertiesSet() throws Exception {
        tracker = new ServiceTracker(bundleContext, clazz.getName(), this);
        tracker.open();
    }

    public void destroy() throws Exception {
        tracker.close();
    }

    public Object addingService(ServiceReference reference) {
        T service = (T) bundleContext.getService(reference);
        Map properties = OsgiServiceReferenceUtils.getServicePropertiesSnapshotAsMap(reference);
        registry.register(service, properties);
        return service;
    }

    public void modifiedService(ServiceReference reference, Object service) {
    }

    public void removedService(ServiceReference reference, Object service) {
        Map properties = OsgiServiceReferenceUtils.getServicePropertiesSnapshotAsMap(reference);
        registry.unregister((T) service, properties);
    }
}
