/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.SchemaClass;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;

/**
 * Some common methods that cannot be put into super mappers are listed here as static
 * utilitiy methods. For examle, method to extract ORGANISM for Pathway for some 
 * PhysicalEntity classes.
 * @author guanming
 *
 */
public class B2RMapperUtilities {
    // Used to check the types of GO terms
    private static Set<String> goMFTerms;
    private static Set<String> goBPTerms;
    private static Set<String> goCCTerms;
    
    public static enum GO_TYPE {
        MF, BP, CC
    }
    
    public static GKInstance createEntityCompartment(OWLIndividual bpLocation,
                                                     BioPAXFactory bpFactory,
                                                     XMLFileAdaptor reactomeAdaptor) throws Exception {
        GKInstance gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.EntityCompartment);
        // Get term as String
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.TERM);
        String term = (String) bpLocation.getPropertyValue(prop);
        gkInstance.setAttributeValue(ReactomeJavaConstants.name, term);
        // Check Xref
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.XREF);
        OWLIndividual xref = (OWLIndividual) bpLocation.getPropertyValue(prop);
        if (xref != null) {
            // Push more information into gkInstance
            prop = bpFactory.getOWLProperty(BioPAXJavaConstants.DB);
            String dbName = (String) xref.getPropertyValue(prop);
            if (dbName != null) {
                GKInstance refDB = getReferenceDB(dbName, reactomeAdaptor);
                gkInstance.setAttributeValue(ReactomeJavaConstants.referenceDatabase,
                                             refDB);
            }
            prop = bpFactory.getOWLProperty(BioPAXJavaConstants.ID);
            String id = (String) xref.getPropertyValue(prop);
            if (id != null) 
                gkInstance.setAttributeValue(ReactomeJavaConstants.accession,
                                             id);
        }
        InstanceDisplayNameGenerator.setDisplayName(gkInstance);
        return gkInstance;
    }
    
    public static String generateSequenceFeatureDisplayName(OWLIndividual bpInstance,
                                                            BioPAXFactory bpFactory) {
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.FEATURE_TYPE);
        OWLIndividual bpFeatureType = (OWLIndividual) bpInstance.getPropertyValue(prop);
        String term = null;
        if (bpFeatureType != null) {
            prop = bpFactory.getOWLProperty(BioPAXJavaConstants.TERM);
            term = (String) bpFeatureType.getPropertyValue(prop);
        }
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.FEATURE_LOCATION);
        OWLIndividual bpLocation = (OWLIndividual) bpInstance.getPropertyValue(prop);
        String location = null;
        if (bpLocation != null) {
            location = getSequenceFeatureLocation(bpInstance, bpFactory);
        }
        if (location != null)
            return term + " at " + location;
        else
            return term;
    }
    
    private static String getSequenceFeatureLocation(OWLIndividual bpInstance,
                                                     BioPAXFactory bpFactory) {
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.FEATURE_LOCATION);
        OWLIndividual bpLocation = (OWLIndividual) bpInstance.getPropertyValue(prop);
        if (bpLocation == null)
            return null;
        String rtn = null;
        if (bpLocation.getRDFType() == bpFactory.getsequenceSiteClass()) {
            return getSequenceSiteAsString(bpFactory, bpLocation);
        }
        else if (bpLocation.getRDFType() == bpFactory.getsequenceIntervalClass()) {
            prop = bpFactory.getOWLProperty(BioPAXJavaConstants.SEQUENCE_INTERVAL_BEGIN);
            OWLIndividual begin = (OWLIndividual) bpLocation.getPropertyValue(prop);
            String beginStr = null;
            if (begin != null)
                beginStr = getSequenceSiteAsString(bpFactory, begin);
            prop = bpFactory.getOWLProperty(BioPAXJavaConstants.SEQUENCE_INTERVAL_END);
            OWLIndividual end = (OWLIndividual) bpLocation.getPropertyValue(prop);
            String endStr = null;
            if (end != null)
                endStr = getSequenceSiteAsString(bpFactory, end);
            return beginStr + "-" + endStr;
        }
        return rtn;
    }

    private static String getSequenceSiteAsString(BioPAXFactory bpFactory, OWLIndividual bpLocation) {
        OWLProperty prop;
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.POSITION_STATUS);
        String status = (String) bpLocation.getPropertyValue(prop);
        if (status.equals("EQUAL"))
            status = "";
        else if (status.equals("LESS-THAN"))
            status = "<";
        else if (status.equals("GREATER-THAN"))
            status = ">";
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.SEQUENCE_POSITION);
        // The returned type is RDFSLiteral, which is not mapped to Integer automatically.
        Object position = bpLocation.getPropertyValue(prop);
        if (position != null)
            return status + " " + position;
        return null;
    }
    
    public static GKInstance fetchGKInstanceBasedOnBPID(String bpId,
                                                        String gkClsName,
                                                        XMLFileAdaptor reactomeAdaptor) throws Exception {
        // BioPAX ID is used as a name if applicable
        SchemaClass gkCls = reactomeAdaptor.getSchema().getClassByName(gkClsName);
        if (!gkCls.isValidAttribute(ReactomeJavaConstants.name))
            return null;
        Collection list = reactomeAdaptor.fetchInstanceByAttribute(gkClsName,
                                                                   ReactomeJavaConstants.name,
                                                                   "=",
                                                                   bpId);
        if (list != null && list.size() > 0) {
            return (GKInstance) list.iterator().next();
        }
        return null;
    }
    
    public static void mapOrganismProperty(OWLIndividual bpInstance,
                                           BioPAXFactory bpFactory,
                                           GKInstance gkInstance,
                                           Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        // Safety checking
        if (!gkInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.species))
            return;
        // Extract ORGANISM information
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.ORGANISM);
        // Single value
        OWLIndividual bpOrganism = (OWLIndividual) bpInstance.getPropertyValue(prop);
        if (bpOrganism != null) {
            GKInstance rSpecies = bpToRInstanceMap.get(bpOrganism);
            if (rSpecies != null)
                gkInstance.setAttributeValue(ReactomeJavaConstants.species,
                                           rSpecies);
        }
    }
    
    public static GO_TYPE getGOType(String goTerm) throws IOException {
        if (goMFTerms == null)
            loadGOTerms();
        if (goMFTerms.contains(goTerm))
            return GO_TYPE.MF;
        if (goBPTerms.contains(goTerm))
            return GO_TYPE.BP;
        if (goCCTerms.contains(goTerm))
            return GO_TYPE.CC;
        return null;
    }
    
    private static void loadGOTerms() throws IOException {
        String fileName = "resources/GO.terms_and_ids.txt";
        goMFTerms = new HashSet<String>();
        goBPTerms = new HashSet<String>();
        goCCTerms = new HashSet<String>();
        FileReader fileReader = new FileReader(fileName);
        BufferedReader reader = new BufferedReader(fileReader);
        String line = null;
        String[] tokens = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("!"))
                continue; // Comment line
            tokens = line.split("\t");
            if (tokens[2].equals("F"))
                goMFTerms.add(tokens[0]);
            else if (tokens[2].equals("P"))
                goBPTerms.add(tokens[0]);
            else if (tokens[2].equals("C"))
                goCCTerms.add(tokens[0]);
        }
    }
    
    public static GKInstance getReferenceDB(String dbName, 
                                            XMLFileAdaptor fileAdaptor) throws Exception {
        GKInstance referenceDB = null;
        // Check if it is created already
        Collection list = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase,
                                                               ReactomeJavaConstants.name,
                                                               "=",
                                                               dbName);
        if (list != null && list.size() > 0)
            referenceDB = (GKInstance) list.iterator().next();
        else {
            referenceDB = fileAdaptor.createNewInstance(ReactomeJavaConstants.ReferenceDatabase);
            referenceDB.setAttributeValue(ReactomeJavaConstants.name,
                                          dbName);
        }
        return referenceDB;
    }

    public static String grepDBAndIDFromXref(OWLIndividual bpInstance, 
                                             BioPAXFactory bpFactory) {
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.XREF);
        OWLIndividual xref = (OWLIndividual) bpInstance.getPropertyValue(prop);
        if (xref == null)
            return null;
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.DB);
        String db = (String) xref.getPropertyValue(prop);
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.ID);
        String id = (String) xref.getPropertyValue(prop);
        return db + ":" + id;
    }
    
}
