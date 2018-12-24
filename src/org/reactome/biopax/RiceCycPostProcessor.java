/*
 * Created on Jul 5, 2007
 *
 */
package org.reactome.biopax;

import java.io.File;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.gk.database.DefaultInstanceEditHelper;
import org.gk.database.EventCheckOutHandler;
import org.gk.database.SynchronizationManager;
import org.gk.database.util.ChEBIAttributeAutoFiller;
import org.gk.elv.InstanceCloneHelper;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.slicing.SlicingEngine;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.Test;
import org.reactome.b2rMapper.B2RMapperUtilities;
import org.reactome.convert.common.PostProcessHelper;
import org.reactome.convert.common.ReactomeProjectDumper;
import org.reactome.px.util.FileUtility;
import org.reactome.px.util.InteractionUtilities;

/**
 * The following procedures should be used to merge Rice Reactome into gk_central base:
 * Here are procedures to merge rice reactome:

1). Create a new person Guanming Wu for project "Rice Reactome"
2). Use its DB_ID for method in runPreDumpOnDb() and run the method. 
3). Change column definition: alter table DatabaseIdentifier change column identifier identifier varchar(50) 
4). Use its DB_ID from 1) for method dumpToDatabase() and run the method. 
    If needed, run processBeforeDump(). (use "-Djava.awt.Headless=true" when run at reactomedev).
6). Run fixReferenceGenesAfterDump() to replace Ensebml by RiceCyc, and change _displayNames 
    for these ReferenceGeneProducts.
 * @author wgm
 *
 */
public class RiceCycPostProcessor implements BioPAXToReactomePostProcessor {
    private static final Logger logger = Logger.getLogger(RiceCycPostProcessor.class);
    private final String DIR_NAME = "/Users/wgm/Documents/gkteam/Liya/";
    //private final String DIR_NAME = "/Users/wgm/Documents/gkteam/Arabidopsis/";
//    private final String DIR_NAME = "resources/";
    
    public RiceCycPostProcessor() {
    }
    
    @Test
    public void runPostProcess() throws Exception {
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        fileAdaptor.setSource(DIR_NAME + "ath00010_1.rtpj");
        postProcess(null, fileAdaptor);
    }

    public void postProcess(MySQLAdaptor dbAdaptor, 
                            XMLFileAdaptor fileAdaptor) throws Exception {
        logger.info("Starting changeDisplayNameForRefGeneProducts()...");
        changeDisplayNameForRefGeneProducts(fileAdaptor);
        logger.info("copyNameFromRefToEntity...");
        copyNameFromRefToEntity(fileAdaptor);
        logger.info("mergePEs...");
        mergePEs(fileAdaptor);
        // The above should be called before the following so that
        // PEs can be merged together.
        logger.info("createEntitySetForMultipleCAS...");
        createEntitySetForMultipleCAS(fileAdaptor);
        logger.info("splitCatalystActivitites...");
        splitCatalystActivities(fileAdaptor);
        logger.info("attachSpecies...");
        attachSpecies(dbAdaptor, 
                      fileAdaptor);
        logger.info("assignCompartmentsToReactions...");
        assignCompartmentsToReactions(fileAdaptor);
        logger.info("assign display name for reaction");
        assignReactionDisplayNameFromInputOutput(fileAdaptor);
        logger.info("processing LiteratureReference...");
        PostProcessHelper.processLiteratureReferences(dbAdaptor, 
                                                      fileAdaptor);
        // Update the database first so that GO can be compared
        logger.info("Update Reference Database...");
        updateReferenceDatabases(fileAdaptor, 
                                 dbAdaptor);
        logger.info("Update GO compartments...");
        updateGOCellularComponent(fileAdaptor,
                                  dbAdaptor);
        // update RiceCyc:LOG_*** as identifier for ReferenceGeneProduct
        logger.info("Update ReferenceGeneProducts...");
        updateReferenceGeneProduct(fileAdaptor);
        logger.info("Update ReferenceMolecules...");
        updateReferenceMolecules(fileAdaptor, dbAdaptor);
        logger.info("Merge EC numbers...");
        updateECNumbers(fileAdaptor, dbAdaptor);
        logger.info("Using SimpleEntities in DB...");
        PostProcessHelper.useDBInstances(ReactomeJavaConstants.SimpleEntity, 
                                         dbAdaptor, 
                                         fileAdaptor);
        logger.info("Update PE names...");
        updatePENames(fileAdaptor);
        logger.info("Cleaning DatabaseIdentifiers...");
        cleanUpDatabaseIdentifiers(fileAdaptor);
        //fileAdaptor.save(DIR_NAME + "tmp.rtpj");
        //changePathwayAttName("tmp.rtpj");
    }
    
    private void assignReactionDisplayNameFromInputOutput(XMLFileAdaptor fileAdaptor) throws Exception {
        Collection<?> reactions = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        StringBuilder builder = new StringBuilder();
        for (Iterator<?> it = reactions.iterator(); it.hasNext();) {
            GKInstance reaction = (GKInstance) it.next();
            String displayName = reaction.getDisplayName();
            if (displayName.startsWith("RXN") ||
                displayName.startsWith("TRANS-RXN") ||
                displayName.endsWith("-RXN")) {
                List<?> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
                List<?> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
                if ((inputs == null || inputs.size() == 0) ||
                    (outputs == null || outputs.size() == 0))
                    continue; // Do nothing if no inputs or outputs: not very interesting if either one is not there
                builder.setLength(0);
                for (Iterator<?> it1 = inputs.iterator(); it1.hasNext();) {
                    GKInstance input = (GKInstance) it1.next();
                    String name = getMeaningfulNameFromPE(input);
                    builder.append(name);
                    if (it1.hasNext())
                        builder.append("+");
                }
                builder.append("->");
                for (Iterator<?> it1 = outputs.iterator(); it1.hasNext();) {
                    GKInstance output = (GKInstance) it1.next();
                    String name = getMeaningfulNameFromPE(output);
                    builder.append(name);
                    if (it1.hasNext())
                        builder.append("+");
                }
                
                List<String> names = reaction.getAttributeValuesList(ReactomeJavaConstants.name);
                names.add(0, builder.toString());
                InstanceDisplayNameGenerator.setDisplayName(reaction);
            }
        }
    }
    
    private String getMeaningfulNameFromPE(GKInstance pe) throws Exception {
        List<?> names = pe.getAttributeValuesList(ReactomeJavaConstants.name);
        for (Iterator<?> it = names.iterator(); it.hasNext();) {
            String name = (String) it.next();
            if (!name.startsWith("phys-ent-participant"))
                return name;
        }
        return (String) names.get(0);
    }
    
    @Test
    public void fixDisplayNames() throws Exception {
        String srcFileName = DIR_NAME + "ricecyc_v3_0_biopax2.rtpj";
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor(srcFileName);
        changeDisplayNameForRefGeneProducts(fileAdaptor);
        copyNameFromRefToEntity(fileAdaptor);
        fileAdaptor.save(DIR_NAME + "ricecyc_v3_0_biopax2_fixed.rtpj");
    }
    
    @Test
    public void fixInitialInPersons() throws Exception {
        String srcFileName = DIR_NAME + "aracyc_v7_0_biopax-level2_5.rtpj";
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor(srcFileName);
        Collection<?> c = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Person);
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance person = (GKInstance) it.next();
            String initial = (String) person.getAttributeValue(ReactomeJavaConstants.initial);
            if (initial == null || initial.length() < 5)
                continue;
            initial = initial.substring(0, 5).toUpperCase();
            person.setAttributeValue(ReactomeJavaConstants.initial, initial);
            System.out.println("Fix: " + person);
        }
        fileAdaptor.save(DIR_NAME + "aracyc_v7_0_biopax-level2_5_fix_initial.rtpj");
    }
    
    /**
     * PEP is not reusable in BioPAX. However, it should be re-usable in Reactome.
     * @param fileAdaptor
     * @throws Exception
     */
    private void mergePEs(XMLFileAdaptor fileAdaptor) throws Exception {
        // Start from ReferenceEntity instances
        Collection refEntities = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceEntity);
        if (refEntities == null || refEntities.size() == 0)
            return; // Nothing can be done
        // Pre-generating a map to avoid call the slower process XMLFileAdaptor.getReferrers()
        Map<GKInstance, Set<GKInstance>> refToPEs = new HashMap<GKInstance, Set<GKInstance>>();
        Collection peps = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.PhysicalEntity);
        for (Iterator it = peps.iterator(); it.hasNext();) {
            GKInstance pe = (GKInstance) it.next();
            if (pe.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
                GKInstance refEntity = (GKInstance) pe.getAttributeValue(ReactomeJavaConstants.referenceEntity);
                if (refEntity == null)
                    continue; // Don't work on null
                Set<GKInstance> set = refToPEs.get(refEntity);
                if (set == null) {
                    set = new HashSet<GKInstance>();
                    refToPEs.put(refEntity, set);
                }
                set.add(pe);
            }
        }
        for (GKInstance refEntity : refToPEs.keySet()) {
            Set<GKInstance> pes = refToPEs.get(refEntity);
            logger.info("Merging PEs for " + refEntity + "...");
            mergePEs(pes, fileAdaptor);
        }
    }
    
    /**
     * The actual merge method.
     * @param pes
     * @param fileAdpator
     * @throws Exception
     */
    private void mergePEs(Collection pes,
                          XMLFileAdaptor fileAdaptor) throws Exception {
        List list = new ArrayList(pes);
        List<GKInstance> removed = new ArrayList<GKInstance>();
        for (int i = 0; i < list.size() - 1; i++) {
            GKInstance inst1 = (GKInstance) list.get(i);
            for (int j = i + 1; j < list.size(); j++) {
                GKInstance inst2 = (GKInstance) list.get(j);
                if (InstanceUtilities.areMergable(inst1, inst2)) {
                    mergeInstance(inst1, 
                                  inst2,
                                  fileAdaptor);
                    removed.add(inst2);
                }
            }
            list.removeAll(removed);
            removed.clear();
        }
    }
    
    private void mergeInstance(GKInstance kept,
                               GKInstance removed,
                               XMLFileAdaptor fileAdaptor) throws Exception {
        // Default to merge names
        mergeInstance(kept, 
                      removed, 
                      fileAdaptor, 
                      true);
    }
    
    private void mergeInstance(GKInstance kept,
                               GKInstance removed,
                               XMLFileAdaptor fileAdaptor,
                               boolean needMergeName) throws Exception {
        if (needMergeName) {
            if (kept.getSchemClass().isValidAttribute(ReactomeJavaConstants.name)) {
                // Merge names only
                Set<String> names = new HashSet<String>();
                names.addAll(kept.getAttributeValuesList(ReactomeJavaConstants.name));
                names.addAll(removed.getAttributeValuesList(ReactomeJavaConstants.name));
                kept.setAttributeValue(ReactomeJavaConstants.name, new ArrayList<String>(names));
            }
        }
        List referrers = fileAdaptor.getReferers(removed);
        if (referrers != null && referrers.size() > 0) {
            for (Iterator it = referrers.iterator(); it.hasNext();){
                GKInstance referrer = (GKInstance) it.next();
                // Find the referrer and replace it with keptInstance
                InstanceUtilities.replaceReference(referrer, removed, kept);
            }
        }
        // Deleting the deletingInstance
        fileAdaptor.deleteInstance(removed);
    }
    
    private void copyNameFromRefToEntity(XMLFileAdaptor fileAdaptor) throws Exception {
        Collection entities = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.PhysicalEntity);
        if (entities == null || entities.size() == 0)
            return;
        GKInstance entity = null;
        for (Iterator it = entities.iterator(); it.hasNext();) {
            entity = (GKInstance) it.next();
            if (!entity.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity))
                continue;
            GKInstance reference = (GKInstance) entity.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (reference == null)
                continue;
            String refName = getNameFromReference(reference);
            List list = entity.getAttributeValuesList(ReactomeJavaConstants.name);
            if (list == null || !list.contains(refName)) {
                if (list == null)
                    entity.addAttributeValue(ReactomeJavaConstants.name, refName);
                else
                    list.add(0, refName);
                InstanceDisplayNameGenerator.setDisplayName(entity);
            }
        }
        // Need to reset names in CA since the above changes
        Collection cas = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.CatalystActivity);
        if (cas == null || cas.size() == 0)
            return;
        for (Iterator it = cas.iterator(); it.hasNext();) {
            GKInstance ca = (GKInstance) it.next();
            InstanceDisplayNameGenerator.setDisplayName(ca);
        }
    }
    
    private void assignCompartmentsToReactions(XMLFileAdaptor fileAdaptor) throws Exception {
        Collection reactions = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        if (reactions == null || reactions.size() == 0)
            return ;
        Set<GKInstance> set = new HashSet<GKInstance>();
        // This instance should be removed
        GKInstance unknown = getUnknownCompartment(fileAdaptor);
        for (Iterator it = reactions.iterator(); it.hasNext();) {
            GKInstance rxt = (GKInstance) it.next();
            Set<GKInstance> participants = InstanceUtilities.getReactionParticipants(rxt);
            set.clear();
            for (GKInstance participant : participants) {
                if (participant.getSchemClass().isValidAttribute(ReactomeJavaConstants.compartment)) {
                    List list = participant.getAttributeValuesList(ReactomeJavaConstants.compartment);
                    if (list != null)
                        set.addAll(list);
                }
            }
            if (unknown != null)
                set.remove(unknown);
            if (set.size() > 0) {
                rxt.setAttributeValue(ReactomeJavaConstants.compartment,
                                      new ArrayList<GKInstance>(set));
                assignCompartmentFromRxtToCatalyst(set, 
                                                   unknown, 
                                                   rxt);
            }
        }
    }

    private <E> void assignCompartmentFromRxtToCatalyst(Collection<E> set,
                                                    GKInstance unknown,
                                                    GKInstance rxt) throws Exception {
        // Check catalyst
        GKInstance cas = (GKInstance) rxt.getAttributeValue(ReactomeJavaConstants.catalystActivity);
        if (cas != null) {
            GKInstance ca = (GKInstance) cas.getAttributeValue(ReactomeJavaConstants.physicalEntity);
            if (ca != null) {
                List<?> caCompartments = ca.getAttributeValuesList(ReactomeJavaConstants.compartment);
                caCompartments.remove(unknown);
                if (caCompartments.size() == 0) {
                    ca.setAttributeValue(ReactomeJavaConstants.compartment, 
                                         new ArrayList<E>(set));
                }
                // Need to reset display name.
                InstanceDisplayNameGenerator.setDisplayName(ca);
                InstanceDisplayNameGenerator.setDisplayName(cas);
            }
        }
    }
    
    private GKInstance getUnknownCompartment(XMLFileAdaptor fileAdaptor) throws Exception {
        Collection c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Compartment,
                                                            ReactomeJavaConstants.accession,
                                                            "=",
                                                            "0008372");
        if (c != null && c.size() > 0)
            return (GKInstance) c.iterator().next();
        return null;
    }
    
    private void attachSpecies(MySQLAdaptor dbAdaptor,
                               XMLFileAdaptor fileAdaptor) throws Exception {
        Long riceId = 186860L;
        GKInstance rice = PostProcessHelper.getInstance(riceId, 
                                                        fileAdaptor, 
                                                        dbAdaptor);
        Collection c = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        for (Iterator it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
                GKInstance species = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.species);
                if (species != null)
                    continue;
                inst.setAttributeValue(ReactomeJavaConstants.species, rice);
            }
        }
    }
    
    private void changeDisplayNameForRefGeneProducts(XMLFileAdaptor fileAdaptor) throws Exception {
        Collection refGeneProducts = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
        if (refGeneProducts == null || refGeneProducts.size() == 0)
            return;
        for (Iterator it = refGeneProducts.iterator(); it.hasNext();) {
            GKInstance refGeneProd = (GKInstance) it.next();
            // Check if name and identifier existing
            GKInstance refDb = (GKInstance) refGeneProd.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
            GKInstance identifier = (GKInstance) refGeneProd.getAttributeValue(ReactomeJavaConstants.identifier);
            if (refDb == null && identifier == null) {
                // Get the display name from the name attribute
                String name = (String) refGeneProd.getAttributeValue(ReactomeJavaConstants.name);
                int index = name.indexOf(", "); // Use an extra space in case something like this: 1,3-beta-glucan synthase component domain containing protein, expressed
                if (index > 0)
                    name = name.substring(0, index);
                if (name.length() > 0)
                    refGeneProd.setDisplayName(name);
            }
        }
    }
    
    private String getNameFromReference(GKInstance reference) throws Exception {
        String rtn = null;
        String name = (String) reference.getAttributeValue(ReactomeJavaConstants.name);
        int index = name.indexOf(", ");
        if (index > 0)
            name = name.substring(0, index);
        if (name.length() > 0)
            rtn = name;
        else
            rtn = (String) reference.getAttributeValue(ReactomeJavaConstants.name);
        // Want to copy Locus Id to display name too
        List<?> xrefs = reference.getAttributeValuesList(ReactomeJavaConstants.crossReference);
        if (xrefs != null && xrefs.size() > 0) {
            // Find locus id
            for (Iterator<?> it = xrefs.iterator(); it.hasNext();) {
                GKInstance xref = (GKInstance) it.next();
                String displayName = xref.getDisplayName();
                if (displayName.startsWith("RiceCyc:LOC")) {
                    String locusId = (String) xref.getAttributeValue(ReactomeJavaConstants.identifier);
                    index = locusId.indexOf("-");
                    if (index > 0)
                        locusId = locusId.substring(0, index);
                    rtn = name + " (" + locusId + ")";
                }
            }
        }
        return rtn;
    }
    
    /**
     * Another way to handle multiple CAS converted from BioPAX.
     * @param fileAdaptor
     * @throws Exception
     */
    private void createEntitySetForMultipleCAS(XMLFileAdaptor fileAdaptor) throws Exception {
        Collection reactions = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        if (reactions == null || reactions.size() == 0)
            return;
        // This Map is used to avoid creating many duplication
        Map<String, GKInstance> keyToInstance = new HashMap<String, GKInstance>();
        Set<GKInstance> kept = new HashSet<GKInstance>();
        for (Iterator it = reactions.iterator(); it.hasNext();) {
            GKInstance reaction = (GKInstance) it.next();
            List cas = reaction.getAttributeValuesListNoCheck("CAS");
            if (cas == null || cas.size() < 2)
                continue; // No need
            kept.clear();
            List<GKInstance> entities = new ArrayList<GKInstance>();
            for (Iterator it1 = cas.iterator(); it1.hasNext();) {
                GKInstance ca = (GKInstance) it1.next();
                // Check if there is a Regulation attached to it, if true, leave it alone!
                Collection<?> referrers = ca.getReferers(ReactomeJavaConstants.regulatedEntity);
                if (referrers != null && referrers.size() > 0) {
                    kept.add(ca);
                    continue;
                }
                GKInstance pe = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                GKInstance activity = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.activity);
                if (activity != null)
                    continue; // Want to work with null activity CAS
                if (pe != null) {
                    entities.add(pe);
                }
            }
            if (entities.size() > 1) {
                GKInstance set = createEntitySet(fileAdaptor, 
                                                 entities,
                                                 keyToInstance);
                // Reset the attributes
                GKInstance ca = (GKInstance) reaction.getAttributeValue(ReactomeJavaConstants.catalystActivity);
                kept.add(ca);
                ca.setAttributeValue(ReactomeJavaConstants.physicalEntity,
                                     set);
                // These instances should be deleted
                for (Iterator it1 = cas.iterator(); it1.hasNext();) {
                    GKInstance ca1 = (GKInstance) it1.next();
                    if (ca == ca1 || kept.contains(ca1))
                        continue;
                    fileAdaptor.deleteInstance(ca1);
                }
                if (kept.size() < 2)
                    reaction.setAttributeValueNoCheck("CAS", null);
                else
                    reaction.setAttributeValueNoCheck("CAS", new ArrayList<GKInstance>(kept));
            }
        }
    }

    private GKInstance createEntitySet(XMLFileAdaptor fileAdaptor,
                                       List<GKInstance> entities,
                                       Map<String, GKInstance> keyToInstance) throws Exception {
        // Want to sort based on DB_IDs for simple keying
        Collections.sort(entities, new Comparator<GKInstance>() {
            public int compare(GKInstance inst1, GKInstance inst2) {
                return inst1.getDBID().compareTo(inst2.getDBID());
            }
        });
        StringBuilder builder = new StringBuilder();
        for (GKInstance inst : entities) {
            builder.append(inst.getDBID()).append(":");
        }
        String key = builder.toString();
        GKInstance set = keyToInstance.get(key);
        if (set != null)
            return set;
        set = fileAdaptor.createNewInstance(ReactomeJavaConstants.DefinedSet);
        keyToInstance.put(key, set);
        set.setAttributeValue(ReactomeJavaConstants.hasMember, entities);
        // Assign names from its member
        Set<String> names = new HashSet<String>();
        Set<GKInstance> compartments = new HashSet<GKInstance>();
        for (GKInstance member : entities) {
            String name = (String) member.getAttributeValue(ReactomeJavaConstants.name);
            names.add(name);
            GKInstance compartment = (GKInstance) member.getAttributeValue(ReactomeJavaConstants.compartment);
            if (compartment != null)
                compartments.add(compartment);
        }
        final List<String> nameList = new ArrayList<String>(names);
        Collections.sort(nameList, new Comparator<String>() {
            public int compare(String name1, String name2) {
                return name1.length() - name2.length();
            }
        });
        set.setAttributeValue(ReactomeJavaConstants.name, nameList);
        // Assign compartments
        if (compartments.size() > 0)
            set.setAttributeValue(ReactomeJavaConstants.compartment,
                                  new ArrayList<GKInstance>(compartments));
        InstanceDisplayNameGenerator.setDisplayName(set);
        return set;
    }
    
    /**
     * Only one value is allowed in the standard Reactome model. However, in the
     * extended model, multiple CAs are allowed. This CAs will be split into multiple
     * reactions.
     * @param fileAdaptor
     * @throws Exception
     */
    private void splitCatalystActivities(XMLFileAdaptor fileAdaptor) throws Exception {
        Collection reactions = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        for (Iterator it = reactions.iterator(); it.hasNext();) {
            GKInstance reaction = (GKInstance) it.next();
            //List cas = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
            // Recorded during mapping
            List cas = reaction.getAttributeValuesListNoCheck("CAS");
            if (cas == null || cas.size() < 2)
                continue; // Don't need to do anything
            System.out.println(reaction + " is splitting...");
            GKInstance ca = (GKInstance) cas.get(0);
            reaction.setAttributeValue(ReactomeJavaConstants.catalystActivity, ca);
            // Need to duplicate reaction first
            for (int i = 1; i < cas.size(); i++) {
                ca = (GKInstance) cas.get(i);
                GKInstance duplicated = duplicateReaction(reaction, fileAdaptor);
                duplicated.setAttributeValue(ReactomeJavaConstants.catalystActivity, 
                                           ca);
                // A no conventional use of hasMember to keep a good organization
                reaction.addAttributeValue(ReactomeJavaConstants.hasMember, 
                                           duplicated);
            }
        }
    }
    
    private GKInstance duplicateReaction(GKInstance reaction,
                                         XMLFileAdaptor fileAdaptor) throws Exception {
        GKInstance duplicate = fileAdaptor.createNewInstance(reaction.getSchemClass().getName());
        // Copy properties except DB_ID, catalystActivities
        // Be careful: the same list is used!!!!
        SchemaClass cls = reaction.getSchemClass();
        for (Iterator it = cls.getAttributes().iterator(); it.hasNext();) {
            SchemaAttribute att = (SchemaAttribute) it.next();
            if (att.getName().equals(ReactomeJavaConstants.DB_ID) ||
                att.getName().equals(ReactomeJavaConstants.catalystActivity) ||
                att.getName().equals(ReactomeJavaConstants.hasMember))
                continue;
            List values = reaction.getAttributeValuesList(att);
            if (values == null || values.size() == 0)
                continue;
            duplicate.setAttributeValueNoCheck(att,
                                               new ArrayList(values)); // Need to make a copy
        }
        return duplicate;
    }
    
    public void changePathwayAttName(String sourceName) throws Exception {
        FileUtility fu = new FileUtility();
        String inputFileName = DIR_NAME + sourceName;
        fu.setInput(inputFileName);
        FileUtility outFu = new FileUtility();
        String outputFileName = DIR_NAME + "ath00010_1NewSchema.rtpj";
        outFu.setOutput(outputFileName);
        boolean isInPathway = false;
        String line = null;
        String trimmed = null;
        while ((line = fu.readLine()) != null) {
            if (isInPathway) {
                line = line.replaceAll("hasComponent", "hasEvent");
            }
            else {
                trimmed = line.trim();
                if (trimmed.equals("<Pathway>")) {
                    isInPathway = true;
                }
                else if (trimmed.equals("</Pathway>"))
                    isInPathway = false;
            }
            outFu.printLine(line);
        }
        fu.close();
        outFu.close();
    }
    
    /**
     * Use GO terms from the database. Some of terms may have different class types. In this method,
     * class types will be determined based on the database. So some of them may not be correctly 
     * used in instances as invalid attribute types.
     * @param fileAdaptor
     * @param dba
     * @throws Exception
     */
    public void updateGOCellularComponent(XMLFileAdaptor fileAdaptor,
                                          MySQLAdaptor dba) throws Exception {
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        PersistenceManager.getManager().setActiveMySQLAdaptor(dba);
        Collection<?> instances = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.GO_CellularComponent);
        if (instances == null || instances.size() == 0)
            return;
        for (Iterator<?> it = instances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            String accession = (String) instance.getAttributeValue(ReactomeJavaConstants.accession);
            if (accession == null)
                continue; // Cannot do anything
            Collection<?> dbInstances = dba.fetchInstanceByAttribute(ReactomeJavaConstants.GO_CellularComponent,
                                                                     ReactomeJavaConstants.accession, 
                                                                     "=", 
                                                                     accession);
            if (dbInstances == null || dbInstances.size() == 0)
                continue;
            GKInstance dbInstance = (GKInstance) dbInstances.iterator().next();
            PostProcessHelper.updateFromDB(instance,
                                           dbInstance,
                                           SynchronizationManager.getManager());
        }
        // All other GO_cellularComponent should be deleted since they should not be used at all. However,
        // the compartment information has been kept at the definition slot.
        for (Iterator<?> it = instances.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            if (inst.getDBID() > 0)
                continue;
            String name = inst.getDisplayName();
            Collection<?> referrers = fileAdaptor.getReferers(inst);
            for (Iterator<?> it1 = referrers.iterator(); it1.hasNext();) {
                GKInstance referrer = (GKInstance) it1.next();
                referrer.addAttributeValue(ReactomeJavaConstants.definition,
                                           "Compartment: " + name);
            }
            fileAdaptor.deleteInstance(inst);
        }
        // Need to update display name for entities
        PostProcessHelper.resetDisplayNames(ReactomeJavaConstants.PhysicalEntity, 
                                            fileAdaptor);
    }
    
    /**
     * The actual place to do ReferenceDatabase instance mapping.
     * @param fileAdaptor
     * @param dbAdaptor
     * @param mapFileName
     * @throws Exception
     */
    protected void updateReferenceDatabases(XMLFileAdaptor fileAdaptor,
                                            MySQLAdaptor dbAdaptor,
                                            String mapFileName) throws Exception {
        Collection<?> collection = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceDatabase);
        Map<String, GKInstance> nameToRefDb = new HashMap<String, GKInstance>();
        for (Iterator<?> it = collection.iterator(); it.hasNext();) {
            GKInstance refDb = (GKInstance) it.next();
            String name = refDb.getDisplayName();
            nameToRefDb.put(name, refDb);
        }
        // Load the mapping file
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new File(mapFileName));
        Element root = document.getRootElement();
        List<?> children = root.getChildren();
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        PersistenceManager.getManager().setActiveMySQLAdaptor(dbAdaptor);
        SynchronizationManager manager = SynchronizationManager.getManager();
        for (Iterator<?> it = children.iterator(); it.hasNext();) {
            Element elm = (Element) it.next();
            String name = elm.getAttributeValue("name");
            GKInstance instance = nameToRefDb.get(name);
            if (instance == null)
                continue;
            String matched = elm.getAttributeValue("matched");
            if (matched != null && matched.length() > 0) {
                GKInstance dbInstance = dbAdaptor.fetchInstance(new Long(matched));
                PostProcessHelper.updateFromDB(instance, 
                                               dbInstance, 
                                               manager);
            }
            else {
                // Fill up information
                String url = elm.getAttributeValue("url");
                if (url != null && url.length() > 0) 
                    instance.setAttributeValue(ReactomeJavaConstants.url, url);
                String accessUrl = elm.getAttributeValue("accessUrl");
                if (accessUrl != null && accessUrl.length() > 0)
                    instance.setAttributeValue(ReactomeJavaConstants.accessUrl, accessUrl);
            }
        }
        // It is possible two local ReferenceDatabase instances point to the same instance
        // in the central Reactome database. In this case, these two local ReferenceDatabase 
        // instances should be merged together.
        Map<Long, Set<GKInstance>> dbIdToInsts = new HashMap<Long, Set<GKInstance>>();
        collection = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceDatabase);
        for (Iterator<?> it = collection.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            InteractionUtilities.addElementToSet(dbIdToInsts, inst.getDBID(), inst);
        }
        for (Long dbId : dbIdToInsts.keySet()) {
            Set<GKInstance> set = dbIdToInsts.get(dbId);
            if (set.size() < 2)
                continue;
            // Merging
            GKInstance kept = null;
            for (GKInstance tmp : set) {
                if (kept == null) 
                    kept = tmp;
                else {
                    mergeInstance(kept, tmp, fileAdaptor, false);
                }
            }
        }
    }
    
    /**
     * Update the ReferenceDatabase instances based on an external mapping file.
     * @param fileAdaptor
     * @param dbAdaptor
     * @throws Exception
     */
    public void updateReferenceDatabases(XMLFileAdaptor fileAdaptor,
                                         MySQLAdaptor dbAdaptor) throws Exception {
        String mapFileName = "resources/RiceReactomeDB.xml";
        updateReferenceDatabases(fileAdaptor, dbAdaptor, mapFileName);
    }
    
    /**
     * Run this method before dumping.
     * @throws Exception
     */
    @Test
    public void processBeforeDump() throws Exception {
        String projectFileName = DIR_NAME + "ricecyc_v3_0_biopax2_0709.rtpj";
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        fileAdaptor.setSource(projectFileName);
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "rice_reactome_v3_1",
                                            "root", 
                                            "macmysql01");
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        PersistenceManager.getManager().setActiveMySQLAdaptor(dba);
        postProcess(dba, fileAdaptor);
//        String destFileName = DIR_NAME + "ricecyc_v3_0_biopax2_before_dump_102010.rtpj";
//        String destFileName = DIR_NAME + "ricecyc_v3_0_biopax2_before_dump_112410.rtpj";
//        String destFileName = DIR_NAME + "ricecyc_v3_0_biopax2_before_dump_112910.rtpj";
//        String destFileName = DIR_NAME + "ricecyc_v3_0_biopax2_before_dump_120110.rtpj";
        String destFileName = DIR_NAME + "ricecyc_v3_0_biopax2_before_dump_120610.rtpj";
        fileAdaptor.save(destFileName);
    }
    
    private void updatePENames(XMLFileAdaptor fileAdaptor) throws Exception {
        Collection<?> pes = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.PhysicalEntity);
        for (Iterator<?> it = pes.iterator(); it.hasNext();) {
            GKInstance pe = (GKInstance) it.next();
            String displayName = pe.getDisplayName();
            if (!displayName.startsWith("phys-ent-participant")) {
                continue;
            }
            List names = pe.getAttributeValuesList(ReactomeJavaConstants.name);
            // Find an appropriate name
            String goodName = null;
            for (Iterator<?> it1 = names.iterator(); it1.hasNext();) {
                String name = (String) it1.next();
                if (!name.startsWith("phys-ent-participant")) {
                    goodName = name;
                    it1.remove();
                    break;
                }
            }
            if (goodName != null)
                names.add(0, goodName);
            InstanceDisplayNameGenerator.setDisplayName(pe);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void cleanUpDatabaseIdentifiers(XMLFileAdaptor fileAdaptor) throws Exception {
        Collection dbIds = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseIdentifier);
        Collection pes = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.PhysicalEntity);
        Collection events = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Event);
        Collection res = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceEntity);
        Collection taxons = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Taxon);
        Collection<GKInstance> all = new HashSet<GKInstance>();
        all.addAll(pes);
        all.addAll(events);
        all.addAll(res);
        all.addAll(taxons);
        Set used = new HashSet<GKInstance>();
        // Make the check a little faster
        for (Iterator<GKInstance> it = all.iterator(); it.hasNext();) {
            GKInstance ref = it.next();
            if (ref.getSchemClass().isValidAttribute(ReactomeJavaConstants.crossReference)) {
                List<?> values = ref.getAttributeValuesList(ReactomeJavaConstants.crossReference);
                if (values != null)
                    used.addAll(values);
            }
        }
        // Delete any refPepSeq that have not be used any more
        List<GKInstance> toBeDeleted = new ArrayList<GKInstance>(dbIds);
        toBeDeleted.removeAll(used);
        logger.info("DatabaseIdentifiers to be deleted: " + toBeDeleted.size());
        // To deletion
        for (GKInstance inst : toBeDeleted)
            fileAdaptor.removeFromClassMap(inst); // A quick way to remove a instance from the local project.
    }
    
    /**
     * Find RiceCyc:LOC in the crossReference value list, move values to identifier and ReferenceDatabase.
     * Delete original DatabaseIdentifier instances if they are not used any more.
     * @param fileAdaptor
     * @throws Exception
     */
    private void updateReferenceGeneProduct(XMLFileAdaptor fileAdaptor) throws Exception {
        // To be used
        GKInstance species = fileAdaptor.fetchInstance(186860L);
        Collection<?> instances = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
        for (Iterator<?> it = instances.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            List<?> crossRefs = inst.getAttributeValuesList(ReactomeJavaConstants.crossReference);
            GKInstance riceCycLoc = null;
            for (Iterator<?> it1 = crossRefs.iterator(); it1.hasNext();) {
                GKInstance tmp = (GKInstance) it1.next();
                if (tmp.getDisplayName().startsWith("RiceCyc:LOC_")) {
                    riceCycLoc = tmp;
                    break;
                }
            }
            if (riceCycLoc == null)
                continue; // Do nothing: some weird reference
            String identifier = (String) riceCycLoc.getAttributeValue(ReactomeJavaConstants.identifier);
            // Don't show MONOMER there
            int index = identifier.indexOf("-MONOMER");
            if (index > 0)
                identifier = identifier.substring(0, index); 
            GKInstance db = (GKInstance) riceCycLoc.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
            inst.setAttributeValue(ReactomeJavaConstants.identifier, identifier);
            inst.setAttributeValue(ReactomeJavaConstants.referenceDatabase, db);
            // Delete the original value
            inst.removeAttributeValueNoCheck(ReactomeJavaConstants.crossReference, riceCycLoc);
            // Replace species with "Oryza sativa"
            inst.setAttributeValue(ReactomeJavaConstants.species, species);
        }
        // Delete this species which is used for ReferenceGeneProudct only
        Collection<?> c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species,
                                                               ReactomeJavaConstants._displayName, 
                                                               "=", 
                                                               "Oryza sativa Japonica Group cultivar Nipponbare");
        if (c != null && c.size() > 0) {
            GKInstance inst = (GKInstance) c.iterator().next();
            fileAdaptor.deleteInstance(inst);
        }
    }
    
    @Test
    public void dumpToDatabase() throws Exception {
        ReactomeProjectDumper dumper = new ReactomeProjectDumper();
//        MySQLAdaptor dba = new MySQLAdaptor("localhost",
//                                            "test_gk_central_121010",
//                                            "authortool",
//                                            "T001test",
//                                            3306);
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_central",
                                            "authortool",
                                            "T001test");
        dumper.setMySQLAdaptor(dba);
        //dumper.setDefaultPersonId(1112777L);
        //dumper.setDefaultPersonId(1233923L); // Justin
        dumper.setDefaultPersonId(1385632L);
        String[] fileNames = new String[] {
                //DIR_NAME + "RiceFromBioPAXL1NewSchema.rtpj"
//                DIR_NAME + "ricecyc_v3_0_biopax2_before_dump.rtpj"
//                DIR_NAME + "ricecyc_v3_0_biopax2_before_dump_102010.rtpj"
//                DIR_NAME + "ricecyc_v3_0_biopax2_before_dump_120110.rtpj"
//                DIR_NAME + "ricecyc_v3_0_biopax2_before_dump_120610.rtpj"
//                DIR_NAME + "aracyc_v7_0_biopax-level2_5_fix_initial.rtpj"
                "aracyc_v8_0_biopax-level2_042211_preppedForDB.rtpj"
        };
        dumper.dumpToDB(fileNames);
    }
    
    @Test
    public void checkLocalProject() throws Exception {
        String fileName = DIR_NAME + "ricecyc_v3_0_biopax2_before_dump_112910.rtpj";
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor(fileName);
        Collection<?> c = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
        Set<String> set = new HashSet<String>();
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            String identifier = (String) inst.getAttributeValue(ReactomeJavaConstants.identifier);
            if (identifier == null)
                continue;
            if (identifier.startsWith("LOC_OS")) {
                int index = identifier.indexOf(".");
                set.add(identifier.substring(index + 1));
            }
        }
        for (String text : set)
            System.out.println(text);
    }
    
    /**
     * This method is used to change class type to EntityCompartment for a list of GO terms.
     * More EntityCompartments are needed for rice.
     * @throws Exception
     */
    private void switchClassTypesForCompartments(MySQLAdaptor dba,
                                                 String compartmentListFile,
                                                 GKInstance defaultPerson) throws Exception {
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        PersistenceManager.getManager().setActiveMySQLAdaptor(dba);
        FileUtility fu = new FileUtility();
        fu.setInput(compartmentListFile);
        String line = fu.readLine();
        Set<GKInstance> instances = new HashSet<GKInstance>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            Long dbId = new Long(tokens[2]);
            GKInstance inst = dba.fetchInstance(dbId);
            instances.add(inst);
            PostProcessHelper.downloadDBInstance(inst, fileAdaptor);
        }
        fu.close();
        // Switch types
        List<GKInstance> changedInsts = new ArrayList<GKInstance>();
        GKSchemaClass entityCompartmentCls = (GKSchemaClass) fileAdaptor.getSchema().getClassByName(ReactomeJavaConstants.Compartment);
        for (GKInstance inst : instances) {
            if (inst.getSchemClass().isa(ReactomeJavaConstants.Compartment)) {
                continue;
            }
            fileAdaptor.switchType(inst, entityCompartmentCls);
            changedInsts.add(inst);
        }
        logger.info("EntityCompartment class change: " + changedInsts.size() + " instances.");
        if (changedInsts.size() == 0)
            return; // Don't need do anything
        // Commit changes
        DefaultInstanceEditHelper ieHelper = new DefaultInstanceEditHelper();
        ieHelper.setDefaultPerson(defaultPerson.getDBID());
        GKInstance ie = ieHelper.getDefaultInstanceEdit(null);
        // Call to load all modified slot 
        for (GKInstance inst : changedInsts) {
            inst.getAttributeValue(ReactomeJavaConstants.modified);
        }
        // An InstanceEdit has been cloned in the following call
        ie = ieHelper.attachDefaultIEToDBInstances(changedInsts, ie);
        // First commit ie
        try {
            dba.startTransaction();
            dba.storeInstance(ie);
            for (GKInstance inst : changedInsts) {
                dba.updateInstance(inst);
            }
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
            throw e; // Re-thrown exception
        }
    }
    
    /**
     * This method is used to grep a list of predicted rice pathways into a target database based
     * on a list of pathways.
     * @param targetDBA
     * @param sourceDBA
     * @param defaultPerson
     * @param prePredictMaxDBID the maximum DB_ID before running prediction. This DB_ID is used to determine
     * if a GKInstance is predicted or not.
     * @throws Exception
     */
    private void grepPredictedRicePathways(MySQLAdaptor targetDBA,
                                           MySQLAdaptor sourceDBA,
                                           GKInstance defaultPerson,
                                           String pathwayListFileName,
                                           Long prePredictMaxDBID) throws Exception {
        FileUtility fu = new FileUtility();
        fu.setInput(pathwayListFileName);
        List<GKInstance> pathways = new ArrayList<GKInstance>();
        String line = fu.readLine();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            GKInstance inst = sourceDBA.fetchInstance(new Long(tokens[1]));
            if (inst != null)
                pathways.add(inst);
        }
        fu.close();
        logger.info("Predicted pathways to be checked out: " + pathways.size());
        // Use an XMLFileAdaptor to hold checked out instances temporarily
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        EventCheckOutHandler handler = new EventCheckOutHandler();
        PersistenceManager.getManager().setActiveMySQLAdaptor(sourceDBA);
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        for (GKInstance pathway : pathways) {
            handler.checkOutEvent(pathway, fileAdaptor);
        }
        // Make sure all ReferenceGeneProducts should not be shell
        checkOutFully(sourceDBA,
                      prePredictMaxDBID,
                      fileAdaptor,
                      ReactomeJavaConstants.ReferenceGeneProduct);
        checkOutFully(sourceDBA, 
                      null, 
                      fileAdaptor, 
                      ReactomeJavaConstants.ReferenceDatabase);
        checkOutFully(sourceDBA, 
                      null, 
                      fileAdaptor, 
                      ReactomeJavaConstants.ModifiedResidue);
        Collection<?> referenceGeneProducts = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
        // Switch UniProt to Locus IDs
        switchUniProtToLocus(referenceGeneProducts,
                             targetDBA,
                             fileAdaptor);
        // Don't use any ReferenceGeneProduct
        for (Iterator<?> it = referenceGeneProducts.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            inst.setAttributeValue(ReactomeJavaConstants.referenceGene, null);
        }
        // Try to save instances
        Collection<?> allInstances = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        logger.info("Total checked out instances: " + allInstances.size());
        List<GKInstance> toBeStored = new ArrayList<GKInstance>();
//        List<GKInstance> toBeDeleted = new ArrayList<GKInstance>();
        for (Iterator<?> it = allInstances.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            if (inst.getDBID() < 0) {
                toBeStored.add(inst); // New instances
                continue;
            }
            if (inst.getDBID() <= prePredictMaxDBID ||
                inst.getSchemClass().isa(ReactomeJavaConstants.ReferenceDNASequence  ))
                continue; // Coming from original gk_central, i.e, not a predicted instance
            if (inst.isShell() && 
                inst.getDBID() > prePredictMaxDBID && 
                inst.getSchemClass().isa(ReactomeJavaConstants.Event)) {
                //toBeDeleted.add(inst); // Instances used by other species.
                continue; // 
            }
            toBeStored.add(inst);
        }
        // Don't need to delete them actually. These instances should be used by human instances only
        // which will not be stored.
//        for (GKInstance inst : toBeDeleted) {
//            fileAdaptor.deleteInstance(inst);
//        }       
        // Need to switch the active mysql adatpor
        PersistenceManager.getManager().setActiveMySQLAdaptor(targetDBA);
        SlicingEngine slicingEngine = new SlicingEngine();
        ReactomeProjectDumper dumper = new ReactomeProjectDumper();
        dumper.setMySQLAdaptor(targetDBA);
        Long maxId = dumper.getMaximumDBID();
        logger.info("Get maximum DB_IDs before dumping predicted pathways: " + maxId);
        DefaultInstanceEditHelper ieHelper = new DefaultInstanceEditHelper();
        ieHelper.setDefaultPerson(defaultPerson.getDBID());
        GKInstance ie = ieHelper.getDefaultInstanceEdit(null);
        // An InstanceEdit has been cloned in the following call
        ie = ieHelper.attachDefaultIEToDBInstances(toBeStored, ie);
//      This IE has not been added into the local project
        ie.setDBID(fileAdaptor.getNextLocalID());
        fileAdaptor.addNewInstance(ie);
        fileAdaptor.save(DIR_NAME + "PredictedRicePathways.rtpj");
//        if (true)
//            return;
        toBeStored.add(0, ie); // To be stored as the first new instance
        for (GKInstance inst : toBeStored) {
            maxId ++;
            inst.setDBID(maxId);
        }
        // Start storing
        logger.info("Instances from predicted pathways to be stored: " + toBeStored.size());
        dumper.storeInstances(toBeStored, slicingEngine);
    }

    protected void checkOutFully(MySQLAdaptor sourceDBA,
                                 Long prePredictMaxDBID,
                                 XMLFileAdaptor fileAdaptor,
                                 String clsName) throws Exception {
        Collection<?> referenceGeneProducts = fileAdaptor.fetchInstancesByClass(clsName);
        if (prePredictMaxDBID != null) {
            for (Iterator<?> it = referenceGeneProducts.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                if (inst.isShell() && inst.getDBID() > prePredictMaxDBID) {
                    GKInstance dbInst = sourceDBA.fetchInstance(inst.getDBID());
                    PostProcessHelper.updateFromDB(inst,
                                                   dbInst,
                                                   SynchronizationManager.getManager());
                }
            }
        }
        else {
            for (Iterator<?> it = referenceGeneProducts.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                if (inst.isShell()) {
                    GKInstance dbInst = sourceDBA.fetchInstance(inst.getDBID());
                    PostProcessHelper.updateFromDB(inst,
                                                   dbInst,
                                                   SynchronizationManager.getManager());
                }
            }
        }
    }
    
    private void switchUniProtToLocus(Collection<?> refGeneProducts,
                                      MySQLAdaptor dba,
                                      XMLFileAdaptor fileAdaptor) throws Exception {
        List<GKInstance> locusRefGenes = new ArrayList<GKInstance>();
        logger.info("Replacing UniProt identifiers by Locus ids... ");
        for (Iterator<?> it = refGeneProducts.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            // Make sure it is from Oryza sativa
            GKInstance species = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.species);
            if (!species.getDisplayName().equals("Oryza sativa"))
                continue;
            // Check if identifiers has been in LOC already
            String identifier = (String) inst.getAttributeValue(ReactomeJavaConstants.identifier);
            if (identifier.startsWith("LOC_Os")) {
                // This is good. However, we need to replace ensembl by ricecyc later.
                continue;
            }
            List<?> refGenes = inst.getAttributeValuesList(ReactomeJavaConstants.referenceGene);
            // Check how many LOC ids have been linked to this ReferenceGeneProduct instances
            locusRefGenes.clear();
            for (Iterator<?> it1 = refGenes.iterator(); it1.hasNext();) {
                GKInstance refGene = (GKInstance) it1.next();
                String displayName = refGene.getDisplayName();
                if (displayName.contains("LOC_Os")) {
                    locusRefGenes.add(refGene);
                }
            }
            if (locusRefGenes.size() == 0) {
                // Has no locus genes
//                throw new java.lang.IllegalStateException(inst + " has not ReferenceGene!");
                continue; // Just ignore it.
            }
            if (locusRefGenes.size() == 1) {
                GKInstance locusRefGene = locusRefGenes.get(0);
                switchUniProtToLocus(inst, 
                                     locusRefGene,
                                     dba,
                                     fileAdaptor);
            }
            else { // Need to create DefinedSet
                // Use the first LocusRefGene to do switch
                GKInstance locusRefGene = locusRefGenes.get(0);
                switchUniProtToLocus(inst, 
                                     locusRefGene,
                                     dba,
                                     fileAdaptor);
                Set<GKInstance> set = new HashSet<GKInstance>();
                set.add(inst);
                InstanceCloneHelper cloneHelper = new InstanceCloneHelper();
                for (int i = 1; i < locusRefGenes.size(); i++) {
                    GKInstance refGene = locusRefGenes.get(i);
                    GKInstance clone = cloneHelper.cloneInstance(inst, fileAdaptor);
                    identifier = getLocusId(refGene);
                    clone.setAttributeValue(ReactomeJavaConstants.identifier,
                                            identifier);
                    clone.setAttributeValue(ReactomeJavaConstants.name, identifier);
                    set.add(clone);
                }
                Map<String, List<GKInstance>> refMap = fileAdaptor.getReferrersMap(inst);
                List<GKInstance> ewases = refMap.get(ReactomeJavaConstants.referenceEntity);
                for (GKInstance ewas : ewases) {
                    PostProcessHelper.switchEWASToSet(ewas, set, fileAdaptor);
                }
            }
        }
        // Multiple UniProt identifiers can refer to the same LOCUS ID. These UniProt identifiers should be 
        // merged.
        // Create a map to see what instances should be merged
        logger.info("Merging ReferenceGeneProducts based on locus ids... ");
        Map<String, Set<GKInstance>> locusIdToRefGeneProds = new HashMap<String, Set<GKInstance>>();
        Collection<?> c = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            String identifier = (String) inst.getAttributeValue(ReactomeJavaConstants.identifier);
            if (identifier == null || !identifier.contains("LOC_Os")) 
                continue;
            InteractionUtilities.addElementToSet(locusIdToRefGeneProds,
                                                 identifier,
                                                 inst);
        }
        for (String locusId : locusIdToRefGeneProds.keySet()) {
            Set<GKInstance> set = locusIdToRefGeneProds.get(locusId);
            if (set.size() == 1)
                continue;
            // Need to do merge
            Set<GKInstance> crossRefs = new HashSet<GKInstance>();
            for (GKInstance inst : set) {
                List list = inst.getAttributeValuesList(ReactomeJavaConstants.crossReference);
                if (list != null)
                    crossRefs.addAll(list);
            }
            List<GKInstance> list = new ArrayList<GKInstance>(set);
            GKInstance target = list.get(0);
            target.setAttributeValue(ReactomeJavaConstants.crossReference, new ArrayList<GKInstance>(crossRefs));
            for (int i = 1; i < list.size(); i++) {
                mergeInstance(target,
                              list.get(i),
                              fileAdaptor);
            }
        }
        logger.info("Merging PEs... ");
        // Do another merge to merge PEs since many PEs may point to the same things.
        mergePEs(fileAdaptor); 
        // Switch DefinedSet to EWASs if there is only one member
        logger.info("cleaning up DefinedSet...");
        c = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DefinedSet);
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            List<?> hasMembers = inst.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            if (hasMembers == null || hasMembers.size() != 1)
                continue;
            GKInstance member = (GKInstance) hasMembers.get(0);
            mergeInstance(member, inst, fileAdaptor);
        }
    }
    
    private void switchUniProtToLocus(GKInstance refGeneProduct,
                                      GKInstance locusRefGene,
                                      MySQLAdaptor dba,
                                      XMLFileAdaptor fileAdaptor) throws Exception {
        // Create a new database identifier or get a new identifier from database
        String identifier = (String) refGeneProduct.getAttributeValue(ReactomeJavaConstants.identifier);
        GKInstance refDb = (GKInstance) refGeneProduct.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
        if (identifier != null && refDb != null) {
            GKInstance databaseIdentifier = PostProcessHelper.getDatabaseIdentifier(identifier, 
                                                                                    refDb.getDisplayName(), 
                                                                                    fileAdaptor,
                                                                                    dba);
            if (databaseIdentifier != null)
                refGeneProduct.addAttributeValue(ReactomeJavaConstants.crossReference, databaseIdentifier);
        }
        // Extract Gene Locus ID into identifier
        identifier = getLocusId(locusRefGene);
        refGeneProduct.setAttributeValue(ReactomeJavaConstants.identifier, identifier);
        String name = (String) refGeneProduct.getAttributeValue(ReactomeJavaConstants.name);
        if (name ==  null) // To avoid null in display name
            refGeneProduct.setAttributeValue(ReactomeJavaConstants.name, identifier);
        // Add ENSEMBL as the ReferenceDatabase
        GKInstance ensembl = B2RMapperUtilities.getReferenceDB("Ensembl", fileAdaptor);
        if (ensembl != null)
            refGeneProduct.setAttributeValue(ReactomeJavaConstants.referenceDatabase, ensembl);
        // Remove values in ReferenceGenes
        refGeneProduct.setAttributeValue(ReactomeJavaConstants.referenceGene, null);
        InstanceDisplayNameGenerator.setDisplayName(refGeneProduct);
    }
    
    private String getLocusId(GKInstance refGene) {
        String name = refGene.getDisplayName();
        int index = name.indexOf(":");
        return name.substring(index + 1);
    }

    /**
     * This method is used to prepare a database before dumping. Two things are done in this method:
     * 1). Switch types for a list of GO compartments to EntityCompartment class.
     * 2). Download a list of predicted pathways from a release database and dump into this database.
     * @throws Exception
     */
    @Test
    public void runPreDumpOnDb() throws Exception {
        MySQLAdaptor targetDba = new MySQLAdaptor("localhost",
                                                  "test_gk_central_121010",
                                                  "authortool",
                                                  "T001test");
        String fileName = DIR_NAME + "RiceListOfConvertGOCompartments.txt";
        Long defaultPersonId = 1112777L; // Rice Reactome for Guanming Wu
        GKInstance defaultPerson = targetDba.fetchInstance(defaultPersonId);
        switchClassTypesForCompartments(targetDba,
                                        fileName,
                                        defaultPerson);
        // Get predicted pathways from a release database
        MySQLAdaptor releaseDba = new MySQLAdaptor("localhost", 
                                                   "test_reactome_34", 
                                                   "authortool", 
                                                   "T001test");
        fileName = DIR_NAME + "RicePredictedPathways.txt";
        // The following value is got from test_reactome_34 database: an instance
        // just before Esther's prediction IE.
        Long maxPrePredictDBID = 953812L;
        grepPredictedRicePathways(targetDba, 
                                  releaseDba,
                                  defaultPerson, 
                                  fileName,
                                  maxPrePredictDBID);
    }
    
    @Test
    public void dumpReferenceMolecules() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "rice_reactome",
                                            "root",
                                            "macmysql01",
                                            3306);
        Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.ReferenceMolecule);
        StringBuilder builder = new StringBuilder();
        builder.append("ReactomeID\tCompound_Name\tCAS\tLIGAND\tRiceCyc");
        String outFileName = DIR_NAME + "RiceReferenceMolecules.txt";
        FileUtility fu = new FileUtility();
        //fu.setOutput(outFileName);
        //fu.printLine(builder.toString());
        builder.setLength(0);
        // To check if there is one to one mapping from DB_IDs to DisplayName
        Set<String> displayNames = new HashSet<String>();
        Set<Long> dbIds = new HashSet<Long>();
        Map<String, Set<Long>> nameToIds = new HashMap<String, Set<Long>>();
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            // Check if there if any reference to ChEBI
            GKInstance refDb = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
            if (refDb == null) {
                dbIds.add(inst.getDBID());
                displayNames.add(inst.getDisplayName().trim());
                InteractionUtilities.addElementToSet(nameToIds,
                                                     inst.getDisplayName().trim(), 
                                                     inst.getDBID());
                // Should export
                List<?> list = inst.getAttributeValuesList(ReactomeJavaConstants.crossReference);
                builder.append(inst.getDBID()).append("\t").append(inst.getDisplayName());
                // Search for CAS
                boolean isFound = false;
                builder.append("\t");
                for (Iterator<?> it1 = list.iterator(); it1.hasNext();) {
                    GKInstance crossRef = (GKInstance) it1.next();
                    String name = crossRef.getDisplayName();
                    if (name.startsWith("CAS")) {
                        isFound = true;
                        int index = name.indexOf(":");
                        builder.append(name.substring(index + 1));
                        break;
                    }
                }
                if (!isFound)
                    builder.append("-");
                // Search for Ligands
                isFound = false;
                builder.append("\t");
                for (Iterator<?> it1 = list.iterator(); it1.hasNext();) {
                    GKInstance crossRef = (GKInstance) it1.next();
                    String name = crossRef.getDisplayName();
                    if (name.startsWith("LIGAND")) {
                        isFound = true;
                        int index = name.indexOf(":");
                        builder.append(name.substring(index + 1));
                        break;
                    }
                }
                if (!isFound)
                    builder.append("-");
                // Search for ricecyc
                isFound = false;
                builder.append("\t");
                for (Iterator<?> it1 = list.iterator(); it1.hasNext();) {
                    GKInstance crossRef = (GKInstance) it1.next();
                    String name = crossRef.getDisplayName();
                    if (name.startsWith("RiceCyc")) {
                        isFound = true;
                        int index = name.indexOf(":");
                        builder.append(name.substring(index + 1));
                        break;
                    }
                }
                if (!isFound)
                    builder.append("-");
                //fu.printLine(builder.toString());
                builder.setLength(0);
            }
            else if (!refDb.getDisplayName().equals("ChEBI")) {
                System.out.println(inst + " has no ChEBI reference identifier!");
            }
        }
        //fu.close();
        System.out.println("Total DBIDs: " + dbIds.size());
        System.out.println("Total display names: " + displayNames.size());
        for (String name : nameToIds.keySet()) {
            Set<Long> ids = nameToIds.get(name);
            if (ids.size() > 1)
                System.out.println(name + ": " + ids);
        }
    }
    
    private void updateReferenceMolecules(XMLFileAdaptor fileAdaptor,
                                          MySQLAdaptor dba) throws Exception {
        FileUtility fu = new FileUtility();
        String fileName = DIR_NAME + "ReferenceNameToChEBIId.txt";
        Map<String, String> nameToChEBIId = fu.importMap(fileName);
        Collection<?> c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule,
                                                               ReactomeJavaConstants.identifier,
                                                               "IS NULL", 
                                                               null);
        int count = 0;
        ChEBIAttributeAutoFiller chebiHelper = new ChEBIAttributeAutoFiller();
        chebiHelper.setPersistenceAdaptor(fileAdaptor);
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            String displayName = inst.getDisplayName().trim();
            String chebiId = nameToChEBIId.get(displayName);
            InstanceDisplayNameGenerator.setDisplayName(inst); // Just to remove empty space
            if (chebiId == null)
                continue; // Cannot find a map
            Collection<?> c1 = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule,
                                                           ReactomeJavaConstants.identifier,
                                                           "=",
                                                           chebiId);
            if (c1 == null || c1.size() == 0) {
                System.err.println("Cannot find chebi: " + inst);
                // Need to fetch the detailed information from ChEBI
                inst.setAttributeValue(ReactomeJavaConstants.identifier, chebiId);
                // Want to remove reference to RiceCyc
                inst.setAttributeValue(ReactomeJavaConstants.crossReference, null);
                // Use name from ChEBI
                inst.setAttributeValue(ReactomeJavaConstants.name, null);
                chebiHelper.process(inst, null);
                count ++;
                continue;
            }
            // If there is more than one ReferenceMolecule with the same id, just pick one:
            GKInstance dbInst = (GKInstance) c1.iterator().next();
            PostProcessHelper.updateFromDB(inst, 
                                           dbInst,
                                           SynchronizationManager.getManager());
        }
        System.out.println("Cannot be mapped: " + count);
    }
    
    @Test
    public void generateDisplayNameToChEBIMap() throws Exception {
        Map<String, String> nameToChEBIId = mapReferenceMoleculeToChEBI();
        String fileName = DIR_NAME + "ReferenceNameToChEBIId.txt";
        FileUtility fu = new FileUtility();
        fu.exportMap(nameToChEBIId, fileName);
    }
    
    /**
     * This method is used to map to ChEBIs
     * @throws Exception
     */
    private Map<String, String> mapReferenceMoleculeToChEBI() throws Exception {
        Map<Long, Set<String>> dbIdToChEBI = new HashMap<Long, Set<String>>();
        //String fileName = DIR_NAME + "reactome_chebi_mapping_complete_sorted.txt";
        String fileName = DIR_NAME + "reactome_chebi_mapping_complete_sorted_1.1.txt";
        FileUtility fu = new FileUtility();
        fu.setInput(fileName);
        String line = fu.readLine();
        Map<Long, Set<ReferenceMoleculeMap>> idToMaps = new HashMap<Long, Set<ReferenceMoleculeMap>>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            Long id = new Long(tokens[0]);
            ReferenceMoleculeMap map = new ReferenceMoleculeMap();
            map.dbId = id;
            map.chEBIId = tokens[1];
            map.mapMethod = tokens[2];
            InteractionUtilities.addElementToSet(idToMaps,
                                                 id,
                                                 map);
        }
        fu.close();
        // Need to clean up
        // The target database for search
        MySQLAdaptor dba = new MySQLAdaptor("localhost", "rice_reactome", "root", "macmysql01");
        Map<Long, String> idToSingleMap = new HashMap<Long, String>();
        Set<String> ids = new HashSet<String>();
        for (Long dbId : idToMaps.keySet()) {
            Set<ReferenceMoleculeMap> maps = idToMaps.get(dbId);
            ids.clear();
            for (ReferenceMoleculeMap map : maps) {
                ids.add(map.chEBIId);
            }
            if (ids.size() == 1) {
                idToSingleMap.put(dbId, ids.iterator().next());
                continue; 
            }
            if (maps.size() == 1)
                idToSingleMap.put(dbId, maps.iterator().next().chEBIId);
            else {
                // Need to find a single map as the following rules:
//                1). If there is a one-to-one mapping, use that mapping.
//                2). If there are multiple mappings, map as the following order:
//                    i> If there is LIGAND identifier, it is mapped to ChEBI by one-to-one, use this mapping.
//                    ii> If there are more than one ChEBI from Ligand mapping, check the Reactome database. Pick one in the database. If nothing is found in the database, generate an error.
//                    iii>. if no ligand mapping appeared, try CAS mapping. If one to one mapping can be found, use it.
//                    iv> For more than one CAS mapping. Do as in ii>.
//                     v>. Try to pick what in the database.
                List<ReferenceMoleculeMap> neededMaps = new ArrayList<ReferenceMoleculeMap>();
                for (ReferenceMoleculeMap map : maps) {
                    if (map.mapMethod.equals("LIGAND"))
                        neededMaps.add(map);
                }
                if (neededMaps.size() == 1) {
                    // Found the map
                    idToSingleMap.put(dbId, 
                                      neededMaps.iterator().next().chEBIId);
                    continue;
                }
                else if (neededMaps.size() > 1) {
                    // Search the database for one map
                    String id = fetchChEBIInDB(neededMaps, dba);
                    if (id !=  null) {
                        idToSingleMap.put(dbId, id);
                        continue;
                    }
                }
                // Cannot work via LIGAND. Now try CAS
                for (ReferenceMoleculeMap map : maps) {
                    if (map.mapMethod.equals("CAS"))
                        neededMaps.add(map);
                }
                if (neededMaps.size() == 1) {
                    // Found the map
                    idToSingleMap.put(dbId, 
                                      neededMaps.iterator().next().chEBIId);
                    continue;
                }
                else if (neededMaps.size() > 1) {
                    // Search the database for one map
                    String id = fetchChEBIInDB(neededMaps, dba);
                    if (id !=  null) {
                        idToSingleMap.put(dbId, id);
                        continue;
                    }
                }
                // Try to fetch directly with ids
                String id = fetchChEBIInDB(ids, dba);
                if (id != null)
                    idToSingleMap.put(dbId, id);
            }
        }
        System.out.println("Total map: " + idToMaps.size());
        System.out.println("Single map: " + idToSingleMap.size());
        idToMaps.keySet().removeAll(idToSingleMap.keySet());
        for (Long id : idToMaps.keySet()) {
            System.out.println(id + ": " + idToMaps.get(id).size());
        }
        Map<String, Set<String>> nameToIds = new HashMap<String, Set<String>>();
        for (Long id : idToSingleMap.keySet()) {
            GKInstance inst = dba.fetchInstance(id);
            String name = inst.getDisplayName().trim();
            if (nameToIds.keySet().contains(name)) {
                System.out.println(inst + " has duplicated name!");
            }
            String chebiId = idToSingleMap.get(id);
            InteractionUtilities.addElementToSet(nameToIds, 
                                                 inst.getDisplayName().trim(),
                                                 chebiId);
        }
        System.out.println("Total name to id map: " + nameToIds.size());
        System.out.println("DisplayName mapped to multiple ids: ");
        Map<String, String> nameToChEBIId = new HashMap<String, String>();
        for (String name : nameToIds.keySet()) {
            ids = nameToIds.get(name);
            if (ids.size() > 1) {
                System.out.println(name + ": " + ids);
            }
            else {
                String id = ids.iterator().next();
                int index = id.indexOf(":");
                nameToChEBIId.put(name, id.substring(index + 1));
            }
        }
        return nameToChEBIId;
    }
    
    public void updateECNumbers(XMLFileAdaptor fileAdaptor,
                                MySQLAdaptor dbAdaptor) throws Exception {
        GKInstance ec = fileAdaptor.fetchInstance(4L);
        Collection<?> c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseIdentifier,
                                                               ReactomeJavaConstants.referenceDatabase,
                                                               "=",
                                                               ec); // Check for EC
        Map<String, Set<GKInstance>> nameToInstances = new HashMap<String, Set<GKInstance>>();
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            String name = inst.getDisplayName();
            InteractionUtilities.addElementToSet(nameToInstances,
                                                 name,
                                                 inst);
        }
        // Do some merging
        for (String name : nameToInstances.keySet()) {
            Set<GKInstance> set = nameToInstances.get(name);
            if (set.size() == 1)
                continue;
            // Do a merge
            GKInstance first = set.iterator().next();
            for (GKInstance inst : set) {
                if (inst == first)
                    continue;
                mergeInstance(first, inst, fileAdaptor);
            }
        }
        // Do a match
        c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseIdentifier,
                                                 ReactomeJavaConstants.referenceDatabase,
                                                 "=",
                                                 ec); // Check for EC
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            Collection matched = dbAdaptor.fetchIdenticalInstances(inst);
            if (matched !=  null && matched.size() > 0) {
                GKInstance dbInst = (GKInstance) matched.iterator().next();
                PostProcessHelper.updateFromDB(inst,
                                               dbInst, 
                                               SynchronizationManager.getManager());
            }
        }
    }
    
    private String fetchChEBIInDB(List<ReferenceMoleculeMap> maps,
                                  MySQLAdaptor dba) throws Exception {
        int index = 0;
        for (ReferenceMoleculeMap map : maps) {
            index = map.chEBIId.indexOf(":");
            String id = map.chEBIId.substring(index + 1);
            Collection<?> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule,
                                                        ReactomeJavaConstants.identifier,
                                                        "=",
                                                        id);
            if (c != null && c.size() > 0) {
                return id; // Just pick one
            }
        }
        return null;
    }
    
    private String fetchChEBIInDB(Set<String> ids,
                                  MySQLAdaptor dba) throws Exception {
        int index = 0;
        for (String id : ids) {
            index = id.indexOf(":");
            id = id.substring(index + 1);
            Collection<?> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule,
                                                        ReactomeJavaConstants.identifier,
                                                        "=",
                                                        id);
            if (c != null && c.size() > 0) {
                return id; // Just pick one
            }
        }
        return null;
    }
    
    @Test
    public void checkUniProtInRice() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "rice_reactome_v3_1",
                                            "root",
                                            "macmysql01");
        Collection<?> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct,
                                                       ReactomeJavaConstants.species,
                                                       "=",
                                                       186860L); // Oryza sativa
        System.out.println("Total instances: " + c.size());
        dba.loadInstanceAttributeValues(c, new String[]{ReactomeJavaConstants.referenceGene});
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            List<?> list = inst.getAttributeValuesList(ReactomeJavaConstants.referenceGene);
            if (list == null)
                System.out.println(inst + " has no reference gene!");
            else if (list.size() > 1) {
                // Check how many LOC_Os it has
                int count = 0;
                for (Iterator<?> it1 = list.iterator(); it1.hasNext();) {
                    GKInstance inst1 = (GKInstance) it1.next();
                    if (inst1.getDisplayName().startsWith("ENSEMBL:LOC_Os")) {
                        count ++;
                    }
                }
                if (count > 1)
                    System.out.println(inst + " has more than one Locus ID: " + list);
            }
        }
    }
    
    @Test
    public void fixReferenceGenesAfterDump() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "test_gk_central_121010",
                                            "authortool", 
                                            "T001test");
        // Get RiceCyc database
        Collection<?> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase,
                                                       ReactomeJavaConstants._displayName,
                                                       "=",
                                                       "RiceCyc");
        GKInstance riceCyc = (GKInstance) c.iterator().next();
        System.out.println("RiceCyc: " + riceCyc);
        c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct,
                                                       ReactomeJavaConstants._displayName,
                                                       "LIKE",
                                                       "Ensembl:LOC_Os%");
        System.out.println("Total instances: " +  c.size());
        dba.loadInstanceAttributeValues(c, new String[]{ReactomeJavaConstants.identifier, 
                                                        ReactomeJavaConstants.referenceDatabase});
        try {
            dba.startTransaction();
            for (Iterator<?> it = c.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                inst.setAttributeValue(ReactomeJavaConstants.referenceDatabase,
                                       riceCyc);
                String identifier = (String) inst.getAttributeValue(ReactomeJavaConstants.identifier);
                inst.setDisplayName(riceCyc.getDisplayName() + ":" + identifier);
                dba.updateInstanceAttribute(inst,
                                            ReactomeJavaConstants.referenceDatabase);
                dba.updateInstanceAttribute(inst, 
                                            ReactomeJavaConstants._displayName);
            }
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
            throw e;
        }
    }
    
    @Test
    public void fixReferenceMoleculeAfterDump() throws Exception {
        FileUtility fu = new FileUtility();
        String fileName = "resources/ReferenceNameToChEBIId.txt";
        Map<String, String> nameToChEBIId = fu.importMap(fileName);
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_central",
                                            "authortool",
                                            "T001test");
        Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.ReferenceMolecule);
        dba.loadInstanceAttributeValues(c, new String[]{ReactomeJavaConstants.identifier});
        c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule,
                                                       ReactomeJavaConstants.identifier,
                                                       "IS NULL",
                                                       null);
        System.out.println("Check ReferenceMolecules: " + c.size());
        int index = 0;
        int count = 0;
        int notMapped = 0;
        int mappedInst = 0;
        int mappedInstTooMany = 0;
        long time1 = System.currentTimeMillis();
        ChEBIAttributeAutoFiller chebiHelper = new ChEBIAttributeAutoFiller();
        chebiHelper.setPersistenceAdaptor(dba);
        dba.startTransaction();
        try {
            for (Iterator<?> it = c.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                String name = inst.getDisplayName();
                index = name.indexOf("[unknown");
                name = name.substring(0, index).trim();
                String chebiId = nameToChEBIId.get(name);
                if (chebiId != null) {
                    count ++;
                    Collection<?> mapped = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule,
                                                                        ReactomeJavaConstants.identifier, 
                                                                        "=",
                                                                        chebiId);
                    if (mapped == null || mapped.size() == 0) {
                        System.err.println("Cannot find chebi: " + inst);
                        // Need to fetch the detailed information from ChEBI
                        inst.setAttributeValue(ReactomeJavaConstants.identifier, chebiId);
                        // Want to remove reference to RiceCyc
                        inst.setAttributeValue(ReactomeJavaConstants.crossReference, null);
                        // Use name from ChEBI
                        inst.setAttributeValue(ReactomeJavaConstants.name, null);
                        chebiHelper.process(inst, null);
                        dba.updateInstance(inst);
                        notMapped ++;
                        continue;
                    }
                    if (mapped.size() == 1) {
                        mappedInst ++;
                    }
                    else if (mapped.size() > 1)
                        mappedInstTooMany ++;
                    GKInstance correct = (GKInstance) mapped.iterator().next();
                    Collection<?> references = inst.getReferers(ReactomeJavaConstants.referenceEntity);
                    if (references != null && references.size() > 0) {
                        for (Iterator<?> it1 = references.iterator(); it1.hasNext();) {
                            GKInstance ref = (GKInstance) it1.next();
                            InstanceUtilities.replaceReference(ref, 
                                                               inst,
                                                               correct);
                            dba.updateInstanceAttribute(ref, ReactomeJavaConstants.referenceEntity);
                        }
                    }
                    dba.deleteInstance(inst);
                }
            }
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
        }
        long time2 = System.currentTimeMillis();
        System.out.println("Total mapped chebi: " + count);
        System.out.println("Mapped instances: " + mappedInst);
        System.out.println("Too many mapped: " + mappedInstTooMany);
        System.out.println("Not mapped: " + notMapped);
        System.out.println("\nTotal time used: " + (time2 - time1));
    }
    
    @Test
    public void checkReferenceGeneProduct() throws Exception {
        String fileName = DIR_NAME + "ricecyc_v3_0_biopax2_before_dump.rtpj";
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        fileAdaptor.setSource(fileName);
        Collection<?> instances = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
        int count = 0;
        int duplicatedCount = 0;
        for (Iterator<?> it = instances.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            String identifier = (String) inst.getAttributeValue(ReactomeJavaConstants.identifier);
            if (identifier != null)
                System.out.println(inst + " has identifier: " + identifier);
            List<?> list = inst.getAttributeValuesList(ReactomeJavaConstants.crossReference);
            // Check if there is Loc identifier
            int temp = 0;
            for (Iterator<?> it1 = list.iterator(); it1.hasNext();) {
                GKInstance ref = (GKInstance) it1.next();
                if (ref.getDisplayName().startsWith("RiceCyc:LOC_")) {
                    temp++;
                }
            }
            if (temp == 0) {
                System.out.println("Cannot find RiceCyc:LOC: " + inst);
                count ++;
            }
            else if (temp > 1) {
                System.out.println("Have more than one RiceCyc:LOC: " + inst);
                duplicatedCount ++;
            }
        }
        System.out.println("Total checked ReferenceGeneProduct instances: " + instances.size());
        System.out.println("Instances don't have LOC identifiers: " + count);
        System.out.println("More than one LOC: " + duplicatedCount);
    }
    
    private class ReferenceMoleculeMap {
        Long dbId;
        String displayName;
        String mapMethod;
        String chEBIId;
    }
    
    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configure("resources/log4j.properties");
        String method = args[0];
        RiceCycPostProcessor processor = new RiceCycPostProcessor();
        try {
            if (method.equals("runPreDumpOnDb")) {
                processor.runPreDumpOnDb();
            }
            else if (method.equals("dumpToDatabase")) {
                processor.dumpToDatabase();
            }
            else if (method.equals("fixReferenceGenesAfterDump")) {
                processor.fixReferenceGenesAfterDump();
            }
            else if (method.equals("fixReferenceMoleculeAfterDump")) {
                processor.fixReferenceMoleculeAfterDump();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}
