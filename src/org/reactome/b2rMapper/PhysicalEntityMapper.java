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
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * Except Complex, all other PhysicalEntities (dna, rna, protein, smallMolecule) are handled
 * in this class. These PhysicalEntities are mapped to ReferenceSequence classes 
 * (ReferenceDNASequence, ReferenceRNASequence and ReferencePeptideSequence) and
 * ReferenceMolecule in Reactome. 
 * @author guanming
 *
 */
public class PhysicalEntityMapper extends AbstractBioPAXToReactomeMapper {
    // Used to record mapped BP physicalEntity. The same physical entity
    // can be used in several files.  
    private Set<GKInstance> mappedInstanceIds;
    // PhysicalEntity can be determined by its unificationXref. INOH actually
    // has duplication for physicalEntity individual definition. Use UnificationXref
    // to control duplication.
    private Map<OWLIndividual, GKInstance> uniXref2GKInstance; 
    
    public PhysicalEntityMapper() {
        mappedInstanceIds = new HashSet<GKInstance>();
        uniXref2GKInstance = new HashMap<OWLIndividual, GKInstance>();
    }
    
    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory,
                         XMLFileAdaptor fileAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        String bpID = bpInstance.getLocalName();
        // This checking should be done before the next checking.
        GKInstance rInstance = B2RMapperUtilities.fetchGKInstanceBasedOnBPID(bpID, 
                                                                             ReactomeJavaConstants.ReferenceEntity,
                                                                             fileAdaptor);
        if (rInstance != null) {
            bpToRInstancesMap.put(bpInstance, rInstance);
            return;
        }
        // PhysicalEntity can be determined by its unificationXref. INOH actually
        // has duplication for physicalEntity individual definition. Use UnificationXref
        // to control duplication.
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.XREF);
        OWLIndividual xref = getUnificationXref(bpInstance, bpFactory);
        if (xref != null) {
            rInstance = uniXref2GKInstance.get(xref);
            if (rInstance == null) {
                rInstance = createReferenceEntity(bpInstance, bpFactory, fileAdaptor);
                uniXref2GKInstance.put(xref, rInstance);
            }
        }
        else
            rInstance = createReferenceEntity(bpInstance, bpFactory, fileAdaptor);
        if (rInstance != null)
            bpToRInstancesMap.put(bpInstance, rInstance);
    } 
    
    private OWLIndividual getUnificationXref(OWLIndividual bpInstance,
                                             BioPAXFactory bpFactory) throws Exception {
        // PhysicalEntity can be determined by its unificationXref. INOH actually
        // has duplication for physicalEntity individual definition. Use UnificationXref
        // to control duplication.
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.XREF);
        Collection xrefs = bpInstance.getPropertyValues(prop);
        // There is an error in NCI Pathways, EnzymeConsortium is used as UnificationXref
        // for proteins. This should be excluded.
        if (xrefs == null || xrefs.size() == 0)
            return null;
        for (Iterator it = xrefs.iterator(); it.hasNext();) {
            OWLIndividual tmp = (OWLIndividual) it.next();
            if (tmp.getRDFType() != bpFactory.getunificationXrefClass())
                continue;
            if (tmp.getLocalName().startsWith("EnzymeConsortium"))
                continue;
            return tmp;
        }
        return null;
    }
    
    private GKInstance createReferenceEntity(OWLIndividual bpInstance,
                                             BioPAXFactory bpFactory,
                                             XMLFileAdaptor fileAdaptor) throws Exception {
        RDFSClass cls = bpInstance.getRDFType();
        GKInstance rInstance = null;
        if (cls == bpFactory.getproteinClass()) {
            String proteinClsName = getProteinClass(fileAdaptor);
            rInstance = fileAdaptor.createNewInstance(proteinClsName);
        }
        else if (cls == bpFactory.getrnaClass())
            rInstance = fileAdaptor.createNewInstance(ReactomeJavaConstants.ReferenceRNASequence);
        else if (cls == bpFactory.getdnaClass())
            rInstance = fileAdaptor.createNewInstance(ReactomeJavaConstants.ReferenceDNASequence);
        else if (cls == bpFactory.getsmallMoleculeClass())
            rInstance = fileAdaptor.createNewInstance(ReactomeJavaConstants.ReferenceMolecule);
        return rInstance;
    }
    
    private String getProteinClass(XMLFileAdaptor fileAdaptor) {
        if (fileAdaptor.getSchema().isValidClass(ReactomeJavaConstants.ReferencePeptideSequence))
            return ReactomeJavaConstants.ReferencePeptideSequence;
        //if (fileAdaptor.getSchema().isValidClass(ReactomeJavaConstants.ReferenceGeneProduct))
        return ReactomeJavaConstants.ReferenceGeneProduct;
    }
    
    /**
     * This is a template method. All concrete class should implement another method called
     * mapClassProperties(OWLIndividual, Map<OWLIndividual, GKInstance>).
     * @param bpFactory
     */
    public void mapProperties(OWLIndividual bpInstance, 
                              BioPAXFactory bpFactory,
                              XMLFileAdaptor reactomeAdaptor, 
                              Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        GKInstance gkInstance = bpToRInstancesMap.get(bpInstance);
        if (mappedInstanceIds.contains(gkInstance))
            return;
        super.mapProperties(bpInstance, bpFactory, reactomeAdaptor, bpToRInstancesMap);
        mappedInstanceIds.add(gkInstance);
    }
    
    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance gkInstance = bpToRInstanceMap.get(bpInstance);
        if (gkInstance == null)
            return;
        if (gkInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.species))
            B2RMapperUtilities.mapOrganismProperty(bpInstance, 
                                                   bpFactory,
                                                   gkInstance, 
                                                   bpToRInstanceMap);
    }

}
