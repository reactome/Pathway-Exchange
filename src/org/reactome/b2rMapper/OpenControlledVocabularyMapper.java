/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.util.Collection;
import java.util.Map;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.reactome.b2rMapper.B2RMapperUtilities.GO_TYPE;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * GO terms are mapped to corresponding GO classes in Reactome. All GO cellular components
 * are mapped to Reactome GO_CellularComponent for the time being. If they are used by
 * SequenceParticipant, their type will be switched to EntityComponent in SequenceParticipantMapper.
 * Other openControlledVocabularyMappers are mapped to DatabaseIdentifier, but handled by XRef since
 * Reactome cannot support other ControlledVocabulary.
 * @author guanming
 *
 */
public class OpenControlledVocabularyMapper extends
        AbstractBioPAXToReactomeMapper {

    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory,
                         XMLFileAdaptor fileAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass cls = bpInstance.getRDFType();
        if (cls != bpFactory.getopenControlledVocabularyClass())
            return;
        // Need to pull out properties
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.XREF);
        OWLIndividual bpXref = (OWLIndividual) bpInstance.getPropertyValue(prop);
        // bpXref should NOT be null
        //if (bpXref == null)
        //    throw new IllegalStateException(bpInstance.toString() + " has no XREF defined!");
        // Some openControlledVocabulary individuals have null XREF
        if (bpXref == null)
            return;
        // The main body here is trying to map GO terms
        // Term is handled in this method to avoid duplicated efforts
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.TERM);
        String term = (String) bpInstance.getPropertyValue(prop);
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.DB);
        String dbName = (String) bpXref.getPropertyValue(prop);
        GKInstance gkInstance = null;
        if (dbName.equals("GO")) {
            // Should mapped to one of GO classes
            prop = bpFactory.getOWLProperty(BioPAXJavaConstants.ID);
            String id = (String) bpXref.getPropertyValue(prop);
            if (!id.startsWith("GO:"))
                id = "GO:" + id; // In case it doesn't contain GO
            GO_TYPE goType = B2RMapperUtilities.getGOType(id);
            // Don't create new instance if it exist already
            if (goType == GO_TYPE.BP) {
                gkInstance = createGOInstance(ReactomeJavaConstants.GO_BiologicalProcess,
                                              id,
                                              term,
                                              fileAdaptor);
            }
            else if (goType == GO_TYPE.MF)
                gkInstance = createGOInstance(ReactomeJavaConstants.GO_MolecularFunction,
                                              id,
                                              term,
                                              fileAdaptor);
            else if (goType == GO_TYPE.CC)
                gkInstance = createGOInstance(ReactomeJavaConstants.GO_CellularComponent,
                                              id,
                                              term,
                                              fileAdaptor);
            if (gkInstance != null)
                bpToRInstancesMap.put(bpInstance, gkInstance);
        }
    }
    
    private GKInstance createGOInstance(String type, 
                                        String id, 
                                        String term,
                                        XMLFileAdaptor fileAdaptor) throws Exception {
        // GO: is not used for ID in Reactome
        if (id.startsWith("GO:"))
            id = id.substring(3);
        GKInstance gkInstance = null;
        Collection list = fileAdaptor.fetchInstanceByAttribute(type,
                                                               ReactomeJavaConstants.accession,
                                                               "=",
                                                               id);
        if (list != null && list.size() > 0)
            gkInstance = (GKInstance) list.iterator().next();
        else {
            gkInstance = fileAdaptor.createNewInstance(type);
            gkInstance.setAttributeValue(ReactomeJavaConstants.accession, id);
            gkInstance.setAttributeValue(ReactomeJavaConstants.name, term);
            GKInstance referenceDB = B2RMapperUtilities.getReferenceDB("GO", fileAdaptor);
            gkInstance.setAttributeValue(ReactomeJavaConstants.referenceDatabase,
                                         referenceDB);
        }
        return gkInstance;
    }
}
