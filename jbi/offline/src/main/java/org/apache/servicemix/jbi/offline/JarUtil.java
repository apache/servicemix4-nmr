package org.apache.servicemix.jbi.offline;

import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.springframework.core.io.Resource;
import org.springframework.osgi.internal.test.util.JarUtils;

public class JarUtil {

    private Manifest manifest; 
    
    public String getJarContent(Resource resource) {
        return JarUtils.dumpJarContent(resource);
    }
    
    public String getManifestContent(Resource resource) {
        manifest = JarUtils.getManifest(resource);
        StringBuffer content = new StringBuffer();
        final String indent = " "; 
        
        Map<String, Attributes> entries = manifest.getEntries();
        
        for (String string : entries.keySet()) {
            String entryName = (String) string;
            content.append(entryName);
            Attributes attrs = (Attributes) entries.get(entryName);
            
            for (Object element : attrs.keySet()) {
                Attributes.Name name = (Attributes.Name) element;
                String attr = attrs.getValue(name);
                content.append(indent + attr);
            }
        }
        
        return content.toString();
    }

}
