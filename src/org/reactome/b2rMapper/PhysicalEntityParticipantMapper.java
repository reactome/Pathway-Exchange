/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.util.Map;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaAttribute;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * PhysicalEntityParticipant can be mapped to Complex, SmallMolecule or OtherEntity depeneds on
 * the wrapped PhysicalEntity.
 * @author guanming
 *
 */
public class PhysicalEntityParticipantMapper extends
        AbstractBioPAXToReactomeMapper {
    
    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory,
                         XMLFileAdaptor fileAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass cls = bpInstance.getRDFType();
        if (cls != bpFactory.getphysicalEntityParticipantClass())
            return;
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.PHYSICAL_ENTITY);
        OWLIndividual physicalEntity = (OWLIndividual) bpInstance.getPropertyValue(prop);
        RDFSClass peCls = physicalEntity.getRDFType();
        if (peCls == bpFactory.getcomplexClass()) {
            // Check if a Complex has been created
            GKInstance complex = bpToRInstancesMap.get(physicalEntity);
            if (complex == null) {
                complex = fileAdaptor.createNewInstance(ReactomeJavaConstants.Complex);
                // Complex and its PhysicalEntityParticipant will be merged as one in the Reactome
                // data model.
                bpToRInstancesMap.put(physicalEntity, complex);
            }
            // It is possible the wrapped Complex has been mapped
            bpToRInstancesMap.put(bpInstance, complex);
        }
        else if (peCls == bpFactory.getsmallMoleculeClass()) {
            GKInstance simpleEntity = fileAdaptor.createNewInstance(ReactomeJavaConstants.SimpleEntity);
            bpToRInstancesMap.put(bpInstance, simpleEntity);
        }
        else if (peCls == bpFactory.getproteinClass() ||
                 peCls == bpFactory.getrnaClass() ||
                 peCls == bpFactory.getdnaClass()) {
            // In cases these types are wrapped in PhysicalEntityParticipant. These
            // types actually should be wrapped in SequenceEntityParticipants.
            GKInstance ewasEntity = fileAdaptor.createNewInstance(ReactomeJavaConstants.EntityWithAccessionedSequence);
            bpToRInstancesMap.put(bpInstance, ewasEntity); 
        }
        else {
            GKInstance otherEntity = fileAdaptor.createNewInstance(ReactomeJavaConstants.OtherEntity);
            bpToRInstancesMap.put(bpInstance, otherEntity);
        }
    }
    
    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance gkInstance = bpToRInstanceMap.get(bpInstance);
        if (gkInstance == null)
            return;
        // Map CELLULAR-LOCATION property
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.CELLULAR_LOCATION);
        OWLIndividual bpLocation = (OWLIndividual) bpInstance.getPropertyValue(prop);
        if (bpLocation != null) {
            GKInstance gkLocation = bpToRInstanceMap.get(bpLocation);
            if (gkLocation == null) {
                // OpenControlledVocabulary used for location is handled by OpenControlledVocabulary mapper.
                // However, that mapper can be used to handle GO terms only. If other types of controled vocabulary
                // are used, they may not be handled.
                gkLocation = B2RMapperUtilities.createEntityCompartment(bpLocation, 
                                                                        bpFactory, 
                                                                        reactomeAdaptor); 
                bpToRInstanceMap.put(bpLocation, gkLocation);
            }
            
            // have to make sure gkLocation's type is EntityComponent. Otherwise, it cannot 
            // be used for PhysicalEntity's location
            if (!gkLocation.getSchemClass().isa(ReactomeJavaConstants.EntityCompartment)) {
                // Need to switch type
                GKSchemaClass entityComponentCls = 
                    (GKSchemaClass) reactomeAdaptor.getSchema().getClassByName(ReactomeJavaConstants.EntityCompartment);
                reactomeAdaptor.switchType(gkLocation, entityComponentCls);
            }
            gkInstance.setAttributeValue(ReactomeJavaConstants.compartment,
                                         gkLocation);
        }
        if (gkInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
            prop = bpFactory.getOWLProperty(BioPAXJavaConstants.PHYSICAL_ENTITY);
            OWLIndividual pe = (OWLIndividual) bpInstance.getPropertyValue(prop);
            if (pe != null) {
                GKInstance referenceEntity = bpToRInstanceMap.get(pe);
                SchemaAttribute referenceEntityAtt = gkInstance.getSchemClass().getAttribute(ReactomeJavaConstants.referenceEntity);
                if (referenceEntity != null && referenceEntityAtt.isValidValue(referenceEntity))
                    gkInstance.setAttributeValue(ReactomeJavaConstants.referenceEntity,
                                                 referenceEntity);
            }
        }
    }
}
