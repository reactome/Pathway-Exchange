/*
 * Created on Apr 15, 2013
 *
 */
package org.reactome.biopax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.Test;

/**
 * All BioPAX related test, checking and other things that cannot be assigned to other classes should be
 * listed here.
 * @author gwu
 *
 */
public class BioPAXTest {
    
    public BioPAXTest() {
        
    }
    
    /**
     * This method is used to make sure one UnificationXref should be used for one OWL instance only as
     * pointed by Igor's email on April 15, 2013.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void checkUnificationUsage() throws Exception {
        // Load the whole OWL file as JDOM document. Most likely, PAXTool should be used in the future.
        String fileName = "tmp/biopax3/Homo sapiens.owl";
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(fileName);
        Element root = document.getRootElement();
        List<Element> children = root.getChildren();
        Map<String, List<Element>> unificationIdToElements = new HashMap<String, List<Element>>();
        for (Element child : children) {
            List<Element> list = child.getChildren();
            for (Element child1 : list) {
                String name = child1.getName();
                if (name.equals("xref")) {
                    String resource = child1.getAttributeValue("resource", BioPAX3JavaConstants.rdfNS);
                    if (resource.startsWith("#UnificationXref")) {
                        addKeyAndValue(unificationIdToElements,
                                       resource, 
                                       child);
                    }
                }
            }
        }
        // Print out UnificationXref has been used in more than one Element
        for (String key : unificationIdToElements.keySet()) {
            List<Element> list = unificationIdToElements.get(key);
            if (list.size() > 1) {
                System.out.println(key + ": " + list.size());
                for (Element elm : list) {
                    System.out.println("\t" + elm.getAttributeValue("ID", BioPAX3JavaConstants.rdfNS));
                }
            }
        }
    }
    
    private void addKeyAndValue(Map<String, List<Element>> map,
                                String key,
                                Element value) {
        List<Element> list = map.get(key);
        if (list == null) {
            list = new ArrayList<Element>();
            map.put(key, list);
        }
        list.add(value);
    }
    
}
