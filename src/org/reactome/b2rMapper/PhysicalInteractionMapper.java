/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.util.Collection;
import java.util.Map;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.SchemaAttribute;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;
import org.gk.model.ReactomeJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * The BioPAX physicalInteraction is mapped to Reaction for the time being since there is no
 * Interaction class in the Reactome data model right now. A flag called "PhysicalInteracction from
 * BioPAX" is used in the definition slot for the Reactome reaction. All participants are copied
 * as input.
 * @author guanming
 *
 */
public class PhysicalInteractionMapper extends AbstractBioPAXToReactomeMapper {
    
    /**
     * Map BioPAX PhysicalInteraction to Reactome Reaction that has only input. Add a label
     * to reaction as "PhysicalInteraction" in the defintion slot.
     */
    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory, 
                         XMLFileAdaptor reactomeAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass bpCls = bpInstance.getRDFType();
        if (!bpCls.getLocalName().equals("physicalInteraction"))
            return;
        GKInstance rInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.Interaction);
        try {
            rInstance.setAttributeValue(ReactomeJavaConstants.definition, "PhysicalInteraction from BioPAX");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        bpToRInstancesMap.put(bpInstance, rInstance);
    }

    @Override
    protected void mapClassProperties(OWLIndividual bpInstance, 
                                      BioPAXFactory bpFactory, 
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance rReaction = bpToRInstanceMap.get(bpInstance);
        if (rReaction == null)
            return;
        extractParticipants(bpInstance, rReaction, bpFactory, bpToRInstanceMap);
    }
    
    private void extractParticipants(OWLIndividual bpInstance,
                                     GKInstance rReaction,
                                     BioPAXFactory bpFactory,
                                     Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        // Make this method work for PhysicalInteraction only. Conversion should be handled by itself.
        RDFSClass cls = bpInstance.getRDFType();
        if (cls != bpFactory.getphysicalInteractionClass())
            return;
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.PARTICIPANTS);
        Collection participants = bpInstance.getPropertyValues(prop);
        if (participants == null || participants.size() == 0)
            return;
        OWLIndividual bpParticipant = null;
        GKInstance rInteractor = null;
        SchemaAttribute rInteractorAtt = rReaction.getSchemClass().getAttribute(ReactomeJavaConstants.interactor);
        for (Object obj : participants) {
            bpParticipant = (OWLIndividual) obj;
            rInteractor = bpToRInstanceMap.get(bpParticipant);
            if (rInteractor != null && rInteractorAtt.isValidValue(rInteractor)) {
                rReaction.addAttributeValue(ReactomeJavaConstants.interactor,
                                            rInteractor);
            }
        }
    }
}
