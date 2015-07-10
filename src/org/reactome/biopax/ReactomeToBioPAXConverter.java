/*
 * Created on Jun 3, 2005
 */
package org.reactome.biopax;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;

import com.hp.hpl.jena.util.FileUtils;

import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.jena.parser.ProtegeOWLParser;
import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;


/**
 * This class is used to convert Reactome data model to BioPAX data model. It will take an 
 * Event GKInstance (Pathway or Reaction) and convert to OWLModel.
 * @author wgm
 * @deprecated use ReactomeToBioPAXXMLConverter. Using ProtegeOWL API for converting is too
 * slow, though it is pretty strictly.
 */
@SuppressWarnings("unchecked")
public class ReactomeToBioPAXConverter {
    // To hold biopax data
    private BioPAXFactory biopaxFactory;
    // The top level pathway or reaction to be converted
    private GKInstance topEvent;
    // A map from Reactome Instance to BioPAX Individuals
    private Map rToBInstanceMap;
    // A map from event to PathwayStep
    private Map eventToPathwayStepMap;
    // A map from reaction to catalysis 
    private Map rxtToControlMap;
    // A map from Event to Evidence. The Event instance
    // is used in inferredFrom slot in other Event instances
    private Map eventToEvidenceMap;
    // For XREF referring back to Reactome
    private Map idToXrefMap;
    // To keep track ID to avoid duplication
    private Set idSet;
    // To track complex and GenericEntity mapping to BioPAX PhysicalEntiy so that
    // PhysicalEntity in BioPAX can be reused. This map is differnt from rToBInstanceMap.
    // Instances in rToBInstanceMap are direct mappings.
    private Map rEntityToBEntityMap;
    
    public ReactomeToBioPAXConverter() throws Exception {
        init();
    }
    
    /**
     * The uri should be imported by some explicitly way.
     * @param uri
     * @throws Exception
     */
    private void initBioPAX() throws Exception {
        JenaOWLModel model = ProtegeOWL.createJenaOWLModel();
        model.getNamespaceManager().setDefaultNamespace(BioPAXFactory.BIOPAX_URL + "#");
        ProtegeOWLParser.addImport(model, new URI(BioPAXFactory.BIOPAX_URL));
        model.getDefaultOWLOntology().addImports(new URI(BioPAXFactory.BIOPAX_ONT_DOWLOAD_URL));
        biopaxFactory = new BioPAXFactory(model);
    }
    
    private void init() {
        rToBInstanceMap = new HashMap();
        idSet = new HashSet();
        eventToPathwayStepMap = new HashMap();
        rxtToControlMap = new HashMap();
        eventToEvidenceMap = new HashMap();
        idToXrefMap = new HashMap();
        rEntityToBEntityMap = new HashMap();
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
        initBioPAX();
        if (biopaxFactory == null)
            throw new IllegalStateException("ReactomeToBioPAXConverter.convert(): BioPAX ontology is not definied!");
        if (topEvent == null)
            throw new IllegalStateException("ReactomeToBioPAXConverter.convert(): Reactome event to be converted is not specified.");
        handleEvent(topEvent);
        // Do another rond to figure out NEXT-STEP in PATHWAY-STEPS
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
                                                     OWLIndividual bpCatalyst,
                                                     GKInstance rEvent) throws Exception {
        Collection regulations = ca.getReferers("regulatedEntity");
        if (regulations == null || regulations.size() == 0)
            return;
        GKInstance regulation = null;
        OWLIndividual modulation = null;
        OWLProperty prop = biopaxFactory.getOWLProperty("CONTROLLED");
        for (Iterator it = regulations.iterator(); it.hasNext();) {
            regulation = (GKInstance) it.next();
            modulation = createModulationFromRegulation(regulation);
            modulation.addPropertyValue(prop, bpCatalyst);
            List list = (List) rxtToControlMap.get(rEvent);
            if (list == null) {
                list = new ArrayList();
                rxtToControlMap.put(rEvent, list);
            }
            list.add(modulation);
        }
    }
    
    private void handleRegulation(GKInstance regulatedEntity, OWLIndividual bpEvent) throws Exception {
        Collection regulations = regulatedEntity.getReferers("regulatedEntity");
        if (regulations == null || regulations.size() == 0)
            return;
        GKInstance regulation = null;
        OWLIndividual modulation = null;
        OWLProperty prop = biopaxFactory.getOWLProperty("CONTROLLED");
        for (Iterator it = regulations.iterator(); it.hasNext();) {
            regulation = (GKInstance) it.next();
            modulation = createModulationFromRegulation(regulation);
            modulation.addPropertyValue(prop, bpEvent);
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
    private OWLIndividual createModulationFromRegulation(GKInstance regulation) throws Exception {
        String id = getOWLIDFromDisplayName(regulation);
        OWLIndividual modulation = (OWLIndividual) biopaxFactory.getmodulationClass().createInstance(id);
        handleNames(regulation, modulation);
        String type = getControlTypeFromRegulation(regulation);
        if (type != null) {
            OWLProperty prop = biopaxFactory.getOWLProperty("CONTROL-TYPE");
            modulation.setPropertyValue(prop, type);
        }
        // Need to handle summation
        handleEventSummation(regulation, modulation);
        // Need to handle literatureReference
        handleEventLiteratureReferences(regulation, modulation);
        // Need to handle regulator
        GKInstance regulator = (GKInstance) regulation.getAttributeValue("regulator");
        if (regulator != null && regulator.getSchemClass().isa("PhysicalEntity")) {
            OWLIndividual entityParticipant = createEntityParticipant(regulator);
            if (entityParticipant != null) {
                OWLProperty prop = biopaxFactory.getOWLProperty("CONTROLLER");
                modulation.setPropertyValue(prop, entityParticipant);
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
        GKInstance regulationType = (GKInstance) regulation.getAttributeValue("regulationType");
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
        if (regulation.getSchemClass().isa("PositiveRegulation"))
            return "ACTIVATION";
        else if (regulation.getSchemClass().isa("NegativeRegulation"))
            return "INHIBITION";
        else
            return null;
    }
    
    private void handlePathwaySteps() throws Exception {
        GKInstance event = null;
        OWLIndividual pathwayStep = null;
        List precedingEvents = null;
        GKInstance precedingEvent = null;
        OWLIndividual prevPathwayStep = null;
        OWLProperty nextStepProp = biopaxFactory.getOWLProperty("NEXT-STEP");
        for (Iterator it = eventToPathwayStepMap.keySet().iterator(); it.hasNext();) {
            event = (GKInstance) it.next();
            precedingEvents = event.getAttributeValuesList("precedingEvent");
            if (precedingEvents == null || precedingEvents.size() == 0)
                continue;
            // Handle next step
            pathwayStep = (OWLIndividual) eventToPathwayStepMap.get(event);
            for (Iterator it1 = precedingEvents.iterator(); it1.hasNext();) {
                precedingEvent = (GKInstance) it1.next();
                prevPathwayStep = (OWLIndividual) eventToPathwayStepMap.get(precedingEvent);
                if (prevPathwayStep != null)
                    prevPathwayStep.addPropertyValue(nextStepProp, pathwayStep);
            }
        }
        OWLProperty stepInteractionsProp = biopaxFactory.getOWLProperty("STEP-INTERACTIONS");
        List list = null;
        for (Iterator it = rxtToControlMap.keySet().iterator(); it.hasNext();) {
            event = (GKInstance) it.next();
            pathwayStep = (OWLIndividual) eventToPathwayStepMap.get(event);
            if (pathwayStep == null) // A reaction might not be contained by a PathwayStep.
                continue;            // E.g. some orphan reactions.
            list = (List) rxtToControlMap.get(event);
            for (Iterator it1 = list.iterator(); it1.hasNext();) {
                pathwayStep.addPropertyValue(stepInteractionsProp, it1.next());
            }
        }
    }
    
    private void handleEvent(GKInstance event) throws Exception {
        if (event.getSchemClass().isa("Pathway"))
            handlePathway(event);
        else
            handleReaction(event);
        OWLIndividual bpEvent = (OWLIndividual) rToBInstanceMap.get(event);
        if (bpEvent == null)
            return;
        handleNames(event, bpEvent);
        attachReactomeIDAsXref(event, bpEvent);
        handleEventSummation(event, bpEvent);
        handleEventLiteratureReferences(event, bpEvent);
        handleEventGOBP(event, bpEvent);
        handleCompartment(event, bpEvent);
        handleInferredFrom(event, bpEvent);
        handleRegulation(event, bpEvent);
    }
    
    private void handleInferredFrom(GKInstance event, OWLIndividual bpEvent) throws Exception {
        List ifInstances = event.getAttributeValuesList("inferredFrom");
        if (ifInstances == null || ifInstances.size() == 0)
            return;
        OWLProperty prop = biopaxFactory.getOWLProperty("EVIDENCE");
        GKInstance ifInstance = null;
        OWLIndividual bpEvidence = null;
        for (Iterator it = ifInstances.iterator(); it.hasNext();) {
            ifInstance = (GKInstance) it.next();
            bpEvidence = createEvidenceFromInferredFrom(ifInstance);
            bpEvent.addPropertyValue(prop, bpEvidence);
        }
    }
    
    private OWLIndividual createEvidenceFromInferredFrom(GKInstance ifInstance) throws Exception {
        OWLIndividual bpEvidence = (OWLIndividual) eventToEvidenceMap.get(ifInstance);
        if (bpEvidence != null)
            return bpEvidence;
        String id = "InferredFrom_" + ifInstance.getDisplayName();
        id = generateOWLID(id);
        bpEvidence = (OWLIndividual) biopaxFactory.getevidenceClass().createInstance(id);
        eventToEvidenceMap.put(ifInstance, bpEvidence);
        // Check if there is Summation in ifInstance
        GKInstance summation = (GKInstance) ifInstance.getAttributeValue("summation");
        // Get all pubMed LiteratureReference
        Set literatureReferences = new HashSet();
        List list = ifInstance.getAttributeValuesList("literatureReference");
        if (list != null)
            literatureReferences.addAll(list);
        if (summation != null) {
            list = summation.getAttributeValuesList("literatureReference");
            if (list != null)
                literatureReferences.addAll(list);
        }
        // Convert LiteratureReference to publicationXref
        if (literatureReferences.size() > 0) {
            OWLIndividual bpXref = null;
            GKInstance literatureReference = null;
            OWLProperty prop = biopaxFactory.getOWLProperty("XREF");
            for (Iterator it = literatureReferences.iterator(); it.hasNext();) {
                literatureReference = (GKInstance) it.next();
                bpXref = convertLiteratureReferenceToPublicationXref(literatureReference);
                if (bpXref != null)
                    bpEvidence.addPropertyValue(prop, bpXref);
            }
        }
        // Add comments to evidence
        OWLProperty prop = biopaxFactory.getOWLProperty("COMMENT");
        if (summation != null) {
            String text = (String) summation.getAttributeValue("text");
            if (text != null)
                bpEvidence.addPropertyValue(prop, text);
        }
        return bpEvidence;
    }
    
    private void handleEventGOBP(GKInstance event, OWLIndividual bpEvent) throws Exception {
        GKInstance goBP = (GKInstance) event.getAttributeValue("goBiologicalProcess");
        if (goBP == null)
            return;
        OWLIndividual goBPOWL = createGOOWL(goBP);
        if (goBPOWL != null) {
            OWLProperty xrefProp = biopaxFactory.getOWLProperty("XREF");
            bpEvent.addPropertyValue(xrefProp, goBPOWL);
        }
    }
    
    private OWLIndividual createGOOWL(GKInstance goInstance) throws Exception {
        OWLIndividual goOWL = (OWLIndividual) rToBInstanceMap.get(goInstance);
        if (goOWL != null)
            return goOWL;
        String owlID = getOWLIDFromDisplayName(goInstance);
        goOWL = (OWLIndividual) biopaxFactory.getrelationshipXrefClass().createRDFIndividual(owlID);
        OWLProperty prop = biopaxFactory.getOWLProperty("DB");
        goOWL.addPropertyValue(prop, "GO");
        String identifier = (String) goInstance.getAttributeValue("accession");
        if (identifier != null) {
            prop = biopaxFactory.getOWLProperty("ID");
            goOWL.addPropertyValue(prop, identifier);
        }
        rToBInstanceMap.put(goInstance, goOWL);
        return goOWL;
    }
    
    private void handleEventLiteratureReferences(GKInstance event, OWLIndividual bpEvent) throws Exception {
        List references = event.getAttributeValuesList("literatureReference");
        if (references == null || references.size() == 0)
            return;
        GKInstance reference = null;
        OWLProperty xrefProp = biopaxFactory.getOWLProperty("XREF");
        for (Iterator it = references.iterator(); it.hasNext();) {
            reference = (GKInstance) it.next();
            OWLIndividual pubXref = convertLiteratureReferenceToPublicationXref(reference);
            if (pubXref != null) {
                bpEvent.addPropertyValue(xrefProp, pubXref);
            }
        }
    }
    
    private void handlePathway(GKInstance pathway) throws Exception {
        OWLNamedClass owlCls = (OWLNamedClass) biopaxFactory.getpathwayClass();
        String id = getOWLIDFromDisplayName(pathway);
        OWLIndividual bpPathway = owlCls.createOWLIndividual(id);
        rToBInstanceMap.put(pathway, bpPathway);
        List components = pathway.getAttributeValuesList("hasComponent");
        handlePathwayComponents(components, bpPathway);
        handleTaxon(pathway, bpPathway, "taxon");
    }
    
    private void handleTaxon(GKInstance pathway, OWLIndividual bpPathway, String taxonAttName) throws Exception {
        if (!pathway.getSchemClass().isValidAttribute(taxonAttName))
            return;
        GKInstance taxon = (GKInstance) pathway.getAttributeValue(taxonAttName);
        if (taxon == null)
            return;
        OWLIndividual bpTaxon = createTaxonBPIndividual(taxon);
        if (bpTaxon != null) {
            OWLProperty taxonProp = biopaxFactory.getOWLProperty("ORGANISM");
            bpPathway.addPropertyValue(taxonProp, bpTaxon);
        }
    }
    
    private OWLIndividual createTaxonBPIndividual(GKInstance taxon) throws Exception {
        OWLIndividual bpTaxon = (OWLIndividual) rToBInstanceMap.get(taxon);
        if (bpTaxon != null) {
            return bpTaxon;
        }
        String id = getOWLIDFromDisplayName(taxon);
        bpTaxon = (OWLIndividual) biopaxFactory.getbioSourceClass().createRDFIndividual(id);
        // Extract name from taxon to bpTaxon
        OWLProperty nameProp = biopaxFactory.getOWLProperty("NAME");
        bpTaxon.setPropertyValue(nameProp, taxon.getDisplayName());
        // Extract crossReference to TAXON-XREF
        GKInstance crossRef = (GKInstance) taxon.getAttributeValue("crossReference");
        if (crossRef != null) {
            GKInstance db = (GKInstance) crossRef.getAttributeValue("referenceDatabase");
            String identifier = (String) crossRef.getAttributeValue("identifier");
            id = getOWLIDFromDisplayName(crossRef);
            OWLIndividual bpXref = (OWLIndividual) biopaxFactory.getxrefClass().createRDFIndividual(id);
            OWLProperty prop = null;
            if (db != null) {
                prop = biopaxFactory.getOWLProperty("DB");
                bpXref.addPropertyValue(prop, db.getDisplayName());
            }
            if (identifier != null) {
                prop = biopaxFactory.getOWLProperty("ID");
                bpXref.addPropertyValue(prop, identifier);
            }
            prop = biopaxFactory.getOWLProperty("COMMENT");
            bpXref.addPropertyValue(prop, taxon.getDisplayName());
            prop = biopaxFactory.getOWLProperty("TAXON-XREF");
            bpTaxon.addPropertyValue(prop, bpXref);
        }
        rToBInstanceMap.put(taxon, bpTaxon);       
        return bpTaxon;
    }
    
    private void handleEventSummation(GKInstance gkEvent, OWLIndividual bpEvent) throws Exception {
        List summationInstances = gkEvent.getAttributeValuesList("summation");
        if (summationInstances == null || summationInstances.size() == 0)
            return;
        GKInstance summation = null;
        OWLIndividual bpEvidence = null;
        OWLProperty evidenceProp = biopaxFactory.getOWLProperty("EVIDENCE");
        for (Iterator it = summationInstances.iterator(); it.hasNext();) {
            summation = (GKInstance) it.next();
            // Use the text in Summation as comment
            String text = (String) summation.getAttributeValue("text");
            if (text != null && text.trim().length() > 0) {
                OWLProperty commentProp = biopaxFactory.getOWLProperty("COMMENT");
                bpEvent.addPropertyValue(commentProp, text);
            }
            // Create a new evidence based on LiteratureReference instances in Summation
            bpEvidence = createEvidenceFromSummation(summation);
            bpEvent.addPropertyValue(evidenceProp, bpEvidence);
        }
    }
    
    private OWLIndividual createEvidenceFromSummation(GKInstance summation) throws Exception {
        OWLIndividual bpEvidence = (OWLIndividual) rToBInstanceMap.get(summation);
        if (bpEvidence != null)
            return bpEvidence;
        String id = "Summation_" + summation.getDBID();
        id = generateOWLID(id);
        bpEvidence = (OWLIndividual) biopaxFactory.getevidenceClass().createInstance(id);
        rToBInstanceMap.put(summation, bpEvidence);
        // Use LiteratureReference as XREF with type pubXref for evidence
        List literatures = summation.getAttributeValuesList("literatureReference");
        if (literatures != null && literatures.size() > 0) {
            OWLProperty xrefProp = biopaxFactory.getOWLProperty("XREF");
            for (Iterator it1 = literatures.iterator(); it1.hasNext();) {
                GKInstance reference = (GKInstance) it1.next();
                OWLIndividual pubXref = convertLiteratureReferenceToPublicationXref(reference);
                if (pubXref != null)
                    bpEvidence.addPropertyValue(xrefProp, pubXref);
            }
        }    
        return bpEvidence;
    }
    
    private OWLIndividual convertLiteratureReferenceToPublicationXref(GKInstance literatureReference) throws Exception {
        OWLIndividual pubXrefIndividual = (OWLIndividual) rToBInstanceMap.get(literatureReference);
        if (pubXrefIndividual != null)
            return pubXrefIndividual;
        Integer pmid = (Integer) literatureReference.getAttributeValue("pubMedIdentifier");
        OWLNamedClass publicationXrefCls = (OWLNamedClass) biopaxFactory.getpublicationXrefClass();
        String id = "Pubmed_" + pmid;
        id = generateOWLID(id);
        pubXrefIndividual = publicationXrefCls.createOWLIndividual(id);
        OWLProperty prop = null;
        if (pmid != null) {
            prop = biopaxFactory.getOWLProperty("ID");
            // ID is a String in BioPAX. Need to convert to String.
            pubXrefIndividual.addPropertyValue(prop, pmid.toString());
            prop = biopaxFactory.getOWLProperty("DB");
            pubXrefIndividual.addPropertyValue(prop, "Pubmed");
        }
        Integer year = (Integer) literatureReference.getAttributeValue("year");
        if (year != null) {
            prop = biopaxFactory.getOWLProperty("YEAR");
            // Year is a double type.
            pubXrefIndividual.addPropertyValue(prop, year);
        }
        // Title
        String title = (String) literatureReference.getAttributeValue("title");
        if (title != null) {
            prop = biopaxFactory.getOWLProperty("TITLE");
            pubXrefIndividual.addPropertyValue(prop, title);
        }
        // Authors
        List authors = literatureReference.getAttributeValuesList("author");
        if (authors != null && authors.size() > 0) {
            prop = biopaxFactory.getOWLProperty("AUTHORS");
            // Two persons might have the same display name. E.g. pmid: 15070733.
            List names = new ArrayList();
            for (Iterator it = authors.iterator(); it.hasNext();) {
                GKInstance person = (GKInstance) it.next();
                names.add(person.getDisplayName());
                //pubXrefIndividual.addPropertyValue(prop, person.getDisplayName());
            }
            pubXrefIndividual.setPropertyValues(prop, names);
        }
        // Source is from Journal title, volume and page
        StringBuffer source = new StringBuffer();
        String journal = (String) literatureReference.getAttributeValue("journal");
        if (journal != null) 
            source.append(journal);
        Integer volume = (Integer) literatureReference.getAttributeValue("volume");
        if (volume != null)
            source.append(" ").append(volume).append(":");
        String page = (String) literatureReference.getAttributeValue("pages");
        if (page != null)
            source.append(page);
        if (source.length() > 0) {
            prop = biopaxFactory.getOWLProperty("SOURCE");
            pubXrefIndividual.addPropertyValue(prop, source.toString());
        }
        rToBInstanceMap.put(literatureReference, pubXrefIndividual);
        return pubXrefIndividual;
    }
    
    private void attachReactomeIDAsXref(GKInstance reactionInstance, OWLIndividual bpInstance){
        Long DBID = reactionInstance.getDBID();
        OWLIndividual xref = (OWLIndividual) idToXrefMap.get(DBID);
        if (xref == null) {
            xref = (OWLIndividual) biopaxFactory.getunificationXrefClass().createInstance("Reactome" + DBID);
            idToXrefMap.put(DBID, xref);
            xref.setPropertyValue(biopaxFactory.getOWLProperty("DB"),
            		              "Reactome");
            xref.setPropertyValue(biopaxFactory.getOWLProperty("DB-VERSION"),
            					  "14");
            xref.setPropertyValue(biopaxFactory.getOWLProperty("ID"),
                    		      DBID.toString());
            xref.setPropertyValue(biopaxFactory.getOWLProperty("COMMENT"),
            	                  "http://www.reactome.org");
        }
        bpInstance.addPropertyValue(biopaxFactory.getOWLProperty("XREF"),
                                    xref);
    }
    
    private void handlePathwayComponents(List components, OWLIndividual bpPathway) throws Exception {
        if (components == null || components.size() == 0)
            return;
        GKInstance comp = null;
        Set pathwaySteps = new HashSet();
        Set pathwayComponents = new HashSet();
        Set gkEvents = new HashSet();
        for (Iterator it = components.iterator(); it.hasNext();) {
            comp = (GKInstance) it.next();
            if (comp.getSchemClass().isa("GenericEvent")) {
                // Get the deepest value in hasInstance slot to be added to pathway
                List instances = getInstancesInGenericInstance(comp);
                for (Iterator it1 = instances.iterator(); it1.hasNext();) {
                    GKInstance tmp = (GKInstance) it1.next();
                    handleEvent(tmp);
                    OWLIndividual bpComp = (OWLIndividual) rToBInstanceMap.get(tmp);
                    OWLIndividual pathwayStep = createPathwayStep(tmp, bpComp);
                    pathwaySteps.add(pathwayStep);
                }
            }
            else {
                handleEvent(comp);
                OWLIndividual bpComp = (OWLIndividual) rToBInstanceMap.get(comp);
                OWLIndividual pathwayStep = createPathwayStep(comp, bpComp);
                pathwaySteps.add(pathwayStep);
            }
        }
        if (pathwaySteps != null) {
            OWLProperty propName = biopaxFactory.getOWLProperty("PATHWAY-COMPONENTS");
            bpPathway.setPropertyValues(propName, pathwaySteps);
        }
    }
    
    private OWLIndividual createPathwayStep(GKInstance event, OWLIndividual bpEvent) throws Exception {
        OWLIndividual pathwayStep = (OWLIndividual) eventToPathwayStepMap.get(event);
        if (pathwayStep != null) {
            return pathwayStep;
        }
        OWLNamedClass pathwayStepCls = (OWLNamedClass) biopaxFactory.getpathwayStepClass();
        String id = event.getDisplayName() + "Step";
        id = generateOWLID(id);
        pathwayStep = pathwayStepCls.createOWLIndividual(id);
        OWLProperty prop = biopaxFactory.getOWLProperty("STEP-INTERACTIONS");
        pathwayStep.addPropertyValue(prop, bpEvent);
        eventToPathwayStepMap.put(event, pathwayStep);
        return pathwayStep;
    }
    
    private List getInstancesInGenericInstance(GKInstance genericEvent) throws Exception {
        Set instances = new HashSet();
        Set current = new HashSet();
        Set next = new HashSet();
        current.add(genericEvent);
        GKInstance tmp = null;
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                tmp = (GKInstance) it.next();
                if (tmp.getSchemClass().isValidAttribute("hasInstance")) {
                    List values = tmp.getAttributeValuesList("hasInstance");
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
    
    private void handleReferenceEntityNames(GKInstance rInstance, OWLIndividual bpInstance) throws Exception {
        String displayName = rInstance.getDisplayName();
        OWLProperty prop = biopaxFactory.getOWLProperty("NAME");
        if (displayName != null)
            bpInstance.setPropertyValue(prop, displayName);
        List names = rInstance.getAttributeValuesList("name");
        Set synonyms = new HashSet();
        if (names != null && names.size() > 0) {
            prop = biopaxFactory.getOWLProperty("SHORT-NAME");
            // Pick the first one as the short name
            String firstName = (String) names.get(0);
            bpInstance.setPropertyValue(prop, firstName);
            for (int i = 1; i < names.size(); i++)
                synonyms.add(names.get(i));
        }
        // Use gene names as synonyms if applicable
        if (rInstance.getSchemClass().isValidAttribute("geneName")) {
            List geneNames = rInstance.getAttributeValuesList("geneName");
            if (geneNames != null) {
                for (Iterator it = geneNames.iterator(); it.hasNext();)
                    synonyms.add(it.next());
            }
        }
        if (synonyms.size() > 0) {
            prop = biopaxFactory.getOWLProperty("SYNONYMS");
            bpInstance.setPropertyValues(prop, synonyms);
        }
    }
    
    private void handleNames(GKInstance reactomeInstance, OWLIndividual bpInstance) throws Exception {
        OWLProperty propName = biopaxFactory.getOWLProperty("NAME");
        // Use _displayName as the name
        String displayName = reactomeInstance.getDisplayName();
        if (displayName != null)
            bpInstance.setPropertyValue(propName, displayName);
        // Use the first value in as short name
        List names = reactomeInstance.getAttributeValuesList("name");
        if (names.size() > 0) {
            String firstName = (String) names.get(0);
            propName = biopaxFactory.getOWLProperty("SHORT-NAME");
            bpInstance.setPropertyValue(propName, firstName);
        }
        // Add all other names to SYNONYM
        if (names.size() > 1) {
            List synonyms = new ArrayList(names.size() - 1);
            for (int i = 1; i < names.size(); i++) {
                synonyms.add(names.get(i));
            }
            bpInstance.setPropertyValues(propName, synonyms);
        }
    }
    
    /**
     * A helper method to create a unique OWL id from the displayName of
     * the specified GKInstance.
     * @param instance
     * @return
     */
    private String getOWLIDFromDisplayName(GKInstance instance) {
        String displayName = instance.getDisplayName();
        return generateOWLID(displayName);
    }
    
    private String generateOWLID(String id) {
        //String tmp = id.replaceAll("[ :,\\(\\)\\[\\]\\\\/]", "_");
        // Replace all non word character by "_"
        String tmp = id.replaceAll("\\W", "_");
        // Have to make sure digit should not be in the first place
        // The following code cannot work. Don't why?
        // if (tmp.matches("^\\d"))
        Pattern pattern = Pattern.compile("^\\d");
        if (pattern.matcher(tmp).find()) {
            tmp = "_" + tmp;
        }
        int c = 1;
        String rtn = tmp;
        // To keep the returned id unique.
        while (idSet.contains(rtn)) {
            // Have to find a new id
            rtn = tmp + c;
            c ++;
        }
        idSet.add(rtn);
        return rtn;        
    }
    
    private void handleReaction(GKInstance reaction) throws Exception {
        OWLNamedClass bpReactionCls = (OWLNamedClass) biopaxFactory.getbiochemicalReactionClass();
        String id = getOWLIDFromDisplayName(reaction);
        OWLIndividual bpReaction = bpReactionCls.createOWLIndividual(id);
        rToBInstanceMap.put(reaction, bpReaction);
        // Get the input information
        List inputs = reaction.getAttributeValuesList("input");
        OWLProperty stoiProp = biopaxFactory.getOWLProperty("STOICHIOMETRIC-COEFFICIENT");
        if (inputs != null && inputs.size() > 0) {
            OWLProperty inputProp = biopaxFactory.getOWLProperty("LEFT");
            Map inputMap = getStoichiometryMap(inputs);
            GKInstance input = null;
            OWLIndividual bpInput = null;
            Integer stoi = null;
            for (Iterator it = inputMap.keySet().iterator(); it.hasNext();) {
                input = (GKInstance) it.next();
                bpInput = createEntityParticipant(input);
                stoi = (Integer) inputMap.get(input);
                if (stoi.intValue() > 1)
                    bpInput.setPropertyValue(stoiProp, new Float(stoi.toString()));
                bpReaction.addPropertyValue(inputProp, bpInput);
            }
        }
        // Get the output information
        List outputs = reaction.getAttributeValuesList("output");
        if (outputs != null && outputs.size() > 0) {
            OWLProperty outputProp = biopaxFactory.getOWLProperty("RIGHT");
            GKInstance output = null;
            OWLIndividual bpOutput = null;
            Map outputMap = getStoichiometryMap(outputs);
            Integer stoi = null;
            for (Iterator it = outputMap.keySet().iterator(); it.hasNext();) {
                output = (GKInstance) it.next();
                bpOutput = createEntityParticipant(output);
                stoi = (Integer) outputMap.get(output);
                if (stoi.intValue() > 1)
                    bpOutput.setPropertyValue(stoiProp, new Float(stoi.toString()));
                bpReaction.addPropertyValue(outputProp, bpOutput);
            }
        }
        // Get the catalystActivity information
        List cas = reaction.getAttributeValuesList("catalystActivity");
        if (cas != null && cas.size() > 0) {
            GKInstance ca = null;
            OWLIndividual catalysis = null;
            List list = new ArrayList(cas.size());
            for (Iterator it = cas.iterator(); it.hasNext();) {
                ca = (GKInstance) it.next();
                catalysis = handleCatalystActivity(ca, bpReaction, reaction);
                list.add(catalysis);
            }
            rxtToControlMap.put(reaction, list);
        }
    }
    
    private OWLIndividual handleCatalystActivity(GKInstance ca, 
                                                 OWLIndividual bpControlled,
                                                 GKInstance rReaction) throws Exception {
        String id = getOWLIDFromDisplayName(ca);
        OWLIndividual bpCatalyst = (OWLIndividual) biopaxFactory.getcatalysisClass().createRDFIndividual(id);
        GKInstance controller = (GKInstance) ca.getAttributeValue("physicalEntity");
        if (controller != null) {
            OWLIndividual bpController = createEntityParticipant(controller);
            OWLProperty prop = biopaxFactory.getOWLProperty("CONTROLLER");
            bpCatalyst.setPropertyValue(prop, bpController);
        }
        OWLProperty prop = biopaxFactory.getOWLProperty("CONTROLLED");
        bpCatalyst.setPropertyValue(prop, bpControlled);
        prop = biopaxFactory.getOWLProperty("DIRECTION");
        bpCatalyst.setPropertyValue(prop, "PHYSIOL-LEFT-TO-RIGHT");
        // Based on activity to create a xref to point a GO term
        GKInstance activity = (GKInstance) ca.getAttributeValue("activity");
        if (activity != null) {
            OWLIndividual bpXref = createGOOWL(activity);
            prop = biopaxFactory.getOWLProperty("XREF");
            bpCatalyst.addPropertyValue(prop, bpXref);
        }
        // Need to handle reverse attributes for regulation here.
        handleRegulationForCatalystActivity(ca, bpCatalyst, rReaction);
        return bpCatalyst;
    }
    
    private OWLIndividual createEntityParticipant(GKInstance rEntity) throws Exception {
        // Although the biopax doc suggests that each reaction should use its own entityParticipant, however,
        // from the Reactome's view, it will be nice to share entityParticipant in several reactions.
        // But since a stoichiometry is used in the reaction, indeed, it should NOT be reused.
        //OWLIndividual bpEntityParticipant = (OWLIndividual) rToBInstanceMap.get(rEntity);
        //if (bpEntityParticipant != null)
        //    return bpEntityParticipant;
        String id = getOWLIDFromDisplayName(rEntity);
        boolean isGeneric = rEntity.getSchemClass().isa("GenericEntity");
        SchemaClass reactomeType = getReactomeEntityType(rEntity);
        OWLIndividual bpEntityParticipant = null;
        if (rEntity.getSchemClass().isa("Complex")) {
            bpEntityParticipant = (OWLIndividual) biopaxFactory.getcomplexParticipantClass().createRDFIndividual(id);
        }
        // Try to figure out the type based on the referenceEntity
        else if (reactomeType == null) {
            bpEntityParticipant = (OWLIndividual) biopaxFactory.getphysicalEntityParticipantClass().createRDFIndividual(id);
        }
        else if (reactomeType.isa("ReferencePeptideSequence")) {
            // This is a protein
            bpEntityParticipant = (OWLIndividual)biopaxFactory.getproteinParticipantClass().createRDFIndividual(id);
        }
        else if (reactomeType.isa("ReferenceMolecule")) {
            bpEntityParticipant = (OWLIndividual)biopaxFactory.getsmallMoleculeParticipantClass().createRDFIndividual(id);
        }
        else if (reactomeType.isa("ReferenceGroup")) {
            bpEntityParticipant = (OWLIndividual)biopaxFactory.getsmallMoleculeParticipantClass().createRDFIndividual(id);
        }
        else if (reactomeType.isa("ReferenceDNASequence")) {
            bpEntityParticipant = (OWLIndividual)biopaxFactory.getdnaParticipantClass().createRDFIndividual(id);
        }
        else if (reactomeType.isa("ReferenceRNASequence")) {
            bpEntityParticipant = (OWLIndividual)biopaxFactory.getrnaParticipantClass().createRDFIndividual(id);
        }
        else {
            bpEntityParticipant = (OWLIndividual)biopaxFactory.getphysicalEntityParticipantClass().createRDFIndividual(id);
        }
        if (isGeneric) {
            // Add a comment
            OWLProperty commentProp = biopaxFactory.getOWLProperty("COMMENT");
            bpEntityParticipant.addPropertyValue(commentProp, "Converted from GenericEntity in Reactome");
        }
        // Get the compartment information
        handleCompartment(rEntity, bpEntityParticipant);
        handleHasDomain(rEntity, bpEntityParticipant);
        handleReferenceEntity(rEntity, bpEntityParticipant);
        //rToBInstanceMap.put(rEntity, bpEntityParticipant);
        return bpEntityParticipant;
    }

    private void handleHasDomain(GKInstance rEntity, OWLIndividual bpEntityParticipant) throws Exception {
    	if (!rEntity.getSchemClass().isValidAttribute("hasDomain"))
    		return;
    	List hasDomains = rEntity.getAttributeValuesList("hasDomain");
    	if (hasDomains == null || hasDomains.size() == 0)
    		return;
    	GKInstance domain = null;
    	OWLProperty prop = biopaxFactory.getOWLProperty("SEQUENCE-FEATURE-LIST");
    	for (Iterator it = hasDomains.iterator(); it.hasNext();) {
    		domain = (GKInstance) it.next();
    		// The allowed type might be not a Domain
    		if (!domain.getSchemClass().isa("SequenceDomain"))
    			continue;
    		// Only Domain can be converted to SequenceFeature instances in BioPAX
    		OWLIndividual sequenceFeature = createSequenceFeatureFromDomain(domain);
    		bpEntityParticipant.addPropertyValue(prop, sequenceFeature);
    	}
    }
    
   /**
    * Only SequenceDomain is handled right now.
    * @param domain
    * @return
    * @throws Exception
    */
   private OWLIndividual createSequenceFeatureFromDomain(GKInstance domain) throws Exception {
       if (!domain.getSchemClass().isa("SequenceDomain"))
           throw new IllegalStateException("ReactomeToBioConverter.createSequenceFeatureFromDomain(): Only SequenceDomain can be handled.");
       OWLIndividual sequenceFeature = (OWLIndividual) rToBInstanceMap.get(domain);
       if (sequenceFeature != null)
           return sequenceFeature;
       // Need to get ID
       String id = getOWLIDFromDisplayName(domain);
       OWLNamedClass sfClass = (OWLNamedClass) biopaxFactory.getsequenceFeatureClass();
       sequenceFeature = sfClass.createOWLIndividual(id);
       handleNames(domain, sequenceFeature);
       OWLProperty positionProp = biopaxFactory.getOWLProperty("SEQUENCE-POSITION");
       Integer start = (Integer) domain.getAttributeValue("startCoordinate");
       Integer end = (Integer) domain.getAttributeValue("endCoordinate");
       OWLIndividual sequenceSite = null;
       if (start != null && end != null && start.equals(end)) {
           // SequenceSite will be used for featureLocation property.
           id = "SequenceSite" + start + "_";
           id = generateOWLID(id);
           sequenceSite = (OWLIndividual) biopaxFactory.getsequenceSiteClass().createInstance(id);
           sequenceSite.setPropertyValue(positionProp, start);
       }
       else {
           id = "SequenceInternal" + start + "_" + end + "_";
           id = generateOWLID(id);
           sequenceSite = (OWLIndividual) biopaxFactory.getsequenceIntervalClass().createInstance(id);
           if (start != null) {
               String id1 = "SequenceSite" + start + "_";
               id1 = generateOWLID(id1);
               OWLIndividual ss1 = (OWLIndividual) biopaxFactory.getsequenceSiteClass().createInstance(id1);
               ss1.setPropertyValue(positionProp, start);
               OWLProperty sequenceIntervalBegin = biopaxFactory.getOWLProperty("SEQUENCE-INTERVAL-BEGIN");
               sequenceSite.setPropertyValue(sequenceIntervalBegin, ss1);
           }
           if (end != null) {    
               String id1 = "SequenceSite" + end + "_";
               id1 = generateOWLID(id1);
               OWLIndividual ss1 = (OWLIndividual) biopaxFactory.getsequenceSiteClass().createInstance(id1);
               ss1.setPropertyValue(positionProp, end);
               OWLProperty sequenceIntervalEnd = biopaxFactory.getOWLProperty("SEQUENCE-INTERVAL-END");
               sequenceSite.setPropertyValue(sequenceIntervalEnd, ss1);
           }
       }
       OWLProperty featureLocationProp = biopaxFactory.getOWLProperty("FEATURE-LOCATION");
       sequenceFeature.addPropertyValue(featureLocationProp, sequenceSite);
       rToBInstanceMap.put(domain, sequenceFeature);
       return sequenceFeature;
   }
    
    private SchemaClass getReactomeEntityType(GKInstance rEntity) throws Exception {
        if (rEntity.getSchemClass().isa("ConcreteEntity")) {
            if (!rEntity.getSchemClass().isValidAttribute("referenceEntity"))
                return null;
            GKInstance referenceEntity = (GKInstance) rEntity.getAttributeValue("referenceEntity");
            if (referenceEntity == null)
                return null;
            else
                return referenceEntity.getSchemClass();
        }
        else {
            GKInstance referenceEntity = null;
            if (rEntity.getSchemClass().isValidAttribute("referenceEntity"))
                referenceEntity = (GKInstance) rEntity.getAttributeValue("referenceEntity");
            if (referenceEntity != null)
                return referenceEntity.getSchemClass();
            else {
                // Get all leaf nodes in the hasInstance branches
                List instances = getInstancesInGenericInstance(rEntity);
                Set types = new HashSet();
                GKInstance instance = null;
                for (Iterator it = instances.iterator(); it.hasNext();) {
                    instance = (GKInstance) it.next();
                    if (!instance.getSchemClass().isValidAttribute("referenceEntity"))
                        continue;
                    referenceEntity = (GKInstance) instance.getAttributeValue("referenceEntity");
                    if (referenceEntity != null)
                        types.add(referenceEntity.getSchemClass());
                }
                if (types.size() == 0 || types.size() > 1)
                    return null;
                else
                    return (SchemaClass) types.iterator().next();
            }
        }
    }
    
    private void handleReferenceEntityForGenericEntity(GKInstance genericEntity,
                                                       OWLIndividual bpEntityParticipant) throws Exception {
        OWLIndividual bpEntity = (OWLIndividual) rEntityToBEntityMap.get(genericEntity);
        if (bpEntity != null) {
            OWLProperty prop = biopaxFactory.getOWLProperty("PHYSICAL-ENTITY");
            bpEntityParticipant.addPropertyValue(prop, bpEntity);
            return;
        }
        String id = getOWLIDFromDisplayName(genericEntity);
        bpEntity = createEntityFromParticipant(id, bpEntityParticipant);
        OWLProperty prop = biopaxFactory.getOWLProperty("PHYSICAL-ENTITY");
        bpEntityParticipant.addPropertyValue(prop, bpEntity);
        prop = biopaxFactory.getOWLProperty("NAME");
        bpEntity.addPropertyValue(prop, genericEntity.getDisplayName());
        handleTaxon(genericEntity, bpEntity, "taxon");
        prop = biopaxFactory.getOWLProperty("COMMENT");
        bpEntity.addPropertyValue(prop, "Converted from GenericEntity in Reactome. " +
        		                        "Each synonym is a name of a ConcreteEntity, " +
        		                        "and each XREF points to one ConcreteEntity");
        List instances = getInstancesInGenericInstance(genericEntity);
        GKInstance instance = null;
        prop = biopaxFactory.getOWLProperty("XREF");
        OWLProperty synonymProp = biopaxFactory.getOWLProperty("SYNONYMS");
        for (Iterator it = instances.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            if (!instance.getSchemClass().isValidAttribute("referenceEntity"))
                continue;
            GKInstance refEntity = (GKInstance) instance.getAttributeValue("referenceEntity");
            if (refEntity == null)
                continue;
            OWLIndividual xref = createXREF(refEntity);
            if (xref != null) {
                bpEntity.addPropertyValue(prop, xref);
                // Only record name for non-null XREF
                bpEntity.addPropertyValue(synonymProp, instance.getDisplayName());
            }
        }
        rEntityToBEntityMap.put(genericEntity, bpEntity);
    }
    
    private OWLIndividual createEntityFromParticipant(String id, OWLIndividual bpEntityParticipant) {
        RDFSClass clsType = bpEntityParticipant.getRDFType();
        OWLIndividual bpEntity = null;
        if (clsType == biopaxFactory.getproteinParticipantClass()) 
            bpEntity = (OWLIndividual) biopaxFactory.getproteinClass().createRDFIndividual(id);
        else if (clsType == biopaxFactory.getsmallMoleculeParticipantClass())
            bpEntity = (OWLIndividual) biopaxFactory.getsmallMoleculeClass().createRDFIndividual(id);
        else if (clsType == biopaxFactory.getdnaParticipantClass())
            bpEntity = (OWLIndividual) biopaxFactory.getdnaClass().createRDFIndividual(id);
        else if (clsType == biopaxFactory.getrnaParticipantClass())
            bpEntity = (OWLIndividual) biopaxFactory.getrnaClass().createRDFIndividual(id);
        else
            bpEntity = (OWLIndividual) biopaxFactory.getphysicalEntityClass().createRDFIndividual(id);
        return bpEntity;
    }
    
    private void handleReferenceEntity(GKInstance rEntity, OWLIndividual bpEntityParticipant) throws Exception {
        if (rEntity.getSchemClass().isa("Complex")) {
            handleComplexInComplexParticipant(rEntity, bpEntityParticipant);
            return;
        }
        GKInstance referenceEntity = (GKInstance) rEntity.getAttributeValue("referenceEntity");
        if (referenceEntity == null) {
            if (rEntity.getSchemClass().isa("GenericEntity"))
                handleReferenceEntityForGenericEntity(rEntity, bpEntityParticipant);
            return;
        }
        OWLIndividual bpEntity = (OWLIndividual) rToBInstanceMap.get(referenceEntity);
        OWLProperty physicalEntityProp = biopaxFactory.getOWLProperty("PHYSICAL-ENTITY");
        if (bpEntity != null) {
            bpEntityParticipant.setPropertyValue(physicalEntityProp, bpEntity);
            return;
        }
        // Need id for OWLIndividual
        String id = getOWLIDFromDisplayName(referenceEntity);
        // Need to create a new physicalEntity individual
        bpEntity = createEntityFromParticipant(id, bpEntityParticipant);
        rToBInstanceMap.put(referenceEntity, bpEntity);
        handleTaxon(referenceEntity, bpEntity, "species");
        handleReferenceEntityNames(referenceEntity, bpEntity);
        bpEntityParticipant.setPropertyValue(physicalEntityProp, bpEntity);
        // Create a unificationXref for bpEntity
        OWLIndividual xref = createXREF(referenceEntity);
        if (xref != null) {
            OWLProperty prop = biopaxFactory.getOWLProperty("XREF");
            bpEntity.addPropertyValue(prop, xref);
        }
        // Get comment or description
        String comment = null;
        if (referenceEntity.getSchemClass().isValidAttribute("comment"))
            comment = (String) referenceEntity.getAttributeValue("comment");
        if (comment == null && referenceEntity.getSchemClass().isValidAttribute("description"))
            comment = (String) referenceEntity.getAttributeValue("description");
        if (comment != null) {
            OWLProperty prop = biopaxFactory.getOWLProperty("COMMENT");
            bpEntity.addPropertyValue(prop, comment);
        }
    }
    
    private OWLIndividual createXREF(GKInstance referenceEntity) throws Exception {
        // Create a unificationXref for bpEntity
        String identifier = (String) referenceEntity.getAttributeValue("identifier");
        GKInstance refDB = (GKInstance) referenceEntity.getAttributeValue("referenceDatabase");
        if (identifier != null && refDB != null) {
            String dbName = refDB.getDisplayName();
            String id = dbName + "_" + identifier;
            // It might be used??? A very weird thing
            id = generateOWLID(id);
            OWLIndividual xref = (OWLIndividual) biopaxFactory.getunificationXrefClass().createInstance(id);
            OWLProperty prop = biopaxFactory.getOWLProperty("DB");
            xref.setPropertyValue(prop, dbName);
            prop = biopaxFactory.getOWLProperty("ID");
            xref.setPropertyValue(prop, identifier);
            prop = biopaxFactory.getOWLProperty("XREF");
            return xref;
        }
        return null;
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
                                                   OWLIndividual complexParticipant) throws Exception {
        OWLIndividual bpComplex = (OWLIndividual) rEntityToBEntityMap.get(complex);
        if (bpComplex == null) {
            String id = getOWLIDFromDisplayName(complex);
            bpComplex = (OWLIndividual) biopaxFactory.getcomplexClass().createRDFIndividual(id);
            rEntityToBEntityMap.put(complex, bpComplex);
            OWLProperty prop = null;
            // Get information on components
            List components = complex.getAttributeValuesList("hasComponent");
            if (components != null && components.size() > 0) {
                prop = biopaxFactory.getOWLProperty("COMPONENTS");
                // Need to figure out the stoichiometries
                Map compMap = getStoichiometryMap(components);
                OWLProperty stoiProp = biopaxFactory.getOWLProperty("STOICHIOMETRIC-COEFFICIENT");
                for (Iterator it = compMap.keySet().iterator(); it.hasNext();) {
                    GKInstance comp = (GKInstance) it.next();
                    OWLIndividual bpComp = createEntityParticipant(comp);
                    Integer stoi = (Integer) compMap.get(comp);
                    if (stoi.intValue() > 1) {
                        // Even though this is a double value, however, it can take only
                        // Float. Probably this is a bug.
                        bpComp.setPropertyValue(stoiProp, new Float(stoi.toString()));
                    }
                    bpComplex.addPropertyValue(prop, bpComp);
                }
            }
            // Handle taxon
            handleTaxon(complex, bpComplex, "taxon");
            handleReferenceEntityNames(complex, bpComplex);
        }
        OWLProperty prop = biopaxFactory.getOWLProperty("PHYSICAL-ENTITY");
        complexParticipant.setPropertyValue(prop, bpComplex);
    }
    
    private void handleCompartment(GKInstance rEntity, OWLIndividual bpInstance) throws Exception {
        GKInstance compartment = (GKInstance) rEntity.getAttributeValue("compartment");
        if (compartment == null)
            return;
        OWLIndividual bpCompartment = (OWLIndividual) rToBInstanceMap.get(compartment);
        OWLProperty prop = biopaxFactory.getOWLProperty("CELLULAR-LOCATION");
        if (bpCompartment != null) {
            bpInstance.setPropertyValue(prop, bpCompartment);
            return;
        }
        // Need to create a new bpCompartment
        String id = getOWLIDFromDisplayName(compartment);
        bpCompartment = (OWLIndividual) biopaxFactory.getopenControlledVocabularyClass().createRDFIndividual(id);
        rToBInstanceMap.put(compartment, bpCompartment);
        String term = (String) compartment.getAttributeValue("name");
        if (term != null) {
            OWLProperty termProp = biopaxFactory.getOWLProperty("TERM");
            bpCompartment.addPropertyValue(termProp, term);
        }
        // Need to create a xref for GO
        String accession = (String) compartment.getAttributeValue("accession");
        String goID = "GO_" + accession;
        OWLIndividual xref = (OWLIndividual) biopaxFactory.getxrefClass().createRDFIndividual(goID);
        OWLProperty dbProp = biopaxFactory.getOWLProperty("DB");
        xref.setPropertyValue(dbProp, "DB");
        OWLProperty idProp = biopaxFactory.getOWLProperty("ID");
        xref.setPropertyValue(idProp, accession);
        OWLProperty xrefProp = biopaxFactory.getOWLProperty("XREF");
        bpCompartment.addPropertyValue(xrefProp, xref);
    }
    
    public OWLModel getBioPAXModel() {
        if (biopaxFactory != null)
            return biopaxFactory.getOWLModel();
        return null;
    }
    
    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Usage java org.reactome.biopax.ReactomeToBioPAXConverter " +
            		"dbHost dbName dbUser dbPwd dbPort eventID");
            System.exit(0);
        }
        try {
            ReactomeToBioPAXConverter converter = new ReactomeToBioPAXConverter();
            GKInstance topEvent = getTopLevelEvent(args);
            converter.setReactomeEvent(topEvent);
            converter.convert();
            OWLModel biopaxModel = converter.getBioPAXModel();
            Collection errors = new ArrayList();
            ((JenaOWLModel)biopaxModel).save(System.out, FileUtils.langXMLAbbrev, errors);
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
}
