/*
 * Created on Feb 5, 2010
 *
 */
package org.reactome.convert.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;


/**
 * This class is used to work with front page item, another way to create a top-level pathways for converting.
 * @author wgm
 *
 */
public class FrontPageItemHelper {
  
    public FrontPageItemHelper() {
        
    }
    
    @SuppressWarnings("unchecked")
    public Map<GKInstance, List<GKInstance>> generateSpeciesToFrontPageItemMap(MySQLAdaptor dba) throws Exception {
        Map<GKInstance, List<GKInstance>> map = new HashMap<GKInstance, List<GKInstance>>();
        Collection c = dba.fetchInstancesByClass(ReactomeJavaConstants.FrontPage);
        // There should be only one FrontPage instance.
        GKInstance frontPage = (GKInstance) c.iterator().next();
        // Get the list of FrontPageItem
        List items = frontPage.getAttributeValuesList(ReactomeJavaConstants.frontPageItem);
        // All should be human
        // Get list all instances together
        Set<GKInstance> allItems = new HashSet<GKInstance>();
        allItems.addAll(items);
        for (Iterator it = items.iterator(); it.hasNext();) {
            GKInstance item = (GKInstance) it.next();
            Collection others = item.getReferers(ReactomeJavaConstants.orthologousEvent);
            if (others == null)
                continue;
            allItems.addAll(others);
        }
        // Sort based on Species
        for (GKInstance item : allItems) {
            List values = item.getAttributeValuesList(ReactomeJavaConstants.species);
            // Species is a multiple value in case it is multiple
            for (Iterator it = values.iterator(); it.hasNext();) {
                GKInstance species = (GKInstance) it.next();
                List<GKInstance> list = map.get(species);
                if (list == null) {
                    list = new ArrayList<GKInstance>();
                    map.put(species, list);
                }
                list.add(item);
            }
        }
        return map;
    }
    
    @Test
    public void testGenerateSpeciesToFrontPageItemMap() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "gk_current_ver31",
                                            "root",
                                            "macmysql01");
        Map<GKInstance, List<GKInstance>> speciesToItems = generateSpeciesToFrontPageItemMap(dba);
        List<GKInstance> list = new ArrayList<GKInstance>(speciesToItems.keySet());
        InstanceUtilities.sortInstances(list);
        for (GKInstance species : list) {
            List<GKInstance> items = speciesToItems.get(species);
            System.out.println(species.getDisplayName() + ": " + items.size());
        }
    }
    
}
