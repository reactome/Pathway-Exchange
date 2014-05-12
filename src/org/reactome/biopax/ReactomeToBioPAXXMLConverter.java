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
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Test;

/**
 * This converter from Reactome to BioPAX uses JDOM to handle xml directly to speed up performance.
 * @author guanming
 */
@SuppressWarnings("unchecked")
public class ReactomeToBioPAXXMLConverter {
    // The top level pathway or reaction to be converted
    private GKInstance topEvent;
    // A map from Reactome Instance to BioPAX Individuals
    private Map rToBInstanceMap;
    // a map from modification to biopax individuals.
    // A new map is used in case modification is used in other places.
    private Map modificationToFeatTypeMap;
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
//    // To keep track ID to avoid duplication
//    private Set idSet;
    private BioPAXOWLIDGenerator idGenerator;
    // To track complex and GenericEntity mapping to BioPAX PhysicalEntiy so that
    // PhysicalEntity in BioPAX can be reused. This map is differnt from rToBInstanceMap.
    // Instances in rToBInstanceMap are direct mappings.
    private Map rEntityToBEntityMap;
    // To control ReferenceEntity to Xref
    private Map refEntityToXrefMap;
    // Generated BioPAX document
    private Document biopaxDoc;
    // The root element
    private Element rootElm;
    // Reused NameSpaces
    private Namespace bpNS = BioPAXJavaConstants.bpNS;
    private Namespace rdfNS = BioPAXJavaConstants.rdfNS;
    private Namespace rdfsNS = BioPAXJavaConstants.rdfsNS;
    private Namespace owlNS = BioPAXJavaConstants.owlNS;
    private Namespace xsdNS = BioPAXJavaConstants.xsdNS;
    private Namespace reactomeNS = BioPAXJavaConstants.reactomeNS;
    // To be used as Datasource property for entity instances
    private Element reactomeDatasource;
    // To be used for sequence feature type
    private Element lengthFeatureType;
    // Cache a release specific database name
    private String currentDbName;
    
    public ReactomeToBioPAXXMLConverter() {
    }
    
    public Map getRToBInstanceMap() {
        return this.rToBInstanceMap;
    }
    
    public void setIDGenerator(BioPAXOWLIDGenerator idGenetator) {
        this.idGenerator = idGenetator;
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
        if (topEvent == null) {
            if (idGenerator.getSpecies() != null) {
                defaultNSText = BioPAXJavaConstants.REACTOME_NS + "/" + idGenerator.getSpecies().getDBID();
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
            defaultNSText = BioPAXJavaConstants.REACTOME_NS + "/" + topEvent.getDBID();
            // Get a namespace for this event
            defaultNS = Namespace.getNamespace(defaultNSText + "#");
        }
        // As suggeted by Igor, don't use default NS since it is not used at all
//        rootElm.addNamespaceDeclaration(defaultNS);
        // As suggest by Igor, add "#" at the end of base URI
        rootElm.setAttribute(new Attribute("base", 
                                           defaultNSText.endsWith("#") ? defaultNSText : defaultNSText + "#", 
                                           Namespace.XML_NAMESPACE));
        biopaxDoc.setRootElement(rootElm);
        Element ontElm = new Element("Ontology", owlNS);
        ontElm.setAttribute(new Attribute("about", "", rdfNS));
        rootElm.addContent(ontElm);
        // Add imports for Biopax Ontology
        Element importElm = new Element("imports", owlNS);
        importElm.setAttribute(new Attribute("resource", BioPAXJavaConstants.BIOPAX_DOWNLOAD_URI, rdfNS));
        ontElm.addContent(importElm);
        // Add a description
        Element commentElm = new Element("comment", rdfsNS);
        commentElm.setAttribute("datatype", BioPAXJavaConstants.XSD_STRING, rdfNS);
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
        if (modificationToFeatTypeMap == null)
            modificationToFeatTypeMap = new HashMap();
        else
            modificationToFeatTypeMap.clear();
        if (refEntityToXrefMap == null)
            refEntityToXrefMap = new HashMap();
        else
            refEntityToXrefMap.clear();
        if (idGenerator == null)
            idGenerator = new BioPAXOWLIDGenerator();
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
    
    public void convert(Collection events) throws Exception {
        initMap();	
        initBioPAX(null);
        for (Iterator ei = events.iterator(); ei.hasNext();) {
            GKInstance e = (GKInstance) ei.next();
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
        Collection regulations = ca.getReferers(ReactomeJavaConstants.regulatedEntity);
        if (regulations == null || regulations.size() == 0)
            return;
        GKInstance regulation = null;
        Element modulation = null;
        for (Iterator it = regulations.iterator(); it.hasNext();) {
            regulation = (GKInstance) it.next();
            modulation = createModulationFromRegulation(regulation, 
                                                        BioPAXJavaConstants.modulation);
            createObjectPropElm(modulation, BioPAXJavaConstants.CONTROLLED, bpCatalyst);
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
    
    /**
     * TODO: Need to map the regulation type.
     * @param regulatedEntity
     * @param bpEvent
     * @throws Exception
     */
    private void handleRegulation(GKInstance regulatedEntity, Element bpEvent) throws Exception {
        Collection regulations = regulatedEntity.getReferers(ReactomeJavaConstants.regulatedEntity);
        if (regulations == null || regulations.size() == 0)
            return;
        GKInstance regulation = null;
        Element modulation = null;
        for (Iterator it = regulations.iterator(); it.hasNext();) {
            regulation = (GKInstance) it.next();
            modulation = createModulationFromRegulation(regulation, 
                                                        BioPAXJavaConstants.control);
            createObjectPropElm(modulation, BioPAXJavaConstants.CONTROLLED, bpEvent);
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
        Element modulation = createIndividualElm(controlType);
        handleNames(regulation, modulation);
        String type = getControlTypeFromRegulation(regulation);
        if (type != null) {
            createDataPropElm(modulation, 
                     		    BioPAXJavaConstants.CONTROL_TYPE,
                     		    BioPAXJavaConstants.XSD_STRING,
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
            Element entityParticipant = createEntityParticipant(regulator);
            if (entityParticipant != null) {
                createObjectPropElm(modulation, BioPAXJavaConstants.CONTROLLER, entityParticipant);
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
        GKInstance regulationType = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulationType);
        if (regulationType != null) {
            String displayName = regulationType.getDisplayName();
            if (displayName.equalsIgnoreCase("allosteric activation")) {
                return "ACTIVATION-ALLOSTERIC";
            }
            else if (displayName.equalsIgnoreCase("allosteric inhibition"))
                return "INHIBITION-ALLOSTERIC";
            else if (displayName.equalsIgnoreCase("competitive inhibition"))
                return "INHIBITION-COMPETITIVE";
            else if (displayName.equalsIgnoreCase("non-competitive inhibition"))
                return "INHIBITION-NONCOMPETITIVE";
        }
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
                    createObjectPropElm(prevPathwayStep, BioPAXJavaConstants.NEXT_STEP, pathwayStep);
            }
        }
        List list = null;
        for (Iterator it = rxtToControlMap.keySet().iterator(); it.hasNext();) {
            event = (GKInstance) it.next();
            pathwayStep = (Element) eventToPathwayStepMap.get(event);
            if (pathwayStep == null) // A reaction might not be contained by a PathwayStep.
                continue;            // E.g. some orphan reactions.
            list = (List) rxtToControlMap.get(event);
            createObjectPropElm(pathwayStep, BioPAXJavaConstants.STEP_INTERACTIONS, list);
        }
    }
    
    private void handleEvent(GKInstance event) throws Exception {
        if (event.getSchemClass().isa(ReactomeJavaConstants.Pathway))
            handlePathway(event);
        // All subclasses to ReactionlikeEvent can be handled by handleReaction() method
        // hasEvent in BlackboxEvent will not be exported for the time being.
        else if (event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
            handleReaction(event);
        // For backward schema compatibility
        else if (event.getSchemClass().isa(ReactomeJavaConstants.Reaction))
            handleReaction(event); 
        else if (event.getSchemClass().isa(ReactomeJavaConstants.ConceptualEvent))
            handleEventSet(event);
        else if (event.getSchemClass().isa(ReactomeJavaConstants.EquivalentEventSet))
            handleEventSet(event);
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
        // Since the type cannot be mapped (interaction type should be mapped to PSI-MI), this
        // method is disabled for the time being.
        // The type is not required. Turn this on back on June 24, 2009.
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
                              BioPAXJavaConstants.COMMENT,
                              BioPAXJavaConstants.XSD_STRING,
                              comment);
        }
    }
    
    private void attachReactomeDatasource(Element entityElm) {
        if (reactomeDatasource == null) {
            reactomeDatasource = createIndividualElm(BioPAXJavaConstants.dataSource);
            createDataPropElm(reactomeDatasource,
                              BioPAXJavaConstants.NAME,
                              BioPAXJavaConstants.XSD_STRING,
                              "Reactome");
            createDataPropElm(reactomeDatasource,
                              BioPAXJavaConstants.COMMENT,
                              BioPAXJavaConstants.XSD_STRING,
                              "http://www.reactome.org");
        }
        createObjectPropElm(entityElm, 
                            BioPAXJavaConstants.DATA_SOURCE,
                            reactomeDatasource);
    }
    
    private void handleEventSet(GKInstance event) throws Exception { 
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
            return ; // Cannot determine the type. Nothing to converted
        if (type.isa(ReactomeJavaConstants.Reaction))
            handleReaction(event);
        else if (type.isa(ReactomeJavaConstants.Pathway))
            handlePathway(event);
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
            createObjectPropElm(bpEvent, BioPAXJavaConstants.EVIDENCE, bpEvidence);
        }
    }
    
    private Element createEvidenceFromInferredFrom(GKInstance ifInstance, 
                                                   GKInstance evidenceType) throws Exception {
        Element bpEvidence = createIndividualElm(BioPAXJavaConstants.evidence);
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
                    createObjectPropElm(bpEvidence, BioPAXJavaConstants.XREF, bpXref);
            }
        }
        // Add comments to evidence
        if (summation != null) {
            String text = (String) summation.getAttributeValue(ReactomeJavaConstants.text);
            if (text != null)
                createDataPropElm(bpEvidence, BioPAXJavaConstants.COMMENT,
                                  BioPAXJavaConstants.XSD_STRING, text);
        }
        // Need to add EVIDENCE_CODE
        Element evidenceCode = convertEvidenceTypeToCode(evidenceType);
        if (evidenceCode != null)
            createObjectPropElm(bpEvidence,
                                BioPAXJavaConstants.EVIDENCE_CODE,
                                evidenceCode);
        return bpEvidence;
    }
    
    private Element convertEvidenceTypeToCode(GKInstance evidenceType) throws Exception {
        Element evidence = (Element) rToBInstanceMap.get(evidenceType);
        if (evidence != null)
            return evidence;
        evidence = createIndividualElm(BioPAXJavaConstants.openControlledVocabulary);
        createDataPropElm(evidence, 
                          BioPAXJavaConstants.TERM, 
                          BioPAXJavaConstants.XSD_STRING, 
                          evidenceType.getDisplayName());
        Element goEvidenceXref = createIndividualElm(BioPAXJavaConstants.unificationXref);
        createDataPropElm(goEvidenceXref, 
                          BioPAXJavaConstants.DB,
                          BioPAXJavaConstants.XSD_STRING,
                          "GO");
        // Get the id from evidenceType
        List names = evidenceType.getAttributeValuesList(ReactomeJavaConstants.name);
        if (names != null && names.size() > 0) {
            // Get the shortest one
            String shortest = (String) names.get(0);
            for (int i = 1; i < names.size(); i++) {
                String tmp = (String) names.get(i);
                if (tmp.length() < shortest.length())
                    shortest = tmp;
            }
            createDataPropElm(goEvidenceXref,
                              BioPAXJavaConstants.ID,
                              BioPAXJavaConstants.XSD_STRING,
                              shortest);
            // Add other names as comments
            String displayName = evidenceType.getDisplayName();
            for (Iterator it = names.iterator(); it.hasNext();) {
                String tmp = (String) it.next();
                if (tmp.equals(displayName))
                    continue;
                // Push all names except displayName into comment attributes
                createDataPropElm(evidence,
                                  BioPAXJavaConstants.COMMENT,
                                  BioPAXJavaConstants.XSD_STRING,
                                  tmp);
            }
        }
        createObjectPropElm(evidence, BioPAXJavaConstants.XREF, goEvidenceXref);
        return evidence;
    }
    
    private void handleEventGOBP(GKInstance event, Element bpEvent) throws Exception {
        GKInstance goBP = (GKInstance) event.getAttributeValue(ReactomeJavaConstants.goBiologicalProcess);
        if (goBP == null)
            return;
        Element goBPOWL = createGOOWL(goBP, "GO biological process");
        if (goBPOWL != null) {
            createObjectPropElm(bpEvent, BioPAXJavaConstants.XREF, goBPOWL);
        }
    }
    
    private Element createGOOWL(GKInstance goInstance, String relationShipType) throws Exception {
        Element goOWL = (Element) rToBInstanceMap.get(goInstance);
        if (goOWL != null)
            return goOWL;
        goOWL = createIndividualElm(BioPAXJavaConstants.relationshipXref);
        createDataPropElm(goOWL, BioPAXJavaConstants.DB, BioPAXJavaConstants.XSD_STRING, "GO");
        String identifier = (String) goInstance.getAttributeValue(ReactomeJavaConstants.accession);
        if (identifier != null) {
            createDataPropElm(goOWL, BioPAXJavaConstants.ID, BioPAXJavaConstants.XSD_STRING, identifier);
        }
        createDataPropElm(goOWL, 
                          BioPAXJavaConstants.RELATIONSHIP_TYPE,
                          BioPAXJavaConstants.XSD_STRING,
                          relationShipType);
        rToBInstanceMap.put(goInstance, goOWL);
        return goOWL;
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
                createObjectPropElm(bpEvent, BioPAXJavaConstants.XREF, pubXref);
        }
    }
    
    private void handlePathway(GKInstance pathway) throws Exception {
        Element bpPathway = createIndividualElm(BioPAXJavaConstants.pathway);
        rToBInstanceMap.put(pathway, bpPathway);
        // A ConceptualEvent or EquivalentEventSet might be sorted as a Pathway. So validAttribute
        // checking is necessary.
        if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent)) {
            List components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            handlePathwayComponents(components, bpPathway);
        }
        else if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
            List components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            handlePathwayComponents(components, bpPathway);
        }
        handleTaxon(pathway, bpPathway, ReactomeJavaConstants.species);
    }
    
    private void handleTaxon(GKInstance rInstance, 
                             Element bpInstance, 
                             String taxonAttName) throws Exception {
        if (bpInstance.getName().equals(BioPAXJavaConstants.physicalEntity) ||
            // In case there is an error in Reactome (e.g. species in a smallMolecule DefinedSet)    
            bpInstance.getName().equals(BioPAXJavaConstants.smallMolecule)) 
            return; // A physicalEntity instance cannot have ORGANISM value
        // A check in case the taxon name is not correct
        if (!rInstance.getSchemClass().isValidAttribute(taxonAttName))
            return;
        GKInstance taxon = (GKInstance) rInstance.getAttributeValue(taxonAttName);
        if (taxon == null)
            return;
        Element taxonElm = createTaxonBPIndividual(taxon);
        if (taxonElm != null) {
            createObjectPropElm(bpInstance, BioPAXJavaConstants.ORGANISM, taxonElm);
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
        bpTaxon = createIndividualElm(BioPAXJavaConstants.bioSource);
        rToBInstanceMap.put(taxon, bpTaxon);
        // Extract name from taxon to bpTaxon
        createDataPropElm(bpTaxon, BioPAXJavaConstants.NAME, BioPAXJavaConstants.XSD_STRING, taxon.getDisplayName());
        // Extract crossReference to TAXON-XREF
        GKInstance crossRef = (GKInstance) taxon.getAttributeValue(ReactomeJavaConstants.crossReference);
        if (crossRef != null) {
            GKInstance db = (GKInstance) crossRef.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
            String identifier = (String) crossRef.getAttributeValue(ReactomeJavaConstants.identifier);
            Element bpXref = createIndividualElm(BioPAXJavaConstants.unificationXref);
            if (db != null) {
                createDataPropElm(bpXref, 
                                  BioPAXJavaConstants.DB, 
                                  BioPAXJavaConstants.XSD_STRING,
                                  db.getDisplayName());
            }
            if (identifier != null) {
                createDataPropElm(bpXref,
                                  BioPAXJavaConstants.ID,
                                  BioPAXJavaConstants.XSD_STRING,
                                  identifier);
            }
            createObjectPropElm(bpTaxon, BioPAXJavaConstants.TAXON_XREF, bpXref);
        }
        return bpTaxon;
    }
    
    private Element createIndividualElm(String elmName) {
        String id = idGenerator.generateOWLID(elmName);
        return ReactomeToBioPAXUtilities.createIndividualElm(elmName, 
                                                             id, 
                                                             bpNS,
                                                             rdfNS,
                                                             rootElm);
    }
    
    private Element createObjectPropElm(Element domainElm,
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
    
    private Element createDataPropElm(Element domainElm,
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
                                  BioPAXJavaConstants.COMMENT,
                                  BioPAXJavaConstants.XSD_STRING,
                                  text);
            }
            // Create a new evidence based on LiteratureReference instances in Summation
            //Element bpEvidence = createEvidenceFromSummation(summation);
            //createObjectPropElm(bpEvent, BioPAXJavaConstants.EVIDENCE, bpEvidence);
        }
    }
    
//    private Element createEvidenceFromSummation(GKInstance summation) throws Exception {
//        Element bpEvidence = (Element) rToBInstanceMap.get(summation);
//        if (bpEvidence != null)
//            return bpEvidence;
//        String id = "Summation_" + summation.getDBID();
//        id = generateOWLID(id);
//        bpEvidence = createIndividualElm(BioPAXJavaConstants.evidence, id);
//        rToBInstanceMap.put(summation, bpEvidence);
//        // Use LiteratureReference as XREF with type pubXref for evidence
//        List literatures = summation.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
//        if (literatures != null && literatures.size() > 0) {
//            for (Iterator it1 = literatures.iterator(); it1.hasNext();) {
//                GKInstance reference = (GKInstance) it1.next();
//                Element pubXref = convertLiteratureReferenceToPublicationXref(reference);
//                if (pubXref != null) 
//                    createObjectPropElm(bpEvidence, BioPAXJavaConstants.XREF, pubXref);
//            }
//        }    
//        return bpEvidence;
//    }
    
    private Element convertLiteratureReferenceToPublicationXref(GKInstance literatureReference) throws Exception {
        Element pubXrefIndividual = (Element) rToBInstanceMap.get(literatureReference);
        if (pubXrefIndividual != null)
            return pubXrefIndividual;
        ReactomeToBioPAXPublicationConverter helper = new ReactomeToBioPAXPublicationConverter();
        helper.setIsForLevel2(true);
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
        Long DBID = gkInstance.getDBID();
        Element xref = (Element) idToXrefMap.get(DBID);
        if (xref == null) {
            xref = createIndividualElm(BioPAXJavaConstants.unificationXref);
            idToXrefMap.put(DBID, xref);
            // Get the current database name
            if (currentDbName == null) {
                currentDbName = ReactomeToBioPAXUtilities.getCurrentReleaseDbName(gkInstance);
            }
            createDataPropElm(xref, 
                              BioPAXJavaConstants.DB, 
                              BioPAXJavaConstants.XSD_STRING, 
                              currentDbName);
            createDataPropElm(xref, BioPAXJavaConstants.ID, BioPAXJavaConstants.XSD_STRING, DBID.toString());
            String comment = "Database identifier. Use this URL to connect to the web page of this " +
                             "instance in Reactome: " +
                             "http://www.reactome.org/cgi-bin/eventbrowser?DB=gk_current&ID=" +
                             DBID.toString();
            createDataPropElm(xref, BioPAXJavaConstants.COMMENT, BioPAXJavaConstants.XSD_STRING, comment);
        }
        createObjectPropElm(bpInstance, BioPAXJavaConstants.XREF, xref);
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
            xref = createIndividualElm(BioPAXJavaConstants.unificationXref);
            stableIdToXrefMap.put(key, xref);
            createDataPropElm(xref,
                              BioPAXJavaConstants.DB, 
                              BioPAXJavaConstants.XSD_STRING,
                              BioPAXJavaConstants.REACTOME_STABLE_ID);
            createDataPropElm(xref, BioPAXJavaConstants.ID, BioPAXJavaConstants.XSD_STRING, identifier);
            createDataPropElm(xref,
                              BioPAXJavaConstants.ID_VERSION,
                              BioPAXJavaConstants.XSD_STRING,
                              version);
            String comment = "Reactome stable identifier. Use this URL to connect to the web page of this " +
                             "instance in Reactome: " +
                             "http://www.reactome.org/cgi-bin/eventbrowser_st_id?ST_ID=" +
                             key;
            createDataPropElm(xref, BioPAXJavaConstants.COMMENT, BioPAXJavaConstants.XSD_STRING, comment);
        }
        createObjectPropElm(bpInstance, BioPAXJavaConstants.XREF, xref);
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
                }
            }
        }
        if (pathwaySteps != null) {
            createObjectPropElm(bpPathway, BioPAXJavaConstants.PATHWAY_COMPONENTS, pathwaySteps);
        }
    }
    
    private Element createPathwayStep(GKInstance event, Element bpEvent) throws Exception {
        Element pathwayStep = (Element) eventToPathwayStepMap.get(event);
        if (pathwayStep != null) {
            return pathwayStep;
        }
        String id = idGenerator.generateOWLID(BioPAXJavaConstants.pathwayStep);
        pathwayStep = createIndividualElm(BioPAXJavaConstants.pathwayStep);
        createObjectPropElm(pathwayStep, BioPAXJavaConstants.STEP_INTERACTIONS, bpEvent);
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
    
    private List getInstancesInEntitySet(GKInstance entitySet) throws Exception {
        Set instances = new HashSet();
        Set current = new HashSet();
        Set next = new HashSet();
        current.add(entitySet);
        GKInstance tmp = null;
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                tmp = (GKInstance) it.next();
                if (tmp.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
                    List values = tmp.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                    if (values != null && values.size() > 0)
                        next.addAll(values);
                    else
                        instances.add(tmp);
                }
                else if (tmp.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
                    List values = tmp.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
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
    
    private void handleReferenceEntityNames(GKInstance rInstance, Element bpInstance) throws Exception {
        String displayName = rInstance.getDisplayName();
        if (displayName != null)
            createDataPropElm(bpInstance,
                              BioPAXJavaConstants.NAME,
                              BioPAXJavaConstants.XSD_STRING,
                              displayName);
        List names = rInstance.getAttributeValuesList(ReactomeJavaConstants.name);
        Set synonyms = new HashSet();
        if (names != null && names.size() > 0) {
            // Pick the first one as the short name
            String firstName = (String) names.get(0);
            createDataPropElm(bpInstance,
                              BioPAXJavaConstants.SHORT_NAME,
                              BioPAXJavaConstants.XSD_STRING,
                              firstName);
            for (int i = 1; i < names.size(); i++)
                synonyms.add(names.get(i));
        }
        // Use gene names as synonyms if applicable
        if (rInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.geneName)) {
            List geneNames = rInstance.getAttributeValuesList(ReactomeJavaConstants.geneName);
            if (geneNames != null) {
                for (Iterator it = geneNames.iterator(); it.hasNext();)
                    synonyms.add(it.next());
            }
        }
        if (synonyms.size() > 0) {
            createDataPropElm(bpInstance,
                              BioPAXJavaConstants.SYNONYMS,
                              BioPAXJavaConstants.XSD_STRING,
                              synonyms);
        }
    }
    
    private void handleNames(GKInstance reactomeInstance, Element bpInstance) throws Exception {
        if (reactomeInstance.getDisplayName() != null) {
            createDataPropElm(bpInstance,
                              BioPAXJavaConstants.NAME,
                              BioPAXJavaConstants.XSD_STRING,
                              reactomeInstance.getDisplayName());
        }
        // Use the first value in as short name
        List names = reactomeInstance.getAttributeValuesList(ReactomeJavaConstants.name);
        if (names.size() > 0) {
            String firstName = (String) names.get(0);
            createDataPropElm(bpInstance,
                              BioPAXJavaConstants.SHORT_NAME,
                              BioPAXJavaConstants.XSD_STRING,
                              firstName);
        }
        // Add all other names to SYNONYM
        if (names.size() > 1) {
            List synonyms = new ArrayList(names.size() - 1);
            for (int i = 1; i < names.size(); i++) {
                synonyms.add(names.get(i));
            }
            createDataPropElm(bpInstance, 
                              BioPAXJavaConstants.SYNONYMS,
                              BioPAXJavaConstants.XSD_STRING,
                              synonyms);
        }
    }
        
    private void handleReaction(GKInstance reaction) throws Exception {
        Element bpReaction = createIndividualElm(BioPAXJavaConstants.biochemicalReaction);
        rToBInstanceMap.put(reaction, bpReaction);
        // Get the input information
        List inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        if (inputs != null && inputs.size() > 0) {
            Map inputMap = getStoichiometryMap(inputs);
            GKInstance input = null;
            Element bpInput = null;
            Integer stoi = null;
            for (Iterator it = inputMap.keySet().iterator(); it.hasNext();) {
                input = (GKInstance) it.next();
                bpInput = createEntityParticipant(input);
                stoi = (Integer) inputMap.get(input);
                // Cannot distinguish 1 or unknown in the Reactome data model
                createDataPropElm(bpInput, 
                                  BioPAXJavaConstants.STOICHIOMETRIC_COEFFICIENT,
                                  BioPAXJavaConstants.XSD_DOUBLE,
                                  stoi);
                createObjectPropElm(bpReaction, BioPAXJavaConstants.LEFT, bpInput);
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
                bpOutput = createEntityParticipant(output);
                stoi = (Integer) outputMap.get(output);
                createDataPropElm(bpOutput,
                                  BioPAXJavaConstants.STOICHIOMETRIC_COEFFICIENT,
                                  BioPAXJavaConstants.XSD_DOUBLE,
                                  stoi);
                createObjectPropElm(bpReaction, BioPAXJavaConstants.RIGHT, bpOutput);
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
    }
    
    private void extractECNumberForReaction(GKInstance ca,
                                            Element bpReaction) throws Exception {
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
                          BioPAXJavaConstants.EC_NUMBER,
                          BioPAXJavaConstants.XSD_STRING,
                          ecNumbers);
    }
    
    private Element handleCatalystActivity(GKInstance ca, 
                                           Element bpControlled,
                                           GKInstance rReaction) throws Exception {
        Element bpCatalyst = createIndividualElm(BioPAXJavaConstants.catalysis);
        GKInstance controller = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
        if (controller != null) {
            Element bpController = createEntityParticipant(controller);
            createObjectPropElm(bpCatalyst, BioPAXJavaConstants.CONTROLLER, bpController);
        }
        createObjectPropElm(bpCatalyst, BioPAXJavaConstants.CONTROLLED, bpControlled);
        createDataPropElm(bpCatalyst,
                          BioPAXJavaConstants.DIRECTION,
                          BioPAXJavaConstants.XSD_STRING,
                          "PHYSIOL-LEFT-TO-RIGHT");
        // CONTROL-TYPE value should be ACTIVATION: This is required
        createDataPropElm(bpCatalyst,
                          BioPAXJavaConstants.CONTROL_TYPE,
                          BioPAXJavaConstants.XSD_STRING,
                          "ACTIVATION");
        // Based on activity to create a xref to point a GO term
        GKInstance activity = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.activity);
        if (activity != null) {
            Element bpXref = createGOOWL(activity, "GO molecular function");
            createObjectPropElm(bpCatalyst, BioPAXJavaConstants.XREF, bpXref);
        }
        // Need to handle reverse attributes for regulation here.
        handleRegulationForCatalystActivity(ca, bpCatalyst, rReaction);
        attachReactomeIDAsXref(ca, bpCatalyst);
        attachReactomeDatasource(bpCatalyst);
        return bpCatalyst;
    }
    
    private Element createEntityParticipant(GKInstance rEntity) throws Exception {
        // Although the biopax doc suggests that each reaction should use its own entityParticipant, however,
        // from the Reactome's view, it will be nice to share entityParticipant in several reactions.
        // But since a stoichiometry is used in the reaction, indeed, it should NOT be reused.
        //OWLIndividual bpEntityParticipant = (OWLIndividual) rToBInstanceMap.get(rEntity);
        //if (bpEntityParticipant != null)
        //    return bpEntityParticipant;
        //String id = generateOWLIDFromDisplayName(rEntity);
        boolean isGeneric = rEntity.getSchemClass().isa(ReactomeJavaConstants.EntitySet);
        SchemaClass reactomeType = getReactomeEntityType(rEntity);
        Element bpEntityParticipant = null;
        if (rEntity.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
            bpEntityParticipant = createIndividualElm(BioPAXJavaConstants.physicalEntityParticipant);
        }
        // Try to figure out the type based on the referenceEntity
        else if (reactomeType == null) { // For Polymer, OtherEntity and other entities which don't have referenceEntity values
            bpEntityParticipant = createIndividualElm(BioPAXJavaConstants.physicalEntityParticipant);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceGeneProduct) ||
                 reactomeType.isa(ReactomeJavaConstants.ReferencePeptideSequence)) {
            // This is a protein
            bpEntityParticipant = createIndividualElm(BioPAXJavaConstants.sequenceParticipant);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceMolecule) ||
                 reactomeType.isa(ReactomeJavaConstants.ReferenceGroup) ||
                 reactomeType.isa(ReactomeJavaConstants.ReferenceMoleculeClass)) {
            bpEntityParticipant = createIndividualElm(BioPAXJavaConstants.physicalEntityParticipant);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceDNASequence)) {
            bpEntityParticipant = createIndividualElm(BioPAXJavaConstants.sequenceParticipant);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceRNASequence)) {
            bpEntityParticipant = createIndividualElm(BioPAXJavaConstants.sequenceParticipant);
        }
        else {
            bpEntityParticipant = createIndividualElm(BioPAXJavaConstants.physicalEntityParticipant);
        }
        if (isGeneric) {
            // Add a comment. No other good ways to indicate that this BioPAX 
            // entity is converted from EntitySet in the Reactome.
            createDataPropElm(bpEntityParticipant, 
                              BioPAXJavaConstants.COMMENT, 
                              BioPAXJavaConstants.XSD_STRING, 
                              "Converted from EntitySet in Reactome");
        }
        // Get the compartment information
        handleCompartment(rEntity, bpEntityParticipant);
        handleHasDomain(rEntity, bpEntityParticipant);
        handleReferenceEntity(rEntity, bpEntityParticipant);
        handleModifiedResidue(rEntity, bpEntityParticipant);
        handleStartAndEndCoordinates(rEntity, bpEntityParticipant);
        exportDbIdAsComment(rEntity, bpEntityParticipant);
        //rToBInstanceMap.put(rEntity, bpEntityParticipant);
        return bpEntityParticipant;
    }
    
    private void exportDbIdAsComment(GKInstance rEntity,
                                     Element bpEntityParticipant) {
        String comment = "Reactome DB_ID: " + rEntity.getDBID();
        createDataPropElm(bpEntityParticipant, 
                          BioPAXJavaConstants.COMMENT,
                          BioPAXJavaConstants.XSD_STRING,
                          comment);
    }
    
    private void handleStartAndEndCoordinates(GKInstance rEntity,
                                              Element bpEntityParticipant) throws Exception {
        if (!rEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.startCoordinate) ||
            !rEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.endCoordinate))
            return; // No need
        // Just a sanity check
        if (!bpEntityParticipant.getName().equals(BioPAXJavaConstants.sequenceParticipant))
            return;
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
        createObjectPropElm(bpEntityParticipant,
                            BioPAXJavaConstants.SEQUENCE_FEATURE_LIST,
                            lengthFeature);
    }
    
    private void handleModifiedResidue(GKInstance rEntity, 
                                       Element bpEntityParticipant) throws Exception {
        if (!rEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasModifiedResidue))
            return; // No need
        // Just a sanity check
        if (!bpEntityParticipant.getName().equals(BioPAXJavaConstants.sequenceParticipant))
            return;
        List modifiers = rEntity.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        if (modifiers == null || modifiers.size() == 0)
            return;
        GKInstance modifier = null;
        Element seqFeatureElm = null;
        for (Iterator it = modifiers.iterator(); it.hasNext();) {
            modifier = (GKInstance) it.next();
            seqFeatureElm = createSeqFeatureForModifier(modifier);
            createObjectPropElm(bpEntityParticipant,
                                BioPAXJavaConstants.SEQUENCE_FEATURE_LIST,
                                seqFeatureElm);
        }
    }
    
    private Element createSeqFeatureForFragment(int start,
                                                int end) throws Exception {
        // All SeqFeature instances cannot be reused.
        Element seqFeatureElm = createIndividualElm(BioPAXJavaConstants.sequenceFeature);
        // Need to add a feature type for seqFeaureElm -- required attribute
        if (lengthFeatureType == null) {
            initLengthFeatureType();
        }
        createObjectPropElm(seqFeatureElm,
                            BioPAXJavaConstants.FEATURE_TYPE,
                            lengthFeatureType);
        // Get the coordinates
        Element sequenceIntervalElm = createIndividualElm(BioPAXJavaConstants.sequenceInterval);
        Element startSite = createSequenceSite(start);
        Element endSite = createSequenceSite(end);
        createObjectPropElm(sequenceIntervalElm, 
                            BioPAXJavaConstants.SEQUENCE_INTERVAL_BEGIN, 
                            startSite);
        createObjectPropElm(sequenceIntervalElm,
                            BioPAXJavaConstants.SEQUENCE_INTERVAL_END,
                            endSite);
        createObjectPropElm(seqFeatureElm,
                            BioPAXJavaConstants.FEATURE_LOCATION,
                            sequenceIntervalElm);
        return seqFeatureElm;
    }
    
    private void initLengthFeatureType() {
        lengthFeatureType = createIndividualElm(BioPAXJavaConstants.openControlledVocabulary);
        createDataPropElm(lengthFeatureType, 
                          BioPAXJavaConstants.TERM,
                          BioPAXJavaConstants.XSD_STRING,
                          "Chain Coordinates");
    }

    private Element createSequenceSite(Integer position) {
        Element seqSiteElm = createIndividualElm(BioPAXJavaConstants.sequenceSite);
        createDataPropElm(seqSiteElm, 
                          BioPAXJavaConstants.SEQUENCE_POSITION, 
                          BioPAXJavaConstants.XSD_INTEGER,
                          position.toString());
        // Required
        createDataPropElm(seqSiteElm, 
                          BioPAXJavaConstants.POSITION_STATUS, 
                          BioPAXJavaConstants.XSD_STRING,
                          "EQUAL");
        return seqSiteElm;
    }
    
    private Element createSeqFeatureForModifier(GKInstance modifier) throws Exception {
        // All SeqFeature instances cannot be reused.
        Element seqFeatureElm = createIndividualElm(BioPAXJavaConstants.sequenceFeature);
        // Get the coordinate
        if (modifier.getSchemClass().isValidAttribute(ReactomeJavaConstants.coordinate)) {
            Integer position = (Integer) modifier.getAttributeValue(ReactomeJavaConstants.coordinate);
            if (position != null) {
                // Need to construct a seqfeatureSite
                Element seqSiteElm = createSequenceSite(position);
                // Attach SequenceSite
                createObjectPropElm(seqFeatureElm,
                                    BioPAXJavaConstants.FEATURE_LOCATION,
                                    seqSiteElm);
            }
        }
        // As of Nov 10, 2009, since new PSI-MOD has been used, values in psiMod for ModifiedResidue
        // will be used as Feature_Type via openControlledVocabulary
        if (modifier.getSchemClass().isValidAttribute(ReactomeJavaConstants.psiMod)) {
            GKInstance psiMod = (GKInstance) modifier.getAttributeValue(ReactomeJavaConstants.psiMod);
            if (psiMod != null) {
                Element featureTypeElm = createFeatureTypeFromPsiMod(psiMod);
                createObjectPropElm(seqFeatureElm,
                                    BioPAXJavaConstants.FEATURE_TYPE,
                                    featureTypeElm);
                if (modifier.getSchemClass().isValidAttribute(ReactomeJavaConstants.modification)) {
                    // values in ChEBI from modification will be used as values for XREF
                    GKInstance modification = (GKInstance) modifier.getAttributeValue(ReactomeJavaConstants.modification);
                    if (modification != null) {
                        Element xrefElm = createXREFFromRefEntity(modification,
                                                                  BioPAXJavaConstants.relationshipXref,
                                "modification");
                        if (xrefElm != null)
                            createObjectPropElm(seqFeatureElm,
                                                BioPAXJavaConstants.XREF,
                                                xrefElm);
                    }
                }
            }
            else if (modifier.getSchemClass().isValidAttribute(ReactomeJavaConstants.modification)) {
                // However, since feature type is required, if psiMod has not specified, the original ChEBI will
                // be used for modification.
                // Want to map ReferenceGroup or ReferenceMolecule as openControlledVocabulary back
                // to ChEBI.
                GKInstance modification = (GKInstance) modifier.getAttributeValue(ReactomeJavaConstants.modification);
                Element modificationElm = createFeatureTypeFromModification(modification);
                createObjectPropElm(seqFeatureElm, 
                                    BioPAXJavaConstants.FEATURE_TYPE, 
                                    modificationElm);
            }
        }
        // Add the displayname.
        String displayName = modifier.getDisplayName();
        createDataPropElm(seqFeatureElm, 
                          BioPAXJavaConstants.NAME, 
                          BioPAXJavaConstants.XSD_STRING,
                          displayName);
        // Attach the residue as comment to seqFeature. This residue should not
        // be attached to sequenceSite in case the site is not known.
        if (modifier.getSchemClass().isValidAttribute(ReactomeJavaConstants.residue)) {
            GKInstance residue = (GKInstance) modifier.getAttributeValue(ReactomeJavaConstants.residue);
            if (residue != null) {
                String comment = "residue: " + residue.getDisplayName();
                createDataPropElm(seqFeatureElm,
                                  BioPAXJavaConstants.COMMENT,
                                  BioPAXJavaConstants.XSD_STRING,
                                  comment);
            }
        }
        return seqFeatureElm;
    }
    
    private Element createFeatureTypeFromModification(GKInstance modification) throws Exception {
        Element ftElm = (Element) modificationToFeatTypeMap.get(modification);
        if (ftElm != null)
            return ftElm;
        // Use reactome for id since ChEBI is is used for unification
        ftElm = createIndividualElm(BioPAXJavaConstants.openControlledVocabulary);
        modificationToFeatTypeMap.put(modification,
                                      ftElm);
        // Use name for term
        String displayName = modification.getDisplayName();
        createDataPropElm(ftElm,
                          BioPAXJavaConstants.TERM, 
                          BioPAXJavaConstants.XSD_STRING, 
                          displayName);
        // Modification may be a SimpleEntity
        Element xref = null;
        if (modification.getSchemClass().isa(ReactomeJavaConstants.ReferenceEntity))
            xref = createXREFFromRefEntity(modification); // Use ChEBI for unification 
        else if (modification.getSchemClass().isa(ReactomeJavaConstants.SimpleEntity)) {
            // Need to get referenceEntity
            GKInstance referenceEntity = (GKInstance) modification.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (referenceEntity != null)
                xref = createXREFFromRefEntity(referenceEntity);
        }
        if (xref != null)
            createObjectPropElm(ftElm, 
                                BioPAXJavaConstants.XREF, 
                                xref);
        return ftElm;
    }
    
    private Element createFeatureTypeFromPsiMod(GKInstance psiMod) throws Exception {
        Element ftElm = (Element) modificationToFeatTypeMap.get(psiMod);
        if (ftElm != null)
            return ftElm;
        // Use reactome for id since ChEBI is is used for unification
        ftElm = createIndividualElm(BioPAXJavaConstants.openControlledVocabulary);
        modificationToFeatTypeMap.put(psiMod,
                                      ftElm);
        // Use name for term
        String displayName = psiMod.getDisplayName();
        createDataPropElm(ftElm,
                          BioPAXJavaConstants.TERM, 
                          BioPAXJavaConstants.XSD_STRING, 
                          displayName);
        // Modification may be a SimpleEntity
        // Borrow method for ReferenceEntity to create an xref element.
        Element xref = createXREFFromRefEntity(psiMod);
        if (xref != null)
            createObjectPropElm(ftElm, 
                                BioPAXJavaConstants.XREF, 
                                xref);
        return ftElm;
    }
    
    private void handleHasDomain(GKInstance rEntity, Element bpEntityParticipant) throws Exception {
        // Just a sanity check
        if (!bpEntityParticipant.getName().equals(BioPAXJavaConstants.sequenceParticipant))
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
            Element sequenceFeature = createSequenceFeatureFromDomain(domain);
            createObjectPropElm(bpEntityParticipant, BioPAXJavaConstants.SEQUENCE_FEATURE_LIST, sequenceFeature);
        }
    }
    
   /**
    * Only SequenceDomain is handled right now.
    * @param domain
    * @return
    * @throws Exception
    */
   private Element createSequenceFeatureFromDomain(GKInstance domain) throws Exception {
       if (!domain.getSchemClass().isa("SequenceDomain"))
           throw new IllegalStateException("ReactomeToBioConverter.createSequenceFeatureFromDomain(): Only SequenceDomain can be handled.");
       Element sequenceFeature = (Element) rToBInstanceMap.get(domain);
       if (sequenceFeature != null)
           return sequenceFeature;
       // Need to get ID
       sequenceFeature = createIndividualElm(BioPAXJavaConstants.sequenceFeature);
       handleNames(domain, sequenceFeature);
       Integer start = (Integer) domain.getAttributeValue(ReactomeJavaConstants.startCoordinate);
       Integer end = (Integer) domain.getAttributeValue(ReactomeJavaConstants.endCoordinate);
       Element sequenceSite = null;
       if (start != null && end != null && start.equals(end)) {
           // SequenceSite will be used for featureLocation property.
           sequenceSite = createIndividualElm(BioPAXJavaConstants.sequenceSite);
           createDataPropElm(sequenceSite, 
                             BioPAXJavaConstants.SEQUENCE_POSITION,
                             BioPAXJavaConstants.XSD_UNSIGNEDLONG, 
                             start);
       }
       else {
           sequenceSite = createIndividualElm(BioPAXJavaConstants.sequenceInterval);
           if (start != null) {
               Element ss1 = createIndividualElm(BioPAXJavaConstants.sequenceSite);
               createDataPropElm(ss1,
                                 BioPAXJavaConstants.SEQUENCE_POSITION,
                                 BioPAXJavaConstants.XSD_UNSIGNEDLONG,
                                 start);
               createObjectPropElm(sequenceSite, BioPAXJavaConstants.SEQUENCE_INTERVAL_BEGIN, ss1);
           }
           if (end != null) {    
               Element ss1 = createIndividualElm(BioPAXJavaConstants.sequenceSite);
               createDataPropElm(ss1,
                                 BioPAXJavaConstants.SEQUENCE_POSITION,
                                 BioPAXJavaConstants.XSD_UNSIGNEDLONG,
                                 end);
               createObjectPropElm(sequenceSite, BioPAXJavaConstants.SEQUENCE_INTERVAL_END, ss1);
           }
       }
       createObjectPropElm(sequenceFeature, BioPAXJavaConstants.FEATURE_LOCATION, sequenceSite);
       rToBInstanceMap.put(domain, sequenceFeature);
       return sequenceFeature;
   }
    
    private SchemaClass getReactomeEntityType(GKInstance rEntity) throws Exception {
        // Peek referenceEntity attribute. The type should be determined by it.
        if (rEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
            GKInstance referenceEntity = (GKInstance) rEntity.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (referenceEntity != null)
                return referenceEntity.getSchemClass();
        }
        // Check EntitySet
        if (rEntity.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
            List instances = getInstancesInEntitySet(rEntity);
            Set<SchemaClass> types = new HashSet<SchemaClass>();
            GKInstance instance = null;
            GKInstance referenceEntity = null;
            for (Iterator it = instances.iterator(); it.hasNext();) {
                instance = (GKInstance) it.next();
                if (instance.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
                    types.add(instance.getSchemClass());
                    continue;
                }
                if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity))
                    continue;
                referenceEntity = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceEntity);
                if (referenceEntity != null)
                    types.add(referenceEntity.getSchemClass());
            }
            if (types.size() == 0 || types.size() > 1)
                return null;
            else
                return (SchemaClass) types.iterator().next();
        }
        return null;
    }
    
    private void handleReferenceEntityForEntitySet(GKInstance entitySet,
                                                   Element bpEntityParticipant) throws Exception {
        Element bpEntity = (Element) rEntityToBEntityMap.get(entitySet);
        if (bpEntity != null) {
            createObjectPropElm(bpEntityParticipant, 
                                BioPAXJavaConstants.PHYSICAL_ENTITY,
                                bpEntity);
            return;
        }
        bpEntity = createBEntityFromREntity(entitySet);
        createObjectPropElm(bpEntityParticipant, BioPAXJavaConstants.PHYSICAL_ENTITY, bpEntity);
        createDataPropElm(bpEntity,
                          BioPAXJavaConstants.NAME,
                          BioPAXJavaConstants.XSD_STRING,
                          entitySet.getDisplayName());
        handleTaxon(entitySet, bpEntity, ReactomeJavaConstants.species);
        createDataPropElm(bpEntity,
                          BioPAXJavaConstants.COMMENT,
                          BioPAXJavaConstants.XSD_STRING,
                          "Converted from EntitySet in Reactome. " +
        		             "Each synonym is a name of a PhysicalEntity, " +
        		             "and each XREF points to one PhysicalEntity");
        List instances = getInstancesInEntitySet(entitySet);
        GKInstance instance = null;
        for (Iterator it = instances.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity))
                continue;
            GKInstance refEntity = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (refEntity == null)
                continue;
            Element xref = createXREFFromRefEntity(refEntity);
            if (xref != null) {
                createObjectPropElm(bpEntity, BioPAXJavaConstants.XREF, xref);
                //Only record name for non-null XREF
                createDataPropElm(bpEntity, 
                                  BioPAXJavaConstants.SYNONYMS, 
                                  BioPAXJavaConstants.XSD_STRING,
                                  instance.getDisplayName());
            }
        }
        rEntityToBEntityMap.put(entitySet, bpEntity);
    }
    
    private Element createBEntityFromREntity(GKInstance rEntity) throws Exception {
        Element bpEntity = null;
        SchemaClass reactomeType = getReactomeEntityType(rEntity);
        if (reactomeType == null)
            bpEntity = createIndividualElm(BioPAXJavaConstants.physicalEntity);
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceGeneProduct) ||
                 reactomeType.isa(ReactomeJavaConstants.ReferencePeptideSequence)) {
            // This is a protein
            bpEntity = createIndividualElm(BioPAXJavaConstants.protein);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceMolecule) || 
                reactomeType.isa(ReactomeJavaConstants.ReferenceGroup) ||
                reactomeType.isa(ReactomeJavaConstants.ReferenceMoleculeClass)) {
            bpEntity = createIndividualElm(BioPAXJavaConstants.smallMolecule);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceDNASequence)) {
            bpEntity = createIndividualElm(BioPAXJavaConstants.dna);
        }
        else if (reactomeType.isa(ReactomeJavaConstants.ReferenceRNASequence)) {
            bpEntity = createIndividualElm(BioPAXJavaConstants.rna);
        }
        else {
            bpEntity = createIndividualElm(BioPAXJavaConstants.physicalEntity);
        }
        attachReactomeDatasource(bpEntity);
        return bpEntity;
    }
    
    private void handleNullReferenceEntityForEntity(GKInstance rEntity, Element bpEntityParticipant) throws Exception {
        Element bpEntity = (Element) rEntityToBEntityMap.get(rEntity);
        if (bpEntity == null) {
            bpEntity = createIndividualElm(BioPAXJavaConstants.physicalEntity);
            rEntityToBEntityMap.put(rEntity, bpEntity);
            handleReferenceEntityNames(rEntity, bpEntity);
            attachReactomeDatasource(bpEntity);
        }
        createObjectPropElm(bpEntityParticipant, BioPAXJavaConstants.PHYSICAL_ENTITY, bpEntity);
    }
    
    private void handleReferenceEntity(GKInstance rEntity, Element bpEntityParticipant) throws Exception {
        if (rEntity.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
            handleComplexInComplexParticipant(rEntity, bpEntityParticipant);
            return;
        }
        // Handle EntitySet
        if (rEntity.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
            handleReferenceEntityForEntitySet(rEntity, bpEntityParticipant);
            return;
        }
        if (!rEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity) ||
             rEntity.getAttributeValue(ReactomeJavaConstants.referenceEntity) == null) {
            handleNullReferenceEntityForEntity(rEntity, bpEntityParticipant);
            return; // Cannot do anything here
        }
        GKInstance referenceEntity = (GKInstance) rEntity.getAttributeValue(ReactomeJavaConstants.referenceEntity);
        Element bpEntity = (Element) rToBInstanceMap.get(referenceEntity);
        if (bpEntity != null) {
            createObjectPropElm(bpEntityParticipant,
                                BioPAXJavaConstants.PHYSICAL_ENTITY,
                                bpEntity);
            return;
        }
        // Need id for OWLIndividual
        // Need to create a new physicalEntity individual
        bpEntity = createBEntityFromREntity(rEntity);
        rToBInstanceMap.put(referenceEntity, bpEntity);
        handleTaxon(referenceEntity, bpEntity, ReactomeJavaConstants.species);
        handleReferenceEntityNames(referenceEntity, bpEntity);
        createObjectPropElm(bpEntityParticipant,
                            BioPAXJavaConstants.PHYSICAL_ENTITY,
                            bpEntity);
        // Create a unificationXref for bpEntity
        Element xref = createXREFFromRefEntity(referenceEntity);
        if (xref != null) {
            createObjectPropElm(bpEntity,
                                BioPAXJavaConstants.XREF,
                                xref);
        }
        // Get comment or description
        String comment = null;
        if (referenceEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.comment))
            comment = (String) referenceEntity.getAttributeValue(ReactomeJavaConstants.comment);
        if (comment == null && referenceEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.description))
            comment = (String) referenceEntity.getAttributeValue(ReactomeJavaConstants.description);
        if (comment != null) {
            createDataPropElm(bpEntity, 
                              BioPAXJavaConstants.COMMENT,
                              BioPAXJavaConstants.XSD_STRING,
                              comment);
        }
    }
    
    protected Element createXREFFromRefEntity(GKInstance referenceEntity) throws Exception {
        return createXREFFromRefEntity(referenceEntity, 
                                       BioPAXJavaConstants.unificationXref,
                                       null);
    }
    
    protected Element createXREFFromRefEntity(GKInstance referenceEntity,
                                              String xrefClsType,
                                              String relationType) throws Exception {
        if (!referenceEntity.getSchemClass().isa(ReactomeJavaConstants.ReferenceEntity))
            return null;
        // Make sure this method will work for ReferenceEntity only
        Element xref = (Element) refEntityToXrefMap.get(referenceEntity);
        if (xref != null ||
            refEntityToXrefMap.containsKey(referenceEntity)) // It might have been checked, but null is created.
            return xref;
        // Create a unificationXref for bpEntity
        // Should use variantIdentifier first
        String identifier = null;
        if (referenceEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier))
            identifier = (String) referenceEntity.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
        if (identifier == null) {
            identifier = (String) referenceEntity.getAttributeValue(ReactomeJavaConstants.identifier);
        }
        GKInstance refDB = (GKInstance) referenceEntity.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
        if (identifier != null && refDB != null) {
            String dbName = refDB.getDisplayName();
            String id = dbName + "_" + identifier;
            // It might be used??? A very weird thing
            xref = createIndividualElm(xrefClsType);
            createDataPropElm(xref, BioPAXJavaConstants.DB,
                              BioPAXJavaConstants.XSD_STRING, dbName);
            createDataPropElm(xref, BioPAXJavaConstants.ID,
                              BioPAXJavaConstants.XSD_STRING, identifier);
            if (relationType != null)
                createDataPropElm(xref, 
                                  BioPAXJavaConstants.RELATIONSHIP_TYPE, 
                                  BioPAXJavaConstants.XSD_STRING,
                                  relationType);
        }
        refEntityToXrefMap.put(referenceEntity, xref);
        return xref;
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
    
    private void handleComplexInComplexParticipant(GKInstance complex,
                                                   Element complexParticipant) throws Exception {
        Element bpComplex = (Element) rEntityToBEntityMap.get(complex);
        if (bpComplex == null) {
            bpComplex = createIndividualElm(BioPAXJavaConstants.complex);
            rEntityToBEntityMap.put(complex, bpComplex);
            // Get information on components
            List components = complex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            if (components != null && components.size() > 0) {
                // Need to figure out the stoichiometries
                Map compMap = getStoichiometryMap(components);
                for (Iterator it = compMap.keySet().iterator(); it.hasNext();) {
                    GKInstance comp = (GKInstance) it.next();
                    Element bpComp = createEntityParticipant(comp);
                    Integer stoi = (Integer) compMap.get(comp);
                    createDataPropElm(bpComp, 
                                      BioPAXJavaConstants.STOICHIOMETRIC_COEFFICIENT,
                                      BioPAXJavaConstants.XSD_DOUBLE,
                                      stoi);
                    createObjectPropElm(bpComplex,
                                        BioPAXJavaConstants.COMPONENTS,
                                        bpComp);
                }
            }
            // Handle taxon
            handleTaxon(complex, bpComplex, ReactomeJavaConstants.species);
            handleReferenceEntityNames(complex, bpComplex);
            attachReactomeIDAsXref(complex, bpComplex);
            attachReactomeDatasource(bpComplex);
        }
        createObjectPropElm(complexParticipant, BioPAXJavaConstants.PHYSICAL_ENTITY, bpComplex);
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
            createObjectPropElm(bpInstance, BioPAXJavaConstants.CELLULAR_LOCATION, bpCompartment);
            return;
        }
        // Need to create a new bpCompartment
        bpCompartment = createIndividualElm(BioPAXJavaConstants.openControlledVocabulary);
        rToBInstanceMap.put(compartment, bpCompartment);
        String term = (String) compartment.getAttributeValue(ReactomeJavaConstants.name);
        if (term != null) {
            createDataPropElm(bpCompartment, BioPAXJavaConstants.TERM, BioPAXJavaConstants.XSD_STRING, term);
        }
        // Need to create a xref for GO
        String accession = (String) compartment.getAttributeValue(ReactomeJavaConstants.accession);
        Element xref = createIndividualElm(BioPAXJavaConstants.unificationXref);
        createDataPropElm(xref, BioPAXJavaConstants.DB, BioPAXJavaConstants.XSD_STRING, "GO");
        createDataPropElm(xref, BioPAXJavaConstants.ID, BioPAXJavaConstants.XSD_STRING, accession);
        createObjectPropElm(bpCompartment, BioPAXJavaConstants.XREF, xref);
    }
    
    public Document getBioPAXModel() {
        return biopaxDoc;
    }
    
    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Usage java org.reactome.biopax.ReactomeToBioPAXConverter " +
            		"dbHost dbName dbUser dbPwd dbPort eventID");
            System.exit(1);
        }
        try {
            ReactomeToBioPAXXMLConverter converter = new ReactomeToBioPAXXMLConverter();
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
    
    @Test
    public void testConvert() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_central_102312",
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
//         Glycolysis to test Book publication
//        GKInstance topEvent = dba.fetchInstance(70171L);
        // Alternative complement activation to test URL publication
//        GKInstance topEvent = dba.fetchInstance(173736L);
        // Platelet degranulation to check EntitySet conversion: an EntitySet has mixed types of members.
//        GKInstance topEvent = dba.fetchInstance(114608L);
        
        // A mutated pathway in FGFR
//        GKInstance topEvent = dba.fetchInstance(1839094L);
        
        // A reaction containing a deletion fragment
        GKInstance topEvent = dba.fetchInstance(1248655L);
        
        ReactomeToBioPAXXMLConverter converter = new ReactomeToBioPAXXMLConverter();
        converter.setReactomeEvent(topEvent);
        converter.convert();
        Document biopaxModel = converter.getBioPAXModel();
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        String name = (String) topEvent.getAttributeValue(ReactomeJavaConstants.name);
        name = name.replaceAll("( )|(/)", "_");
        System.out.println("Name: " + name);
        FileOutputStream fos = new FileOutputStream("tmp/" + name + ".owl");
        outputter.output(biopaxModel, fos);
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
}

