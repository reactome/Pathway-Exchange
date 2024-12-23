/*
 * Created on Dec 20, 2010
 *
 */
package org.reactome.biopax;

import java.util.Collection;
import java.util.Iterator;

import org.gk.model.GKInstance;
import org.gk.model.PersistenceAdaptor;
import org.gk.persistence.MySQLAdaptor;
import org.jdom.Element;
import org.jdom.Namespace;

public class ReactomeToBioPAXUtilities {
    
    public static Element createIndividualElm(String elmName,
                                              String id,
                                              Namespace bpNS,
                                              Namespace rdfNS,
                                              Element rootElm) {
        Element rtn = new Element(elmName, bpNS);
        rtn.setAttribute("ID", id, rdfNS);
        if (rootElm != null)
            rootElm.addContent(rtn);
        return rtn;
    }
    
    public static Element createDataPropElm(Element domainElm,
                                            String propName,
                                            String dataType,
                                            Object value,
                                            Namespace bpNS,
                                            Namespace rdfNS) {
        Element rtn = new Element(propName, bpNS);
        domainElm.addContent(rtn);
        rtn.setAttribute("datatype", dataType, rdfNS);
        rtn.setText(value + "");
        return rtn;
    }
    
    public static void createDataPropElm(Element domainElm,
                                         String propName,
                                         String dataType,
                                         Collection<?> values,
                                         Namespace bpNS,
                                         Namespace rdfNS) {
        Object value = null;
        for (Iterator<?> it = values.iterator(); it.hasNext();) {
            value = it.next();
            createDataPropElm(domainElm, 
                              propName, 
                              dataType, 
                              value,
                              bpNS,
                              rdfNS);
        }
    }
    
    public static String getCurrentReleaseDbName(GKInstance inst) throws Exception {
        PersistenceAdaptor adaptor = inst.getDbAdaptor();
        if (adaptor instanceof MySQLAdaptor) {
            MySQLAdaptor dba = (MySQLAdaptor) adaptor;
            Integer releaseNumber = dba.getReleaseNumber();
            if (releaseNumber != null)
                return BioPAXJavaConstants.REACTOME_DB_ID + " Release " + releaseNumber;
        }
        return BioPAXJavaConstants.REACTOME_DB_ID;
    }
    
    /**
     * Fetch the current release number from a passed GKInstance object.
     * @param inst
     * @return it may be null if the passed GKInstance is from a local project or release number is not set
     * in the db.
     * @throws Exception
     */
    public static Integer getCurrentReleaseNumber(GKInstance inst) throws Exception {
        PersistenceAdaptor adaptor = inst.getDbAdaptor();
        if (adaptor instanceof MySQLAdaptor) {
            MySQLAdaptor dba = (MySQLAdaptor) adaptor;
            Integer releaseNumber = dba.getReleaseNumber();
            System.out.println("Release number is " + releaseNumber);
            return releaseNumber;
        }
        return null;
    }
    
}
