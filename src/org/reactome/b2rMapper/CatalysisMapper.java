/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * Catalysis is mapped to CatalystActivity.
 * @author guanming
 *
 */
public class CatalysisMapper extends AbstractBioPAXToReactomeMapper {

    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory, 
                         XMLFileAdaptor reactomeAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass type = bpInstance.getRDFType();
        if (type != bpFactory.getcatalysisClass())
            return;
        GKInstance gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.CatalystActivity);
        bpToRInstancesMap.put(bpInstance, gkInstance);
    }

    @Override
    protected void mapClassProperties(OWLIndividual bpInstance, 
                                      BioPAXFactory bpFactory, 
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance gkInstance = bpToRInstanceMap.get(bpInstance);
        if (gkInstance == null)
            return;
        OWLProperty prop;
        mapController(bpInstance, bpFactory, bpToRInstanceMap, gkInstance);
        mapCofactors(bpInstance, bpFactory, bpToRInstanceMap, gkInstance);
        mapControlled(bpInstance, bpFactory, gkInstance, bpToRInstanceMap);
    }

    @SuppressWarnings("unchecked")
    private void mapControlled(OWLIndividual bpInstance, 
                               BioPAXFactory bpFactory,
                               GKInstance gkInstance,
                               Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        OWLProperty prop;
        // Get the controlled.
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.CONTROLLED);
        OWLIndividual controlled = (OWLIndividual) bpInstance.getPropertyValue(prop);
        if (controlled != null) {
            GKInstance gkControlled = bpToRInstanceMap.get(controlled);
            if (gkControlled == null)
                return;
            gkControlled.addAttributeValue(ReactomeJavaConstants.catalystActivity,
                                           gkInstance);
            // There are multiple Catalysis in BioPAX. However, only one CA is allowed.
            // Use this way to keep these CAS for the time being.
            gkControlled.addAttributeValueNoCheck("CAS",
                                                  gkInstance);
        }
        // Have to figure out the direction. Inputs and outputs should be switched in case.
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.DIRECTION);
        String direction = (String) bpInstance.getPropertyValue(prop);
        if (direction != null &&
            direction.endsWith("RIGHT-TO-LEFT") &&
            controlled != null) {
            // Get the reaction
            GKInstance reaction = bpToRInstanceMap.get(controlled);
            if (reaction != null && reaction.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                // Need to switch inputs and outputs
                List inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
                List inputsCopy = null;
                if (inputs != null)
                    inputsCopy = new ArrayList(inputs);
                List outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
                List outputsCopy = null;
                if (outputs != null)
                    outputsCopy = new ArrayList(outputs);
                reaction.setAttributeValue(ReactomeJavaConstants.input, outputsCopy);
                reaction.setAttributeValue(ReactomeJavaConstants.output, inputsCopy);
            }
        }
    }

    private void mapCofactors(OWLIndividual bpInstance, 
                              BioPAXFactory bpFactory, 
                              Map<OWLIndividual, GKInstance> bpToRInstanceMap, 
                              GKInstance gkInstance) throws InvalidAttributeException, InvalidAttributeValueException {
        OWLProperty prop;
        // Get the cofactor. Cofactor might have more than one.
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.COFACTOR);
        Collection bpCofactorCollection = bpInstance.getPropertyValues(prop);
        if (bpCofactorCollection != null && bpCofactorCollection.size() > 0) {
            for (Iterator it = bpCofactorCollection.iterator(); it.hasNext();) {
                OWLIndividual bpCofactor = (OWLIndividual) it.next();
                GKInstance gkFactor = bpToRInstanceMap.get(bpCofactor);
                if (gkFactor != null)
                    gkInstance.addAttributeValue(ReactomeJavaConstants.physicalEntity,
                                                 gkFactor);
            }
        }
    }

    private void mapController(OWLIndividual bpInstance, 
                               BioPAXFactory bpFactory, 
                               Map<OWLIndividual, GKInstance> bpToRInstanceMap, 
                               GKInstance gkInstance) throws InvalidAttributeException, InvalidAttributeValueException {
        // Get the controller
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.CONTROLLER);
        OWLIndividual controller = (OWLIndividual) bpInstance.getPropertyValue(prop);
        if (controller != null) {
            GKInstance gkCatalyst = bpToRInstanceMap.get(controller);
            if (gkCatalyst != null)
                gkInstance.addAttributeValue(ReactomeJavaConstants.physicalEntity,
                                             gkCatalyst);
        }
    }

}
