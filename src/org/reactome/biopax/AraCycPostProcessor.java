/*
 * Created on Jul 5, 2007
 *
 */
package org.reactome.biopax;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
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
 * The following procedures should be used to merge Arabidopsis Reactome into gk_central base:
 * Here are procedures to merge arabidopsis reactome:

1). Create a new person Guanming Wu for project "Arabidopsis Reactome"
2). Use its DB_ID for method in runPreDumpOnDb() and run the method. 
3). Change column definition: alter table DatabaseIdentifier change column identifier 
    identifier varchar(50) 
4). Use its DB_ID from 1) for method dumpToDatabase() and run the method. 
    If needed, run processBeforeDump().
5). Run fixReferenceGenesAfterDump() to replace Ensembl by AraCyc, and change _displayNames 
    for these ReferenceGeneProducts.
 * @author wgm, ed. Justin Preece (OSU)
 *
 */
public class AraCycPostProcessor implements BioPAXToReactomePostProcessor {
    private static final Logger logger = Logger.getLogger(AraCycPostProcessor.class);
    //private final String DIR_NAME = "resources/";
    private final String DIR_NAME = "/home/preecej/Documents/projects/plant_reactome/aracyc_to_reactome_conversion/aracyc_data/aracyc_v11/"; // linux
    //private final String DIR_NAME = "/Users/preecej/Documents/projects/plant_reactome/aracyc_to_reactome_conversion/aracyc_data/aracyc_v11/"; // mac
    
    public AraCycPostProcessor() {
    }

    @Test
    public void runPostProcess() throws Exception {
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        fileAdaptor.setSource(DIR_NAME + "aracyc_v11_biopax-level2.rtpj.5.part_1");
        //fileAdaptor.setSource(DIR_NAME + "tryptophan_biosynthesis_pathway-biopax-lvl2.rtpj.compartmentMischiefManaged");
        MySQLAdaptor dbAdaptor = new MySQLAdaptor("floret.cgrb.oregonstate.edu", 
                "gk_central_121613",
                "reactome_aracyc", 
                "r3actom3_aracyc");
        postProcess(dbAdaptor, fileAdaptor);
        String destFileName = DIR_NAME + "aracyc_v11_biopax-level2.rtpj.5.part_2";
        //String destFileName = DIR_NAME + "tryptophan_biosynthesis_pathway-biopax-lvl2.rtpj.dbInstancesRetrieved";
        fileAdaptor.save(destFileName);
    }

    public void postProcess(MySQLAdaptor dbAdaptor, 
                            XMLFileAdaptor fileAdaptor) throws Exception {
/*
		// run this first section of methods from BToRConverterUnitTest
    	logger.info("Update RefMols to remove \"a\" and \"an\" prefixes on incoming A.th. data...");
    	removeArticlesFromRefMolNames(fileAdaptor, dbAdaptor); // new
    	logger.info("Starting changeDisplayNameForRefGeneProducts...");
        changeDisplayNameForRefGeneProducts(fileAdaptor);
        logger.info("copyNameFromRefToEntity...");
        copyNameFromRefToEntity(fileAdaptor);
    	logger.info("mergePEs...");
        mergePEs(fileAdaptor);
        // The above should be called before the following so that PEs can be merged together.
        logger.info("createEntitySetForMultipleCAS...");
        createEntitySetForMultipleCAS(fileAdaptor);
        logger.info("splitCatalystActivitites...");
        splitCatalystActivities(fileAdaptor);
        logger.info("attachSpecies...");
        attachSpecies(dbAdaptor, fileAdaptor);
        // now, need to remove the species from A.th. SEs so they can be mapped to rice SEs
        logger.info("Remove species attribute from SimpleEntities...");
        delSpeciesFromSimpleEntities(fileAdaptor, dbAdaptor); // new
        logger.info("assignCompartmentsToReactions...");
        assignCompartmentsToReactions(fileAdaptor);
        logger.info("assign display name for reaction");
        assignReactionDisplayNameFromInputOutput(fileAdaptor);
        logger.info("processing LiteratureReference...");
        PostProcessHelper.processLiteratureReferences(dbAdaptor, fileAdaptor);
        // Update the database first so that GO can be compared
        logger.info("Update Reference Database...");
        updateReferenceDatabases(fileAdaptor, dbAdaptor);
        logger.info("Update GO compartments...");
        updateGOCellularComponent(fileAdaptor, dbAdaptor);
        logger.info("Update ReferenceGeneProducts...");
        updateReferenceGeneProduct(fileAdaptor);
*/
        /*
        STOP process and run chebi mapping here:
	    	1. dumpReferenceMolecules() - source data for reactome_chebi_mapping.pl
	    	2. run reactome_chebi_mapping.pl w/ fresh ChEBI OBO file (from EBI)
	    	3. generateDisplayNameToChEBIMap()
 		*/
        
    	// run this section from this.runPostProcess()
        logger.info("Update ReferenceMolecules (now that new ChEBI mappings have been created)...");
        updateReferenceMolecules(fileAdaptor, dbAdaptor);
        logger.info("Merge EC numbers...");
        updateECNumbers(fileAdaptor, dbAdaptor);
        logger.info("Update PE names...");
        updatePENames(fileAdaptor);
        logger.info("Updating Regulation display names...");
        updateRegulationDisplayNames(fileAdaptor);
        logger.info("Updating Complex instances and removing extra species...");
        updateComplexes(fileAdaptor);
        logger.info("Fixing Person names and merging duplicate instances...");
        fixPersonNames(fileAdaptor);
        logger.info("Cleaning DatabaseIdentifiers...");
        cleanUpDatabaseIdentifiers(fileAdaptor);

//        logger.info("Remove unused AraCyc RegulationTypes...");
//        removeRegulationTypes(fileAdaptor); // new
        logger.info("Remove unused KEGG-legacy Reference...");
        removeUnusedRefDBs(fileAdaptor); // new
        logger.info("Adapting Isoforms...");
        adaptIsoforms(fileAdaptor); // new
        logger.info("Update Defined Sets...");
        updateDefinedSets(fileAdaptor); // new
        logger.info("add GO Molecular Activity To CatalystActivity based on Reaction.EC...");
        addMolecularActivityToCA(fileAdaptor, dbAdaptor); // new
     	logger.info("set instance compartments on PEs and RXNs to cytoplasm");
        compartmentMischief(fileAdaptor, dbAdaptor); // new
    	//avoids gk_central duplication for SimpleEntities, ReferenceMolecules, and DatabaseIdentifiers
        logger.info("Replace reference molecules with no ChEBI ID based on name match...");
        retrieveNoChebiRefMols(fileAdaptor, dbAdaptor); // new
        logger.info("Use SimpleEntities, ReferenceMolecules, and DatabaseIdentifiers in DB...");
        retrieveEntitiesAndIdentifiersFromDB(fileAdaptor, dbAdaptor); // new
   	    logger.info("Map A.th. to rice ReferenceMolecules on the basis of Cyc Frame ID (*Cyc:) xref matches");
  	    advancedRefMolMapping(fileAdaptor, dbAdaptor); // new
		logger.info("Merge duplicate RefMols based on name identity...");
        mergeDupeInstancesOnName(fileAdaptor, ReactomeJavaConstants.ReferenceMolecule); // new
    	
    	// DON'T DO THESE FOR THIS CONVERSION; NOT USEFUL
     	//logger.info("Map file to db ReferenceMolecules after removing 'a' and 'an' prefixes");
      	//advancedRefMolNameMapping(fileAdaptor, dbAdaptor); // only 1 hit
     	//logger.info("Further update RefMols with ChEBI IDs using Janna Hasting's RiceCyc data");
    	//advancedChEBIMapping(fileAdaptor, dbAdaptor); // only 14 hits
    }

/***********************************************************************************************************/

    // update RefMols to remove "a" and "an" prefixes on incoming A.th. data
    private void removeArticlesFromRefMolNames(XMLFileAdaptor fileAdaptor, MySQLAdaptor dbAdaptor) throws Exception {
    	// iterate over RefMols in data file with no ChEBI ID
    	Collection<?> fileRMs = fileAdaptor.fetchInstanceByAttribute(
        		ReactomeJavaConstants.ReferenceMolecule, 
        		ReactomeJavaConstants.name,																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									 
        		"REGEXP",
        		"^(a|an)\\s");

    	int count = 0;
        for (Iterator<?> it = fileRMs.iterator(); it.hasNext();) {
            GKInstance curRM = (GKInstance) it.next();
            if (curRM.getDBID() < 0) {
            	//logger.info(curRM);
            	List<String> names = curRM.getAttributeValuesList(ReactomeJavaConstants.name);
            	if (names != null) {
            		String curName = names.get(0);
            		String prefName = null;
            		if (curName.startsWith("a ")) {
            			prefName = curName.replaceFirst("^a\\s", "");
        				count++;
            		} else if (curName.startsWith("an ")) {
            			prefName = curName.replaceFirst("^an\\s", "");
        				count++;
            		}
            		if (prefName != null) {
            			names.remove(0);
            			names.add(0, prefName);
            			curRM.setAttributeValue(ReactomeJavaConstants.name, names);
                        InstanceDisplayNameGenerator.setDisplayName(curRM);
        				logger.info("Updated " + curRM);
            		}
            	}
            }
        }
    	logger.info("count: " + count);
    }
    
    // Map file to db ReferenceMolecules after removing 'a' and 'an' prefixes
    private void advancedRefMolNameMapping(XMLFileAdaptor fileAdaptor, MySQLAdaptor dbAdaptor) throws Exception {
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        PersistenceManager.getManager().setActiveMySQLAdaptor(dbAdaptor);

    	// iterate over RefMols in data file with no ChEBI ID
    	Collection<?> fileRMs = fileAdaptor.fetchInstanceByAttribute(
        		ReactomeJavaConstants.ReferenceMolecule, 
        		ReactomeJavaConstants.name,																																																																																																																																																																																																																																																																																																																																																																																																																																																																																																									 
        		"REGEXP",
        		"^(a|an)\\s");

    	// hash db RefMols by name(0)::db_id
    	Map<String, Long> dbMap = new HashMap<String, Long>();
    	Collection<GKInstance> dbRMs = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceMolecule);
    	for (GKInstance dbRM : dbRMs) {
        	List<String> names = dbRM.getAttributeValuesList(ReactomeJavaConstants.name);
        	if (names != null)
        		dbMap.put(names.get(0), dbRM.getDBID());  
    	}
    	
    	int count = 0;
        for (Iterator<?> it = fileRMs.iterator(); it.hasNext();) {
            GKInstance curRM = (GKInstance) it.next();
            if (curRM.getDBID() < 0) {
            	//logger.info(curRM);
            	List<String> names = curRM.getAttributeValuesList(ReactomeJavaConstants.name);
            	if (names != null) {
            		String curName = names.get(0);
            		String prefName = null;
            		if (curName.startsWith("a ")) {
            			prefName = curName.replaceFirst("^a\\s", "");
        				count++;
            		} else if (curName.startsWith("an ")) {
            			prefName = curName.replaceFirst("^an\\s", "");
        				count++;
            		}
            		if (prefName != null) {
	        			if (dbMap.containsKey(prefName)) {
	        				logger.info("Match: " + prefName + " (" + dbMap.get(prefName) + ")");
	            		}
            		}
            	}
            }
        }
    	logger.info("count: " + count);
    }

	// Cyc ID mapping between AraCyc (file) and RiceCyc (db) 
    private void advancedRefMolMapping(XMLFileAdaptor fileAdaptor, MySQLAdaptor dbAdaptor) throws Exception {

        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        PersistenceManager.getManager().setActiveMySQLAdaptor(dbAdaptor);

    	// iterate over RefMols in A.th. data file with no ChEBI ID
    	Collection<?> fileRMs = fileAdaptor.fetchInstanceByAttribute(
        		ReactomeJavaConstants.ReferenceMolecule, 
        		ReactomeJavaConstants.identifier, 
        		"IS NULL",
        		"");

    	int count = 0;
    	// build a map of db RM "Cyc:" xrefs (xref:DB_ID)
    	Map<String, Long> dbRefMolMap = new HashMap<String, Long>();
    	Collection<GKInstance> dbRMs = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceMolecule);
    	for (GKInstance curRM : dbRMs) {
    		if (curRM.getAttributeValue(ReactomeJavaConstants.crossReference) != null) {
    			List<?>curXrefs = curRM.getAttributeValuesList(ReactomeJavaConstants.crossReference);
    	        for (Iterator<?> it = curXrefs.iterator(); it.hasNext();) {
    	            GKInstance curXref = (GKInstance) it.next();
    	            if (curXref.getDisplayName() != null) {
	    	            if (curXref.getDisplayName().matches("RiceCyc:.*")) {
	    	            	dbRefMolMap.put((curXref.getDisplayName().split(":"))[1].toUpperCase(), curRM.getDBID());
			            	count++;
	    	            }
    	            }
    	        }
    		}
    	}
        //logger.info("Count of dbRMs with \"Cyc:\" in xref: " + count);
        //logger.info(dbRefMolMap);
        
    	count = 0; // re-init
    	int match_count = 0;
        for (Iterator<?> it = fileRMs.iterator(); it.hasNext();) {
            GKInstance curRM = (GKInstance) it.next();
    		// filter to those with a negative db_id
            if (curRM.getDBID() < 0) {
            	List<GKInstance> xrefs = curRM.getAttributeValuesList(ReactomeJavaConstants.crossReference);
	            for (GKInstance xref : xrefs) {
		            // filter down to those with an AraCyc xref
		            if (xref.getDisplayName().toUpperCase().startsWith("ARACYC:")) {
		            	//logger.info(xref.getDisplayName());
		            	count++;
			        	// look for this xref in the gk_central.RefMols map
		            	String xrefName = xref.getDisplayName().split(":")[1].toUpperCase();
			            if (dbRefMolMap.containsKey(xrefName)) {
			            	match_count++;
			            	System.out.println("RefMol match: file instance " + curRM + " to db instance " + dbRefMolMap.get(xrefName) + " on " + xrefName);
			            	// if found, replace the A.th. file version with the instance in the db (with or without ChEBI)
			            	PostProcessHelper.updateFromDB(curRM, dbAdaptor.fetchInstance((Long)dbRefMolMap.get(xrefName)), SynchronizationManager.getManager());
			            }
		            }
	            }
            }
        }
        //logger.info("Count of fileRMs with no ChEBI, neg. DB_ID, and \"AraCyc:\" in xref: " + count);
        logger.info("Xref \"Cyc\" Frame ID matches: " + match_count);
    }

    // build map of all names or xref db ids (unique keys) to chebi ids (potentially non-uniq values)
    private Map<String, String> importChebiMap(String fileName, boolean isNameMap) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        FileUtility fu = new FileUtility();
        fu.setInput(fileName);
        String line = null;

        // xref db id regex matches, if needed
    	String pubchemExp = "PUBCHEM";
    	String ligandExp = "LIGAND.*";
        
        fu.readLine(); // skip header
        while ((line = fu.readLine()) != null) {
        	String[] curLine = line.replace("\"", "").split("\t");
            String chebiId = curLine[1].trim(); // ignore first column (Cyc Frame ID)

            if (isNameMap) { // matching on names and synonyms
	            map.put(curLine[2].trim().toLowerCase(), chebiId); // chebi canonical name 
	            for (String name : curLine[3].trim().split("\\$")) { // other names and synonyms
	            	map.put(name.trim().toLowerCase(), chebiId);
	            }
            } else { // matching on xref db ids (CAS, LIGAND, PubChem)
            	if (curLine.length == 7) { // there are xrefs to examine
	            	String[] aryIDs = curLine[6].trim().replaceAll("\\$"," ").replaceAll("[()]","").split("\\s");
	            	Integer idIndex = 0;
		            for (String id : aryIDs) { // xref db ids
		            	if (id.matches(".*CAS.*")) {
		            		map.put("CAS:" + aryIDs[idIndex+1], chebiId);
		            	} else
		            	if (id.matches(".*LIGAND.*")) {
		            		map.put("LIGAND:" + aryIDs[idIndex+1], chebiId);
		            	} else
		            	if (id.matches(".*PUBCHEM.*")) {
		            		map.put("PUBCHEM:" + aryIDs[idIndex+1], chebiId);
		            	}
		            	idIndex++;
		            }
            	}
            }
        }
        fu.close();
        return map;
    }

    private void advancedChEBIMapping(XMLFileAdaptor fileAdaptor, MySQLAdaptor dbAdaptor) throws Exception {
    	// compare CHEBI file to Ath file
        String filePath = "resources/ricecyc_inChEBI_noHTML.txt";

    	// load new ChEBI mappings (Janna Hastings - RiceCyc) from HTML-decoded file into Map (CHEBI_ID->[name,synonym1,2,3...])
        Map<String, String> nameMap = importChebiMap(filePath, true); // set up the name map
        Map<String, String> xrefDbIdMap = importChebiMap(filePath, false); // set up the xref db id map

        //for (String key : nameMap.keySet()) logger.info(key + " -> " + nameMap.get(key)); // TEST
        //logger.info(nameMap.size()); // TEST
        
        //for (String key : xrefDbIdMap.keySet()) logger.info(key + " -> " + xrefDbIdMap.get(key)); // TEST
        //logger.info(xrefDbIdMap.size()); // TEST

        // iterate through only new (not known to be in db yet), chebi-less RefMols
    	Collection<?> RMs = fileAdaptor.fetchInstanceByAttribute(
        		ReactomeJavaConstants.ReferenceMolecule, 
        		ReactomeJavaConstants.identifier, 
        		"IS NULL",
        		"");
        
    	int count = 0;
        for (Iterator<?> it = RMs.iterator(); it.hasNext();) {
            GKInstance curRM = (GKInstance) it.next();
            if (curRM.getDBID() < 0) {
        		// name mapping
            	List<String> names = curRM.getAttributeValuesList(ReactomeJavaConstants.name);
	            //System.out.println(curRM.getDBID() + "\t" + names.get(0)); // TEST
				// compare name(0) to ChEBI name, name, and synonyms
	            for (String name : names) {
		            //if (nameMap.containsKey(names.get(0).toLowerCase())) {
		            if (nameMap.containsKey(name.toLowerCase())) {
		            	// if you hit a match; update the RM w/ the ChEBI ID
		            	//logger.info("match! " + names.get(0) + " : " + nameMap.get(names.get(0).toLowerCase()));
		            	logger.info("match! " + name + " : " + nameMap.get(name.toLowerCase()));
		            	count++;
				    	// compare resulting RMs to db (based on ID)
		            		// update from db
		            		// if not in DB, get data from EBI
		            }
	            }

        		// xref db id mapping
            	List<GKInstance> xrefs = curRM.getAttributeValuesList(ReactomeJavaConstants.crossReference);
	            //System.out.println(curRM.getDBID() + "\t" + ((GKInstance)xrefs.get(0)).getDisplayName()); // TEST
	            
	            for (GKInstance xref : xrefs) {
		            if (xrefDbIdMap.containsKey(xref.getDisplayName().toUpperCase())) {
		            	// if you hit a match; update the RM w/ the ChEBI ID
		            	logger.info("match! " + xref.getDisplayName() + " : " + xrefDbIdMap.get(xref.getDisplayName().toUpperCase()));
		            	count++;
				    	// compare resulting RMs to db (based on ID)
		            		// TODO: update from db (not needed right now)
		            }
	            }
            }
        }
        logger.info("Matches: " + count);
    }
    
    // Merge duplicate instances based on name identity alone (b/c identity field is not available)
    // NOTE: this is a very loose merge, in terms of merge criteria, so use with caution 
    private void mergeDupeInstancesOnName(XMLFileAdaptor fileAdaptor, String className) throws Exception {
    	Collection<GKInstance> refMols = fileAdaptor.fetchInstancesByClass(className);
        Map<String, GKInstance> refMolHash = new HashMap<String, GKInstance>();
        Integer count = 0;

        // build a hash of no-identity instances indexed by name; check for dupes and merge as you go 
        for (Iterator<?> it = refMols.iterator(); it.hasNext();) {
            GKInstance curRM = (GKInstance) it.next();
            List<String> names = curRM.getAttributeValuesList(ReactomeJavaConstants.name);
            String curName = names.get(0);
            if (curRM.getAttributeValue(ReactomeJavaConstants.identifier) == null) {
	            if (refMolHash.get(curName) == null)
	            	refMolHash.put(curName,curRM);
	            else {
	                count++;
	            	GKInstance keptRefMol = refMolHash.get(curName);
	            	logger.info("Duplicate found; merging " + curRM + " into " + keptRefMol);
	            	mergeInstance(keptRefMol, curRM, fileAdaptor);
	            	keptRefMol = reorderNames(keptRefMol);
	            }
            }
        }
        logger.info("total duplicate instances, based on name: " + count);
    }

    // check for ReferenceMolecules, SimpleEntities, and DatabaseIdentifiers already in DB; replace locals
    private void retrieveNoChebiRefMols(XMLFileAdaptor fileAdaptor, MySQLAdaptor dbAdaptor) throws Exception {

        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        PersistenceManager.getManager().setActiveMySQLAdaptor(dbAdaptor);

    	Collection<?> gk_central_RefMols = dbAdaptor.fetchInstanceByAttribute(
        		ReactomeJavaConstants.ReferenceMolecule, 
        		ReactomeJavaConstants.identifier, 
        		"IS NULL", 
        		"");
        Collection<?> aracyc_RefMols = fileAdaptor.fetchInstanceByAttribute(
        		ReactomeJavaConstants.ReferenceMolecule, 
        		ReactomeJavaConstants.identifier, 
        		"IS NULL", 
        		"");

        Map<String, GKInstance> fileRefMolHash = new HashMap<String, GKInstance>();

        // build a hash of file instances indexed by name, then loop through db instances, 
        for (Iterator<?> it = aracyc_RefMols.iterator(); it.hasNext();) {
            GKInstance curARM = (GKInstance) it.next();
            List<String> names = curARM.getAttributeValuesList(ReactomeJavaConstants.name);
            if (curARM.getDBID() > 0) {
                continue; // This reference is already downloaded from database.
            }
            else {
            	fileRefMolHash.put(names.get(0),curARM);
            	logger.info(curARM + ", name: " + names.get(0));
            }
        }

        // locate file instance on contains name and replace with db instance  
        Integer count = 0;
        for (Iterator<?> it2 = gk_central_RefMols.iterator(); it2.hasNext();) {
            GKInstance curGKRM = (GKInstance) it2.next();
            List<String> names = curGKRM.getAttributeValuesList(ReactomeJavaConstants.name);
            if (names != null && names.size() > 0) {
	            if (fileRefMolHash.containsKey(names.get(0))) {
                    logger.info(fileRefMolHash.get(names.get(0)) + " will be replaced with " + curGKRM);
	            	PostProcessHelper.updateFromDB(fileRefMolHash.get(names.get(0)), curGKRM, SynchronizationManager.getManager());
                    count++;
	            }
            }
        }
        logger.info("total merged ref mols, based on name: " + count);
    }
    
    // check for ReferenceMolecules, SimpleEntities, and DatabaseIdentifiers already in DB; replace locals
    private void retrieveEntitiesAndIdentifiersFromDB(XMLFileAdaptor fileAdaptor, MySQLAdaptor dbAdaptor) throws Exception {
	    // Make sure the following class instances not duplicated
	    String[] classNames = new String[] {
	            ReactomeJavaConstants.DatabaseIdentifier,
	            ReactomeJavaConstants.ReferenceMolecule,
	            ReactomeJavaConstants.SimpleEntity
	    };
	    for (String clsName : classNames) {
	        PostProcessHelper.useDBInstances(clsName,
	                                         dbAdaptor, 
	                                         fileAdaptor);
	    }
    }

    // refactored code to handle compartment setting for individual instances
    private GKInstance setCompartmentToCytoplasm(GKInstance instance, GKInstance newCompartment) throws Exception {
        // remove all other compartment references
        List<?> compartments = instance.getAttributeValuesList(ReactomeJavaConstants.compartment);
        compartments.clear();
    	// set instance to new compartment (even if empty)
        instance.setAttributeValue(ReactomeJavaConstants.compartment, newCompartment);
        return instance; 
    }
    
    // hack to make sure the name matching the displayName is in the first names slot 
    // (otherwise, future resetDisplayNames() may unintentionally use the wrong name)
    private GKInstance reorderNames(GKInstance instance) throws Exception {
    	List<String> names = instance.getAttributeValuesList(ReactomeJavaConstants.name);
    	String displayName = instance.getDisplayName().split("\\[.*\\]$")[0].trim(); // strip compartment name
    	for (String name : names) {
    		if (name.equals(displayName)) {
    			names.remove(name);
    			names.add(0, name); // re-add the name that matches the displayname to the front of the list
    			break;
    		}
    	}
    	return instance;
    }
    
    // set all compartments to cytoplasm for PEs and Reactions
    @SuppressWarnings("unchecked")
    private void compartmentMischief(XMLFileAdaptor fileAdaptor, MySQLAdaptor dbAdaptor) throws Exception {
    	GKInstance cytoplasm = fileAdaptor.fetchInstance(459L); // cytoplasm instance from gk_central
        Collection<GKInstance> PEs = (Collection<GKInstance>)fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.PhysicalEntity);
        for (Iterator<?> it = PEs.iterator(); it.hasNext();) {
            GKInstance curPE = (GKInstance)it.next();
			// get rid of empty "Compartment:" string in definition attribute
            if ((curPE.getAttributeValue(ReactomeJavaConstants.definition)) != null) {
	            if ((curPE.getAttributeValue(ReactomeJavaConstants.definition)).equals("Compartment: ")) {
	            	curPE.setAttributeValue(ReactomeJavaConstants.definition, null);
	            }
            }
            // if it's a SimpleEntity, clone from from current cytosol instance, or merge into existing cytoplasm instance
            if (curPE.getSchemClass().getName().equals(ReactomeJavaConstants.SimpleEntity)) {
            	// check for a pre-existing cytoplasm instance to merge into
            	GKInstance cytoplasmSE = null;
            	GKInstance refEntity = (GKInstance)curPE.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            	if (refEntity != null) {
                    List<GKInstance> SErefs  = fileAdaptor.getReferers(refEntity);
                    SErefs.remove(curPE); // don't need to compare to self
                    if (SErefs != null && SErefs.size() > 0) {
                        for (Iterator<?> it4 = SErefs.iterator(); it4.hasNext();) {
                            GKInstance curSEref = (GKInstance) it4.next();
                            GKInstance curSErefCompartment = (GKInstance)curSEref.getAttributeValue(ReactomeJavaConstants.compartment);
                            if (curSErefCompartment != null) { 
	                            if (curSErefCompartment.getDisplayName().matches("cytoplasm")) {
	                            	cytoplasmSE = curSEref;
	                            }
                            }
                        }
                    }
            	}
            	// if SE [cytoplasm] already detected, merge the current SE with the pre-existing SE 
            	if (cytoplasmSE != null) {
                    mergeInstance(cytoplasmSE, curPE, fileAdaptor);
                    cytoplasmSE = reorderNames(cytoplasmSE);
	              	logger.info("SimpleEntity " + curPE + " merged into " + cytoplasmSE);
            	}
            	else {
	            	// otherwise, clone it to a new cytoplasm-located instance and delete the original cytosol instance
	            	GKInstance newSE = (GKInstance)curPE.clone();
					newSE.setDBID(fileAdaptor.getNextLocalID());
					fileAdaptor.addNewInstance(newSE);
	            	newSE = setCompartmentToCytoplasm(newSE, cytoplasm);
	            	// repair references to reactions: point to new cytoplasm instance
	                List<GKInstance> referrers = fileAdaptor.getReferers(curPE);
	                if (referrers != null && referrers.size() > 0) {
	                    for (Iterator<?> it3 = referrers.iterator(); it3.hasNext();) {
	                        GKInstance referrer = (GKInstance) it3.next();
	                        // Find the referrer and replace it with keptInstance
	                        InstanceUtilities.replaceReference(referrer, curPE, newSE);
	                    }
	                }
	                fileAdaptor.deleteInstance(curPE); // remove original cytosol SE
		            InstanceDisplayNameGenerator.setDisplayName(newSE); // reset display name to include new compartment
	              	logger.info("SimpleEntity " + newSE + " created from " + curPE);
            	}
            }
            else {
                // set PE to cytoplasm
            	curPE = setCompartmentToCytoplasm(curPE, cytoplasm);
				// also get rid of empty "Compartment:" string in definition attribute on EWAS
	            if (curPE.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
	            	// reset display name to include new compartment; this is an EWAS-specific hack to preserve the (AT#) in the displayName
	            	curPE.setDisplayName(curPE.getDisplayName() + " [cytoplasm]"); 
	            }
	            else {
	            	InstanceDisplayNameGenerator.setDisplayName(curPE); // reset display name to include new compartment
	            }
              	logger.info(curPE + " compartment changed to cytoplasm");
            }
        }
        // now that PEs compartments are changed and this this info is propagated to displayNames, reset the displayNames on CAs  
    	PostProcessHelper.resetDisplayNames(ReactomeJavaConstants.CatalystActivity, fileAdaptor);
        // gather reactions and reset their compartment to cytoplasm, following the same rule as above
        Collection<GKInstance> RXNs = (Collection<GKInstance>)fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Reaction);
        for (Iterator<?> it2 = RXNs.iterator(); it2.hasNext();) {
            GKInstance curRXN = (GKInstance)it2.next();
            // set RXN to cytoplasm
        	curRXN = setCompartmentToCytoplasm(curRXN, cytoplasm);
			// get rid of empty "Compartment:" string in definition attribute
            if ((curRXN.getAttributeValue(ReactomeJavaConstants.definition)) != null) {
	            if ((curRXN.getAttributeValue(ReactomeJavaConstants.definition)).equals("Compartment: ")) {
	            	curRXN.setAttributeValue(ReactomeJavaConstants.definition, null);
	            }
            }
        }
    }
    
    private void delSpeciesFromSimpleEntities(XMLFileAdaptor fileAdaptor, MySQLAdaptor dbAdaptor) throws Exception {
    	Collection<?> SEs = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.SimpleEntity);
        for (Iterator<?> it = SEs.iterator(); it.hasNext();) {
            GKInstance curSE = (GKInstance) it.next();
            GKInstance species = (GKInstance)curSE.getAttributeValue(ReactomeJavaConstants.species);
            if (species != null) {
            	curSE.setAttributeValue(ReactomeJavaConstants.species, null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addMolecularActivityToCA(XMLFileAdaptor fileAdaptor, MySQLAdaptor dbAdaptor) throws Exception {
    	// load EC2GO mapping file (HashMap<EC #, GO ID>)
        String EC2GO_filepath = "resources/ec2go_101413.tab";
    	FileUtility fileUtility = new FileUtility();
        Map<String, String> EC2GO_Map = fileUtility.importMap(EC2GO_filepath);

        // TEST
		for (String key : EC2GO_Map.keySet()) 
        {
        	key = key.toUpperCase();
            String value = EC2GO_Map.get(key);
            logger.info(key + "\t" + value);
        }
        
        // retrieve reactions w/ EC-NUMBER in crossReference attrib
        Collection<GKInstance> reactions = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Reaction);
        if (reactions != null) {
            for (GKInstance reaction : reactions) {
            	// get EC number
            	String ECNumber = "";
            	Collection<GKInstance> xrefs = reaction.getAttributeValuesList(ReactomeJavaConstants.crossReference);
            	for (GKInstance xref : xrefs) {
            		if (xref.getDisplayName().contains("EC-NUMBER:")) {
            			ECNumber = "EC:" + xref.getDisplayName().substring(10); // strip "EC-NUMBER" and replace w/ "EC"
            			break; // assume only one
            		}
            	}
            	// look for EC# in EC2GO mapping file, if found...
            	if (EC2GO_Map.containsKey(ECNumber)) {
            		//logger.info("match: " + reaction.getDisplayName() + " (" + ECNumber + ") :: " + EC2GO_Map.get(ECNumber));
					// import GO_MolecularFunction instance from gk_central (based on GO ID)
                    Collection<GKInstance> GO_matches = dbAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.GO_MolecularFunction,
												                    		ReactomeJavaConstants.accession,
												                    		"=", 
												                    		EC2GO_Map.get(ECNumber).substring(3)); // strip "GO:"
                    if (GO_matches != null) {
	                    if (GO_matches.size() > 1)
	                    	logger.info("More than one GO term available (" + GO_matches.toString() + ") for EC number " + ECNumber);
	                    GKInstance GO_MF = null;
	                    for (GKInstance curGO_MF : GO_matches) {
	                    	GO_MF = PostProcessHelper.getInstance(curGO_MF.getDBID(), fileAdaptor, dbAdaptor);
	                    	logger.info("Term " + EC2GO_Map.get(ECNumber) + " [" + curGO_MF.getDBID() + "] downloaded for " + ECNumber);
	                    	break; // assume one or use the first available
	                    }
						// retrieve curr reaction's catalyst activity
	                    GKInstance ca = (GKInstance)reaction.getAttributeValue(ReactomeJavaConstants.catalystActivity);
	                    if (ca != null) {
							// set CA's activity attribute to GO_MolecularFunction instance
	                    	ca.setAttributeValue(ReactomeJavaConstants.activity, GO_MF);
	                    	PostProcessHelper.resetDisplayNames(ReactomeJavaConstants.CatalystActivity, fileAdaptor);
	                    	logger.info("CatalystActivity.activity set to " + GO_MF + " ( in " + reaction + ")");
	                    }
	                    else {
	                    	logger.info("No CatalystActivity available for " + reaction);
	                    }
                    }
                    else {
                    	logger.info("No GO_MolecularFunction instances available for " + ECNumber + " ( in " + reaction + ")");
                    }
            	}
            }
        }
    }
    
    // make RGPs into isoforms, set attributes, fix references, create new RGPs for "orphan" isoforms
    private void adaptIsoforms(XMLFileAdaptor fileAdaptor) throws Exception {
    	String athExp = "AT.G.*";
    	String isoformExpression = "AT.G\\d{5}\\.[1-9]";
    	//iterate RGPs
    	Collection<?> RGPs = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
        for (Iterator<?> it = RGPs.iterator(); it.hasNext();) {
            GKInstance curRGP = (GKInstance) it.next();
            String displayName = curRGP.getDisplayName();
            String identifier = curRGP.getAttributeValue(ReactomeJavaConstants.identifier).toString();;
            List<String> names = curRGP.getAttributeValuesList(ReactomeJavaConstants.name);
            List<GKInstance> referrers = fileAdaptor.getReferers(curRGP);

            logger.info("curRGP: " + displayName + "|" + identifier + "|" + names);
            
            //if AT# is not in displayname (assumes canonical gene with chemical name present)
            // or displayName contains -MONOMER (assumes displayName contains AT#)
            if (!displayName.matches(athExp) || displayName.contains("-MONOMER")) {
            	//set to identifier (make sure old displayName is in name(0))
            	if (!names.contains(displayName)) {
            		names.add(0, displayName);
            	}
           		curRGP.setDisplayName(identifier);
            	//set related displayNames to "name (AT#)"
                if (referrers != null && referrers.size() > 0) {
                    for (Iterator it2 = referrers.iterator(); it2.hasNext();){
                        GKInstance referrer = (GKInstance) it2.next();
                        // if AT# is in displayName, update EWAS and DefinedSet displayName
                        if (displayName.contains("-MONOMER")) {
                            if (referrer.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
	                    		logger.info("Referrer " + referrer.getDisplayName() + " set to " + identifier); 
	                          	referrer.setDisplayName(identifier);
                            }
                        }
                        // if AT# not in displayname, update EWAS displayName only
                        else if (referrer.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
                          	logger.info("Referrer " + referrer.getDisplayName() + " set to " + displayName + " (" + identifier + ")"); 
                        	referrer.setDisplayName(displayName + " (" + identifier + ")");
                        }
                    }
                }
            }
            //elseif displayName contains isoform .#
            else if (displayName.matches(isoformExpression)) {
            	//make new ReferenceIsoform and copy over all attributes
            	GKInstance isoform = fileAdaptor.createNewInstance(ReactomeJavaConstants.ReferenceIsoform);
            	isoform.setAttributeValue(ReactomeJavaConstants.identifier, displayName);
            	isoform.setAttributeValue(ReactomeJavaConstants.referenceDatabase,
            			curRGP.getAttributeValue(ReactomeJavaConstants.referenceDatabase));
            	isoform.setAttributeValue(ReactomeJavaConstants.species,
            			curRGP.getAttributeValue(ReactomeJavaConstants.species));
            	isoform.setAttributeValue(ReactomeJavaConstants.crossReference,
            			curRGP.getAttributeValue(ReactomeJavaConstants.crossReference));
            	List<String> isoformNames = names; 
            	//move GQT-identifier to name(0), if needed
            	if (!isoformNames.contains(identifier)) {
            		isoformNames.add(0, identifier);
            	}
            	isoform.setAttributeValue(ReactomeJavaConstants.name,
            			isoformNames);
                isoform.setDisplayName(displayName);
                
              	logger.info("New isoform " + isoform.getDisplayName() + " created."); 

              	String truncDisplayName = displayName.substring(0, displayName.indexOf(".")); 
    	    	Collection<?> otherRGPs = fileAdaptor.fetchInstanceByAttribute(
    	    		ReactomeJavaConstants.ReferenceGeneProduct,
    	    		ReactomeJavaConstants.identifier,
    	    		"=",
    	    		truncDisplayName);
            	//if isoform is only instance of this AT#
	    		if (otherRGPs == null || otherRGPs.size() == 0) {
	    			// there is no other RGP with this AT#
	              	logger.info("Orig RGP " + curRGP.getDisplayName() + " given new identifier " + truncDisplayName);
            		//update orig RGP
	    			curRGP.setAttributeValue(ReactomeJavaConstants.identifier, truncDisplayName);
	    			curRGP.setDisplayName(truncDisplayName);
	    			// get rid of old isoform name
	            	if (names.contains(displayName)) {
	            		names.remove(names.indexOf(displayName));
	            	}
	            	// add new canonical name
	            	if (!names.contains(identifier)) {
	            		names.add(0, identifier);
	            	}
	            	// update displayName of corresponding EWAS
		            if (referrers != null && referrers.size() > 0) {
		                for (Iterator it3 = referrers.iterator(); it3.hasNext();){
		                	GKInstance referrer = (GKInstance) it3.next();
	                        if (referrer.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
	        	              	logger.info("Orig RGP EWAS displayName " + referrer.getDisplayName() + " updated to " + truncDisplayName);
	        	              	referrer.setDisplayName(truncDisplayName);
	                        }
		                }
		            }
		            // associate new isoform with this RGP
		            isoform.addAttributeValue(ReactomeJavaConstants.isoformParent, curRGP);
	              	logger.info("Isoform parent " + curRGP + " added for " + isoform); 
	    		}
            	//else, there is already an RGP
	    		else {
	    			// remove EWAS referrer; no longer needed
		            if (referrers != null && referrers.size() > 0) {
		                for (Iterator it4 = referrers.iterator(); it4.hasNext();){
		                	GKInstance referrer = (GKInstance) it4.next();
	                        if (referrer.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
	        	              	logger.info("Orig RGP EWAS " + referrer.getDisplayName() + " removed."); 
	                        	fileAdaptor.deleteInstance(referrer);
	                        }
		                }
		            }
            		//remove orig RGP that is now represented by an isoform (b/c there is still another RGP available)
	              	logger.info("Orig RGP " + curRGP.getDisplayName() + " removed."); 
	    			fileAdaptor.deleteInstance(curRGP);
		            // associate new isoform with this RGP (assuming only one)
	    			if (otherRGPs.size() == 1) {
		    			for (Iterator it5 = otherRGPs.iterator(); it5.hasNext();){
		                	GKInstance otherRGP = (GKInstance) it5.next();
	                		isoform.addAttributeValue(ReactomeJavaConstants.isoformParent, otherRGP);
			              	logger.info("Isoform parent " + otherRGP + " added for " + isoform); 
		    			}
	    			}
	    			else {
		              	logger.info("More than one RGP for " + isoform + ". No isoform parent added."); 
	    			}
	    		}
	    		/* perhaps none of this is needed?
            	//update all referrers to the RGP instance(s) in EWAS (displayNames, referenceEntity)
	            if (referrers != null && referrers.size() > 0) {
	                for (Iterator it3 = referrers.iterator(); it3.hasNext();){
	                	GKInstance referrer = (GKInstance) it3.next();

	                	// make new accompanying EWAS for original RGP, if needed
	    	    		if (otherRGP == null || otherRGP.size() == 0) {
	    	    			if (referrer.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
	    	    				// NOTE: assumes only one EWAS referrer (perhaps a dangerous assumption)
	    	    				GKInstance newEWAS = (GKInstance) referrer.clone();
	    	    				newEWAS.setDBID(fileAdaptor.getNextLocalID());
	    	    				fileAdaptor.addNewInstance(newEWAS);
	    	    				newEWAS.setDisplayName(truncDisplayName);
	    		              	logger.info("New EWAS " + newEWAS.getDisplayName() + " created.");

	    		              	List<String> newEWASnames = newEWAS.getAttributeValuesList(ReactomeJavaConstants.name);
	    		    			// get rid of old isoform name
	    		            	if (newEWASnames.contains(displayName)) {
	    		            		newEWASnames.remove(newEWASnames.indexOf(displayName));
	    		            	}
	    		            	// add new canonical name
	    		            	if (!newEWASnames.contains(truncDisplayName)) {
	    		            		newEWASnames.add(0, truncDisplayName);
	    		            	}
	    		              	
	    		              	newEWAS.setAttributeValue(ReactomeJavaConstants.referenceEntity, curRGP);
	    		              	logger.info("Original RGP " + newEWAS.getAttributeValue(ReactomeJavaConstants.referenceEntity) 
	    		              			+ " assigned as reference entity for new " + newEWAS);
	    	    			}
	    	    		}
	                    // replace the original reference to RGP with reference to isoform instead
	                    InstanceUtilities.replaceReference(referrer, curRGP, isoform);
		              	logger.info("Reference (from " + referrer.getSchemClass().getName() + " " + referrer.getDisplayName() 
		              			+ ") to RGP " + curRGP.getDisplayName() + " replaced with isoform " + isoform.getDisplayName());
	                }
	            }*/
            }
        }
    }
    
    //in DefinedSets, remove every reference to an isoform string and replace w/ the RGP, as needed
    private void updateDefinedSets(XMLFileAdaptor fileAdaptor) throws Exception {
    	String athExp = "AT.G.*";
    	String isoformExpression = "AT.G\\d{5}\\.[1-9]";
    	// iterate through defined sets
    	Collection<?> DS = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DefinedSet);
        for (Iterator<?> it = DS.iterator(); it.hasNext();) {
            GKInstance curSet = (GKInstance) it.next();
            String displayName = curSet.getDisplayName();
            List<GKInstance> members = curSet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            List<String> names = curSet.getAttributeValuesList(ReactomeJavaConstants.name);

            logger.info("curSet: " + displayName + " | " + members);

            /*
            Set<String> uniqMembers = new HashSet<String>(); // holds list of unique AT#'s w/o isoforms or other non-AT members 
    		// iterate over members
            for (Iterator<?> it2 = members.iterator(); it2.hasNext();) {
                GKInstance curMember = (GKInstance) it2.next();
    			// add member display name -- or ReferenceEntity's displayName -- (AT* w/o .#) to uniq list
            	uniqMembers.add(getMemberCanonicalGeneOrSimpleEntity(curMember));
    			// remove any EWAS w/ a reference entity pointing to a ReferenceIsoform 
                if (((GKInstance)curMember.getAttributeValue(ReactomeJavaConstants.referenceEntity)).getSchemClass().getName()
                		== ReactomeJavaConstants.ReferenceIsoform) {
                	it2.remove(); // remove the member reference from the hasMembers collection
                	fileAdaptor.deleteInstance(curMember); // remove the member EWAS itself
                }
            }
    		// iterate over uniq list, look for missing members in defined set and add back an RGP EWAS if needed
            for (Iterator<?> it3 = uniqMembers.iterator(); it3.hasNext();) {
            	String uniqMember = (String) it3.next();
                for (GKInstance member : members) {
                	if (member.getDisplayName().contains(uniqMember)) { // quick way of checking for the presence of this gene locus id
                		it3.remove();
                	}
                }
            }
            // using any leftover uniqMembers not accounted for in the hasMembers list, add needed EWAS to hasMembers
        	for (String uniqMember : uniqMembers) {
        		// get EWAS for members to be added
    	    	Collection<GKInstance> c = fileAdaptor.fetchInstanceByAttribute(
    	    			ReactomeJavaConstants.EntityWithAccessionedSequence, 
    	    			ReactomeJavaConstants.identifier, "=", uniqMember);
                // add to members collection
    	    	if (c.size() == 1) {
    	    		for (GKInstance newEWASMember : c) {
    	    			members.add(newEWASMember);
    	    			continue; // "There can be only one."
    	    		}
    	    	}
    	    	else {
    	    		throw new Exception(uniqMember + " exists as more than one EWAS.");
    	    	}
        	}
    		// if only one member (or no members) left, remove defined set
            if (members.size() <= 1) {
            	// change any referrers to appropriate RGP EWAS
            	if (members.size() == 1) {
            		GKInstance remainingMember = members.get(0);
	                List<GKInstance> referrers = fileAdaptor.getReferers(curSet);
	            	for (GKInstance referrer : referrers) {
	            		InstanceUtilities.replaceReference(referrer, curSet, remainingMember);
	            	}
            	}
            	fileAdaptor.deleteInstance(curSet);
            }*/
    		// remove all names, and re-add all display names of members to names
            names.clear();
            for (GKInstance member : members) {
            	names.add(member.getDisplayName().split("\\[.*\\]$")[0].trim()); // strip compartments TODO: verify that this works
            }

            // clean up existing display name before comparing to member list
            String revisedDisplayName = displayName;
            if (displayName.indexOf(" [") >= 0)
            	revisedDisplayName = displayName.substring(0, displayName.indexOf(" [")); // remove vestigial compartment
            if (displayName.indexOf("-MONOMER") >= 0)
            	revisedDisplayName = revisedDisplayName.substring(0, revisedDisplayName.indexOf("-MONOMER"));
            if (displayName.indexOf(".") >= 0)
            	revisedDisplayName = displayName.substring(0, displayName.indexOf("."));

            // Update displayName to match primary canonical gene. Use enzymatic name if possible; otherwise, use AT#.
            for (Iterator<?> it2 = members.iterator(); it2.hasNext();) {
                GKInstance curMember = (GKInstance) it2.next();
                String memberDisplayName = curMember.getDisplayName();
                if (memberDisplayName.contains(revisedDisplayName))
                	if (memberDisplayName.indexOf(" (" + revisedDisplayName + ")") > 0) // meaning that it's an AT# in ()
                		revisedDisplayName = memberDisplayName.substring(0, memberDisplayName.indexOf(" (" + revisedDisplayName));
            }
            // add (revised) display name to front of names list, if not already present
        	if (names.contains(revisedDisplayName)) {
        		names.remove(names.indexOf(revisedDisplayName));
        	}
        	names.add(0, revisedDisplayName);
            InstanceDisplayNameGenerator.setDisplayName(curSet);
    	}
    }

    // utility method to isolate the AT # from a Defined Set's member EWAS. It will either be in the member's displayName or
    // in the member's referenceEntity attribute, or not there at all in the case of the SimpleEntities
    public String getMemberCanonicalGeneOrSimpleEntity(GKInstance member) throws Exception {
    	String athExp = "AT.G.*";
    	String locName = member.getDisplayName();
    	Integer pos = locName.indexOf(".");
    	GKInstance refEntity = (GKInstance)member.getAttributeValue(ReactomeJavaConstants.referenceEntity);
    	
    	if ((refEntity != null) && refEntity.getSchemClass().getName() == ReactomeJavaConstants.SimpleEntity) {
    		return locName; // it's a SimpleEntity, just take the chemical name
    	} else {
	    	if (pos >= 0)
	    		locName = locName.substring(0, pos); // canonical gene locus name
	    	if (locName.matches(athExp)) {
	        	return locName;
	        } else { // it's an isoform EWAS 
	        	String refEntityDisplayName = (refEntity.getDisplayName()); 
	        	pos = refEntityDisplayName.indexOf(".");
	        	if (pos >= 0)
	        		refEntityDisplayName = refEntityDisplayName.substring(0, pos);
	        	if (refEntityDisplayName.matches(athExp)) {
	        		return refEntityDisplayName;
	        	} else {
	        		throw new Exception("Cannot locate a confirming locus ID for Defined Set member " + member);
	        	}
	    	}
    	}
    }
    
    private void removeUnusedRefDBs(XMLFileAdaptor fileAdaptor) throws Exception {
        List<String> removeDBsList = Arrays.asList("KEGG-legacy");
    	Collection<?> refdbs = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceDatabase);
        for (Iterator<?> it = refdbs.iterator(); it.hasNext();) {
            GKInstance refdb = (GKInstance) it.next();
            if (removeDBsList.contains(refdb.getDisplayName())) {
                fileAdaptor.deleteInstance(refdb);
        		logger.info(refdb.getDisplayName() + " deleted.");
            }
        }
    }

//    private void removeRegulationTypes(XMLFileAdaptor fileAdaptor) throws Exception {
//        Collection<?> reactions = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.RegulationType);
//        for (Iterator<?> it = reactions.iterator(); it.hasNext();) {
//            GKInstance reaction = (GKInstance) it.next();
//            fileAdaptor.deleteInstance(reaction);
//    		logger.info(reaction.getDisplayName() + " deleted.");
//        }
//    }
    
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
    // not used; probably leftover from unit testing
    public void fixDisplayNames() throws Exception {
        String srcFileName = DIR_NAME + "ricecyc_v8_0_biopax2.rtpj";
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor(srcFileName);
        changeDisplayNameForRefGeneProducts(fileAdaptor);
        copyNameFromRefToEntity(fileAdaptor);
        fileAdaptor.save(DIR_NAME + "ricecyc_v8_0_biopax2_fixed.rtpj");
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
        if (kept.getSchemClass().isValidAttribute(ReactomeJavaConstants.name)) {
            // Merge names only
            Set<String> names = new HashSet<String>();
            names.addAll(kept.getAttributeValuesList(ReactomeJavaConstants.name));
            names.addAll(removed.getAttributeValuesList(ReactomeJavaConstants.name));
            kept.setAttributeValue(ReactomeJavaConstants.name, new ArrayList<String>(names));
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
        Long species_instance_Id = 170905L;
        GKInstance species_instance = PostProcessHelper.getInstance(species_instance_Id, 
                                                        fileAdaptor, 
                                                        dbAdaptor);
        Collection c = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        for (Iterator it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
                GKInstance species = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.species);
                if (species != null)
                    continue;
                inst.setAttributeValue(ReactomeJavaConstants.species, species_instance);
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
            //GKInstance identifier = (GKInstance) refGeneProd.getAttributeValue(ReactomeJavaConstants.identifier);
            String identifier = (String)refGeneProd.getAttributeValue(ReactomeJavaConstants.identifier);
            if (refDb == null && identifier == null) {
                // Get the display name from the name attribute
                String name = (String) refGeneProd.getAttributeValue(ReactomeJavaConstants.name);
                int index = name.indexOf(", "); // Use an extra space in case something like this: 1,3-beta-glucan synthase component domain containing protein, expressed
                if (index > 0)
                    name = name.substring(0, index);
                if (name.length() > 0)
                    refGeneProd.setDisplayName(name);
            } else //
            	refGeneProd.setDisplayName(identifier);
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
        // Want to copy Locus Id to display name too - only in RiceCyc
        /*
        List<?> xrefs = reference.getAttributeValuesList(ReactomeJavaConstants.crossReference);
        if (xrefs != null && xrefs.size() > 0) {
            // Find locus id
            for (Iterator<?> it = xrefs.iterator(); it.hasNext();) {
                GKInstance xref = (GKInstance) it.next();
                String displayName = xref.getDisplayName();
                if (displayName.startsWith("AraCyc:")) {
                    String locusId = (String) xref.getAttributeValue(ReactomeJavaConstants.identifier);
                    index = locusId.indexOf("-");
                    if (index > 0)
                        locusId = locusId.substring(0, index);
                    rtn = name + " (" + locusId + ")";
                }
            }
        }*/
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
            System.out.println(reaction + " is splitting... (multiple catalytic agents)");
            GKInstance ca = (GKInstance) cas.get(0);
            reaction.setAttributeValue(ReactomeJavaConstants.catalystActivity, ca);
            System.out.println("0: " + ca + "catalyzes " + reaction);
            // Need to duplicate reaction first
            for (int i = 1; i < cas.size(); i++) {
                ca = (GKInstance) cas.get(i);
                System.out.println(i + ": " + ca + "catalyzes " + reaction);
                GKInstance duplicated = duplicateReaction(reaction, fileAdaptor);
                duplicated.setAttributeValue(ReactomeJavaConstants.catalystActivity, ca);
                // A non-conventional use of hasMember to keep things organized 
                // Deprecated. This attribute no longer exists in the schema JP 06.05.13
                //reaction.addAttributeValue(ReactomeJavaConstants.hasMember, duplicated);
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
    
    public void updateReferenceDatabases(XMLFileAdaptor fileAdaptor,
                                         MySQLAdaptor dbAdaptor) throws Exception {
        Collection<?> collection = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceDatabase);
        Map<String, GKInstance> nameToRefDb = new HashMap<String, GKInstance>();
        for (Iterator<?> it = collection.iterator(); it.hasNext();) {
            GKInstance refDb = (GKInstance) it.next();
            String name = refDb.getDisplayName();
            nameToRefDb.put(name, refDb);
        }
        // Load the mapping file
        String fileName = "resources/AraReactomeDB.xml";
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new File(fileName));
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
     * Find AraCyc: in the crossReference value list, move values to identifier and ReferenceDatabase.
     * Delete original DatabaseIdentifier instances if they are not used any more.
     * @param fileAdaptor
     * @throws Exception
     */
    private void updateReferenceGeneProduct(XMLFileAdaptor fileAdaptor) throws Exception {
        // To be used
        GKInstance species = fileAdaptor.fetchInstance(170905L);
        Collection<?> instances = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
        for (Iterator<?> it = instances.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            List<?> crossRefs = inst.getAttributeValuesList(ReactomeJavaConstants.crossReference);
            GKInstance araCycLoc = null;
            for (Iterator<?> it1 = crossRefs.iterator(); it1.hasNext();) {
                GKInstance tmp = (GKInstance) it1.next();
                if (tmp.getDisplayName().startsWith("AraCyc:")) {
                    araCycLoc = tmp;
                    break;
                }
            }
            if (araCycLoc == null)
                continue; // Do nothing: some weird reference
            String identifier = (String) araCycLoc.getAttributeValue(ReactomeJavaConstants.identifier);
            // Don't show MONOMER there
            int index = identifier.indexOf("-MONOMER");
            if (index > 0)
                identifier = identifier.substring(0, index); 
            GKInstance db = (GKInstance) araCycLoc.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
            inst.setAttributeValue(ReactomeJavaConstants.identifier, identifier);
            inst.setAttributeValue(ReactomeJavaConstants.referenceDatabase, db);
            // Delete the original value
            inst.removeAttributeValueNoCheck(ReactomeJavaConstants.crossReference, araCycLoc);
            // Replace species with "Arabidopsis thaliana"
            inst.setAttributeValue(ReactomeJavaConstants.species, species);
        }
    }
    
    @Test
    public void dumpToDatabase() throws Exception {
        ReactomeProjectDumper dumper = new ReactomeProjectDumper();
        MySQLAdaptor dba = new MySQLAdaptor("floret.cgrb.oregonstate.edu",
                                            "gk_central_021314_ath_merge",
                                            "react-app-user",
                                            "react-app-user_pw",
                                            3306);
        dumper.setMySQLAdaptor(dba);
        dumper.setDefaultPersonId(1385632L); // For Justin in AraCyc
        String[] fileNames = new String[] {
                DIR_NAME + "aracyc_v11_biopax-level2.rtpj.5.part_2"
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
        XMLFileAdaptor fileAdaptor = _grepPredictedPathways(pathwayListFileName, 
                                                            sourceDBA, 
                                                            prePredictMaxDBID);
        Collection<?> referenceGeneProducts = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
        // Don't use any ReferenceGeneProduct
        for (Iterator<?> it = referenceGeneProducts.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            inst.setAttributeValue(ReactomeJavaConstants.referenceGene, null);
        }
        // Try to save instances
        Collection<?> allInstances = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        logger.info("Total checked out instances: " + allInstances.size());
//        List<GKInstance> toBeDeleted = new ArrayList<GKInstance>();
        dumpPredictedProject(targetDBA, 
                             defaultPerson, 
                             prePredictMaxDBID,
                             fileAdaptor);
    }

    private void dumpPredictedProject(MySQLAdaptor targetDBA,
                                      GKInstance defaultPerson,
                                      Long prePredictMaxDBID,
                                      XMLFileAdaptor fileAdaptor) throws SQLException, Exception {
        List<GKInstance> toBeStored = new ArrayList<GKInstance>();
        Collection<?> allInstances = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        for (Iterator<?> it = allInstances.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            if (inst.getDBID() < 0) {
                toBeStored.add(inst); // New instances
                continue;
            }
            if (inst.getDBID() <= prePredictMaxDBID)
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
//        fileAdaptor.save(DIR_NAME + "PredictedRicePathways.rtpj");
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
    
    private XMLFileAdaptor _grepPredictedPathways(String pathwayListFileName,
                                                  MySQLAdaptor sourceDBA,
                                                  Long prePredictMaxDBID) throws Exception {
        List<Long> pathwayIds = loadPathwayIds(pathwayListFileName);
        List<GKInstance> pathways = new ArrayList<GKInstance>();
        for (Long dbId : pathwayIds) {
            GKInstance inst = sourceDBA.fetchInstance(dbId);
            if (inst != null)
                pathways.add(inst);
        }
        logger.info("Predicted pathways to be checked out: " + pathways.size());
        // Use an XMLFileAdaptor to hold checked out instances temporarily
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        EventCheckOutHandler handler = new EventCheckOutHandler();
        handler.addEscapeAttribute(ReactomeJavaConstants.orthologousEvent);
        handler.addEscapeAttribute(ReactomeJavaConstants.inferredFrom);
        handler.addEscapeAttribute("inferredTo");
        PersistenceManager.getManager().setActiveMySQLAdaptor(sourceDBA);
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        for (GKInstance pathway : pathways) {
            handler.checkOutEvent(pathway, fileAdaptor);
        }
        // Make sure all ReferenceGeneProducts should not be shell
//        checkOutFully(sourceDBA,
//                      prePredictMaxDBID,
//                      fileAdaptor,
//                      ReactomeJavaConstants.ReferenceGeneProduct);
//        checkOutFully(sourceDBA, 
//                      null, 
//                      fileAdaptor, 
//                      ReactomeJavaConstants.ReferenceDatabase);
//        checkOutFully(sourceDBA, 
//                      null, 
//                      fileAdaptor, 
//                      ReactomeJavaConstants.ModifiedResidue);
        // Make sure all new instances should be checked out fully
        Collection<?> all = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        int preSize = all.size();
        while (true) {
            checkOutFully(sourceDBA, 
                          prePredictMaxDBID, 
                          fileAdaptor, 
                          ReactomeJavaConstants.DatabaseObject);
            all = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
            if (all.size() == preSize)
                break;
            preSize = all.size();
        }
        // Switch UniProt to Locus IDs
//        switchUniProtToLocus(referenceGeneProducts,
//                             targetDBA,
//                             fileAdaptor);
        return fileAdaptor;
    }

    private void checkOutFully(MySQLAdaptor sourceDBA,
                               Long prePredictMaxDBID,
                               XMLFileAdaptor fileAdaptor,
                               String clsName) throws Exception {
        Collection<?> referenceGeneProducts = fileAdaptor.fetchInstancesByClass(clsName);
        if (prePredictMaxDBID != null) {
            for (Iterator<?> it = referenceGeneProducts.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                if (inst.isShell() && inst.getDBID() > prePredictMaxDBID) {
                    GKInstance dbInst = sourceDBA.fetchInstance(inst.getDBID());
//                    System.out.println(dbInst);
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
            // Make sure it is from Arabidopsis thaliana
            GKInstance species = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.species);
            if (!species.getDisplayName().equals("Arabidopsis thaliana"))
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
                                                  "rice_reactome_v3",
                                                  "root",
                                                  "macmysql01");
        String fileName = DIR_NAME + "RiceListOfConvertGOCompartments.txt";
        Long defaultPersonId = 1055071L; // Rice Reactome for Guanming Wu
        GKInstance defaultPerson = targetDba.fetchInstance(defaultPersonId);
        switchClassTypesForCompartments(targetDba,
                                        fileName,
                                        defaultPerson);
        // Get predicted pathways from a release database
        MySQLAdaptor releaseDba = new MySQLAdaptor("localhost", 
                                                   "gk_current_ver34", 
                                                   "root", 
                                                   "macmysql01");
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
    public void generateProjectForPredictedPathways() throws Exception {
        String pathwayListFileName = DIR_NAME + "AraReactomePredictedPathwayList.txt";
        MySQLAdaptor sourceDBA = new MySQLAdaptor("localhost", 
                                                  "gk_current_ver37",
                                                  "root", 
                                                  "macmysql01");
        // The following DB_ID is searched from the release database directly.
        // The first InstanceEdit instance created by Orthologous sccript
        Long prePredictMaxDBID = 1362508L; 
        XMLFileAdaptor fileAdaptor = _grepPredictedPathways(pathwayListFileName,
                                                            sourceDBA, 
                                                            prePredictMaxDBID);
        fileAdaptor.save(DIR_NAME + "AraReactomePredictedPathways.rtpj");
    }
    
    @Test
    public void dumpPredictedPathways() throws Exception {
        String srcName = DIR_NAME + "AraReactomePredictedPathways.rtpj";
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor(srcName);
        MySQLAdaptor targetDBA = new MySQLAdaptor("localhost",
                                                  "gk_central",
                                                  "authortool",
                                                  "T001test");
        Long prePredictMaxDBID = 1362508L; 
        Long defaultPersonId = 1385632L; // Preece, J in AraCyc
        GKInstance defaultPerson = targetDBA.fetchInstance(defaultPersonId);
        PersistenceManager.getManager().setActiveMySQLAdaptor(targetDBA);
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        dumpPredictedProject(targetDBA, 
                             defaultPerson, 
                             prePredictMaxDBID,
                             fileAdaptor);
    }
    
    @Test
    public void fixExtractedProjectFromAraReactome() throws Exception {
        String srcName = DIR_NAME + "AraReactomePathways_CurrentSchema.rtpj";
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor(srcName);
        MySQLAdaptor targetDBA = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                                  "gk_central",
                                                  "authortool",
                                                  "T001test");
        Long prePredictMaxDBID = 1362508L; 
        Long defaultPersonId = 1385632L; // Preece, J in AraCyc
        GKInstance defaultPerson = targetDBA.fetchInstance(defaultPersonId);
        PersistenceManager.getManager().setActiveMySQLAdaptor(targetDBA);
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        // Make sure the following class instances not duplicated
        String[] classNames = new String[] {
                ReactomeJavaConstants.Affiliation,
                ReactomeJavaConstants.ReferenceDatabase,
                ReactomeJavaConstants.DatabaseIdentifier,
                ReactomeJavaConstants.GO_CellularComponent,
                ReactomeJavaConstants.GO_MolecularFunction,
                ReactomeJavaConstants.ReferenceEntity,
                ReactomeJavaConstants.Taxon,
                // Because a BiologicalProcess (GO:0006351) has been used as 
                // a component of MolecularFunction (GO:0003899), need to manually
                // update this class.
                // However, it works if this class is placed at the end of the file.
                // But with two Instances created. So manual editing is required.
                ReactomeJavaConstants.GO_BiologicalProcess,
        };
        for (String clsName : classNames) {
            PostProcessHelper.useDBInstances(clsName,
                                             targetDBA, 
                                             fileAdaptor);
        }
        fileAdaptor.save(DIR_NAME + "AraReactomePathways_CurrentSchema_matched.rtpj");
        // Do manual editing in the curator tool so that any obviously duplicated GO related instances
        // can be fixed.
    }
    
    /**
     * Dumps the ReferenceMolecule contents of a converted .rtpj file to another tab file.
     */
    @Test
    public void dumpReferenceMolecules() throws Exception {
    	String rtpjSourceDir = "/home/preecej/Documents/projects/plant_reactome/aracyc_to_reactome_conversion/aracyc_data/aracyc_v11/";
    	String refDestinationDir = "/home/preecej/Documents/projects/plant_reactome/reactome_to_chebi_mapping/AraCyc/aracyc_11/";
    	XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
    	fileAdaptor.setSource(rtpjSourceDir + "aracyc_v11_biopax-level2.rtpj.5.part_1");
        Collection<?> c = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceMolecule);
        StringBuilder builder = new StringBuilder();
        builder.append("ReactomeID\tCompound_Name\tCAS\tLIGAND\tAraCyc");
        String outFileName = refDestinationDir + "AraReferenceMolecules.txt";
        //String outFileName = DIR_NAME + "AraReferenceMolecules.txt";
        FileUtility fu = new FileUtility();
        fu.setOutput(outFileName);
        fu.printLine(builder.toString());
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
                // remove "[unknown:unknown]" from molecule display name; trim whitespace
                builder.append(inst.getDBID()).append("\t").append(inst.getDisplayName().replaceAll("unknown:unknown","").replace('[',' ').replace(']',' ').trim());
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
                // Search for Ligand
                isFound = false;
                builder.append("\t");
                for (Iterator<?> it1 = list.iterator(); it1.hasNext();) {
                    GKInstance crossRef = (GKInstance) it1.next();
                    String name = crossRef.getDisplayName();
                    if (name.startsWith("LIGAND") || name.startsWith("LIGAND-compound") || name.startsWith("KEGG")) {
                        isFound = true;
                        int index = name.indexOf(":");
                        builder.append(name.substring(index + 1));
                        break;
                    }
                }
                if (!isFound)
                    builder.append("-");
                // NOTE: Not needed. Cyc ID's not stored in ChEBI OBO file
                isFound = false;
                builder.append("\t");
                for (Iterator<?> it1 = list.iterator(); it1.hasNext();) {
                    GKInstance crossRef = (GKInstance) it1.next();
                    String name = crossRef.getDisplayName();
                    if (name.startsWith("AraCyc")) {
                        isFound = true;
                        int index = name.indexOf(":");
                        builder.append(name.substring(index + 1));
                        break;
                    }
                }
                if (!isFound)
                    builder.append("-");
                fu.printLine(builder.toString());
                builder.setLength(0);
            }
            else if (!refDb.getDisplayName().equals("ChEBI")) {
                System.out.println(inst + " has a non-ChEBI reference identifier!");
            }
        }
        fu.close();
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
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        PersistenceManager.getManager().setActiveMySQLAdaptor(dba);
    	FileUtility fu = new FileUtility();
        String fileName = "resources/Ara_ReferenceNameToChEBIId.txt";
        Map<String, String> nameToChEBIId = fu.importMap(fileName);
        Collection<?> c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule,
                                                               ReactomeJavaConstants.identifier,
                                                               "IS NULL", 
                                                               "");
        int count = 0;
        ChEBIAttributeAutoFiller chebiHelper = new ChEBIAttributeAutoFiller();
        chebiHelper.setPersistenceAdaptor(fileAdaptor);
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            // Strip "[unknown:unknown]" from name to ensure a possible match and identify the correct chebi term.
            // NOTE: This next line acts as a "sieve" and reduces the number of successful chebi mappings by trying to 
            // identify a chebi object on the basis of its name. 
            // To avoid this problem in the future, enhance dumpReferenceMolecules() and this current method to work with all
            // available mapping cross-references when attempting to instantiate reactome chebi molecule instances.
            String displayName = inst.getDisplayName().replaceAll("unknown:unknown","").replace('[',' ').replace(']',' ').trim();
            String chebiId = nameToChEBIId.get(displayName);
            InstanceDisplayNameGenerator.setDisplayName(inst); // Just to remove empty space
        	System.out.println("Attempting to find ChEBI object from ChEBI ID " + chebiId + " (Ref Molecule:" + displayName + ")...");

        	if (chebiId == null) {
            	// Cannot find a map; let's check the xrefs for a ChEBI id and use it if present
            	System.out.println("No ChEBI mapping; attempting to find ChEBI object from xref entry...");
                List<?> list = inst.getAttributeValuesList(ReactomeJavaConstants.crossReference);
	            // remove "[unknown:unknown]" from molecule display name; trim whitespace
	            for (Iterator<?> it1 = list.iterator(); it1.hasNext();) {
	                GKInstance crossRef = (GKInstance) it1.next();
	                String name = crossRef.getDisplayName();
	                if (name.startsWith("ChEBI")) {
	                	System.out.println("...found!");
	                    int index = name.indexOf(":");
	                    chebiId = name.substring(index + 1);
	                    // remove ChEBI id from xrefs
	                    list.remove(crossRef);
	                    inst.setAttributeValue(ReactomeJavaConstants.crossReference, list);
	                    break;
	                }
	            }
	            if (chebiId == null) // still null?
                	continue; // ok, there really is no ChEBI id
            }
            
        	// otherwise, continue setting identifier w/ proper ChEBI object
            Collection<?> c1 = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule,
                                                           ReactomeJavaConstants.identifier,
                                                           "=",
                                                           chebiId);
            if (c1 == null || c1.size() == 0) {
            	System.out.println("Cannot instantiate ChEBI object from " + inst + " (" + displayName + "); asking EBI...");
            	//System.err.println("Cannot find chebi: " + inst);
                // Need to fetch the detailed information from ChEBI
                inst.setAttributeValue(ReactomeJavaConstants.identifier, chebiId);
                // Want to remove reference to AraCyc - No, we don't! Remove only the ChEBI entry when it gets "promoted" to the main identifier. - JP 06.10.13
                //inst.setAttributeValue(ReactomeJavaConstants.crossReference, null);
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
        	System.out.println("Ref Molecule updated: " + inst + " (" + displayName + ") with ChEBI ID " + chebiId);
        }
        System.out.println("Cannot be mapped: " + count);
    }
    
    @Test
    public void generateDisplayNameToChEBIMap() throws Exception {
        Map<String, String> nameToChEBIId = mapReferenceMoleculeToChEBI();
        String fileName = "resources/Ara_ReferenceNameToChEBIId.txt";
        FileUtility fu = new FileUtility();
        fu.exportMap(nameToChEBIId, fileName);
    }
    
    /**
     * This method is used to map to ChEBIs
     * @throws Exception
     */
    private Map<String, String> mapReferenceMoleculeToChEBI() throws Exception {
        Map<Long, Set<String>> dbIdToChEBI = new HashMap<Long, Set<String>>();
        String fileName = "resources/1.2_reactome_chebi_mapping_complete_sorted.txt";
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
    	String rtpjSourceDir = "/home/preecej/Documents/projects/plant_reactome/aracyc_to_reactome_conversion/aracyc_data/aracyc_v11/";
    	XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
    	fileAdaptor.setSource(rtpjSourceDir + "aracyc_v11_biopax-level2.rtpj.5.part_1");

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
            	/* Need to find a single map as the following rules:
	                1). If there is a one-to-one mapping, use that mapping.
	                2). If there are multiple mappings, map as the following order:
	                    i> If there is LIGAND identifier, it is mapped to ChEBI by one-to-one, use this mapping.
	                    ii> If there are more than one ChEBI from Ligand mapping, check the Reactome database. Pick one in the database. If nothing is found in the database, generate an error.
	                    iii>. if no ligand mapping appeared, try CAS mapping. If one to one mapping can be found, use it.
	                    iv> For more than one CAS mapping. Do as in ii>.
	                     v>. Try to pick what in the database.
            	*/
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
                    String id = fetchChEBIInFile(neededMaps, fileAdaptor);
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
                    String id = fetchChEBIInFile(neededMaps, fileAdaptor);
                    if (id !=  null) {
                        idToSingleMap.put(dbId, id);
                        continue;
                    }
                }
                // Try to fetch directly with ids
                String id = fetchChEBIInFile(ids, fileAdaptor);
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
            GKInstance inst = fileAdaptor.fetchInstance(id);
            String name = inst.getDisplayName().trim();
            if (nameToIds.keySet().contains(name)) {
                System.out.println(inst + " has duplicated name!");
            }
            String chebiId = idToSingleMap.get(id);
            // remove "[unknown:unknown]" from molecule display name; trim whitespace
            InteractionUtilities.addElementToSet(nameToIds, 
                                                 inst.getDisplayName().replaceAll("unknown:unknown","").replace('[',' ').replace(']',' ').trim(),
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

    private String fetchChEBIInFile(List<ReferenceMoleculeMap> maps,
            XMLFileAdaptor fileAdaptor) throws Exception {
    	int index = 0;
    	for (ReferenceMoleculeMap map : maps) {
    		index = map.chEBIId.indexOf(":");
    		String id = map.chEBIId.substring(index + 1);
    		Collection<?> c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule,
                                  ReactomeJavaConstants.identifier,
                                  "=",
                                  id);
    		if (c != null && c.size() > 0) {
    			return id; // Just pick one
    		}
    	}
    	return null;
    }

    private String fetchChEBIInFile(Set<String> ids,
    		XMLFileAdaptor fileAdaptor) throws Exception {
    	int index = 0;
    	for (String id : ids) {
    		index = id.indexOf(":");
    		id = id.substring(index + 1);
    		Collection<?> c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule,
                                  ReactomeJavaConstants.identifier,
                                  "=",
                                  id);
    		if (c != null && c.size() > 0) {
    			return id; // Just pick one
    		}
    	}
    	return null;
    }

    public void updateRegulationDisplayNames(XMLFileAdaptor fileAdaptor) throws Exception {
		/*
		 * first reset display names for PE's, CA's, and Events (all upstream to Regulation and may therefore effect 
		 * the setting of the Regulation display name )
		 */
    	// PE
    	Collection<?> pes = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.PhysicalEntity);
        if (pes == null || pes.size() == 0)
            return;
        for (Iterator<?> it = pes.iterator(); it.hasNext();) {
            GKInstance pe = (GKInstance) it.next();
            InstanceDisplayNameGenerator.setDisplayName(pe);
        }      
    	// CA
    	Collection<?> cas = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.CatalystActivity);
        if (cas == null || cas.size() == 0)
            return;
        for (Iterator<?> it = cas.iterator(); it.hasNext();) {
            GKInstance ca = (GKInstance) it.next();
            InstanceDisplayNameGenerator.setDisplayName(ca);
        }
        // Events
        Collection<?> events = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Event);
        if (events == null || events.size() == 0)
            return;
        for (Iterator<?> it = events.iterator(); it.hasNext();) {
            GKInstance event = (GKInstance) it.next();
            InstanceDisplayNameGenerator.setDisplayName(event);
        }
        // Regulation
		Collection<?> c = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Regulation);
        if (c == null || c.size() == 0)
            return;
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
    		//commit displayName to reactome instance
            InstanceDisplayNameGenerator.setDisplayName(inst);
            logger.info("Updated Regulation display name to " + inst.getDisplayName());
        }
    }

    /**
     * Change the species attribute in Complex objects from "Arabidopsis thaliana col" to "Arabidopsis thaliana"
     * so that it matches all other species attributes in the data set. Then remove the species instance "Arabidopsis
     * thaliana".
     * @throws Exception
     */
    public void updateComplexes(XMLFileAdaptor fileAdaptor) throws Exception {
    	
    	String incorrect_species_name = "Arabidopsis thaliana col";
    	String correct_species_name = "Arabidopsis thaliana";
    	GKInstance speciesToDelete = new GKInstance();
    	GKInstance speciesToKeep = new GKInstance();
    	
    	// gather both Species instances
    	Collection<?> sps = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Species);
        if (sps == null || sps.size() == 0)
            return;
        for (Iterator<?> it = sps.iterator(); it.hasNext();) {
            GKInstance sp = (GKInstance) it.next();
            if (sp.getDisplayName().equals(incorrect_species_name))
            	speciesToDelete = sp;
            else if (sp.getDisplayName().equals(correct_species_name)) {
            	speciesToKeep = sp;
            }
        }

        // get all Complex instances
    	Collection<?> cpxs = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Complex);
        if (cpxs == null || cpxs.size() == 0)
            return;
        for (Iterator<?> it = cpxs.iterator(); it.hasNext();) {
            GKInstance cpx = (GKInstance) it.next();
            if (((GKInstance)cpx.getAttributeValue("species")).getDisplayName().equals(incorrect_species_name)) {
	            // update species attribute
            	cpx.setAttributeValue("species", speciesToKeep);
	            logger.info("Updated the species attribute of Complex " + cpx.getDisplayName() + " to '" + ((GKInstance)cpx.getAttributeValue("species")).getDisplayName() + "'");
            }
        }
 
        // remove the species instance 'Arabidopsis thaliana col'
        fileAdaptor.deleteInstance(speciesToDelete);
        logger.info("Removed Species instance '" + incorrect_species_name + "'");
    }
    
    /**
     * Correct swapped surnames & initials, remove periods and dashes from initials, truncate to single-char initials 
     * from AraCyc firstnames as appropriate
     * @throws Exception
     */
    public void fixPersonNames(XMLFileAdaptor fileAdaptor) throws Exception {

    	Collection<?> c = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Person);
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance person = (GKInstance) it.next();
            String initial = (String) person.getAttributeValue(ReactomeJavaConstants.initial);
            if (initial == null || initial.length() < 3)
                continue;
            StringBuilder sb = new StringBuilder();

            // detect reversed surname and initials; flip them
            if ((((String)(person.getAttributeValue(ReactomeJavaConstants.surname))).indexOf('.') >= 0)) {
            	String tmpOrigInitial = (String)person.getAttributeValue(ReactomeJavaConstants.surname);
            	person.setAttributeValue(ReactomeJavaConstants.surname, initial);
            	initial = tmpOrigInitial;
                sb.append("Swapped surname/initial: " + person.getAttributeValue(ReactomeJavaConstants.surname) + "/" + initial + " | ");
            }
            // strip any "<br>" html tags from intitials as well
            initial = replaceAll(initial, "<br>", "");

            sb.append("Surname: " + person.getAttributeValue(ReactomeJavaConstants.surname) + " | ");
            sb.append("Original Initial: " + initial + " | ");

            if (initial.indexOf('.') < 0 && initial.indexOf('-') < 0 && initial.length() > 3)
        		initial = initial.substring(0, 1).toUpperCase();
        	else {
        		// note: the "initial" field in the Person class (Reactome db) is varchar(10) 
            	initial = initial.substring(0, (initial.length() > 10 ? 10 : initial.length()));
                // strip periods and dashes from initials
                initial = replaceAll(initial, ".", "");
            	initial = replaceAll(initial, "-", "");
        	}

            person.setAttributeValue(ReactomeJavaConstants.initial, initial);
            sb.append("Adjusted Initial: " + person.getAttributeValue(ReactomeJavaConstants.initial));
            InstanceDisplayNameGenerator.setDisplayName(person);
            logger.info("Adjusted Person Name: " + sb.toString());
        }
        mergePersons(c, fileAdaptor); // merge duplicate Persons
    }

    /**
     * merge duplicate Persons on the basis of instance equivalence
     * @throws Exception
     */
    private void mergePersons(Collection ps, XMLFileAdaptor fileAdaptor) throws Exception {
		List<GKInstance> list = new ArrayList(ps);
		List<GKInstance> removed = new ArrayList<GKInstance>();
		for (int i = 0; i < list.size() - 1; i++) {
			GKInstance inst1 = (GKInstance) list.get(i);
			for (int j = i + 1; j < list.size(); j++) {
				GKInstance inst2 = (GKInstance) list.get(j);
				if (!(inst1.isShell()) && (InstanceUtilities.areMergable(inst1, inst2))) {
					logger.info("Merging Persons " + inst1 + " and " + inst2);
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

    /*
     * Local replaceAll function to properly handle simple string replacements w/o regex
     */
    private static String replaceAll(String source,String toReplace,String replacement) {
		int idx = source.lastIndexOf( toReplace );
		if ( idx != -1 ) {
			StringBuffer ret = new StringBuffer( source );
			ret.replace( idx, idx+toReplace.length(), replacement );
			while( (idx=source.lastIndexOf(toReplace, idx-1)) != -1 ) {
				ret.replace( idx, idx+toReplace.length(), replacement );
			}
			source = ret.toString();
		}
		return source;
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
                                                       170905L); // Arabidopsis thaliana
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

    /*
     * Generates two files (one pretty, one tab) of ReactionIDs without meaningful displayNames (and their inputs/outputs).
     * Will be used to generate statistics (with an external Perl script) on common reaction outputs 
     * and hopefully, a set of rules on which to name future Reactions when only a chemical reaction description
     * is available.
     * - Justin Preece, OSU 3/9/11
     */
    @Test
    public void dumpUnnamedReactionEntities() throws Exception {
    	// open and parse .rtpj file
    	String dir = "/home/preecej/Documents/projects/reactome/aracyc_to_reactome_conversion/aracyc_data/";

    	XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
    	fileAdaptor.setSource(dir + "aracyc_v8_0_biopax-level2.rtpj");

    	// set up output files
        StringBuilder builder = new StringBuilder();
        builder.append("[Unnamed Cyc Reactions - Arabidopsis thaliana]");
        StringBuilder builderTab = new StringBuilder();
        builderTab.append("[Unnamed Cyc Reactions - Arabidopsis thaliana]");

        String outFileName = dir + "aracyc_v8_0_reaction_name_stats.txt";
        String outFileNameTab = dir + "aracyc_v8_0_reaction_name_stats_tab.txt";

        FileUtility fu = new FileUtility();
        fu.setOutput(outFileName);
        FileUtility fut = new FileUtility();
        fut.setOutput(outFileNameTab);

    	// pull out all Reaction objects with assembled _displayNames (includes '->')
        Collection<?> c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Reaction,
                ReactomeJavaConstants._displayName,
                "LIKE", 
                "%->%");

        // provide a total count of Reaction objects
        if (c != null && c.size() > 0) {
            builder.append(": " + c.size());
            builderTab.append(": " + c.size());
        }
        fu.printLine(builder.toString());
        builder.setLength(0);
        fut.printLine(builderTab.toString());
        builderTab.setLength(0);

        // create column headers for tab file
        builderTab.append("Reaction\tCross-Reference\tCatalyst\tInputs\tOutputs");      
        fut.printLine(builderTab.toString());
        builderTab.setLength(0);
        
    	// iterate, gather attributes and stats, build output file
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            
        	// get Reaction._displayName, place in header line (or first column, for pretty file)
            builder.append("RXN: " + inst.getDisplayName() + "\n");
            builderTab.append(inst.getDisplayName() + "\t");

            // get crossRef, catalyst
            GKInstance xRef = (GKInstance)inst.getAttributeValue(ReactomeJavaConstants.crossReference);
            GKInstance catalystActivity = (GKInstance)inst.getAttributeValue(ReactomeJavaConstants.catalystActivity);            

            // build pipe-del lists of inputs, outputs
            List<GKInstance> lInputs = inst.getAttributeValuesList(ReactomeJavaConstants.input);
            String inputs = "";
            if (lInputs != null && lInputs.size() > 0) {
	            for (Iterator<?> itIn = lInputs.iterator(); itIn.hasNext();) {
	            	GKInstance instInput = (GKInstance)itIn.next();
	            	inputs += instInput.getDisplayName() + (itIn.hasNext() ? "|" : "");
	            }
            }
            List<GKInstance> lOutputs = inst.getAttributeValuesList(ReactomeJavaConstants.output);
            String outputs = "";
            if (lOutputs != null && lOutputs.size() > 0) {
	            for (Iterator<?> itOut = lOutputs.iterator(); itOut.hasNext();) {
	            	GKInstance instOutput = (GKInstance)itOut.next();
	            	outputs += instOutput.getDisplayName() + (itOut.hasNext() ? "|" : "");
	            }
            }

            // ...place in following 4 lines of pretty file
            builder.append("   XREF:\t"		 + ((xRef != null) ? xRef.getDisplayName() : "null") + "\n");
            builder.append("   CATALYST:\t"	 + ((catalystActivity != null) ? catalystActivity.getDisplayName() : "null") + "\n");
            builder.append("   INPUTS:\t"	 + (inputs != "" ? inputs : "null") + "\n");
            builder.append("   OUTPUTS:\t"	 + (outputs != "" ? outputs : "null"));

            // ...and also place in following 4 columns of tab file
            builderTab.append(((xRef != null) ? xRef.getDisplayName() : "null") + "\t");
            builderTab.append(((catalystActivity != null) ? catalystActivity.getDisplayName() : "null") + "\t");
            builderTab.append((inputs != "" ? inputs : "null") + "\t");
            builderTab.append((outputs != "" ? outputs : "null"));
            
            fu.printLine(builder.toString()); // write to pretty file
            builder.setLength(0);
            fut.printLine(builderTab.toString()); // write to tab file
            builderTab.setLength(0);
        }
        fu.close();
        fut.close();
    }
    
    @Test
    public void fixReferenceGenesAfterDump() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("floret.cgrb.oregonstate.edu",
                                            "gk_central_ath_dump_testing",
                                            "reactome_aracyc", 
                                            "r3actom3_aracyc");
        // Get AraCyc database
        Collection<?> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase,
                                                       ReactomeJavaConstants._displayName,
                                                       "=",
                                                       "AraCyc");
        GKInstance araCyc = (GKInstance) c.iterator().next();
        System.out.println("AraCyc: " + araCyc);
        c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct,
                                                       ReactomeJavaConstants._displayName,
                                                       "LIKE",
                                                       "AT%");
        System.out.println("Total instances: " +  c.size());
        dba.loadInstanceAttributeValues(c, new String[]{ReactomeJavaConstants.identifier, 
                                                        ReactomeJavaConstants.referenceDatabase});
        try {
            dba.startTransaction();
            for (Iterator<?> it = c.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                inst.setAttributeValue(ReactomeJavaConstants.referenceDatabase,
                                       araCyc);
                String identifier = (String) inst.getAttributeValue(ReactomeJavaConstants.identifier);
                inst.setDisplayName(araCyc.getDisplayName() + ":" + identifier);
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
    
    /**
     * This method is used to generate a local rtpj project based on a list of pathway ids
     * in a local file. The Arabidoposis Reactome was downloaded from their web site, and still
     * used an old schema. So an old schema should be used based on this database.
     * @throws Exception
     */
    @Test
    public void generateProjectFromAraReactome() throws Exception {
        // Get a list of DB_IDs
        String fileName = DIR_NAME + "AraReactomePathwayList.txt";
        List<Long> dbIds = loadPathwayIds(fileName);
        // Link to the database
        MySQLAdaptor sourceDBA = new MySQLAdaptor("localhost",
                                               "ara_reactome",
                                               "root",
                                               "macmysql01");
        // Note: the above database has been changed from the original downloaded mysqldump from
        // http://www.arabidopsisreactome.org/download/index.html. The content in table Event_2_orthologousEvent
        // have been emptied to avoid pulling out any thing from non-arabidopsis species.
        
        // Used to check out
     // Use an XMLFileAdaptor to hold checked out instances temporarily
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        EventCheckOutHandler handler = new EventCheckOutHandler();
        PersistenceManager.getManager().setActiveMySQLAdaptor(sourceDBA);
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        Set<GKInstance> pathways = new HashSet<GKInstance>();
        for (Long dbId : dbIds) {
            GKInstance pathway = sourceDBA.fetchInstance(dbId);
            pathways.add(pathway);
        }
        handler.checkOutEvents(pathways, fileAdaptor);
//        // Need to clean up and remove all events not in arabidopsis
//        GKInstance arabidopsis = fileAdaptor.fetchInstance(5L);
//        Collection<?> events = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Event);
//        for (Object obj : events) {
//            GKInstance event = (GKInstance) obj;
//            List<?> species = event.getAttributeValuesList(ReactomeJavaConstants.species);
//            if (species == null || species.size() == 0 || species.contains(arabidopsis))
//                continue; 
//            // Delete CAS instances
//            if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.catalystActivity)) {
//                GKInstance cas = (GKInstance) event.getAttributeValue(ReactomeJavaConstants.catalystActivity);
//                if (cas != null)
//                    fileAdaptor.deleteInstance(cas);
//            }
//            fileAdaptor.deleteInstance(event);
//        }
        // Only one species is used
        Collection<?> species = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Species);
        for (Object obj : species) {
            GKInstance speciesInst = (GKInstance) obj;
            List<?> referrers = fileAdaptor.getReferers(speciesInst);
            if (referrers.size() == 0)
                fileAdaptor.deleteInstance(speciesInst);
        }
        // The following loop is to make sure there is no shell instances exist
        Collection<?> all = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        int preSize = all.size();
        while (true) {
            checkOutFully(sourceDBA, null, fileAdaptor, ReactomeJavaConstants.DatabaseObject);
            all = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
            if (preSize == all.size()) // Nothing has been changed
                break;
            preSize = all.size();
        }
        // Flip all DB_IDs to negative to avoid any confusing with gk_central
        all = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        for (Object obj : all) {
            GKInstance inst = (GKInstance) obj;
            Long dbId = inst.getDBID();
            inst.setDBID(-dbId);
        }
        fileAdaptor.save(DIR_NAME + "AraReactomePathways.rtpj");
    }

    protected List<Long> loadPathwayIds(String fileName) throws IOException {
        List<Long> dbIds = new ArrayList<Long>();
        FileUtility fu = new FileUtility();
        fu.setInput(fileName);
        String line = null;
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            dbIds.add(new Long(tokens[0]));
        }
        fu.close();
        return dbIds;
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
    
    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configure("resources/log4j.properties");
        String method = args[0];
        AraCycPostProcessor processor = new AraCycPostProcessor();
        try {
            if (method.equals("dumpToDatabase")) 
                processor.dumpToDatabase();
            else if (method.equals("dumpPredictedPathways"))
                processor.dumpPredictedPathways();
            else if (method.equals("runPostProcess"))
            	processor.runPostProcess();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        logger.info("AraCycPostProcessor complete.");
    }
       
    private class ReferenceMoleculeMap {
        Long dbId;
        String displayName;
        String mapMethod;
        String chEBIId;
    }
    
}
