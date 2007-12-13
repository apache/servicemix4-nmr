package org.apache.servicemix.jbi.offline;


import java.io.File;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.UrlResource;

public class JarUtilTest {
    
    private static final String JAR_FILENAME = "org.apache.servicemix.api-4.0-SNAPSHOT.jar";
    
    private URL jarUrl = Thread.currentThread().getContextClassLoader().getResource(JAR_FILENAME);
    
    private JarUtil jarUtil = null;
    
    @Before
    public void setUp() throws Exception {
        jarUtil = new JarUtil();
    }

    @After
    public void tearDown() throws Exception {
    }
    
//    @Test
//    public void readJarFile() throws Exception {
//        File jarFile = new File(jarUrl.toURI());
//        jarUtil.loadJarFile(jarFile);
//    }
    
    @Test
    public void getJarContent() throws Exception {
        System.out.println("Using JAR: " + jarUrl.toURI().toString());
        String content = jarUtil.getJarContent(new UrlResource(jarUrl));
        System.out.println("JAR content: " + content);
    }
    
    @Test
    public void getManifestContent() throws Exception {
        String content = jarUtil.getManifestContent(new UrlResource(jarUrl));
        System.out.println("Manifest content: " + content);
    }

}
