/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * All Xref objects in BioPAX are mapped to DatabaseIdentifier in Reactome no matter what
 * the subclasses they are.
 * @author guanming, ed: Justin Preece
 *
 */
public class XrefMapper extends AbstractBioPAXToReactomeMapper {

    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory, 
                         XMLFileAdaptor reactomeAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass type = bpInstance.getRDFType();
        if (type == bpFactory.getxrefClass())
            throw new IllegalStateException(bpInstance + " should NOT be a Xref. A subclass to Xref" +
                    "should be used.");
        RDFSClass xrefCls = bpFactory.getxrefClass();
        if (!type.isSubclassOf(xrefCls))
            return;
        // No need to create GO. It should be handled by OpenControlledVocabularMapper.
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.DB);
        String dbName = (String) bpInstance.getPropertyValue(prop);
        //if (dbName == null)
        //    throw new IllegalStateException(bpInstance + " should have DB defined!");
        // GO database is used as UnificationXref in NCI PID. So they should not be escaped.
        //if (dbName != null && dbName.equals("GO"))
        //    return;
        // ID and DB are not specified for some relationshipXref in INOH
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.ID);
        String id = (String) bpInstance.getPropertyValue(prop);
        //if (id == null)
          //  throw new IllegalStateException(bpInstance + " should have ID defined!");
        // The following check if for INOH EventRelationship
        if ((id == null || id.length() == 0) &&
            (type == bpFactory.getrelationshipXrefClass())) {
            prop = bpFactory.getOWLProperty(BioPAXJavaConstants.RELATIONSHIP_TYPE);
            id = (String) bpInstance.getPropertyValue(prop);
        }
        GKInstance gkInstance = null;
        // check for pre-existing DatabaseIdentifier objects in the converted Reactome data, based on id
        Collection list = reactomeAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseIdentifier,
                                                                   ReactomeJavaConstants.identifier,
                                                                   "=",
                                                                   id);
        // if there are any, check their db to make sure we're not duplicating it
        if (list != null && list.size() > 0) {
            for (Iterator<?> it = list.iterator(); it.hasNext();) {
                GKInstance xref_instance = (GKInstance) it.next();
                // if it already exists based on db, skip it
                if (xref_instance.getAttributeValue(ReactomeJavaConstants.referenceDatabase).equals(dbName)) {
                    gkInstance = xref_instance;
                    break;
                }
            } 
        }
        if (gkInstance == null) { // since there aren't any, go ahead and create a new one without checking it's db first
            gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.DatabaseIdentifier);
            if (dbName != null && dbName.length() > 0) {
                GKInstance refDB = B2RMapperUtilities.getReferenceDB(dbName, reactomeAdaptor);
                gkInstance.setAttributeValue(ReactomeJavaConstants.referenceDatabase,
                                             refDB);
            }
            if (id != null && id.length() > 0) {
                gkInstance.setAttributeValue(ReactomeJavaConstants.identifier,
                                             id);
            }
        }
        bpToRInstancesMap.put(bpInstance, gkInstance);
    }

    @Override
    protected void mapClassProperties(OWLIndividual bpInstance, 
                                      BioPAXFactory bpFactory, 
                                      XMLFileAdaptor reactomeAdaptor,
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
    }

}
