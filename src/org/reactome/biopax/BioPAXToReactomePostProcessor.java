/*
 * Created on Jul 26, 2006
 *
 */
package org.reactome.biopax;

import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;

/**
 * This interface is used to post-process mapped GKInstances from BioPAX. Different
 * data source might need different post processer. For example, BioPAX files from
 * cellmap needs to map HPRD to UniProt.
 * @author guanming
 *
 */
public interface BioPAXToReactomePostProcessor {
    
    public void postProcess(MySQLAdaptor dbAdaptor,
                            XMLFileAdaptor fileAdaptor) throws Exception;
    
}
