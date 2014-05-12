/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.AraCycPostProcessor;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;

public class PathwayStepMapper extends AbstractBioPAXToReactomeMapper {

    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        // Extract preceding/following relationships
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.STEP_INTERACTIONS);
        Collection stepInteractions = bpInstance.getPropertyValues(prop);
        if (stepInteractions == null || stepInteractions.size() == 0)
            return;
        // Get the next steps
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.NEXT_STEP);
        Collection nextSteps = bpInstance.getPropertyValues(prop);
        if (nextSteps == null || nextSteps.size() == 0)
            return;
        // Extract followingInterctions from nextSteps
        Set<OWLIndividual> nextInteractions = new HashSet<OWLIndividual>();
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.STEP_INTERACTIONS);
        for (Iterator it = nextSteps.iterator(); it.hasNext();) {
            OWLIndividual nextStep = (OWLIndividual) it.next();
            Collection stepInteractions1 = nextStep.getPropertyValues(prop);
            if (stepInteractions1 != null) {
                for (Iterator it1 = stepInteractions1.iterator(); it1.hasNext();) {
                    nextInteractions.add((OWLIndividual)it1.next());
                }
            }
        }
        if (nextInteractions.size() == 0)
            return;
        // Map step interactions first
        Set<GKInstance> precedingGKInstances = new HashSet<GKInstance>();
        for (Iterator it = stepInteractions.iterator(); it.hasNext();) {
            OWLIndividual stepInteraction = (OWLIndividual) it.next();
            GKInstance gkInstance = bpToRInstanceMap.get(stepInteraction);
            if (gkInstance == null) {
                System.err.println("StepInteraction cannot be mapped: " + stepInteraction.getName());
                continue;
            }
            if (gkInstance.getSchemClass().isa(ReactomeJavaConstants.Event))
                precedingGKInstances.add(gkInstance);
        } 
        if (precedingGKInstances.size() == 0)
            return; // It is possible
        // In Reactome, precedingEvent should be used.
        for (OWLIndividual nextInteraction : nextInteractions) {
            GKInstance nextGKInstance = bpToRInstanceMap.get(nextInteraction);
            if (nextGKInstance == null)
                // Make sure all interactions are mapped
                throw new IllegalStateException(nextInteraction.getLocalName() + " is not mapped: " + nextInteraction);
            // Interaction might be converted to CatalystActivity or Control
            if (nextGKInstance.getSchemClass().isa(ReactomeJavaConstants.Event)) {
                for (GKInstance precedingEvent : precedingGKInstances)
                    nextGKInstance.addAttributeValue(ReactomeJavaConstants.precedingEvent,
                                                     precedingEvent);
            }
        }
    }

}
