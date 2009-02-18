package org.apache.servicemix.naming;

import java.util.Hashtable;

import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.InitialContextFactory;
import javax.naming.NamingException;
import javax.naming.Context;

import org.springframework.beans.factory.InitializingBean;
import org.apache.xbean.naming.global.GlobalContextManager;

/**
 */
public class GlobalInitialContextFactoryBuilder implements InitialContextFactoryBuilder, InitializingBean {

    private Context globalContext;

    public Context getGlobalContext() {
        return globalContext;
    }

    public void setGlobalContext(Context globalContext) {
        this.globalContext = globalContext;
    }

    public void afterPropertiesSet() throws Exception {
        GlobalContextManager.setGlobalContext(globalContext);
    }

    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
        return new GlobalContextManager();
    }
}
