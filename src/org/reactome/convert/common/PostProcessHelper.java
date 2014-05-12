/*
 * Created on Aug 8, 2006
 *
 */
package org.reactome.convert.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.gk.database.SynchronizationManager;
import org.gk.database.util.LiteratureReferenceAttributeAutoFiller;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaClass;
import org.reactome.b2rMapper.B2RMapperUtilities;
import org.reactome.biopax.AraCycPostProcessor;
import org.reactome.px.util.FileUtility;


/**
 * This helper class provides some common functions for post-processing the converted 
 * Reactome project from BioPAX.
 * @author guanming
 *
 */
public class PostProcessHelper {
    private static final Logger logger = Logger.getLogger(AraCycPostProcessor.class);

    @SuppressWarnings("unchecked")
    public static void mergeReferenceEntity(GKInstance targetRefPepSeq,
                                            GKInstance sourceRefPepSeq,
                                            XMLFileAdaptor fileAdaptor) throws Exception {
        Collection referrers = sourceRefPepSeq.getReferers(ReactomeJavaConstants.referenceEntity);
        if (referrers != null && referrers.size() > 0) {
            for (Iterator it = referrers.iterator(); it.hasNext();) {
                GKInstance referrer = (GKInstance) it.next();
                List list = referrer.getAttributeValuesListNoCheck(ReactomeJavaConstants.referenceEntity);
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) == sourceRefPepSeq) {
                        list.set(i, targetRefPepSeq);
                        break;
                    }
                }
            }
        }
        fileAdaptor.deleteInstance(sourceRefPepSeq);
    }
    
    public static String mapEntrezToUniProt(String fileName,
                                            String entrez) throws IOException {
        String uniProt = null;
        FileUtility fu = new FileUtility();
        fu.setInput(fileName);
        String line = null;
        int index = 0;
        String part1 = null;
        String part2 = null;
        while (uniProt == null &&
               (line = fu.readLine()) != null){
            index = line.indexOf("\t");
            part1 = line.substring(0, index);
            part2 = line.substring(index + 1);
            index = part1.indexOf(";");
            if (index < 0) {
                if (part1.equals(entrez)) {
                    uniProt = part2;
                    break;
                }
            }
            else {
                String tokens[] = part1.split(";");
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].trim().equals(entrez)) {
                        uniProt = part2;
                        break;
                    }
                }
            }
        }
        fu.close();
        return uniProt;
    }

    /**
     * Use the database instances for the specified classes if corresponding
     * ones can be found.
     * @param clsName
     * @param dbAdaptor
     * @param fileAdaptor
     * @throws Exception
     */
    public static void useDBInstances(String clsName,
                                      MySQLAdaptor dbAdaptor,
                                      XMLFileAdaptor fileAdaptor) throws Exception {
        SynchronizationManager manager = SynchronizationManager.getManager();
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        Collection collection = fileAdaptor.fetchInstancesByClass(clsName);
        if (collection == null || collection.size() == 0)
            return;
        GKInstance local = null;
        GKInstance db = null;
        Collection matchCollection = null;
        for (Iterator it = collection.iterator(); it.hasNext();) {
            local = (GKInstance) it.next();
            if (local.getDBID() > 0)
                continue; // This reference is already downloaded from database.
            matchCollection = dbAdaptor.fetchIdenticalInstances(local);
            if (matchCollection != null && matchCollection.size() > 0) {
                db = (GKInstance) matchCollection.iterator().next();
                logger.info(local + " will be replaced with " + db);
                updateFromDB(local, db, manager);
            }
        }
    }
    
    /**
     * A simple refactored method to query a repository for a RefPepSeq instance.
     * @param uniProtId
     * @param adaptor
     * @return
     * @throws Exception
     */
    public static GKInstance queryRefPepSeq(String uniProtId,
                                            PersistenceAdaptor adaptor) throws Exception {
        Collection<?> c = null;
        if (uniProtId.contains("-")) {
            c = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceIsoform, 
                                                 ReactomeJavaConstants.variantIdentifier,
                                                 "=",
                                                 uniProtId);
            if (c != null && c.size() > 0)
                return (GKInstance) c.iterator().next();
        }
        else {
            c = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct,
                                                 ReactomeJavaConstants.identifier,
                                                 "=",
                                                 uniProtId);
            // We will try to use ReferenceGeneProduct only instead of its isforms
            if (c != null && c.size() > 0) {
                for (Iterator<?> it = c.iterator(); it.hasNext();) {
                    GKInstance inst = (GKInstance) it.next();
                    if (inst.getSchemClass().getName().equals(ReactomeJavaConstants.ReferenceGeneProduct))
                        return inst;
                }
                // Anything should be fine
                return (GKInstance) c.iterator().next();
            }
        }
        return null;
    }
    
    /**
     * Get a GKInstance for the specified uniProtId. The local project will be searched
     * first. If it cannot be found, the specified database will be
     * searched. If a GKInstance still cannot be found in the database, a local one
     * will be created. This method should be used in the post-processing step.
     * @param uniProtId
     * @param dbAdaptor
     * @param fileAdaptor
     * @return A Reference Peptide Sequence
     * @throws Exception
     */
    public static GKInstance getRefPepSeq(String uniProtId,
                                          MySQLAdaptor dbAdaptor,
                                          XMLFileAdaptor fileAdaptor) throws Exception {
        // Have to strip off UniProt in id
        if (uniProtId.startsWith("UniProt:"))
            uniProtId = uniProtId.substring(8);
        // Search in the local project
        GKInstance refPepSeq = queryRefPepSeq(uniProtId, fileAdaptor);
        if (refPepSeq != null)
            return refPepSeq;
        GKInstance dbInstance = queryRefPepSeq(uniProtId, dbAdaptor);
        if (dbInstance != null) {
            // Download it to the local project
            refPepSeq = downloadDBInstance(dbInstance, fileAdaptor);
        }
        if (refPepSeq != null)
            return refPepSeq;
        return createLocalRefPepSeq(uniProtId, dbAdaptor, fileAdaptor);
    }

    public static GKInstance createLocalRefPepSeq(String uniProtId,
                                                   MySQLAdaptor dbAdaptor,
                                                   XMLFileAdaptor fileAdaptor) throws Exception {
        GKInstance refPepSeq;
        if (uniProtId.contains("-")) {
            refPepSeq = fileAdaptor.createNewInstance(ReactomeJavaConstants.ReferenceIsoform);
            refPepSeq.setAttributeValue(ReactomeJavaConstants.variantIdentifier,
                                        uniProtId);
            // Get the actual id
            int index = uniProtId.indexOf("-");
            uniProtId = uniProtId.substring(0, index);
        }
        else {
            // Create a new ReferencePeptideSequence
            refPepSeq = fileAdaptor.createNewInstance(ReactomeJavaConstants.ReferenceGeneProduct);
        }
        refPepSeq.setAttributeValue(ReactomeJavaConstants.identifier,
                                    uniProtId);
        GKInstance uniProt = getUniProtInstance(dbAdaptor, fileAdaptor);
        refPepSeq.setAttributeValue(ReactomeJavaConstants.referenceDatabase,
                                    uniProt);
        return refPepSeq;
    }
    
    public static void switchEWASToSet(GKInstance ewas,
                                       Set<GKInstance> refPepSeqSet,
                                       XMLFileAdaptor fileAdaptor) throws Exception {
        // Should be copied to any member
        List hasModifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        // Need to keep this value otherwise, the list will be emptied after the following type switch
        if (hasModifiedResidues != null && hasModifiedResidues.size() > 0)
            hasModifiedResidues = new ArrayList<GKInstance>(hasModifiedResidues);
        GKSchemaClass definedSetCls = (GKSchemaClass) fileAdaptor.getSchema().getClassByName(ReactomeJavaConstants.DefinedSet);
        fileAdaptor.switchType(ewas, definedSetCls);
        GKInstance compartment = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.compartment);
        for (GKInstance refPepSeq : refPepSeqSet) {
            // Create EWAS for each ReferencePeptideSequence
            GKInstance newEwas = createEWASFromRefPepSeq(compartment, 
                                                         hasModifiedResidues,
                                                         refPepSeq, 
                                                         fileAdaptor);
            ewas.addAttributeValue(ReactomeJavaConstants.hasMember,
                                   newEwas);
        }
    }
    
    private static GKInstance createEWASFromRefPepSeq(GKInstance compartment,
                                                      List hasModifiedResidues,
                                                      GKInstance refPepSeq,
                                                      XMLFileAdaptor fileAdaptor) throws Exception {
        // Since an EWAS may be changed to an set during the process of switchEWASToSet(). The following code
        // has been commented out so that always a new EWAS will be created.
//         // Try to find if an equivalent EWAS has been created already
//         Collection collection = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.EntityWithAccessionedSequence,
//                                                                      ReactomeJavaConstants.referenceEntity,
//                                                                      "=",
//                                                                      refPepSeq);
//         if (collection != null && collection.size() > 0) {
//             GKInstance gkInstance = null;
//             for (Iterator it = collection.iterator(); it.hasNext();) {
//                 gkInstance = (GKInstance) it.next();
//                 // Check compartment
//                 // Have to make sure there is no other defined attribute values
//                 if (gkInstance.getAttributeValue(ReactomeJavaConstants.compartment) == compartment &&
//                     gkInstance.getAttributeValue(ReactomeJavaConstants.hasModifiedResidue) == null &&
//                     gkInstance.getAttributeValue(ReactomeJavaConstants.endCoordinate) == null &&
//                     gkInstance.getAttributeValue(ReactomeJavaConstants.startCoordinate) == null)
//                     return gkInstance;    
//             }
//         }
         // Create EWAS for each ReferencePeptideSequence
         GKInstance newEwas = fileAdaptor.createNewInstance(ReactomeJavaConstants.EntityWithAccessionedSequence);
         // Compartment can be fetched from the original ewas
         if (compartment != null)
             newEwas.setAttributeValue(ReactomeJavaConstants.compartment,
                                       compartment);
         if (hasModifiedResidues != null)
             newEwas.setAttributeValue(ReactomeJavaConstants.hasModifiedResidue, 
                                       hasModifiedResidues);
         // The above should be called first to make _displayName correct.
         InstanceUtilities.copyAttributesFromRefPepSeqToEwas(newEwas, refPepSeq);
         return newEwas;
     }
    
    public static void updateFromDB(GKInstance local,
                                    GKInstance db,
                                    SynchronizationManager manager) throws Exception {
        Long oldDbId = local.getDBID();
        local.setDBID(db.getDBID());
        XMLFileAdaptor fileAdaptor = (XMLFileAdaptor) local.getDbAdaptor();
        fileAdaptor.dbIDUpdated(oldDbId, 
                                local);
        manager.updateFromDB(local, db);
        local.setIsDirty(false);
    }
    
    public static GKInstance downloadDBInstance(GKInstance dbInstance,
                                                 XMLFileAdaptor fileAdaptor) throws Exception {
        // Need to set to avoid null exception
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        SynchronizationManager.getManager().checkOutShallowly(dbInstance);
        GKInstance local = fileAdaptor.fetchInstance(dbInstance.getDBID());
        return local;
    }
    
    public static GKInstance getDatabaseIdentifier(String identifier,
                                                   String dbName,
                                                   XMLFileAdaptor fileAdaptor,
                                                   MySQLAdaptor dbAdaptor) throws Exception {
        // Try to get from the local project
        Collection<?> c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseIdentifier,
                                                               ReactomeJavaConstants.identifier,
                                                               "=",
                                                               identifier);
        if (c != null && c.size() > 0) {
            for (Iterator<?> it = c.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                GKInstance db = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
                if (db != null && db.getDisplayName().equals(dbName))
                    return inst;
            }
        }
        // Check if this instance is in the database
        c = dbAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseIdentifier,
                                               ReactomeJavaConstants.identifier,
                                               "=",
                                               identifier);
        if (c != null && c.size() > 0) {
            for (Iterator<?> it = c.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                GKInstance db = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
                if (db != null && db.getDisplayName().equals(dbName)) {
                    // Need to download this instance to local project
                    return downloadDBInstance(inst, fileAdaptor);
                }
            }
        }
        // Need to create a new instance
        GKInstance inst = fileAdaptor.createNewInstance(ReactomeJavaConstants.DatabaseIdentifier);
        inst.setAttributeValue(ReactomeJavaConstants.identifier, identifier);
        GKInstance db = B2RMapperUtilities.getReferenceDB(dbName, fileAdaptor);
        if (db == null)
            throw new java.lang.IllegalStateException("Cannot find a ReferenceDatabase instance in the local project: " + dbName);
        inst.setAttributeValue(ReactomeJavaConstants.referenceDatabase, db);
        InstanceDisplayNameGenerator.setDisplayName(inst);
        return inst;
    }
    
    /**
     * Use this method to get the ReferenceDatabase GKInstance for the UniProt database.
     * The local project will be searched first. If a local ReferenceDatabase cannot be
     * found, the database will be searched.
     * @param dbAdaptor
     * @param fileAdaptor
     * @return A Uniprot Reference Instance
     * @throws Exception
     */
    public static GKInstance getUniProtInstance(MySQLAdaptor dbAdaptor,
                                                XMLFileAdaptor fileAdaptor) throws Exception {
        GKInstance uniProt = null;
        Collection c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase,
                                                            ReactomeJavaConstants.name,
                                                            "=",
                                                            "UniProt");
        if (c != null && c.size() > 0)
            return (GKInstance) c.iterator().next();
        c = dbAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase,
                                               ReactomeJavaConstants.name,
                                               "=",
                                               "UniProt");
        // Sure c should not be null. Other something is wrong!
        uniProt = (GKInstance) c.iterator().next();
        uniProt = downloadDBInstance(uniProt, fileAdaptor);
        return uniProt;
    }
    
    public static GKInstance getHumanInstance(MySQLAdaptor dbAdaptor,
                                              XMLFileAdaptor fileAdaptor) throws Exception {
        GKInstance human = null;
        Collection c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species,  
                                                            ReactomeJavaConstants.name,
                                                            "=",
                                                            "Homo sapiens");
        if (c != null && c.size() > 0)
            return (GKInstance) c.iterator().next();
        c = dbAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species, 
                                               ReactomeJavaConstants.name, 
                                               "=", 
                                               "Homo sapiens");
        human = (GKInstance) c.iterator().next();
        human = downloadDBInstance(human, fileAdaptor);
        return human;
    }
    
    /**
     * Fetch an GKInstnce with the specified DB_ID. A local version should be returned. If there is
     * no local version available, the database will be queried, and downloaded.
     * @param dbId
     * @param fileAdaptor
     * @param dbAdaptor
     * @return
     * @throws Exception
     */
    public static GKInstance getInstance(Long dbId,
                                         XMLFileAdaptor fileAdaptor,
                                         MySQLAdaptor dbAdaptor) throws Exception {
        GKInstance inst = fileAdaptor.fetchInstance(dbId);
        if (inst != null)
            return inst;
        inst = dbAdaptor.fetchInstance(dbId);
        inst = downloadDBInstance(inst, fileAdaptor);
        return inst;
    }
    
    public static void processLiteratureReferences(MySQLAdaptor dbAdaptor,
                                                  XMLFileAdaptor fileAdaptor) throws Exception {
        // Try to use database instances 
        useDBInstances(ReactomeJavaConstants.LiteratureReference, 
                       dbAdaptor,
                       fileAdaptor);
        Collection litRefs = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.LiteratureReference);
        if (litRefs == null || litRefs.size() == 0)
            return;
        LiteratureReferenceAttributeAutoFiller autoFiller = new LiteratureReferenceAttributeAutoFiller();
        autoFiller.setPersistenceAdaptor(fileAdaptor);
        GKInstance litRef = null;
        for (Iterator it = litRefs.iterator(); it.hasNext();) {
            litRef = (GKInstance) it.next();
            if (litRef.getDBID() > 0)
                continue; // This reference is downloaded from database.
            Object pubmedId = litRef.getAttributeValue(ReactomeJavaConstants.pubMedIdentifier);
            if (pubmedId == null)
                continue; // Cannot do anything
            autoFiller.process(litRef, null);
            InstanceDisplayNameGenerator.setDisplayName(litRef);
        }
    }
    
    /**
     * Set precedingEvent properties for components contained by the specified topPathway.
     * @param topPathway
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static void generatePrecedingProperties(GKInstance topPathway) throws Exception {
        List events = null;
        if (topPathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
            events = topPathway.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        else if (topPathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
            events = topPathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
        if (events == null || events.size() == 0)
            return;
        GKInstance event = null;
        List<GKInstance> precedingEvents = null;
        for (Iterator it = events.iterator(); it.hasNext();) {
            event = (GKInstance) it.next();
            precedingEvents = searchPrecedingEvents(event, events);
            if (precedingEvents.size() > 0) {
                // Need to merge with original values.
                // Note: one event might be used in other top pathways too. So
                // set will overwrite values set in other pathways.
                List originalValue = event.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
                if (originalValue == null || originalValue.size() == 0)
                    event.setAttributeValue(ReactomeJavaConstants.precedingEvent,
                                            precedingEvents);
                else {
                    // Meaning there is something already
                    for (Iterator it1 = precedingEvents.iterator(); it1.hasNext();) {
                        Object next = it1.next();
                        if (!originalValue.contains(next))
                            originalValue.add(next);
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static List<GKInstance> searchPrecedingEvents(GKInstance current, List events) throws Exception {
        List<GKInstance> rtn = new ArrayList<GKInstance>();
        // current's inputs should be outputs of other events
        Set<GKInstance> inputs = new HashSet<GKInstance>();
        if (current.getSchemClass().isValidAttribute(ReactomeJavaConstants.input)) {
            List list = current.getAttributeValuesList(ReactomeJavaConstants.input);
            if (list != null)
                inputs.addAll(list);
        }
        if (current.getSchemClass().isValidAttribute(ReactomeJavaConstants.catalystActivity)) {
            List cas = current.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
            if (cas != null && cas.size() > 0) {
                for (Iterator it = cas.iterator(); it.hasNext();) {
                    GKInstance ca = (GKInstance) it.next();
                    GKInstance catalyst = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                    if (catalyst.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
                        inputs.add(catalyst);
                }
            }
        }
        // Make positive regulations too
        Collection posRegulations = current.getReferers(ReactomeJavaConstants.regulatedEntity);
        if (posRegulations != null && posRegulations.size() > 0) {
            for (Iterator it = posRegulations.iterator(); it.hasNext();) {
                GKInstance regulation = (GKInstance) it.next();
                if (regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation)) {
                    GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                    if (regulator != null && 
                        regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
                        inputs.add(regulator);
                }
            }
        }
        if (inputs == null || inputs.size() == 0)
            return rtn; // No chance to find any preceding events
        GKInstance event = null;
        for (Iterator it = events.iterator(); it.hasNext();) {
            event = (GKInstance) it.next();
            if (event == current)
                continue; // Escape itself
            if (!(event.getSchemClass().isValidAttribute(ReactomeJavaConstants.output)))
                continue; // input, output have been invalid in the Pathway class.
            List outputs = event.getAttributeValuesList(ReactomeJavaConstants.output);
            if (outputs == null || outputs.size() == 0)
                continue;
            if (intersect(inputs, outputs)) {
                rtn.add(event);
            }
        }
        return rtn;
    }
    
    private static boolean intersect(Collection list1, 
                                     Collection list2) {
        for (Iterator it = list1.iterator(); it.hasNext();) {
            if (list2.contains(it.next()))
                return true;
        }
        return false;
    }
    
    public static void resetDisplayNames(String clsName,
                                         XMLFileAdaptor fileAdaptor) throws Exception {
        Collection<?> collections = fileAdaptor.fetchInstancesByClass(clsName);
        if (collections == null || collections.size() == 0)
            return;
        for (Iterator<?> it = collections.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            InstanceDisplayNameGenerator.setDisplayName(instance);
        }
    }
    
}
