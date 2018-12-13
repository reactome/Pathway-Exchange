/*
 * Created on Jul 6, 2005
 *
 */
package org.reactome.biopax;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaClass;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Test;


/**
 * This converter from Reactome to BioPAX level 3 uses JDOM to handle xml directly to speed up performance.
 * @author guanming
 */
@SuppressWarnings("unchecked")
public class ReactomeToBioPAX3XMLConverter {
    // The top level pathway or reaction to be converted
    private GKInstance topEvent;
    // A map from Reactome Instance to BioPAX Individuals
    private Map<GKInstance, Element> rToBInstanceMap;
    // A map from event to PathwayStep
    private Map eventToPathwayStepMap;
    // A map from reaction to catalysis 
    private Map rxtToControlMap;
    // A map from Event to Evidence. The Event instance
    // is used in inferredFrom slot in other Event instances
    private Map eventToEvidenceMap;
    // For XREF referring back to Reactome
    private Map idToXrefMap;
    // For stable id to xref map
    private Map stableIdToXrefMap;
    // To keep track ID to avoid duplication
//    private Set idSet;
    private BioPAXOWLIDGenerator idGenerator;
    // To track complex and GenericEntity mapping to BioPAX PhysicalEntiy so that
    // PhysicalEntity in BioPAX can be reused. This map is differnt from rToBInstanceMap.
    // Instances in rToBInstanceMap are direct mappings.
    private Map rEntityToBEntityMap;
    // To control ReferenceEntity to Xref
    private Map refEntityToXrefMap;
    // Used for relationShip control vocabulary
    private Map<String, Element> relTypeNameToCV;
    // Generated BioPAX document
    private Document biopaxDoc;
    // The root element
    private Element rootElm;
    // Reused NameSpaces
    private Namespace bpNS = BioPAX3JavaConstants.bpNS;
    private Namespace rdfNS = BioPAX3JavaConstants.rdfNS;
    private Namespace rdfsNS = BioPAX3JavaConstants.rdfsNS;
    private Namespace owlNS = BioPAX3JavaConstants.owlNS;
    private Namespace xsdNS = BioPAX3JavaConstants.xsdNS;
    private Namespace reactomeNS = BioPAX3JavaConstants.reactomeNS;
    // To be used as Datasource property for entity instances
    private Element reactomeDatasource;
    // A helper class to handle EntityFeature
    private BioPAX3FeatureHandler featureHandler;
    // For current release database name
    private String currentDbName;
    
    public ReactomeToBioPAX3XMLConverter() {
        featureHandler = new BioPAX3FeatureHandler(this);
    }
    
    public Map getRToBInstanceMap() {
        return this.rToBInstanceMap;
    }
    
    /**
     * The uri should be imported by some explicitly way.
     * @param uri
     * @throws Exception
     */
    private void initBioPAX(GKInstance topEvent) throws Exception {
        biopaxDoc = new Document();
        // Set up necessary name space
        // Default name space is for BioPAX
        rootElm = new Element("RDF", rdfNS);
        rootElm.addNamespaceDeclaration(bpNS);
        rootElm.addNamespaceDeclaration(rdfNS);
        rootElm.addNamespaceDeclaration(rdfsNS);
        rootElm.addNamespaceDeclaration(owlNS);
        rootElm.addNamespaceDeclaration(xsdNS);
        Namespace defaultNS = null;
        String defaultNSText = null;
        // Handle Reactome namespace
        // As suggested by Igor, we want to attach release number into the name space to avoid
        // URI conflicts
        if (topEvent == null) {
            if (idGenerator.getSpecies() != null) {
                Integer releaseNumber = ReactomeToBioPAXUtilities.getCurrentReleaseNumber(idGenerator.getSpecies());
                defaultNSText = BioPAXJavaConstants.REACTOME_NS + "/" + 
                                (releaseNumber != null ? (releaseNumber + "/") : "") +  
                                idGenerator.getSpecies().getDBID();
                defaultNS = Namespace.getNamespace(defaultNSText + "#");
            }
            else {
                defaultNS = reactomeNS;
                defaultNSText = BioPAXJavaConstants.REACTOME_NS;
            }
//            rootElm.addNamespaceDeclaration(reactomeNS);
//            // Use BioPAX as base
//            rootElm.setAttribute(new Attribute("base", BioPAXJavaConstants.REACTOME_NS, Namespace.XML_NAMESPACE));
        }
        else {
            Integer releaseNumber = ReactomeToBioPAXUtilities.getCurrentReleaseNumber(topEvent);
            defaultNSText = BioPAXJavaConstants.REACTOME_NS + "/" + 
                            (releaseNumber != null ? (releaseNumber + "/") : "") +  
                            topEvent.getDBID();
            // Get a namespace for this event
            defaultNS = Namespace.getNamespace(defaultNSText + "#");
        }
//        rootElm.addNamespaceDeclaration(defaultNS);
        rootElm.setAttribute(new Attribute("base", 
                                           defaultNSText.endsWith("#") ? defaultNSText : defaultNSText + "#", 
                                           Namespace.XML_NAMESPACE));
        biopaxDoc.setRootElement(rootElm);
        Element ontElm = new Element("Ontology", owlNS);
        ontElm.setAttribute(new Attribute("about", "", rdfNS));
        rootElm.addContent(ontElm);
        // Add imports for Biopax Ontology
        Element importElm = new Element("imports", owlNS);
        importElm.setAttribute(new Attribute("resource", 
                                             BioPAX3JavaConstants.BIOPAX_DOWNLOAD_URI, 
                                             rdfNS));
        ontElm.addContent(importElm);
        // Add a description
        Element commentElm = new Element("comment", rdfsNS);
        commentElm.setAttribute("datatype", BioPAX3JavaConstants.XSD_STRING, rdfNS);
        if (topEvent != null) {
        		String displayName = topEvent.getDisplayName();
        		commentElm.setText("BioPAX pathway converted from \"" + displayName + "\" in the Reactome database.");
        		ontElm.addContent(commentElm);
        }
    }
    
    private void initMap() {
        if (rToBInstanceMap == null)
            rToBInstanceMap = new HashMap();
        else
            rToBInstanceMap.clear();
        if (eventToEvidenceMap == null)
            eventToPathwayStepMap = new HashMap();
        else
            eventToPathwayStepMap.clear();
        if (rxtToControlMap == null)
            rxtToControlMap = new HashMap();
        else
            rxtToControlMap.clear();
        if (eventToEvidenceMap == null)
            eventToEvidenceMap = new HashMap();
        else
            eventToEvidenceMap.clear();
        if (idToXrefMap == null)
            idToXrefMap = new HashMap();
        else
            idToXrefMap.clear();
        if (stableIdToXrefMap == null)
            stableIdToXrefMap = new HashMap();
        else
            stableIdToXrefMap.clear();
        if (rEntityToBEntityMap == null)
            rEntityToBEntityMap = new HashMap();
        else
            rEntityToBEntityMap.clear();
        if (refEntityToXrefMap == null)
            refEntityToXrefMap = new HashMap();
        else
            refEntityToXrefMap.clear();
        if (relTypeNameToCV == null)
            relTypeNameToCV = new HashMap<String, Element>();
        else
            relTypeNameToCV.clear();
        if (idGenerator == null)
            idGenerator = new BioPAXOWLIDGenerator();
        featureHandler.reset();
    }
    
    public void setIDGenerator(BioPAXOWLIDGenerator generator) {
        this.idGenerator = generator;
    }
    
    /**
     * The Event instance to be converted to BioPAX model.
     * @param event the Event GKInstance to be converted.
     */
    public void setReactomeEvent(GKInstance event) {
        this.topEvent = event;
    }
    
    /**
     * Actual converting method. After calling this method, the client should call
     * getBioPAXModel() method to get the converted result.
     * @throws Exception
     */
    public void convert() throws Exception {
        if (topEvent == null)
            throw new IllegalStateException("ReactomeToBioPAXConverter.convert(): Reactome event to be converted is not specified.");
        initMap();
        initBioPAX(topEvent);
        handleEvent(topEvent);
        // Do another rond to figure out NEXT-STEP in PATHWAY-STEPS
        handlePathwaySteps();
    }
    
    public void convert(Collection<GKInstance> events) throws Exception {
        initMap();	
        initBioPAX(null);
        for (Iterator<GKInstance> ei = events.iterator(); ei.hasNext();) {
            GKInstance e = ei.next();
            handleEvent(e);
        }
        handlePathwaySteps();
    }
    
    /**
     * This method is used to handle Regulation that works on CatalysActivity.
     * @param ca a Reactome CatalysActivity instance
     * @param bpCatalyst BioPAX catalyst individual converted from CatalystActivity
     * @param rEvent the Reactome Event ca working on.
     * @throws Exception
     */
    private void handleRegulationForCatalystActivity(GKInstance ca, 
                                                     Element bpCatalyst,
                                                     GKInstance rEvent) throws Exception {
        // This should not be supported in the new data model after release 63.
        Collection regulations = ca.getReferers(ReactomeJavaConstants.regulatedEntity);
        if (regulations == null || regulations.size() == 0)
            return;
        GKInstance regulation = null;
        Element modulation = null;
        for (Iterator it = regulations.iterator(); it.hasNext();) {
            regulation = (GKInstance) it.next();
            modulation = createModulationFromRegulation(regulation, BioPAX3JavaConstants.Modulation);
            createObjectPropElm(modulation, BioPAX3JavaConstants.controlled, bpCatalyst);
            List list = (List) rxtToControlMap.get(rEvent);
            if (list == null) {
                list = new ArrayList();
                rxtToControlMap.put(rEvent, list);
            }
            list.add(modulation);
            //if (rEvent.getDBID().equals(new Long(112118)))
            //    System.out.println();
        }
    }
    
    private void handleRegulation(GKInstance regulatedEntity,
                                  Element bpEvent) throws Exception {
        Collection<GKInstance> regulations = InstanceUtilities.getRegulations(regulatedEntity);
        if (regulations == null || regulations.size() == 0)
            return;
        GKInstance regulation = null;
        Element modulation = null;
        String controlType = null;
        for (Iterator it = regulations.iterator(); it.hasNext();) {
            regulation = (GKInstance) it.next();
            if (bpEvent.getName().equals(BioPAX3JavaConstants.TemplateReaction))
                controlType = BioPAX3JavaConstants.TemplateReactionRegulation;
            else
                controlType = BioPAX3JavaConstants.Control; // default type
            modulation = createModulationFromRegulation(regulation, controlType);
            createObjectPropElm(modulation, 
                                BioPAX3JavaConstants.controlled,
                                bpEvent);
            List list = (List) rxtToControlMap.get(regulatedEntity);
            if (list == null) {
                list = new ArrayList();
                rxtToControlMap.put(regulatedEntity, list);
            }
            list.add(modulation);
        }
    }
    
    /**
     * Handle reverse attribute for regulations.
     * @param regulation
     * @throws Exception
     */
    private Element createModulationFromRegulation(GKInstance regulation,
                                                   String controlType) throws Exception {
        // In BioPAX, a Modulation should not be re-used
//        Element modulation = rToBInstanceMap.get(regulation);
//        if (modulation != null)
//            return modulation;
        Element modulation = createIndividualElm(controlType);
//        handleNames(regulation, modulation);
        String type = getControlTypeFromRegulation(regulation);
        if (type != null) {
            createDataPropElm(modulation, 
                     		    BioPAX3JavaConstants.controlType,
                     		    BioPAX3JavaConstants.XSD_STRING,
                     		    type);
        }
        attachReactomeIDAsXref(regulation, modulation);
        attachReactomeDatasource(modulation);
        // Need to handle summation
        handleEventSummation(regulation, modulation);
        // Need to handle literatureReference
        handleEventLiteratureReferences(regulation, modulation);
        // Need to handle regulator
        GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
        if (regulator != null && regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity)) {
            Element entityParticipant = createPhysicalEntity(regulator);
            if (entityParticipant != null) {
                createObjectPropElm(modulation, BioPAX3JavaConstants.controller, entityParticipant);
            }
        }
        return modulation;
    }
    
    /**
     * A helper method to get a ControlType based on regulationType or Class type from a Reactome
     * Regulation instance. There is a simple hard coded mapping between RegulationType and Control-Type.
     * However, if such mapping is not existed, generic Activation or Inhibition will be used based on
     * SchemaClass type used for Regulation instance.
     * @param regulation
     * @return
     * @throws Exception  
     */
    private String getControlTypeFromRegulation(GKInstance regulation) throws Exception {
        if (regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation))
            return "ACTIVATION";
        else if (regulation.getSchemClass().isa(ReactomeJavaConstants.NegativeRegulation))
            return "INHIBITION";
        else
            return null;
    }
    
    private void handlePathwaySteps() throws Exception {
        GKInstance event = null;
        Element pathwayStep = null;
        List precedingEvents = null;
        GKInstance precedingEvent = null;
        Element prevPathwayStep = null;
        for (Iterator it = eventToPathwayStepMap.keySet().iterator(); it.hasNext();) {
            event = (GKInstance) it.next();
            precedingEvents = event.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
            if (precedingEvents == null || precedingEvents.size() == 0)
                continue;
            // Handle next step
            pathwayStep = (Element) eventToPathwayStepMap.get(event);
            for (Iterator it1 = precedingEvents.iterator(); it1.hasNext();) {
                precedingEvent = (GKInstance) it1.next();
                prevPathwayStep = (Element) eventToPathwayStepMap.get(precedingEvent);
                if (prevPathwayStep != null)
                    createObjectPropElm(prevPathwayStep, BioPAX3JavaConstants.nextStep, pathwayStep);
            }
        }
        List list = null;
        for (Iterator it = rxtToControlMap.keySet().iterator(); it.hasNext();) {
            event = (GKInstance) it.next();
            pathwayStep = (Element) eventToPathwayStepMap.get(event);
            if (pathwayStep == null) // A reaction might not be contained by a PathwayStep.
                continue;            // E.g. some orphan reactions.
            list = (List) rxtToControlMap.get(event);
            createObjectPropElm(pathwayStep, BioPAX3JavaConstants.stepProcess, list);
        }
    }
    
    private void handleEvent(GKInstance event) throws Exception {
        boolean isNew = false;
        if (event.getSchemClass().isa(ReactomeJavaConstants.Pathway))
            isNew = handlePathway(event);
        // All subclasses to ReactionlikeEvent can be handled by handleReaction() method
        // hasEvent in BlackboxEvent will not be exported for the time being.
        else if (event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
            isNew = handleReaction(event);
        // For backward schema compatibility
        else if (event.getSchemClass().isa(ReactomeJavaConstants.Reaction))
            isNew = handleReaction(event); 
        else if (event.getSchemClass().isa(ReactomeJavaConstants.ConceptualEvent))
            isNew = handleEventSet(event);
        else if (event.getSchemClass().isa(ReactomeJavaConstants.EquivalentEventSet))
            isNew = handleEventSet(event);
        if (!isNew)
            return; // This event has been handled before. No need to create a new copy
        Element bpEvent = (Element) rToBInstanceMap.get(event);
        if (bpEvent == null)
            return;
        handleNames(event, bpEvent);
        attachReactomeIDAsXref(event, bpEvent);
        handleEventSummation(event, bpEvent);
        handleEventLiteratureReferences(event, bpEvent);
        handleEventGOBP(event, bpEvent);
        // There is no compartment attributes for event
        //handleCompartment(event, bpEvent);
        handleInferredFrom(event, bpEvent);
        handleRegulation(event, bpEvent);
        attachReactomeDatasource(bpEvent);
        handleEventAuthorship(event, bpEvent, ReactomeJavaConstants.authored);
        handleEventAuthorship(event, bpEvent, ReactomeJavaConstants.reviewed);
        handleEventAuthorship(event, bpEvent, ReactomeJavaConstants.edited);
    }
    
    private void handleEventAuthorship(GKInstance event,
                                       Element bpEvent,
                                       String attName) throws Exception {
        // reviewed is a multiple values attributes
        List reviewed = event.getAttributeValuesList(attName);
        if (reviewed == null || reviewed.size() == 0)
            return;
        // upcase the first letter
        String title = attName.substring(0, 1).toUpperCase() + attName.substring(1);
        for (Iterator it = reviewed.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            String displayName = inst.getDisplayName();
            if (displayName == null || displayName.length() == 0)
                continue;
            String comment = title + ": " + displayName;
            createDataPropElm(bpEvent,
                              BioPAX3JavaConstants.comment,
                              BioPAX3JavaConstants.XSD_STRING,
                              comment);
        }
    }
    
    private void attachReactomeDatasource(Element entityElm) {
        if (reactomeDatasource == null) {
            reactomeDatasource = createIndividualElm(BioPAX3JavaConstants.Provenance);
            createDataPropElm(reactomeDatasource,
                              BioPAX3JavaConstants.name,
                              BioPAX3JavaConstants.XSD_STRING,
                              "Reactome");
            createDataPropElm(reactomeDatasource,
                              BioPAX3JavaConstants.comment,
                              BioPAX3JavaConstants.XSD_STRING,
                              "http://www.reactome.org");
        }
        createObjectPropElm(entityElm, 
                            BioPAX3JavaConstants.dataSource,
                            reactomeDatasource);
    }
    
    private boolean handleEventSet(GKInstance event) throws Exception { 
        List hasInstances = null;
        if (event.getSchemClass().isa(ReactomeJavaConstants.ConceptualEvent)) {
            // Need to determine if the specified event is a Pathway or Reaction
            hasInstances = event.getAttributeValuesList(ReactomeJavaConstants.hasSpecialisedForm);
        }
        else if (event.getSchemClass().isa(ReactomeJavaConstants.EquivalentEventSet)) {
            hasInstances = event.getAttributeValuesList(ReactomeJavaConstants.hasMember);
        }
        SchemaClass type = determineEventTypeFromInstances(hasInstances);
        if (type == null)
            return false; // Cannot determine the type. Nothing to converted
        if (type.isa(ReactomeJavaConstants.Reaction))
            return handleReaction(event);
        else if (type.isa(ReactomeJavaConstants.Pathway))
            return handlePathway(event);
        return false;
    }
    
    private SchemaClass determineEventTypeFromInstances(List instances) throws Exception {
        if (instances == null || instances.size() == 0)
            return null;
        GKInstance instance = null;
        for (Iterator it = instances.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            if (instance.getSchemClass().isa(ReactomeJavaConstants.Reaction))
                return instance.getSchemClass();
            else if (instance.getSchemClass().isa(ReactomeJavaConstants.Pathway))
                return instance.getSchemClass();
            else if (instance.getSchemClass().isa(ReactomeJavaConstants.ConceptualEvent)) {
                List instances1 = instance.getAttributeValuesList(ReactomeJavaConstants.hasSpecialisedForm);
                return determineEventTypeFromInstances(instances1);
            }
            else if (instance.getSchemClass().isa(ReactomeJavaConstants.EquivalentEventSet)) {
                List instances1 = instance.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                return determineEventTypeFromInstances(instances1);
            }
            else
                return null;
        }
        return null;
    }
    
    private void handleInferredFrom(GKInstance event, Element bpEvent) throws Exception {
        List ifInstances = event.getAttributeValuesList(ReactomeJavaConstants.inferredFrom);
        if (ifInstances == null || ifInstances.size() == 0)
            return;
        GKInstance evidenceType = (GKInstance) event.getAttributeValue(ReactomeJavaConstants.evidenceType);
        if (evidenceType == null)
            return ; // As of BioPAX ver0.92, all evidence instances need at least one of
                     // CONFIDENCE, EVIDENCE-CODE, or EXPERIMENTAL-FORM
        GKInstance ifInstance = null;
        Element bpEvidence = null;
        for (Iterator it = ifInstances.iterator(); it.hasNext();) {
            ifInstance = (GKInstance) it.next();
            bpEvidence = createEvidenceFromInferredFrom(ifInstance, evidenceType);
            createObjectPropElm(bpEvent, BioPAX3JavaConstants.evidence, bpEvidence);
        }
    }
    
    private Element createEvidenceFromInferredFrom(GKInstance ifInstance, 
                                                   GKInstance evidenceType) throws Exception {
        Element bpEvidence = createIndividualElm(BioPAX3JavaConstants.Evidence);
        eventToEvidenceMap.put(ifInstance, bpEvidence);
        // Check if there is Summation in ifInstance
        GKInstance summation = (GKInstance) ifInstance.getAttributeValue(ReactomeJavaConstants.summation);
        // Get all pubMed LiteratureReference
        Set literatureReferences = new HashSet();
        List list = ifInstance.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
        if (list != null)
            literatureReferences.addAll(list);
        if (summation != null) {
            list = summation.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
            if (list != null)
                literatureReferences.addAll(list);
        }
        // Convert LiteratureReference to publicationXref
        if (literatureReferences.size() > 0) {
            Element bpXref = null;
            GKInstance literatureReference = null;
            for (Iterator it = literatureReferences.iterator(); it.hasNext();) {
                literatureReference = (GKInstance) it.next();
                bpXref = convertLiteratureReferenceToPublicationXref(literatureReference);
                if (bpXref != null)
                    createObjectPropElm(bpEvidence, BioPAX3JavaConstants.xref, bpXref);
            }
        }
        // Add comments to evidence
        if (summation != null) {
            String text = (String) summation.getAttributeValue(ReactomeJavaConstants.text);
            if (text != null)
                createDataPropElm(bpEvidence, BioPAX3JavaConstants.comment,
                                  BioPAX3JavaConstants.XSD_STRING, text);
        }
        // Need to add EVIDENCE_CODE
        Element evidenceCode = convertEvidenceTypeToCode(evidenceType);
        if (evidenceCode != null)
            createObjectPropElm(bpEvidence,
                                BioPAX3JavaConstants.evidenceCode,
                                evidenceCode);
        return bpEvidence;
    }
    
    private Element convertEvidenceTypeToCode(GKInstance evidenceType) throws Exception {
        Element evidence = (Element) rToBInstanceMap.get(evidenceType);
        if (evidence != null)
            return evidence;
        evidence = createIndividualElm(BioPAX3JavaConstants.EvidenceCodeVocabulary);
        rToBInstanceMap.put(evidenceType,
                            evidence);
        String termId = evidenceType.getDisplayName();
        if (termId.equalsIgnoreCase("inferred by electronic annotation"))
            termId = "inferred from electronic annotation"; // Use the official name
        createDataPropElm(evidence, 
                          BioPAX3JavaConstants.term, 
                          BioPAX3JavaConstants.XSD_STRING, 
                          termId);
        Element goEvidenceXref = createIndividualElm(BioPAX3JavaConstants.UnificationXref);
        
        String dbName = "GO";
        if (termId.equals("inferred from electronic annotation")) {
            termId = "ECO:0000203";
            dbName = "EVIDENCE CODE";
        }
        createDataPropElm(goEvidenceXref, 
                          BioPAX3JavaConstants.db,
                          BioPAX3JavaConstants.XSD_STRING,
                          dbName);
        createDataPropElm(goEvidenceXref,
                          BioPAX3JavaConstants.id,
                          BioPAX3JavaConstants.XSD_STRING,
                          termId);
        // Get the id from evidenceType
//        List names = evidenceType.getAttributeValuesList(ReactomeJavaConstants.name);
//        if (names != null && names.size() > 0) {
//            // Get the shortest one
//            String shortest = (String) names.get(0);
//            for (int i = 1; i < names.size(); i++) {
//                String tmp = (String) names.get(i);
//                if (tmp.length() < shortest.length())
//                    shortest = tmp;
//            }
//            createDataPropElm(goEvidenceXref,
//                              BioPAX3JavaConstants.id,
//                              BioPAX3JavaConstants.XSD_STRING,
//                              termId);
//            // Add other names as comments
//            String displayName = evidenceType.getDisplayName();
//            for (Iterator it = names.iterator(); it.hasNext();) {
//                String tmp = (String) it.next();
//                if (tmp.equals(displayName))
//                    continue;
//                // Push all names except displayName into comment attributes
//                createDataPropElm(evidence,
//                                  BioPAX3JavaConstants.comment,
//                                  BioPAX3JavaConstants.XSD_STRING,
//                                  tmp);
//            }
//        }
        createObjectPropElm(evidence, BioPAX3JavaConstants.xref, goEvidenceXref);
        return evidence;
    }
    
    private void handleEventGOBP(GKInstance event, Element bpEvent) throws Exception {
        GKInstance goBP = (GKInstance) event.getAttributeValue(ReactomeJavaConstants.goBiologicalProcess);
        if (goBP == null)
            return;
        Element goBPOWL = createGOOWL(goBP, 
                                      BioPAX3JavaConstants.GO_BIOLOGICAL_PROCESS);
        if (goBPOWL != null) {
            createObjectPropElm(bpEvent, BioPAX3JavaConstants.xref, goBPOWL);
        }
    }
    
    private Element createGOOWL(GKInstance goInstance, String relationshipType) throws Exception {
        Element goOWL = (Element) rToBInstanceMap.get(goInstance);
        if (goOWL != null)
            return goOWL;
        goOWL = createIndividualElm(BioPAX3JavaConstants.RelationshipXref);
        createDataPropElm(goOWL,
                          BioPAX3JavaConstants.db, 
                          BioPAX3JavaConstants.XSD_STRING, 
                          "GENE ONTOLOGY");
        String identifier = (String) goInstance.getAttributeValue(ReactomeJavaConstants.accession);
        if (identifier != null) {
            if (!identifier.startsWith("GO:"))
                identifier = "GO:" + identifier;
            createDataPropElm(goOWL, 
                              BioPAX3JavaConstants.id, 
                              BioPAX3JavaConstants.XSD_STRING, 
                              identifier);
        }
        Element cv = getRelationTypeCV(relationshipType);
        createObjectPropElm(goOWL, 
                            BioPAX3JavaConstants.relationshipType,
                            cv);
//        createDataPropElm(goOWL, 
//                          BioPAX3JavaConstants.relationshipType,
//                          BioPAX3JavaConstants.XSD_STRING,
//                          relationShipType);
        rToBInstanceMap.put(goInstance, goOWL);
        return goOWL;
    }
    
    private Element getRelationTypeCV(String relationshipType) {
        Element cv = relTypeNameToCV.get(relationshipType);
        if (cv != null)
            return cv;
        cv = createIndividualElm(BioPAX3JavaConstants.RelationshipTypeVocabulary);
        relTypeNameToCV.put(relationshipType, cv);
        // Just provide an name for the term
        createDataPropElm(cv, 
                          BioPAX3JavaConstants.term,
                          BioPAX3JavaConstants.XSD_STRING, 
                          relationshipType);
        String termId = null;
        if (relationshipType.equals(BioPAX3JavaConstants.GO_MOLECULLAR_FUNCTION)) 
            termId = "MI:0355";
        else if (relationshipType.equals(BioPAX3JavaConstants.GO_BIOLOGICAL_PROCESS))
            termId = "MI:0359";
        if (termId != null) {
            Element bpXref = createIndividualElm(BioPAX3JavaConstants.UnificationXref);
            createDataPropElm(bpXref,
                              BioPAX3JavaConstants.db,
                              BioPAX3JavaConstants.XSD_STRING, 
                              "MI");
            createDataPropElm(bpXref,
                              BioPAX3JavaConstants.id,
                              BioPAX3JavaConstants.XSD_STRING,
                              termId);
            createObjectPropElm(cv, 
                                BioPAX3JavaConstants.xref, 
                                bpXref);
        }
        return cv;
    }
    
    private void handleEventLiteratureReferences(GKInstance event, Element bpEvent) throws Exception {
        Set referencesSet = new HashSet();
        // LiteratureReference instances from summation in event are combined
        List references = event.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
        if (references != null)
            referencesSet.addAll(references);
        // Get LiteratureReference instances from summation
        List summations = event.getAttributeValuesList(ReactomeJavaConstants.summation);
        if (summations != null && summations.size() > 0) {
            for (Iterator it = summations.iterator(); it.hasNext();) {
                GKInstance summation = (GKInstance) it.next();
                List ref = summation.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
                if (ref != null)
                    referencesSet.addAll(ref);
            }
        }
        GKInstance reference = null;
        for (Iterator it = referencesSet.iterator(); it.hasNext();) {
            reference = (GKInstance) it.next();
            Element pubXref = convertLiteratureReferenceToPublicationXref(reference);
            if (pubXref != null)
                createObjectPropElm(bpEvent, BioPAX3JavaConstants.xref, pubXref);
        }
    }
    
    private boolean handlePathway(GKInstance pathway) throws Exception {
        Element bpPathway = rToBInstanceMap.get(pathway);
        if (bpPathway != null)
            return false;
        bpPathway = createIndividualElm(BioPAX3JavaConstants.Pathway);
        rToBInstanceMap.put(pathway, bpPathway);
        // hasComponent is used in the old schema, and hasEvent is used in latest schema
        if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent)) {
            List components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            handlePathwayComponents(components, bpPathway);
        }
        else if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
            List components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            handlePathwayComponents(components, bpPathway);
        }
        handleTaxon(pathway, bpPathway, ReactomeJavaConstants.species);
        return true;
    }
    
    private void handleTaxon(GKInstance rInstance, 
                             Element bpInstance, 
                             String taxonAttName) throws Exception {
        if (bpInstance.getName().equals(BioPAX3JavaConstants.PhysicalEntity) ||
            bpInstance.getName().equals(BioPAX3JavaConstants.SmallMolecule))
            return; // A physicalEntity instance cannot have ORGANISM value
        // A check in case the taxon name is not correct
        if (!rInstance.getSchemClass().isValidAttribute(taxonAttName))
            return;
        GKInstance taxon = (GKInstance) rInstance.getAttributeValue(taxonAttName);
        if (taxon == null)
            return;
        Element taxonElm = createTaxonBPIndividual(taxon);
        if (taxonElm != null) {
            createObjectPropElm(bpInstance, BioPAX3JavaConstants.organism, taxonElm);
        }
    }
    
    /**
     * Taxon ID will be returned.
     * @param taxon
     * @return
     * @throws Exception
     */
    private Element createTaxonBPIndividual(GKInstance taxon) throws Exception {
        Element bpTaxon = (Element) rToBInstanceMap.get(taxon);
        if (bpTaxon != null)
            return bpTaxon;
        bpTaxon = createIndividualElm(BioPAX3JavaConstants.BioSource);
        rToBInstanceMap.put(taxon, bpTaxon);
        // Extract name from taxon to bpTaxon
        createDataPropElm(bpTaxon, BioPAX3JavaConstants.name, BioPAX3JavaConstants.XSD_STRING, taxon.getDisplayName());
        // Extract crossReference to TAXON-XREF
        GKInstance crossRef = (GKInstance) taxon.getAttributeValue(ReactomeJavaConstants.crossReference);
        if (crossRef != null) {
            GKInstance db = (GKInstance) crossRef.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
            String identifier = (String) crossRef.getAttributeValue(ReactomeJavaConstants.identifier);
            Element bpXref = createIndividualElm(BioPAX3JavaConstants.UnificationXref);
            if (db != null) {
                createDataPropElm(bpXref, 
                                  BioPAX3JavaConstants.db, 
                                  BioPAX3JavaConstants.XSD_STRING,
                                  db.getDisplayName());
            }
            if (identifier != null) {
                createDataPropElm(bpXref,
                                  BioPAX3JavaConstants.id,
                                  BioPAX3JavaConstants.XSD_STRING,
                                  identifier);
            }
            createObjectPropElm(bpTaxon, BioPAX3JavaConstants.xref, bpXref);
        }
        return bpTaxon;
    }
    
    protected Element createIndividualElm(String elmName) {
        String id = idGenerator.generateOWLID(elmName);
        return ReactomeToBioPAXUtilities.createIndividualElm(elmName, id, bpNS, rdfNS, rootElm);
    }
    
    protected Element createObjectPropElm(Element domainElm,
                                          String propName,
                                          Element rangeElm) {
        String resourceID = rangeElm.getAttributeValue("ID", rdfNS);
        Element rtn = new Element(propName, bpNS);
        domainElm.addContent(rtn);
        rtn.setAttribute("resource", "#" + resourceID, rdfNS);
        return rtn;
    }
    
    private void createObjectPropElm(Element domainElm,
                                     String propName,
                                     Collection rangeElms) {
        Element value = null;
        for (Iterator it = rangeElms.iterator(); it.hasNext();) {
            value = (Element) it.next();
            createObjectPropElm(domainElm, propName, value);
        }
    }
    
    protected Element createDataPropElm(Element domainElm,
                                        String propName,
                                        String dataType,
                                        Object value) {
        return ReactomeToBioPAXUtilities.createDataPropElm(domainElm, 
                                                           propName, 
                                                           dataType, 
                                                           value, 
                                                           bpNS, 
                                                           rdfNS);
    }
    
    private void createDataPropElm(Element domainElm,
                                   String propName,
                                   String dataType,
                                   Collection values) {
        ReactomeToBioPAXUtilities.createDataPropElm(domainElm, 
                                                    propName, 
                                                    dataType,
                                                    values,
                                                    bpNS,
                                                    rdfNS);
    }
    
    private void handleEventSummation(GKInstance gkEvent, Element bpEvent) throws Exception {
        List summationInstances = gkEvent.getAttributeValuesList(ReactomeJavaConstants.summation);
        if (summationInstances == null || summationInstances.size() == 0)
            return;
        GKInstance summation = null;
        for (Iterator it = summationInstances.iterator(); it.hasNext();) {
            summation = (GKInstance) it.next();
            // Use the text in Summation as comment
            String text = (String) summation.getAttributeValue(ReactomeJavaConstants.text);
            if (text != null && text.trim().length() > 0) {
                createDataPropElm(bpEvent,
                                  BioPAX3JavaConstants.comment,
                                  BioPAX3JavaConstants.XSD_STRING,
                                  text);
            }
            // Create a new evidence based on LiteratureReference instances in Summation
            //Element bpEvidence = createEvidenceFromSummation(summation);
            //createObjectPropElm(bpEvent, BioPAX3JavaConstants.EVIDENCE, bpEvidence);
        }
    }
    
//    private Element createEvidenceFromSummation(GKInstance summation) throws Exception {
//        Element bpEvidence = (Element) rToBInstanceMap.get(summation);
//        if (bpEvidence != null)
//            return bpEvidence;
//        String id = "Summation_" + summation.getDBID();
//        id = generateOWLID(id);
//        bpEvidence = createIndividualElm(BioPAX3JavaConstants.evidence, id);
//        rToBInstanceMap.put(summation, bpEvidence);
//        // Use LiteratureReference as XREF with type pubXref for evidence
//        List literatures = summation.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
//        if (literatures != null && literatures.size() > 0) {
//            for (Iterator it1 = literatures.iterator(); it1.hasNext();) {
//                GKInstance reference = (GKInstance) it1.next();
//                Element pubXref = convertLiteratureReferenceToPublicationXref(reference);
//                if (pubXref != null) 
//                    createObjectPropElm(bpEvidence, BioPAX3JavaConstants.xref, pubXref);
//            }
//        }    
//        return bpEvidence;
//    }
    
    private Element convertLiteratureReferenceToPublicationXref(GKInstance literatureReference) throws Exception {
        Element pubXrefIndividual = (Element) rToBInstanceMap.get(literatureReference);
        if (pubXrefIndividual != null)
            return pubXrefIndividual;
        ReactomeToBioPAXPublicationConverter helper = new ReactomeToBioPAXPublicationConverter();
        helper.setIsForLevel2(false);
        pubXrefIndividual = helper.convertPublication(literatureReference, 
                                                      idGenerator,
                                                      null,
                                                      bpNS,
                                                      rdfNS,
                                                      rootElm);
        rToBInstanceMap.put(literatureReference, pubXrefIndividual);
        return pubXrefIndividual;
    }
    
    private void attachReactomeIDAsXref(GKInstance gkInstance, 
                                        Element bpInstance) throws Exception{
        attachReactomeIDAsXref(gkInstance,
                               bpInstance,
                               BioPAX3JavaConstants.UnificationXref,
                               null);
    }
    
    private void attachReactomeIDAsXref(GKInstance gkInstance, 
                                        Element bpInstance,
                                        String xrefType,
                                        Element relationshipType) throws Exception{
        Long DBID = gkInstance.getDBID();
        Element xref = (Element) idToXrefMap.get(DBID);
        if (xref == null) {
            xref = createIndividualElm(xrefType);
            idToXrefMap.put(DBID, xref);
            if (currentDbName == null)
                currentDbName = ReactomeToBioPAXUtilities.getCurrentReleaseDbName(gkInstance);
            createDataPropElm(xref, 
                              BioPAX3JavaConstants.db, 
                              BioPAX3JavaConstants.XSD_STRING, 
                              currentDbName);
            createDataPropElm(xref, 
                              BioPAX3JavaConstants.id, 
                              BioPAX3JavaConstants.XSD_STRING, 
                              DBID.toString());
            createDataPropElm(xref, 
                              BioPAX3JavaConstants.comment,
                              BioPAX3JavaConstants.XSD_STRING,
                              getComment(DBID.toString()));
            if (relationshipType != null && xrefType.equals(BioPAX3JavaConstants.RelationshipXref)) {
                createObjectPropElm(xref, 
                                    BioPAX3JavaConstants.relationshipType,
                                    relationshipType);
            }
        }
        createObjectPropElm(bpInstance, BioPAX3JavaConstants.xref, xref);
        // These two can always stick together: DB_ID and stable id
        attachReactomeStableIDAsXref(gkInstance, bpInstance);
    }

    private void attachReactomeStableIDAsXref(GKInstance gkInstance,
                                              Element bpInstance) throws Exception {
        if (!gkInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.stableIdentifier))
            return;
        // One value only
        GKInstance stableIdentifier = (GKInstance) gkInstance.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
        if (stableIdentifier == null)
            return;
        String identifier = (String) stableIdentifier.getAttributeValue(ReactomeJavaConstants.identifier);
        if (identifier == null)
            return;
        String version = (String) stableIdentifier.getAttributeValue(ReactomeJavaConstants.identifierVersion);
        if (version == null)
            return;
        String key = identifier + "." + version;
        Element xref = (Element) stableIdToXrefMap.get(key);
        if (xref == null) {
            xref = createIndividualElm(BioPAX3JavaConstants.UnificationXref);
            stableIdToXrefMap.put(key, xref);
            createDataPropElm(xref, 
                              BioPAX3JavaConstants.db, 
                              BioPAX3JavaConstants.XSD_STRING, 
                              BioPAXJavaConstants.REACTOME_STABLE_ID);
            createDataPropElm(xref, BioPAX3JavaConstants.id, BioPAX3JavaConstants.XSD_STRING, identifier);
            createDataPropElm(xref,
                              BioPAX3JavaConstants.idVersion,
                              BioPAX3JavaConstants.XSD_STRING,
                              version);
            createDataPropElm(xref, BioPAX3JavaConstants.comment, BioPAX3JavaConstants.XSD_STRING, getComment(key));
        }
        createObjectPropElm(bpInstance, BioPAX3JavaConstants.xref, xref);
    }

    private String getComment(String id) {
        return "Database identifier. Use this URL to connect to the web page of this " +
                "instance in Reactome: " + getReactomeInstanceURL(id);
    }
    
    private void handlePathwayComponents(List components, Element bpPathway) throws Exception {
        if (components == null || components.size() == 0)
            return;
        GKInstance comp = null;
        Set pathwaySteps = new HashSet();
        Set pathwayComponents = new HashSet();
        Set gkEvents = new HashSet();
        for (Iterator it = components.iterator(); it.hasNext();) {
            comp = (GKInstance) it.next();
            if (comp.getSchemClass().isa(ReactomeJavaConstants.EquivalentEventSet) ||
                comp.getSchemClass().isa(ReactomeJavaConstants.ConceptualEvent)) {
                // Get the deepest value in hasInstance slot to be added to pathway
                List instances = getInstancesFromEventSet(comp);
                for (Iterator it1 = instances.iterator(); it1.hasNext();) {
                    GKInstance tmp = (GKInstance) it1.next();
                    Element bpComp = (Element) rToBInstanceMap.get(tmp);
                    if (bpComp == null) // Use the following call to generate bpComp
                        handleEvent(tmp);
                    bpComp = (Element) rToBInstanceMap.get(tmp);
                    if (bpComp != null) { // It might be null
                        Element pathwayStep = createPathwayStep(tmp, bpComp);
                        pathwaySteps.add(pathwayStep);
                        // Level 3 needs pathwayComponent too
                        createObjectPropElm(bpPathway, 
                                            BioPAX3JavaConstants.pathwayComponent, 
                                            bpComp);
                    }
                }
            }
            else {
                Element bpComp = (Element) rToBInstanceMap.get(comp);
                if (bpComp == null)
                    handleEvent(comp);
                bpComp = (Element) rToBInstanceMap.get(comp);
                if (bpComp != null) {
                    Element pathwayStep = createPathwayStep(comp, bpComp);
                    pathwaySteps.add(pathwayStep);
                    // Level 3 needs pathwayComponent too
                    createObjectPropElm(bpPathway, 
                                        BioPAX3JavaConstants.pathwayComponent, 
                                        bpComp);                    
                }
            }
        }
        if (pathwaySteps != null) {
            createObjectPropElm(bpPathway, 
                                BioPAX3JavaConstants.pathwayOrder,
                                pathwaySteps);
        }
    }
    
    private Element createPathwayStep(GKInstance event, Element bpEvent) throws Exception {
        Element pathwayStep = (Element) eventToPathwayStepMap.get(event);
        if (pathwayStep != null) {
            return pathwayStep;
        }
        pathwayStep = createIndividualElm(BioPAX3JavaConstants.PathwayStep);
        createObjectPropElm(pathwayStep,
                            BioPAX3JavaConstants.stepProcess,
                            bpEvent);
        eventToPathwayStepMap.put(event, pathwayStep);
        return pathwayStep;
    }
    
    private List getInstancesFromEventSet(GKInstance eventSet) throws Exception {
        Set instances = new HashSet();
        Set current = new HashSet();
        Set next = new HashSet();
        current.add(eventSet);
        GKInstance tmp = null;
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                tmp = (GKInstance) it.next();
                if (tmp.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasSpecialisedForm)) {
                    List values = tmp.getAttributeValuesList(ReactomeJavaConstants.hasSpecialisedForm);
                    if (values != null && values.size() > 0)
                        next.addAll(values);
                    else
                        instances.add(tmp);
                }
                else if (tmp.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
                    List values = tmp.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                    if (values != null && values.size() > 0)
                        next.addAll(values);
                    else
                        instances.add(tmp);
                }
                else {
                    instances.add(tmp);
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
        return new ArrayList(instances);
    }
    
    private List<GKInstance> getInstancesInEntitySet(GKInstance entitySet) throws Exception {
        Set<GKInstance> set = InstanceUtilities.getContainedInstances(entitySet, 
                                                                      ReactomeJavaConstants.hasMember,
                                                                      ReactomeJavaConstants.hasCandidate);
        return new ArrayList<GKInstance>(set);
    }
    
    private void handleReferenceEntityNames(GKInstance rInstance, Element bpInstance) throws Exception {
        // Want to add all name information into the name slot.
        // There are no other name related slots in level 3 except 
        // the name slot
        List<String> nameList = new ArrayList<String>();
        String displayName = rInstance.getDisplayName();
        if (displayName != null)
            nameList.add(displayName); // want to keep the order of these names
        List names = rInstance.getAttributeValuesList(ReactomeJavaConstants.name);
        if (names != null && names.size() > 0) {
            for (Iterator it = names.iterator(); it.hasNext();) {
                String name = (String) it.next();
                if (!nameList.contains(name))
                    nameList.add(name);
            }
        }
        // Use gene names as synonyms if applicable
        if (rInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.geneName)) {
            List geneNames = rInstance.getAttributeValuesList(ReactomeJavaConstants.geneName);
            if (geneNames != null) {
                for (Iterator it = geneNames.iterator(); it.hasNext();) {
                    String name = (String) it.next();
                    if (!nameList.contains(name))
                        nameList.add(name);
                }
            }
        }
        if (nameList.size() > 0) {
            createDataPropElm(bpInstance,
                              BioPAX3JavaConstants.name,
                              BioPAX3JavaConstants.XSD_STRING,
                              nameList);
        }
    }
    
    protected void handleNames(GKInstance reactomeInstance, Element bpInstance) throws Exception {
        // Use the first value in as short name
        List names = reactomeInstance.getAttributeValuesList(ReactomeJavaConstants.name);
        // There are no shortName and synonym properties in level 3
        List nameList = new ArrayList();
        if (names != null)
            nameList.addAll(names);
        if (nameList.size() == 0 && // If there are values in the name list, just use these names
            reactomeInstance.getDisplayName() != null &&
            !nameList.contains(reactomeInstance.getDisplayName())) {
            nameList.add(reactomeInstance.getDisplayName());
        }
        if (nameList.size() > 0) {
            // Have to make sure displayName is a valid attribute
            // There are displayName and standardName. Use the shortest
            // name as the displayName
            int shortest = Integer.MAX_VALUE;
            String shortestName = null;
            for (Iterator it = nameList.iterator(); it.hasNext();) {
                String tmp = (String) it.next();
                if (tmp.length() < shortest) {
                    shortest = tmp.length();
                    shortestName = tmp;
                }
            }
            createDataPropElm(bpInstance, 
                              BioPAX3JavaConstants.displayName, 
                              BioPAX3JavaConstants.XSD_STRING, 
                              shortestName);
            // displayName is a sub-property of name. If a name is used as a displayName
            // already, don't list it in the name anymore
            nameList.remove(shortestName);
            if (nameList.size() > 0) {
                createDataPropElm(bpInstance, 
                                  BioPAX3JavaConstants.name,
                                  BioPAX3JavaConstants.XSD_STRING,
                                  nameList);
            }
        }
    }
    
    private Element createStoichiometry(Element pe,
                                        Integer stoiCe) {
        Element stoichiometry = createIndividualElm(BioPAX3JavaConstants.Stoichiometry);
        // There are two properties in a Stoichiometry individual
        createDataPropElm(stoichiometry,
                          BioPAX3JavaConstants.stoichiometricCoefficient, 
                          BioPAX3JavaConstants.XSD_FLOAT, 
                          stoiCe.toString());
        createObjectPropElm(stoichiometry,
                            BioPAX3JavaConstants.physicalEntity,
                            pe);
        return stoichiometry;
    }
    
    private boolean handleReaction(GKInstance reaction) throws Exception {
        Element bpReaction = rToBInstanceMap.get(reaction);
        if (bpReaction != null)
            return false; // No need to create reaction
        String reactionType = guessReactionType(reaction);
        bpReaction = createIndividualElm(reactionType);
        rToBInstanceMap.put(reaction, bpReaction);
        if (reactionType.equals(BioPAX3JavaConstants.TemplateReaction)) {
            handleTemplateReaction(reaction,
                                   bpReaction);
            return true;
        }
        // If the reaction type is a BiochemicalReaction, assign its conversionDirection as "LEFT-TO-RIGHT"
        if (reactionType.equals(BioPAX3JavaConstants.BiochemicalReaction)) {
            createDataPropElm(bpReaction, 
                              BioPAX3JavaConstants.conversionDirection, 
                              BioPAX3JavaConstants.XSD_STRING, 
                              "LEFT-TO-RIGHT"); // For Reactome reactions, the direction is alwasy "LEFT-TO-RIGHT".
        }
        // Get the input information
        List inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        if (inputs != null && inputs.size() > 0) {
            Map inputMap = getStoichiometryMap(inputs);
            GKInstance input = null;
            Element bpInput = null;
            Integer stoi = null;
            for (Iterator it = inputMap.keySet().iterator(); it.hasNext();) {
                input = (GKInstance) it.next();
                bpInput = createPhysicalEntity(input);
                stoi = (Integer) inputMap.get(input);
                // Cannot distinguish 1 or unknown in the Reactome data model
                if (stoi != null && stoi.intValue() > 1) {
                    Element stoichiometry = createStoichiometry(bpInput, stoi);
                    createObjectPropElm(bpReaction, 
                                        BioPAX3JavaConstants.participantStoichiometry,
                                        stoichiometry);
                }
                createObjectPropElm(bpReaction, BioPAX3JavaConstants.left, bpInput);
            }
        }
        // Get the output information
        List outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        if (outputs != null && outputs.size() > 0) {
            GKInstance output = null;
            Element bpOutput = null;
            Map outputMap = getStoichiometryMap(outputs);
            Integer stoi = null;
            for (Iterator it = outputMap.keySet().iterator(); it.hasNext();) {
                output = (GKInstance) it.next();
                bpOutput = createPhysicalEntity(output);
                stoi = (Integer) outputMap.get(output);
                if (stoi != null && stoi.intValue() > 1) {
                    Element stoichiometry = createStoichiometry(bpOutput, stoi);
                    createObjectPropElm(bpReaction, 
                                        BioPAX3JavaConstants.participantStoichiometry,
                                        stoichiometry);
                }
                createObjectPropElm(bpReaction, BioPAX3JavaConstants.right, bpOutput);
            }
        }
        // Get the catalystActivity information
        List cas = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        if (cas != null && cas.size() > 0) {
            GKInstance ca = null;
            Element catalysis = null;
            List tmpList = new ArrayList();
            for (Iterator it = cas.iterator(); it.hasNext();) {
                ca = (GKInstance) it.next();
                catalysis = handleCatalystActivity(ca, bpReaction, reaction);
                if (catalysis != null)
                    tmpList.add(catalysis);
                extractECNumberForReaction(ca, bpReaction);
            }
            // A list for reaction in the rxtToControlMap might be created during handleCatalystActvity().
            // So a tmpList might be used.
            if (tmpList.size() > 0) {
                List list = (List) rxtToControlMap.get(reaction);
                if (list == null) {
                    list = new ArrayList();
                    rxtToControlMap.put(reaction, list);
                }
                list.addAll(tmpList);
            }
        }
        return true;
    }
    
    private void handleTemplateReaction(GKInstance reaction,
                                        Element bpReaction) throws Exception {
        List<?> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        if (outputs == null || outputs.size() == 0)
            return;
        // Only need to add product
        for (Iterator<?> it = outputs.iterator(); it.hasNext();) {
            GKInstance output = (GKInstance) it.next();
            Element bpOutput = createPhysicalEntity(output);
            createObjectPropElm(bpReaction, 
                                BioPAX3JavaConstants.product,
                                bpOutput);
        }
    }
    
    /**
     * This method is used to find appropriate reaction type for a ReactomelikeEvent:
     * 1). If a reaction is a BlackBoxEvent, there is no output, and the reaction name
     * contains "degradation", a Degradation will be returned.
     * 2). If a reactioin is a BlackBoxEvent, there is no input, and one output only,
     * a TemplateReaction will be returned.
     * 3). Otherwise, a BiochemicalReaction should be returned.
     * @param instance
     * @return
     * @throws Exception
     */
    private String guessReactionType(GKInstance instance) throws Exception {
        if (instance.getSchemClass().isa(ReactomeJavaConstants.BlackBoxEvent)) {
            List<?> inputs = instance.getAttributeValuesList(ReactomeJavaConstants.input);
            List<?> outputs = instance.getAttributeValuesList(ReactomeJavaConstants.output);
            // Check if this should be a degradation
            if (inputs != null && inputs.size() == 1 &&
                (outputs == null || outputs.size() == 0) &&
                instance.getDisplayName().toLowerCase().contains("degradation")) 
                return BioPAX3JavaConstants.Degradation;
            else if ((inputs == null || inputs.size() == 0) &&
                    outputs != null && outputs.size() == 1) {
                return BioPAX3JavaConstants.TemplateReaction;
            }
        }
        return BioPAX3JavaConstants.BiochemicalReaction;
    }
    
    private void extractECNumberForReaction(GKInstance ca,
                                            Element bpReaction) throws Exception {
        // ecNumber can be used for BiochemicalReaction and its sub-class only!
        if (!bpReaction.getName().equals(BioPAX3JavaConstants.BiochemicalReaction) &&
            !bpReaction.getName().equals(BioPAX3JavaConstants.TransportWithBiochemicalReaction))
            return;
        // Get the activity from CatalystActivity
        List activities = ca.getAttributeValuesList(ReactomeJavaConstants.activity);
        if (activities == null || activities.size() == 0)
            return;
        // Get the ecNumber for GO_MolecularFunction
        Set ecNumbers = new HashSet();
        GKInstance activity = null;
        for (Iterator it = activities.iterator(); it.hasNext();) {
            activity = (GKInstance) it.next();
            List numbers = activity.getAttributeValuesList(ReactomeJavaConstants.ecNumber);
            if (numbers == null || numbers.size() == 0)
                continue;
            ecNumbers.addAll(numbers);
        }
        // Attach ecNumber for bpReaction
        createDataPropElm(bpReaction,
                          BioPAX3JavaConstants.eCNumber,
                          BioPAX3JavaConstants.XSD_STRING,
                          ecNumbers);
    }
    
    private Element handleCatalystActivity(GKInstance ca, 
                                           Element bpControlled,
                                           GKInstance rReaction) throws Exception {
        // In BioPAX, A catalyst should not be re-used.
//        Element bpCatalyst = rToBInstanceMap.get(ca);
//        if (bpCatalyst != null)
//            return bpCatalyst;
        Element bpCatalyst = createIndividualElm(BioPAX3JavaConstants.Catalysis);
        GKInstance controller = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
        if (controller != null) {
            Element bpController = createPhysicalEntity(controller);
            createObjectPropElm(bpCatalyst, BioPAX3JavaConstants.controller, bpController);
        }
        createObjectPropElm(bpCatalyst, BioPAX3JavaConstants.controlled, bpControlled);
//        createDataPropElm(bpCatalyst,
//                          BioPAX3JavaConstants.direction,
//                          BioPAX3JavaConstants.XSD_STRING,
//                          "PHYSIOL-LEFT-TO-RIGHT");
        // CONTROL-TYPE value should be ACTIVATION: This is required
        createDataPropElm(bpCatalyst,
                          BioPAX3JavaConstants.controlType,
                          BioPAX3JavaConstants.XSD_STRING,
                          "ACTIVATION");
        // Based on activity to create a xref to point a GO term
        GKInstance activity = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.activity);
        if (activity != null) {
            Element bpXref = createGOOWL(activity,
                                         BioPAX3JavaConstants.GO_MOLECULLAR_FUNCTION);
            createObjectPropElm(bpCatalyst, BioPAX3JavaConstants.xref, bpXref);
        }
        // Need to handle reverse attributes for regulation here.
        handleRegulationForCatalystActivity(ca, bpCatalyst, rReaction);
        Element sameCARelationship = getRelationTypeCV("Same Catalyst Activity");
        attachReactomeIDAsXref(ca, 
                               bpCatalyst,
                               BioPAX3JavaConstants.RelationshipXref,
                               sameCARelationship);
        attachReactomeDatasource(bpCatalyst);
//        rToBInstanceMap.put(ca, bpCatalyst);
        return bpCatalyst;
    }
    
    private void handleEntitySetMember(GKInstance entitySet,
                                       Element bpPE) throws Exception {
        // Get all members
        List<GKInstance> members = new ArrayList<GKInstance>();
        if (entitySet.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
            List values = entitySet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            if (values != null) {
                for (Iterator it = values.iterator(); it.hasNext();) {
                    GKInstance value = (GKInstance) it.next();
                    members.add(value);
                }
            }
        }
        if (entitySet.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
            List values = entitySet.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
            if (values != null) {
                for (Iterator it = values.iterator(); it.hasNext();) {
                    GKInstance value = (GKInstance) it.next();
                    members.add(value);
                }
            }
        }
        for (GKInstance member : members) {
            Element element = createPhysicalEntity(member);
            if (element != null)
                createObjectPropElm(bpPE, 
                                    BioPAX3JavaConstants.memberPhysicalEntity,
                                    element);
        }
    }
    
    private Element createPhysicalEntity(GKInstance rEntity) throws Exception {
        // In level 3, PE can be reused as in Reactome.
        Element bpEntity = (Element) rToBInstanceMap.get(rEntity);
        if (bpEntity != null)
            return bpEntity;
        boolean isSet = rEntity.getSchemClass().isa(ReactomeJavaConstants.EntitySet);
        SchemaClass reactomeType = getReactomeEntityType(rEntity);
        // Try to figure out the type based on the referenceEntity
        if (reactomeType == null) { // For Polymer, OtherEntity and other entities which don't have referenceEntity values
            bpEntity = createIndividualElm(BioPAX3JavaConstants.PhysicalEntity);
        }
        else if (rEntity.getSchemClass().isa(ReactomeJavaConstants.Complex) ||
                 reactomeType.isa(ReactomeJavaConstants.Complex)) { // Special case
            bpEntity = createIndividualElm(BioPAX3JavaConstants.Complex);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceGeneProduct) ||
                 reactomeType.isa(ReactomeJavaConstants.ReferencePeptideSequence)) {
            // This is a protein
            bpEntity = createIndividualElm(BioPAX3JavaConstants.Protein);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceMolecule) ||
                 reactomeType.isa(ReactomeJavaConstants.ReferenceGroup) ||
                 reactomeType.isa(ReactomeJavaConstants.ReferenceMoleculeClass)) {
            bpEntity = createIndividualElm(BioPAX3JavaConstants.SmallMolecule);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceDNASequence)) {
            bpEntity = createIndividualElm(BioPAX3JavaConstants.Dna);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceRNASequence)) {
            bpEntity = createIndividualElm(BioPAX3JavaConstants.Rna);
        }
        else {
            bpEntity = createIndividualElm(BioPAX3JavaConstants.PhysicalEntity);
        }
        if (isSet) {
            // In level 3, this comment is used as a flag to indicate
            // this PE is converted from EntitySet. It is for a debug purpose.
            createDataPropElm(bpEntity, 
                              BioPAX3JavaConstants.comment, 
                              BioPAX3JavaConstants.XSD_STRING, 
                              "Converted from EntitySet in Reactome");
            handleEntitySetMember(rEntity,
                                  bpEntity);
        }
        handleNames(rEntity, bpEntity);
        // Get the compartment information
        handleCompartment(rEntity, bpEntity);
        featureHandler.handleHasDomain(rEntity, 
                                       bpEntity,
                                       BioPAX3JavaConstants.feature);
        handleReferenceEntity(rEntity, bpEntity);
        featureHandler.handleModifiedResidue(rEntity, 
                                             bpEntity,
                                             BioPAX3JavaConstants.feature);
        featureHandler.handleStartAndEndCoordinates(rEntity, 
                                                    bpEntity,
                                                    BioPAX3JavaConstants.feature);
        exportDbIdAsComment(rEntity, bpEntity);
        attachReactomeIDAsXref(rEntity, bpEntity);
        attachReactomeDatasource(bpEntity);
        rToBInstanceMap.put(rEntity, bpEntity);
        return bpEntity;
    }
    
    private void exportDbIdAsComment(GKInstance rEntity,
                                     Element bpEntityParticipant) {
        String comment = "Reactome DB_ID: " + rEntity.getDBID();
        createDataPropElm(bpEntityParticipant, 
                          BioPAX3JavaConstants.comment,
                          BioPAX3JavaConstants.XSD_STRING,
                          comment);
    }
    
    private SchemaClass getReactomeEntityType(GKInstance rEntity) throws Exception {
        // Complex is a Complex. No other twister.
        if (rEntity.getSchemClass().isa(ReactomeJavaConstants.Complex))
            return rEntity.getSchemClass();
        // Peek referenceEntity attribute. The type should be determined by it.
        if (rEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
            GKInstance referenceEntity = (GKInstance) rEntity.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (referenceEntity != null)
                return referenceEntity.getSchemClass();
        }
        // Check EntitySet
        if (rEntity.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
            List<GKInstance> instances = getInstancesInEntitySet(rEntity);
//            // This is a special case: an EntitySet is a Complex set. In this case,
//            // Complex will be returned
//            for (Iterator it = instances.iterator(); it.hasNext();) {
//                GKInstance member = (GKInstance) it.next();
//                if (member.getSchemClass().isa(ReactomeJavaConstants.Complex))
//                    return member.getSchemClass();
//            }
            Set<SchemaClass> types = new HashSet<SchemaClass>();
            GKInstance referenceEntity = null;
            for (GKInstance instance : instances) {
                if (instance.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
                    types.add(instance.getSchemClass());
                    continue;
                }
                if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
                    types.add(null); // Since an unknown type Entity will be converted to PE, if a Complex and Polymber mixed together,
                                     // use this label to make the EntitySet as PE as suggested by Emek.
                    continue;
                }
                referenceEntity = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceEntity);
                if (referenceEntity != null) {
                    // Special case for ReferenceIsoForm.
                    if (referenceEntity.getSchemClass().isa(ReactomeJavaConstants.ReferenceIsoform))
                        types.addAll(referenceEntity.getSchemClass().getSuperClasses());
                    else
                        types.add(referenceEntity.getSchemClass());
                }
            }
            // It is more like a bug if types have more than one value
            if (types.size() == 0 || types.size() > 1)
                return null;
            else
                return (SchemaClass) types.iterator().next();
        }
        return null;
    }
    
    private Element createBpEntityRefFromRRefEntity(GKInstance rRefEntity) throws Exception {
        Element bpEntityReference = (Element) rToBInstanceMap.get(rRefEntity);
        if (bpEntityReference != null ||
            rToBInstanceMap.containsKey(rRefEntity))
            return bpEntityReference;
        bpEntityReference = _createBpEntityReference(rRefEntity, true);
        rToBInstanceMap.put(rRefEntity,
                            bpEntityReference);
        return bpEntityReference;
    }

    /**
     * A refactored method to create a
     * @param rRefEntity
     * @return
     * @throws Exception
     * @throws InvalidAttributeException
     */
    private Element _createBpEntityReference(GKInstance rRefEntity,
                                             boolean needProperties)
            throws Exception, InvalidAttributeException {
        Element bpEntityReference;
        SchemaClass reactomeType = rRefEntity.getSchemClass();
        if (reactomeType.isa(ReactomeJavaConstants.ReferenceGeneProduct) ||
            reactomeType.isa(ReactomeJavaConstants.ReferencePeptideSequence)) {
            // This is a protein
            bpEntityReference = createIndividualElm(BioPAX3JavaConstants.ProteinReference);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceMolecule) || 
                reactomeType.isa(ReactomeJavaConstants.ReferenceGroup) ||
                reactomeType.isa(ReactomeJavaConstants.ReferenceMoleculeClass)) {
            bpEntityReference = createIndividualElm(BioPAX3JavaConstants.SmallMoleculeReference);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceDNASequence)) {
            bpEntityReference = createIndividualElm(BioPAX3JavaConstants.DnaReference);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceRNASequence)) {
            bpEntityReference = createIndividualElm(BioPAX3JavaConstants.RnaReference);
        }
        else {
            bpEntityReference = createIndividualElm(BioPAX3JavaConstants.EntityReference);
        }
        handleTaxon(rRefEntity, 
                    bpEntityReference, 
                    ReactomeJavaConstants.species);
        if (needProperties) {
            handleReferenceEntityNames(rRefEntity, 
                                       bpEntityReference);
            // Create a unificationXref for bpEntityReference
            Element xref = createXREFFromRefEntity(rRefEntity);
            if (xref != null) {
                createObjectPropElm(bpEntityReference,
                                    BioPAX3JavaConstants.xref,
                                    xref);
            }
            // Get comment or description
            String comment = null;
            if (rRefEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.comment))
                comment = (String) rRefEntity.getAttributeValue(ReactomeJavaConstants.comment);
            if (comment == null && rRefEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.description))
                comment = (String) rRefEntity.getAttributeValue(ReactomeJavaConstants.description);
            if (comment != null) {
                createDataPropElm(bpEntityReference, 
                                  BioPAX3JavaConstants.comment,
                                  BioPAX3JavaConstants.XSD_STRING,
                                  comment);
            }
        }
        return bpEntityReference;
    }
    
    private void handleReferenceEntity(GKInstance rEntity, 
                                       Element bpEntity) throws Exception {
        if (rEntity.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
            // This should be regarded as a special case. There is no referenceEntity 
            // for Complexes in Reactome. Instead we take this opportunity to handle
            // complex components.
            handleComplexComponents(rEntity, bpEntity);
            return;
        }
//        // Handle EntitySet: This should be handled by EntitySet creation
//        if (rEntity.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
//            handleReferenceEntityForEntitySet(rEntity, bpEntity);
//            return;
//        }
        if (!rEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity) ||
             rEntity.getAttributeValue(ReactomeJavaConstants.referenceEntity) == null) {
            return; // Cannot do anything here: maybe a set
        }
        GKInstance rRefEntity = (GKInstance) rEntity.getAttributeValue(ReactomeJavaConstants.referenceEntity);
        Element bpEntityReference = createBpEntityRefFromRRefEntity(rRefEntity);
        if (bpEntityReference != null) {
            createObjectPropElm(bpEntity,
                                BioPAX3JavaConstants.entityReference,
                                bpEntityReference);
        }
    }
    
    private void handleReferenceEntityForEntitySet(GKInstance entitySet,
                                                   Element bpEntity) throws Exception {
        List<GKInstance> members = new ArrayList<GKInstance>();
        if (entitySet.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
            List values = entitySet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            if (values != null) {
                for (Iterator it = values.iterator(); it.hasNext();) {
                    GKInstance value = (GKInstance) it.next();
                    members.add(value);
                }
            }
        }
        if (entitySet.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
            List values = entitySet.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
            if (values != null) {
                for (Iterator it = values.iterator(); it.hasNext();) {
                    GKInstance value = (GKInstance) it.next();
                    members.add(value);
                }
            }
        }
        if (members.size() == 0)
            return;
        // Need a template for ReferenceEntity
        GKInstance refEntity = null;
        for (GKInstance member : members) {
            refEntity = (GKInstance) member.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (refEntity != null)
                break;
        }
        // Create an ProteinReference first
        Element bpEntityRef = _createBpEntityReference(refEntity, false);
        createObjectPropElm(bpEntity, 
                            BioPAX3JavaConstants.entityReference, 
                            bpEntityRef);
        List<Element> memberRefs = new ArrayList<Element>();
        for (GKInstance member : members) {
            refEntity = (GKInstance) member.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (refEntity == null)
                continue;
            Element memberBpEntityRef = createBpEntityRefFromRRefEntity(refEntity);
            if (memberBpEntityRef != null)
                memberRefs.add(memberBpEntityRef);
        }
        if (memberRefs.size() > 0) {
            createObjectPropElm(bpEntityRef, 
                                BioPAX3JavaConstants.memberEntityReference,
                                memberRefs);
        }
    }
    
    protected Element createXREFFromRefEntity(GKInstance referenceEntity) throws Exception {
        Element xref = (Element) refEntityToXrefMap.get(referenceEntity);
        if (xref != null ||
            refEntityToXrefMap.containsKey(referenceEntity)) // It might have been checked, but null is created.
            return xref;
        // Create a unificationXref for bpEntityRef
        // Should use variantIdentifier first
        String identifier = null;
        boolean isIsoformId = false;
        if (referenceEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier)) {
            identifier = (String) referenceEntity.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
            isIsoformId = true;
        }
        if (identifier == null)
            identifier = (String) referenceEntity.getAttributeValue(ReactomeJavaConstants.identifier);
        GKInstance refDB = (GKInstance) referenceEntity.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
        if (identifier != null && refDB != null) {
            String dbName = refDB.getDisplayName();
            if (dbName.equalsIgnoreCase("UniProt") && isIsoformId)
                dbName = "UniProt Isoform"; // Hard-code the db name as suggested by Igor's email on March 22, 2013.
            xref = createIndividualElm(BioPAX3JavaConstants.UnificationXref);
            createDataPropElm(xref, BioPAX3JavaConstants.db,
                              BioPAX3JavaConstants.XSD_STRING, dbName);
            createDataPropElm(xref,
                              BioPAX3JavaConstants.id,
                              BioPAX3JavaConstants.XSD_STRING, 
                              validateXrefId(dbName, identifier));
        }
        refEntityToXrefMap.put(referenceEntity, xref);
        return xref;
    }
    
    private String validateXrefId(String dbName, 
                                  String identifier) {
        if (dbName.equals("MOD") ||
            dbName.equals("GO") ||
            dbName.equals("ChEBI"))
            return dbName.toUpperCase() + ":" + identifier;
        return identifier;
    }
    
    private Map getStoichiometryMap(List entities) {
        Map map = new HashMap();
        GKInstance entity = null;
        Integer value = null;
        for (Iterator it = entities.iterator(); it.hasNext();) {
            entity = (GKInstance) it.next();
            value = (Integer) map.get(entity);
            if (value == null) {
                value = new Integer(1);
                map.put(entity, value);
            }
            else {
                value = new Integer(value.intValue() + 1);
                // Need to reset the link since the reference has been changed.
                map.put(entity, value);
            }
        }
        return map;
    }
    
    private void handleComplexComponents(GKInstance rComplex,
                                         Element bpComplex) throws Exception {
        // Get information on components
        List components = rComplex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        if (components != null && components.size() > 0) {
            // Need to figure out the stoichiometries
            Map compMap = getStoichiometryMap(components);
            for (Iterator it = compMap.keySet().iterator(); it.hasNext();) {
                GKInstance comp = (GKInstance) it.next();
                Element bpComp = createPhysicalEntity(comp);
                Integer stoi = (Integer) compMap.get(comp);
                Element stoiElm = createStoichiometry(bpComp, stoi);
                createObjectPropElm(bpComplex, 
                                    BioPAX3JavaConstants.componentStoichiometry,
                                    stoiElm);
                createObjectPropElm(bpComplex,
                                    BioPAX3JavaConstants.component,
                                    bpComp);
            }
        }
    }
    
    private void handleCompartment(GKInstance rEntity, Element bpInstance) throws Exception {
        GKInstance compartment = (GKInstance) rEntity.getAttributeValue(ReactomeJavaConstants.compartment);
        if (compartment == null)
            return;
        // should never occur
        if (compartment.getAttributeValue(ReactomeJavaConstants.accession) == null)
            return; 
        Element bpCompartment = (Element) rToBInstanceMap.get(compartment);
        if (bpCompartment != null) {
            createObjectPropElm(bpInstance, BioPAX3JavaConstants.cellularLocation, bpCompartment);
            return;
        }
        // Need to create a new bpCompartment
        bpCompartment = createIndividualElm(BioPAX3JavaConstants.CellularLocationVocabulary);
        rToBInstanceMap.put(compartment, bpCompartment);
        String term = (String) compartment.getAttributeValue(ReactomeJavaConstants.name);
        if (term != null) {
            createDataPropElm(bpCompartment, BioPAX3JavaConstants.term, BioPAX3JavaConstants.XSD_STRING, term);
        }
        // Need to create a xref for GO
        String accession = (String) compartment.getAttributeValue(ReactomeJavaConstants.accession);
        Element xref = createIndividualElm(BioPAX3JavaConstants.UnificationXref);
        createDataPropElm(xref,
                          BioPAX3JavaConstants.db, 
                          BioPAX3JavaConstants.XSD_STRING, 
                          "GENE ONTOLOGY");
        if (!accession.startsWith("GO:"))
            accession = "GO:" + accession;
        createDataPropElm(xref, 
                          BioPAX3JavaConstants.id,
                          BioPAX3JavaConstants.XSD_STRING, 
                          accession);
        createObjectPropElm(bpCompartment, BioPAX3JavaConstants.xref, xref);
        // Don't forget to attach compartment to bpInstance
        createObjectPropElm(bpInstance,
                            BioPAX3JavaConstants.cellularLocation,
                            bpCompartment);
    }
    
    public Document getBioPAXModel() {
        return biopaxDoc;
    }
    
    /**
     * A test method
     * @throws Exception
     */
    @Test
    public void testConvert() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "test_slice_65",
                                            "root",
                                            "macmysql01",
                                            3306);
        // PLC Gamma1 Signaling
        //GKInstance topEvent = dba.fetchInstance(167021L);
        // Response to elevated platelet cytosolic Ca++
        //GKInstance topEvent = dba.fetchInstance(76005L);
        // MyD88 Cascade
        //GKInstance topEvent = dba.fetchInstance(166058L);
        // Human Apoptosis
        //GKInstance topEvent = dba.fetchInstance(109581L);
        // Human Serotonin Neurotransmitter Release Cycle181429
        //        GKInstance topEvent = dba.fetchInstance(181429L);
        // Human mRNA processing
        //        GKInstance topEvent = dba.fetchInstance(75071L);
        // Insulin receptor recycling
        //        GKInstance topEvent = dba.fetchInstance(77387L);
        // Cyclin E associated events during G1/S transition: test TemplateReaction and
        // TemplateReactionREgulation
        //        GKInstance topEvent = dba.fetchInstance(69202L);
        // Glycolysis to test Book publication
        //      GKInstance topEvent = dba.fetchInstance(70171L);
        // Alternative complement activation to test URL publication
        //      GKInstance topEvent = dba.fetchInstance(173736L);
        // Cdc20:Phospho-APC/C mediated degradation of Cyclin A
//        GKInstance topEvent = dba.fetchInstance(174184L);
        // Platelet degranulation to check EntitySet conversion: an EntitySet has mixed types of members.
//        GKInstance topEvent = dba.fetchInstance(114608L);
     // Cancer pathway having new FragmentModification 
//        GKInstance topEvent = dba.fetchInstance(1643713L);
        
//        // A mutated pathway in FGFR
//        GKInstance topEvent = dba.fetchInstance(1839094L);
        
        // A reaction containing a deletion fragment
//        GKInstance topEvent = dba.fetchInstance(1248655L);
        
//        // Check for ModifiedResidue
//        GKInstance topEvent = dba.fetchInstance(69620L);
        // Check for EntitySet conversion
//        GKInstance topEvent = dba.fetchInstance(173488L);
        // Check of mRNA OpenSet
//        GKInstance topEvent = dba.fetchInstance(72706L);
        // Check for Degradation
//        GKInstance topEvent = dba.fetchInstance(201425L);
        // Check for a bug having one reaction exported twice
//        GKInstance topEvent = dba.fetchInstance(162906L);
        // Test ecnumber in Degradation
//        GKInstance topEvent = dba.fetchInstance(2173793L);
        // Check regulation after data model change by moving regulations to RLEs
        GKInstance topEvent = dba.fetchInstance(70221L);
        
        ReactomeToBioPAX3XMLConverter converter = new ReactomeToBioPAX3XMLConverter();
        converter.setReactomeEvent(topEvent);
        converter.convert();
        Document biopaxModel = converter.getBioPAXModel();
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        String name = (String) topEvent.getAttributeValue(ReactomeJavaConstants.name);
        name = name.replaceAll("( )|(/)|(:)", "_");
        FileOutputStream fos = new FileOutputStream("tmp/biopax3/" + name + ".owl");
        outputter.output(biopaxModel, fos);
    }
    
    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Usage java org.reactome.biopax.ReactomeToBioPAX3Converter " +
            		"dbHost dbName dbUser dbPwd dbPort eventID");
            System.exit(1);
        }
        try {
            ReactomeToBioPAX3XMLConverter converter = new ReactomeToBioPAX3XMLConverter();
            GKInstance topEvent = getTopLevelEvent(args);
            converter.setReactomeEvent(topEvent);
            converter.convert();
            Document biopaxModel = converter.getBioPAXModel();
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            
            outputter.output(biopaxModel, System.out);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private static GKInstance getTopLevelEvent(String[] args) throws Exception {
        MySQLAdaptor adaptor = new MySQLAdaptor(args[0],
                                                args[1],
                                                args[2],
                                                args[3],
                                                Integer.parseInt(args[4]));
        GKInstance event = adaptor.fetchInstance(new Long(args[5]));
        return event;
    }

    private String getReactomeInstanceURL(String id) {
        return "http://www.reactome.org/content/detail/" + id;
    }
}
