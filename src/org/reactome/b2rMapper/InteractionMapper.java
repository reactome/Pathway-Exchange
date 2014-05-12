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

/**
 * Some properties common to Reactome Reactions, CatalystActivity and Regulation are handled
 * here. 
 * @author guanming
 *
 */
public class InteractionMapper extends AbstractBioPAXToReactomeMapper {

    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor,
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance gkInstance = bpToRInstanceMap.get(bpInstance);
        if (gkInstance == null)
            return ;
        // Extract Evidences
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.EVIDENCE);
        if (prop == null)
            return; // EVIDENCE is a new property in level 2
        Collection evidenceCollection = bpInstance.getPropertyValues(prop);
        if (evidenceCollection != null && 
            evidenceCollection.size() > 0 &&
            gkInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.summation)) {
            // Remember: CatalystActivity cannot have a summation.
            for (Iterator it = evidenceCollection.iterator(); it.hasNext();) {
                OWLIndividual evidence = (OWLIndividual) it.next();
                GKInstance gkEvidence = bpToRInstanceMap.get(evidence);
                if (gkEvidence != null)
                    gkInstance.addAttributeValue(ReactomeJavaConstants.summation,
                                                 gkEvidence);
            }
        }
    }

}
