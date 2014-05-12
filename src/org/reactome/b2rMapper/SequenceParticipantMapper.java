/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

/**
 * BioPAX SequenceParticipant is mapped to Reactome EntityWithAccessionedSequence. 
 * @author guanming
 *
 */
public class SequenceParticipantMapper extends AbstractBioPAXToReactomeMapper {
    // This map is used to make sure only one GKInstance is created for the same key
    private Map<String, GKInstance> mappedInstance;
    // This set is used to make sure propeties filled only once for mapped GKInstance
    private Set<GKInstance> filledInstances;
    
    public SequenceParticipantMapper() {
        mappedInstance = new HashMap<String, GKInstance>();
        filledInstances = new HashSet<GKInstance>();
    }
    
    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory,
                         XMLFileAdaptor fileAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass cls = bpInstance.getRDFType();
        if (cls != bpFactory.getSequenceParticipantClass())
            return;
        String key = generateKey(bpInstance, bpFactory);
        GKInstance rInstance = mappedInstance.get(key);
        if (rInstance == null) {
            // There is a bug in INOH biopax export: smallMolecules are wrapped aroung sequenceParticipants.
            // The following code if/else checking is used to handle this bug.
            OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.PHYSICAL_ENTITY);
            OWLIndividual bpRef = (OWLIndividual) bpInstance.getPropertyValue(prop);
            if (bpRef.getRDFType() == bpFactory.getsmallMoleculeClass())
                rInstance = fileAdaptor.createNewInstance(ReactomeJavaConstants.SimpleEntity);
            else
                rInstance = fileAdaptor.createNewInstance(ReactomeJavaConstants.EntityWithAccessionedSequence);
            mappedInstance.put(key, rInstance);
        }
        bpToRInstancesMap.put(bpInstance, rInstance);
    }
    
    @Override
    public void mapProperties(OWLIndividual bpInstance,
                              BioPAXFactory bpFactory,
                              XMLFileAdaptor reactomeAdaptor,
                              Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        GKInstance gkInstance = bpToRInstancesMap.get(bpInstance);
        if (filledInstances.contains(gkInstance))
            return;
        super.mapProperties(bpInstance, 
                            bpFactory, 
                            reactomeAdaptor, 
                            bpToRInstancesMap);
        filledInstances.add(gkInstance);
    }

    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        // The wrapped DNA, RNA or Protein instance will be used as referenceEntity
        GKInstance gkInstance = bpToRInstanceMap.get(bpInstance);
        if (gkInstance == null)
            return;
        // This has been moved to PhysicalEntityParticipantMapper as of Jan 24, 2007.
//        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.PHYSICAL_ENTITY);
//        OWLIndividual pe = (OWLIndividual) bpInstance.getPropertyValue(prop);
//        if (pe != null) {
//            GKInstance referenceEntity = bpToRInstanceMap.get(pe);
//            SchemaAttribute referenceEntityAtt = gkInstance.getSchemClass().getAttribute(ReactomeJavaConstants.referenceEntity);
//            if (referenceEntity != null && referenceEntityAtt.isValidValue(referenceEntity))
//                gkInstance.setAttributeValue(ReactomeJavaConstants.referenceEntity,
//                                             referenceEntity);
//        }
        // Get SequenceFeature as modification
        mapSequenceFeatures(bpInstance, bpFactory, gkInstance, bpToRInstanceMap);
    }
    
    private void mapSequenceFeatures(OWLIndividual bpInstance,
                                     BioPAXFactory bpFactory,
                                     GKInstance gkInstance,
                                     Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.SEQUENCE_FEATURE_LIST);
        Collection sfList = bpInstance.getPropertyValues(prop);
        for (Iterator it = sfList.iterator(); it.hasNext();) {
            OWLIndividual sf = (OWLIndividual) it.next();
            GKInstance gkSF = bpToRInstanceMap.get(sf);
            gkInstance.addAttributeValue(ReactomeJavaConstants.hasModifiedResidue,
                                         gkSF);
        }
    }
    
    /**
     * A helper method to generate a unique String key based on the passed PhysicalEntity's
     * properties to consolidate converted SequenceParticipant instances.
     * @param bpIndividual
     * @param bpFactory
     * @return
     */
    private String generateKey(OWLIndividual bpIndividual,
                               BioPAXFactory bpFactory) {
        StringBuilder builder = new StringBuilder();
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.PHYSICAL_ENTITY);
        OWLIndividual pe = (OWLIndividual) bpIndividual.getPropertyValue(prop);
        builder.append(BioPAXJavaConstants.PHYSICAL_ENTITY);
        // In INOH biopax, more than one PhysicalEntity individuals are created for the same
        // unification. This should be consolidated. Use unificationXref in this case.
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.XREF);
        OWLIndividual bpxref = (OWLIndividual) pe.getPropertyValue(prop);
        if (bpxref != null && bpxref.getRDFType() == bpFactory.getunificationXrefClass()) 
            builder.append(":").append(bpxref.getLocalName()).append("\n");
        else
            builder.append(":").append(pe.getLocalName()).append("\n");
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.CELLULAR_LOCATION);
        OWLIndividual cl = (OWLIndividual) bpIndividual.getPropertyValue(prop);
        builder.append(BioPAXJavaConstants.CELLULAR_LOCATION).append(":");
        if (cl == null)
            builder.append("null");
        else {
            prop = bpFactory.getOWLProperty(BioPAXJavaConstants.TERM);
            String term = (String) cl.getPropertyValue(prop);
            if (term != null)
                builder.append(term);
            else {
                // Try XREF
                String xref = B2RMapperUtilities.grepDBAndIDFromXref(bpIndividual, bpFactory);
                builder.append(xref);
            }
        }
        builder.append("\n");
        builder.append(BioPAXJavaConstants.SEQUENCE_FEATURE_LIST).append(":");
        // Grep information from SequenceFeature
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.SEQUENCE_FEATURE_LIST);
        Collection sfList = bpIndividual.getPropertyValues(prop);
        if (sfList == null || sfList.size() == 0)
            builder.append("null");
        else {
            for (Iterator it = sfList.iterator(); it.hasNext();) {
                OWLIndividual sf = (OWLIndividual) it.next();
                String sfDisplayName = B2RMapperUtilities.generateSequenceFeatureDisplayName(sf, bpFactory);
                builder.append(sfDisplayName);
                if (it.hasNext())
                    builder.append(";");
            }
        }
        return builder.toString();
    }
    
}
