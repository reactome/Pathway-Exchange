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
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * SequenceFeaure is mapped to ModifiedResidue in Reactome. _displayName in ModfiedResidue
 * is used for the type. However, semantically, not all SequenceFeatures should be mapped 
 * as ModifiedResidues. This is just a temporary way.
 * @author guanming
 *
 */
public class SequenceFeatureMapper extends AbstractBioPAXToReactomeMapper {
    
    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory, 
                         XMLFileAdaptor reactomeAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass type = bpInstance.getRDFType();
        if (type != bpFactory.getsequenceFeatureClass())
            return;
        String displayName = B2RMapperUtilities.generateSequenceFeatureDisplayName(bpInstance, bpFactory);
        GKInstance gkInstance = null;
        Collection collection = reactomeAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ModifiedResidue,
                                                         ReactomeJavaConstants._displayName,
                                                         "=",
                                                         displayName);
        if (collection != null && collection.size() > 0)
            gkInstance = (GKInstance) collection.iterator().next();
        else {
            gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.ModifiedResidue);
            gkInstance.setDisplayName(displayName);
        }
        bpToRInstancesMap.put(bpInstance, gkInstance);
    }

}
