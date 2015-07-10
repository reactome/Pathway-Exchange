/*
 * Created on Apr 25, 2008
 *
 */
package org.reactome.biopax;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.jdom.Element;

/**
 * This class is used to handle EntityFeature for BioPAX Level 3 during converting Reactome
 * data into Level 3 data.
 * @author wgm
 *
 */
public class BioPAX3FeatureHandler {
    // The owner Object. This class is used as a helper class.
    // So there is a circular reference to its container class
    private ReactomeToBioPAX3XMLConverter converter;
    private Map<GKInstance, Element> modificationToType;
    private Map<GKInstance, Element> domainToFeature;
    private Map<GKInstance, Element> modifiedResidueToFeature;
    // Used to map resiude to regionType
    private Map<GKInstance, Element> residueToRegionType;
    
    public BioPAX3FeatureHandler(ReactomeToBioPAX3XMLConverter converter) {
        this.converter = converter;
    }
    
    protected void reset() {
        if (modificationToType == null)
            modificationToType = new HashMap<GKInstance, Element>();
        else
            modificationToType.clear();
        if (domainToFeature == null)
            domainToFeature = new HashMap<GKInstance, Element>();
        else
            domainToFeature.clear();
        if (residueToRegionType == null)
            residueToRegionType = new HashMap<GKInstance, Element>();
        else
            residueToRegionType.clear();
        if (modifiedResidueToFeature == null)
            modifiedResidueToFeature = new HashMap<GKInstance, Element>();
        else
            modifiedResidueToFeature.clear();
    }
    
    protected void handleStartAndEndCoordinates(GKInstance rEntity,
                                                Element bpEntity,
                                                String featurePropName) throws Exception {
        if (!rEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.startCoordinate) ||
            !rEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.endCoordinate))
            return; // No need
        Integer start = (Integer) rEntity.getAttributeValue(ReactomeJavaConstants.startCoordinate);
        if (start == null)
            start = 1; // default value
        Integer end = (Integer) rEntity.getAttributeValue(ReactomeJavaConstants.endCoordinate);
        if (end == null)
            end = -1; // default value is -1
        // If both start and end are default values, no need to create a SequenceFeature
        if (start == 1 && end == -1)
            return;
        // Create a sequenceFeature with SequenceInterval for fragment information
        Element lengthFeature = createSeqFeatureForFragment(start, end);
        converter.createObjectPropElm(bpEntity,
                                      featurePropName,
                                      lengthFeature);
    }
    
    protected void handleModifiedResidue(GKInstance rEntity, 
                                         Element bpEntity,
                                         String featurePropName) throws Exception {
        // Special case for EntitySet
        if (rEntity.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
            handleModificationForEntitySet(rEntity, 
                                           bpEntity, 
                                           featurePropName);
        if (!rEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasModifiedResidue))
            return; // No need
        List modifications = rEntity.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        if (modifications == null || modifications.size() == 0)
            return;
        GKInstance modification = null;
        Element seqFeatureElm = null;
        for (Iterator it = modifications.iterator(); it.hasNext();) {
            modification = (GKInstance) it.next();
            seqFeatureElm = createEntityFeatureForModification(modification);
            converter.createObjectPropElm(bpEntity,
                                          featurePropName,
                                          seqFeatureElm);
        }
    }
    
    private void handleModificationForEntitySet(GKInstance entitySet,
                                                Element bpEntity,
                                                String featurePropName) throws Exception {
        
    }
    
    
    private Element createSeqFeatureForFragment(int start,
                                                int end) throws Exception {
        Element seqFeatureElm = converter.createIndividualElm(BioPAX3JavaConstants.FragmentFeature);
        // There is no featureType property in level 3 any more.
        // Get the coordinates
        Element bpSeqInterval = createSequenceInterval(start, end);
        converter.createObjectPropElm(seqFeatureElm,
                                      BioPAX3JavaConstants.featureLocation,
                                      bpSeqInterval);
        return seqFeatureElm;
    }
    
    private Element createSequenceInterval(Integer start,
                                           Integer end) {
        Element bpSeqInterval = converter.createIndividualElm(BioPAX3JavaConstants.SequenceInterval);
        if (start!= null) {
            Element startSite = createSequenceSite(start);
            converter.createObjectPropElm(bpSeqInterval, 
                                          BioPAX3JavaConstants.sequenceIntervalBegin, 
                                          startSite);
        }
        if (end != null) {
            Element endSite = createSequenceSite(end);
            converter.createObjectPropElm(bpSeqInterval,
                                          BioPAX3JavaConstants.sequenceIntervalEnd,
                                          endSite);
        }
        return bpSeqInterval;
    }
    
    private Element createSequenceSite(Integer position) {
        Element seqSiteElm = converter.createIndividualElm(BioPAX3JavaConstants.SequenceSite);
        if (position != null) {
            converter.createDataPropElm(seqSiteElm, 
                                        BioPAX3JavaConstants.sequencePosition, 
                                        BioPAX3JavaConstants.XSD_INT,
                                        position.toString());
            // Required
            converter.createDataPropElm(seqSiteElm, 
                                        BioPAX3JavaConstants.positionStatus, 
                                        BioPAX3JavaConstants.XSD_STRING,
                                        "EQUAL");
        }
        return seqSiteElm;
    }
    
    private Element createEntityFeatureForModification(GKInstance modifier) throws Exception {
        Element bpEntityFeature = modifiedResidueToFeature.get(modifier);
        if (bpEntityFeature != null)
            return bpEntityFeature;
        // All SeqFeature instances cannot be reused.
        bpEntityFeature = converter.createIndividualElm(BioPAX3JavaConstants.ModificationFeature);
        // Get the coordinate
        if (modifier.getSchemClass().isValidAttribute(ReactomeJavaConstants.coordinate)) {
            Integer position = (Integer) modifier.getAttributeValue(ReactomeJavaConstants.coordinate);
            if (position != null) {
                // Need to construct a seqfeatureSite
                Element seqSiteElm = createSequenceSite(position);
                // Attach SequenceSite
                converter.createObjectPropElm(bpEntityFeature,
                                              BioPAX3JavaConstants.featureLocation,
                                              seqSiteElm);
            }
        }
        // As of Nov 10, 2009, since new PSI-MOD has been used, values in psiMod for ModifiedResidue
        // will be used as Feature_Type via openControlledVocabulary
        if (modifier.getSchemClass().isValidAttribute(ReactomeJavaConstants.psiMod)) {
            GKInstance psiMod = (GKInstance) modifier.getAttributeValue(ReactomeJavaConstants.psiMod);
            if (psiMod != null) {
                Element featureTypeElm = createModificationTypeFromPsiMod(psiMod);
                converter.createObjectPropElm(bpEntityFeature,
                                              BioPAX3JavaConstants.modificationType,
                                              featureTypeElm);
                // values in ChEBI from modification will be used as values for XREF
                // There is no xref in the ModificationEntityFeature in level 3. Just discard information 
                // for ChEBI
            }
            else if (modifier.getSchemClass().isValidAttribute(ReactomeJavaConstants.modification)) {
                // However, since feature type is required, if psiMod has not specified, the original ChEBI will
                // be used for modification.
                // Want to map ReferenceGroup or ReferenceMolecule as openControlledVocabulary back
                // to ChEBI.
                GKInstance modification = (GKInstance) modifier.getAttributeValue(ReactomeJavaConstants.modification);
                Element modificationElm = createModificationType(modification);
                converter.createObjectPropElm(bpEntityFeature, 
                                              BioPAX3JavaConstants.modificationType, 
                                              modificationElm);
            }
        }
        // For FragmentModification
        if (modifier.getSchemClass().isa(ReactomeJavaConstants.FragmentModification)) {
            String name = modifier.getDisplayName();
            // Add name to comment
            converter.createDataPropElm(bpEntityFeature, 
                                        BioPAX3JavaConstants.comment, 
                                        BioPAX3JavaConstants.XSD_STRING,
                                        name);
        }
        // Add the display name.
        // Name is not an attribute any more for ModificationFeature.
//        String displayName = modifier.getDisplayName();
//        converter.createDataPropElm(bpEntityFeature, 
//                                    BioPAX3JavaConstants.name, 
//                                    BioPAX3JavaConstants.XSD_STRING,
//                                    displayName);
        modifiedResidueToFeature.put(modifier, bpEntityFeature);
        return bpEntityFeature;
    }
    
//    private Element createRegionType(GKInstance residue) throws Exception {
//        Element regionType = (Element) residueToRegionType.get(residue);
//        if (regionType != null)
//            return regionType;
//        regionType = createCVFromChEBI(residue, 
//                                       BioPAX3JavaConstants.SequenceRegionVocabulary);
//        residueToRegionType.put(residue,
//                                regionType);
//        return regionType;
//    }
    
    private Element createCVFromChEBI(GKInstance referenceMolecule,
                                      String cvName) throws Exception {
        // Use Reactome for id since ChEBI is used for unification
        Element bpCV = converter.createIndividualElm(cvName);
        // Use name for term
        String displayName = referenceMolecule.getDisplayName();
        // Do a very simple parsing to remove ChEBI in the display name
        int index = displayName.indexOf("[ChEBI");
        if (index > 0)
            displayName = displayName.substring(0, index).trim();
        converter.createDataPropElm(bpCV,
                                    BioPAX3JavaConstants.term, 
                                    BioPAX3JavaConstants.XSD_STRING, 
                                    displayName);
        // Modification may be a SimpleEntity
        Element xref = null;
        if (referenceMolecule.getSchemClass().isa(ReactomeJavaConstants.ReferenceEntity))
            xref = converter.createXREFFromRefEntity(referenceMolecule); // Use ChEBI for unification 
        else if (referenceMolecule.getSchemClass().isa(ReactomeJavaConstants.SimpleEntity)) {
            // Need to get referenceEntity
            GKInstance referenceEntity = (GKInstance) referenceMolecule.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (referenceEntity != null)
                xref = converter.createXREFFromRefEntity(referenceEntity);
        }
        if (xref != null)
            converter.createObjectPropElm(bpCV, 
                                          BioPAX3JavaConstants.xref, 
                                          xref);
        return bpCV;
    }
    
    private Element createCVFromPsiMod(GKInstance psiMod,
                                       String cvName) throws Exception {
        // Use Reactome for id since ChEBI is used for unification
        Element bpCV = converter.createIndividualElm(cvName);
        // Use name for term
        //String displayName = psiMod.getDisplayName();
        String name = (String) psiMod.getAttributeValue(ReactomeJavaConstants.name);
        if (name == null)
            name = psiMod.getDisplayName(); // It should not occur
        converter.createDataPropElm(bpCV,
                                    BioPAX3JavaConstants.term, 
                                    BioPAX3JavaConstants.XSD_STRING, 
                                    name);
        // Modification may be a SimpleEntity
        Element xref = converter.createXREFFromRefEntity(psiMod);
        if (xref != null)
            converter.createObjectPropElm(bpCV, 
                                          BioPAX3JavaConstants.xref, 
                                          xref);
        return bpCV;
    }
    
    private Element createModificationType(GKInstance modification) throws Exception {
        Element bpModificationType = (Element) modificationToType.get(modification);
        if (bpModificationType != null)
            return bpModificationType;
        // Use Reactome for id since ChEBI is used for unification
        bpModificationType = createCVFromChEBI(modification,
                                               BioPAX3JavaConstants.SequenceModificationVocabulary);
        modificationToType.put(modification,
                               bpModificationType);
        return bpModificationType;
    }
    
    private Element createModificationTypeFromPsiMod(GKInstance psiMod) throws Exception {
        Element bpModificationType = (Element) modificationToType.get(psiMod);
        if (bpModificationType != null)
            return bpModificationType;
        // Use Reactome for id since ChEBI is used for unification
        bpModificationType = createCVFromPsiMod(psiMod,
                                                BioPAX3JavaConstants.SequenceModificationVocabulary);
        modificationToType.put(psiMod,
                               bpModificationType);
        return bpModificationType;
    }
    
    protected void handleHasDomain(GKInstance rEntity, 
    		Element bpEntity,
    		String featurePropName) throws Exception {
    	if (!rEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasDomain))
    		return;
    	List hasDomains = rEntity.getAttributeValuesList(ReactomeJavaConstants.hasDomain);
    	if (hasDomains == null || hasDomains.size() == 0)
    		return;
    	GKInstance domain = null;
    	for (Iterator it = hasDomains.iterator(); it.hasNext();) {
    		domain = (GKInstance) it.next();
    		// The allowed type might be not a Domain
    		if (!domain.getSchemClass().isa(ReactomeJavaConstants.SequenceDomain))
    			continue;
    		// Only Domain can be converted to SequenceFeature instances in BioPAX
    		Element sequenceFeature = createEntityFeatureFromDomain(domain);
    		converter.createObjectPropElm(bpEntity, 
    				featurePropName, 
    				sequenceFeature);
    	}
    }
    
   /**
    * Only SequenceDomain is handled right now.
    * @param domain
    * @return
    * @throws Exception
    */
   private Element createEntityFeatureFromDomain(GKInstance domain) throws Exception {
       if (!domain.getSchemClass().isa("SequenceDomain"))
           throw new IllegalStateException("ReactomeToBioConverter.createSequenceFeatureFromDomain(): Only SequenceDomain can be handled.");
       Element bpEntityFeature = (Element) domainToFeature.get(domain);
       if (bpEntityFeature != null)
           return bpEntityFeature;
       // Need to get ID
       bpEntityFeature = converter.createIndividualElm(BioPAX3JavaConstants.EntityFeature);
       // as of Nov 18, 2009, there are no name and displayNme for EntityFeature.
//       converter.handleNames(domain, 
//                             bpEntityFeature);
       Integer start = (Integer) domain.getAttributeValue(ReactomeJavaConstants.startCoordinate);
       Integer end = (Integer) domain.getAttributeValue(ReactomeJavaConstants.endCoordinate);
       Element sequenceSite = null;
       if (start != null && end != null && start.equals(end)) {
           // SequenceSite will be used for featureLocation property.
           sequenceSite = createSequenceSite(start);
       }
       else {
           sequenceSite = createSequenceInterval(start, end);
       }
       converter.createObjectPropElm(bpEntityFeature,
                                     BioPAX3JavaConstants.featureLocation,
                                     sequenceSite);
       // Add the name to comments to avoid any error
       if (domain.getDisplayName() != null)
           converter.createDataPropElm(bpEntityFeature,
                                       BioPAX3JavaConstants.comment, 
                                       BioPAX3JavaConstants.XSD_STRING,
                                       domain.getDisplayName());
       domainToFeature.put(domain, 
                           bpEntityFeature);
       return bpEntityFeature;
   }
}
