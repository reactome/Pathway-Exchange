/*
 * Created on Jul 22, 2006
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
import org.gk.schema.SchemaAttribute;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

public class PathwayMapper extends AbstractBioPAXToReactomeMapper {

    public PathwayMapper() {
    }
    
    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory,
                         XMLFileAdaptor fileAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass cls = bpInstance.getRDFType();
        if (cls != bpFactory.getpathwayClass())
            return; // this method will work for pathway only
        GKInstance instance = fileAdaptor.createNewInstance(ReactomeJavaConstants.Pathway);
        bpToRInstancesMap.put(bpInstance, instance);
    }

    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor fileAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance rPathway = bpToRInstanceMap.get(bpInstance);
        if (rPathway == null)
            return; // Should NOT occur
        B2RMapperUtilities.mapOrganismProperty(bpInstance,
                                               bpFactory,
                                               rPathway,
                                               bpToRInstanceMap);
        getPathwayComponents(bpInstance, bpFactory, rPathway, bpToRInstanceMap);
    }

    private void getPathwayComponents(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      GKInstance rPathway,
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.PATHWAY_COMPONENTS);
        Collection bpComponents = bpInstance.getPropertyValues(prop);
        if (bpComponents == null || bpComponents.size() == 0)
            return;
        OWLIndividual bpComp;
        RDFSClass bpCompType;
        GKInstance rComp;
        // Used to check value
        // Try to make it work for the new schema (6/13/07 - wgm)
        SchemaAttribute hasComponentAtt = null;
        if (rPathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
            hasComponentAtt = rPathway.getSchemClass().getAttribute(ReactomeJavaConstants.hasComponent);
        else if (rPathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
            hasComponentAtt = rPathway.getSchemClass().getAttribute(ReactomeJavaConstants.hasEvent);
        if (hasComponentAtt == null)
            return;
        // Do a sort based on NEXT_STEP
        for (Iterator it = bpComponents.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (!(obj instanceof OWLIndividual)) {
                System.out.println(obj + " is not an OWLIndividual object!");
                throw new IllegalStateException("Object in the pathway component list is not an OWLIndividual object: " + obj);
            }
            bpComp = (OWLIndividual) obj;
            bpCompType = bpComp.getRDFType();
            if (bpCompType == bpFactory.getpathwayStepClass()) {
                // Grep the wrapped interaction or pathway instances
                prop = bpFactory.getOWLProperty(BioPAXJavaConstants.STEP_INTERACTIONS);
                Collection stepInteractions = bpComp.getPropertyValues(prop);
                if (stepInteractions == null || stepInteractions.size() == 0)
                    continue;
                for (Iterator it1 = stepInteractions.iterator(); it1.hasNext();) {
                    OWLIndividual stepInteraction = (OWLIndividual) it1.next();
                    if (stepInteraction.getRDFType() != bpFactory.getcontrolClass()) {
                        rComp = bpToRInstanceMap.get(stepInteraction);
                        // Some control Individuals are listed under PATHWAY_COMPONENT. However,
                        // they should not be a value in hasComponent
                        if (rComp != null &&
                            hasComponentAtt.isValidValue(rComp))
                            rPathway.addAttributeValue(hasComponentAtt, 
                                                       rComp);
                    }
                }
            }
            else if (bpCompType != bpFactory.getcontrolClass() &&
                     !bpCompType.isSubclassOf(bpFactory.getcontrolClass())) { 
                // Another two types should be: Interaction and Pathway. But should exclude
                // control class. Control class is attached to Reaction in Reactome
                rComp = bpToRInstanceMap.get(bpComp);
                if (rComp != null)
                    rPathway.addAttributeValue(hasComponentAtt,
                                               rComp);
            }
        }
    }

    @Override
    public void postMap(OWLIndividual bpInstance, 
                        BioPAXFactory bpFactor,
                        XMLFileAdaptor reactomeAdaptor,
                        Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        // Sort hasComponent or hasEvent attributes based on preceding/following.
        GKInstance pathway = (GKInstance) bpToRInstanceMap.get(bpInstance);
        SchemaAttribute attribute = null;
        if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
            attribute = pathway.getSchemClass().getAttribute(ReactomeJavaConstants.hasComponent);
        else if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
            attribute = pathway.getSchemClass().getAttribute(ReactomeJavaConstants.hasEvent);
        if (attribute == null)
            return;
        List originalValues = pathway.getAttributeValuesList(attribute);
        if (originalValues == null || originalValues.size() < 2)
            return; // No need to order
        List copy = new ArrayList(originalValues);
        for (Iterator it = copy.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            int index = originalValues.indexOf(inst);
            List preceded = inst.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
            if (preceded == null || preceded.size() == 0)
                continue;
            for (Iterator it1 = preceded.iterator(); it1.hasNext();) {
                GKInstance tmp = (GKInstance) it1.next();
                int tmpIndex = originalValues.indexOf(tmp);
                if (tmpIndex > index) {
                    originalValues.remove(tmpIndex);
                    originalValues.add(index, tmp);
                    index ++; // Move one step
                }
            }
        }
    }
    
    
}
