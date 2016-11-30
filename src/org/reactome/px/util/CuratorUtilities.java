/**
 * 
 */
package org.reactome.px.util;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.gk.database.DefaultInstanceEditHelper;
import org.gk.database.util.ReferencePeptideSequenceAutoFiller;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import com.hp.hpl.jena.query.junit.QueryTest;
import com.sun.xml.internal.fastinfoset.util.StringArray;

/**
 * @author preecej
 * Contains common utilities that manipulate data en masse for curators. It is intended to be a library of methods 
 * used to directly update the database where manual curation is not desirable or efficient.
 */
public class CuratorUtilities
{
    private static final Logger logger = Logger.getLogger(CuratorUtilities.class);
    private List<GKInstance> target_taxa = new ArrayList<GKInstance>(); 
	private Map<String,String> NCBI_map = new HashMap(); 
	private GKInstance defaultPerson = new GKInstance(); 
    private List<GKInstance> changedInsts = new ArrayList<GKInstance>();
    private GKInstance target_instance_edit = new GKInstance();
    private List<GKInstance> target_instances = new ArrayList<GKInstance>();
    private String map_path = new String();
    private String rtpjName = new String();
    private String designatedSpecies = new String();
	public MySQLAdaptor dbAdaptor;
	public XMLFileAdaptor fileAdaptor;
	public PersistenceAdaptor uniAdaptor;
	private Map<SchemaClass, List<GKInstance>> clsMap; // list of class instances

	/**
	 * Estabish a database connection via adaptor.
	 * @param 
	 */
    private MySQLAdaptor getDBA(Element db_elm) throws Exception
    {
    	MySQLAdaptor dba = new MySQLAdaptor(
    			db_elm.getAttributeValue("host"),
    			db_elm.getAttributeValue("name"),
    			db_elm.getAttributeValue("user"),
    			db_elm.getAttributeValue("pwd"),
    			Integer.parseInt(db_elm.getAttributeValue("port")));
    	return dba;
    }
	
	/**
     * Get the configuration values from a config file, typically located in the resources directory. Use to load
     * establish db adaptor and species list.
     * @param configFilePath
     * @throws Exception
	 */
    @SuppressWarnings("unchecked")
	private void loadConfigs(String configFilePath) throws Exception
    {
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new File(configFilePath));

        // open db source
		if (XPath.selectSingleNode(document,"/utility_settings/db_connection_info") != null) { 
			Element db_info = (Element) XPath.selectSingleNode(document,"/utility_settings/db_connection_info");
			this.dbAdaptor = getDBA(db_info);
            logger.info("Database adaptor set to host: " + db_info.getAttributeValue("host")
            	+ ", database: " + db_info.getAttributeValue("name")); // TEST   
		} else {
			logger.info("No database connection credentials provided. If you want to connect to a Reactome database, please check settings in resources/CuratorUtilities.xml");
		}
		
		// open file data source
        if (XPath.selectSingleNode(document,"/utility_settings/data_file") != null) {
			Element file_info = (Element) XPath.selectSingleNode(document,"/utility_settings/data_file");
			this.rtpjName = file_info.getAttributeValue("path") + file_info.getAttributeValue("name");
			this.fileAdaptor = new XMLFileAdaptor();
			fileAdaptor.setSource(this.rtpjName);
            logger.info("File adaptor set to: " + rtpjName); // TEST   
		} else {
			logger.info("No project file path provided. If you want to connect to a Reactome project file (.rptj), please check settings in resources/CuratorUtilities.xml");
		}

        // set universal adaptor (allows code to more generically reference one data source if that is all that is required)
		if (dbAdaptor != null) {
			uniAdaptor = dbAdaptor;
            logger.info("Universal adaptor set to database adaptor.");
		}
		else if (fileAdaptor != null) {
			uniAdaptor = fileAdaptor;
            logger.info("Universal adaptor set to file adaptor.");
		} else {
			throw new Exception("No data adaptor set!");
		}
		
        // get target InstanceEdit, if supplied
		if (XPath.selectSingleNode(document,"/utility_settings/target_instance_edit/@id") != null) {
	        Long instance_id = Long.parseLong(((Attribute) XPath.selectSingleNode(document,"/utility_settings/target_instance_edit/@id")).getValue());
	        logger.info("Target Instance Edit ID: " + instance_id); // TEST   
	        if (instance_id != null) {
	        	this.target_instance_edit = (dbAdaptor != null ? dbAdaptor : fileAdaptor).fetchInstance(instance_id);
	            // build list of modified objects
	        	/*
	    		this.target_instances = (List<GKInstance>) this.target_instance_edit.getReferers(ReactomeJavaConstants.modified);
	    		// ...and add any created instances, as well
	    		List<GKInstance> created_instances = (List<GKInstance>) this.target_instance_edit.getReferers(ReactomeJavaConstants.created);
    	    	if (created_instances != null && created_instances.size() > 0)
        	    	if (this.target_instances != null && this.target_instances.size() > 0)
        	    		this.target_instances.addAll(created_instances);
    	    		logger.info("Num of target instances: " + target_instances.size()); // TEST
	    		*/
	        }
		}
        
        // add target database objects, if supplied
		if (XPath.selectSingleNode(document,"/utility_settings/target_database_objects") != null) { 
			List<Element> target_db_elms = (List<Element>) XPath.selectNodes(document,"/utility_settings/target_database_objects//target_database_object");
			Iterator it = target_db_elms.iterator();
			while (it.hasNext())
			{
				Element elm = (Element)it.next();
				Collection<GKInstance> targets = (Collection<GKInstance>) (uniAdaptor).fetchInstanceByAttribute(
						ReactomeJavaConstants.DatabaseObject, 
						ReactomeJavaConstants.DB_ID,
						"=",
						elm.getAttribute("id").getLongValue());

				for (GKInstance curr_target : targets)
				{
					this.target_instances.add(curr_target);
					break;
				}
			}
		}
        
		// get species filter set
		List<Element> species_list = (List<Element>) XPath.selectNodes(document,"/utility_settings/target_taxa//species");
		Iterator its = species_list.iterator();
		while (its.hasNext())
		{
			Element elm = (Element)its.next();
			Collection<GKInstance> species = (Collection<GKInstance>)(uniAdaptor).fetchInstanceByAttribute(
					ReactomeJavaConstants.Species, 
					ReactomeJavaConstants.name,
					"=",
					elm.getValue());
			for (GKInstance curr_species : species)
			{
				if (elm.getAttributeValue("NCBI_id") != null)
					if (!elm.getAttributeValue("NCBI_id").isEmpty())
						this.NCBI_map.put(curr_species.getDisplayName(), elm.getAttributeValue("NCBI_id"));
				this.target_taxa.add(curr_species);
				break;
			}
		}
        //for (GKInstance curr_species : target_taxa) logger.info(curr_species.getDisplayName()); // TEST

		// get the designated species to assign to any changed instances (if provided)
		if (XPath.selectSingleNode(document,"/utility_settings/designated_species") != null) { 
	        this.designatedSpecies = ((Element) XPath.selectSingleNode(document,"/utility_settings/designated_species")).getValue();
		}
		
		// get any mapping file path, if it exists
		if (XPath.selectSingleNode(document,"/utility_settings/mapping_file/@path") != null) { 
	        this.map_path = ((Attribute) XPath.selectSingleNode(document,"/utility_settings/mapping_file/@path")).getValue();
		}
		
		// set default person to configured editor
		Element editor = (Element)XPath.selectSingleNode(document, "/utility_settings/editor");
		this.defaultPerson = (uniAdaptor).fetchInstance(Long.parseLong(editor.getAttributeValue("person_id")));
    }

	/**
     * Commit all changed instances to the database or file. For databases, uses a transaction to preserve db integrity.
     * @throws Exception
	 */
    private void commitChanges() throws Exception
    {
        try {
			logger.info("Commiting/saving changes...");
	        if (changedInsts.size() == 0) {
				logger.info("...no changed instances to commit/save.");
	            return; // Don't need do anything
	        }

        	if (dbAdaptor != null) {
    			logger.info("Commiting changes to database...");
    			
    			// configure InstanceEdit for this set of updates
    	        DefaultInstanceEditHelper ieHelper = new DefaultInstanceEditHelper();
    	        GKInstance instance_edit = ieHelper.createDefaultInstanceEdit(defaultPerson);
    	        instance_edit.addAttributeValue(ReactomeJavaConstants.dateTime, GKApplicationUtilities.getDateTime());
    	        InstanceDisplayNameGenerator.setDisplayName(instance_edit);
    	
    	        // associate changed instances with the InstanceEdit stamp
    	        instance_edit = ieHelper.attachDefaultIEToDBInstances(changedInsts, instance_edit);

    	        // Commit changes
	        	dbAdaptor.startTransaction();
	            dbAdaptor.storeInstance(instance_edit);
	            for (GKInstance inst : changedInsts) {
	            	dbAdaptor.updateInstance(inst);
	            }
	            dbAdaptor.commit();
            } else {
				logger.info("Saving changes to file...");
            	fileAdaptor.save(this.rtpjName);
            }
        }
        catch(Exception e) {
        	if (dbAdaptor != null)
        		dbAdaptor.rollback();
            throw e; // Re-thrown exception
        }
    }

    /**
     * Changes the display name of reactions
     */
    private void renameReactions() throws Exception
    {
    	logger.info("Running renameReactions()...");
    }

    
    /**
     * Retrieve and list Reference Gene Products
     * @Parms
     * 
     */
    @SuppressWarnings("unchecked")
    private void listRiceRGPs(boolean hasUniProt) throws Exception
    {
    	if (hasUniProt) {
	    	logger.info("Collecting RGP identifiers matching refDB UniProt");
	    	
	    	Collection<GKInstance> c = uniAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct, 
	    			ReactomeJavaConstants.referenceDatabase, 
	    			"=", 
	    			2L);  // UniProt
	    	logger.info("raw size: " + c.size());
	    	String refDb = new String();
	    	String uniprot_id = new String();
	    	String gene_id = new String();
	    	int count = 0;
	        for (GKInstance rgp : c) {
	        	
	        	if (rgp.getAttributeValue(ReactomeJavaConstants.referenceDatabase) != null)
	        		refDb = ((GKInstance)rgp.getAttributeValue(ReactomeJavaConstants.referenceDatabase)).getDisplayName();
	        	if (rgp.getAttributeValue(ReactomeJavaConstants.identifier) != null)
	        		uniprot_id = rgp.getAttributeValue(ReactomeJavaConstants.identifier).toString();
	        	if (rgp.getAttributeValuesList(ReactomeJavaConstants.geneName) != null) {
	        		List<String> geneNames = rgp.getAttributeValuesList(ReactomeJavaConstants.geneName);
	    			for (Iterator<String> it = geneNames.iterator(); it.hasNext();) {
	    	            String currName = (String)it.next().toUpperCase();
	    	            // grab the curated rice RGPs with LOC identifiers for mapping (majority)  
	    	            if (currName.startsWith("LOC_")) {
	    	            	gene_id = currName;
	        				break;
	        			}
	    	            // and more recently curated rice RGPs with OS ids and no LOC (minority)   
	        			if (currName.toUpperCase().startsWith("OS") && currName.length() == 12) {
	        				gene_id = currName;
	        			}
	        		}
	        	}
	        	if (gene_id.length() > 0) {
		        	count++;
		        	System.out.println(rgp.getDBID() + "\t" + refDb + ":" + uniprot_id + "\t" + gene_id);
	        	}
	        	gene_id = "";
	        }
	    	logger.info("filtered size: " + count);

/*    		
    		logger.info("Collecting RGP identifiers matching refDB UniProt and geneName includes 'LOC*'...");
	
	    	Collection<GKInstance> c = uniAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct, 
	    			ReactomeJavaConstants.geneName, 
	    			"LIKE", 
	    			"LOC%");
	    	logger.info("size: " + c.size());
	    	String refDb = new String();
	    	String uniprot = new String();
	    	String loc = new String();
	        for (GKInstance rgp : c) {
	        	
	        	if (rgp.getAttributeValue(ReactomeJavaConstants.referenceDatabase) != null)
	        		refDb = ((GKInstance)rgp.getAttributeValue(ReactomeJavaConstants.referenceDatabase)).getDisplayName();
	        	if (rgp.getAttributeValue(ReactomeJavaConstants.identifier) != null)
	        		uniprot = rgp.getAttributeValue(ReactomeJavaConstants.identifier).toString();
	        	if (rgp.getAttributeValuesList(ReactomeJavaConstants.geneName) != null) {
	        		List<String> geneNames = rgp.getAttributeValuesList(ReactomeJavaConstants.geneName);
	    			for (Iterator<String> it = geneNames.iterator(); it.hasNext();) {
	    	            String currName = (String)it.next().toUpperCase();
	        			if (currName.startsWith("LOC_")) {
	        				loc = currName;
	        			}
	        		}
	        	}
	        	System.out.println(rgp.getDBID() + "\t" + refDb + ":" + uniprot + "\t" + loc);
	        }
*/      
    	} else {
	    	logger.info("Collecting RGP instances with designated species or subspecies ('Oryza sativa*') and referenceDatabase != UniProt");

	    	Collection<GKInstance> c = uniAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct, 
	    			ReactomeJavaConstants.species, 
	    			"=", 
	    			186860L);
    		logger.info("total Os RGP size: " + c.size());
        	int count = 0;
        	
        	for (Iterator<GKInstance> it = c.iterator(); it.hasNext();) {
                GKInstance rgp = (GKInstance) it.next();
                if (rgp.getAttributeValue(ReactomeJavaConstants.referenceDatabase) != null) {
		    		if (rgp.getAttributeValue(ReactomeJavaConstants.referenceDatabase).toString() != "UniProt") {
		    			if (rgp.getAttributeValue(ReactomeJavaConstants.identifier) != null) {
				        	String identifier = rgp.getAttributeValue(ReactomeJavaConstants.identifier).toString();
				        	if (identifier.endsWith(".1")) {
				        		System.out.println(rgp.getDBID() + "\t" + identifier);
				        		count++;
				        	} else if (identifier.startsWith("LOC") & identifier.split("\\.").length < 2) {
				        		System.out.println(rgp.getDBID() + "\t" + identifier + ".1");
				        		count++;
				        	} else {
				        		System.out.println(rgp.getDBID() + "\t" + identifier);
				        		count++;
				        	}
		    			}
		    		}
	    		}
        	}
    		logger.info("filtered size: " + count);
    	}
    }

    @SuppressWarnings("unchecked")
    private void listAthRGPs() throws Exception
    {
    	logger.info("Collecting RGP identifiers matching 'AT*'...");
    	try {
	    	//Collection<GKInstance> c = (dbAdaptor != null ? dbAdaptor : fileAdaptor).fetchInstancesByClass(dbAdaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceGeneProduct));
        	String athExp = "AT.G.*";
        	String isoformExpression = "AT.G\\d{5}\\.[1-9]";
        	//iterate RGPs
        	Integer count = 0;
        	Collection<?> RGPs = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
            for (Iterator<?> it = RGPs.iterator(); it.hasNext();) {
                GKInstance curRGP = (GKInstance) it.next();
	        	String RGPid = curRGP.getAttributeValue(ReactomeJavaConstants.identifier).toString();
	        	if (!RGPid.matches(isoformExpression) && (RGPid.matches(athExp) || RGPid.startsWith("MONOMER-AT"))) {
	        		//logger.info(RGPid);
	            	System.out.println(RGPid); // TEST
	                count++;
	        	}
	        }
	    	logger.info("collection size: " + RGPs.size() + ", filtered size: " + count);
    	}
    	catch (Exception e) {
    		logger.info(e.getMessage());
    	}
    }

    @SuppressWarnings("unchecked")
    private void listAthEWASCompartments() throws Exception
    {
    	logger.info("Collecting EWAS referenceEntities matching '.*AT.G.*' and having a Compartment...");
    	//try {
	    	//Collection<GKInstance> c = (dbAdaptor != null ? dbAdaptor : fileAdaptor).fetchInstancesByClass(dbAdaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceGeneProduct));
        	String athExp = ".*AT.*";
        	String refEntityID = "";
        	String compartment = "";
        	//iterate EWASs
        	Integer count = 0;
        	Collection<?> EWASs = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.EntityWithAccessionedSequence);
            for (Iterator<?> it = EWASs.iterator(); it.hasNext();) {
                GKInstance curEWAS = (GKInstance) it.next();
                if (curEWAS.getAttributeValue(ReactomeJavaConstants.referenceEntity).toString() != null)
                	refEntityID = ((GKInstance)curEWAS.getAttributeValue(ReactomeJavaConstants.referenceEntity)).getDisplayName();
	        	if (refEntityID.matches(athExp)) {
	            	if (curEWAS.getAttributeValue(ReactomeJavaConstants.compartment) != null) {
	                	compartment = ((GKInstance)curEWAS.getAttributeValue(ReactomeJavaConstants.compartment)).getDisplayName();
	        			System.out.println(refEntityID + "\t" + compartment); // TEST
	        			count++;
	            	}
	        	}
	        }
	    	logger.info("filtered size: " + count);
    	/*}
    	catch (Exception e) {
    		logger.info(e.getMessage());
    	}*/
    }
    
    @SuppressWarnings("unchecked")
    private void listCompartments() throws Exception
    {
    	//try {
	    	Collection<GKInstance> c = uniAdaptor.fetchInstancesByClass(uniAdaptor.getSchema().getClassByName(ReactomeJavaConstants.Compartment));
	        for (GKInstance comp : c) {
	        	String comp_name = (String)comp.getAttributeValue(ReactomeJavaConstants._displayName);
	            System.out.println(comp_name); // TEST
	        }
	    	logger.info("size: " + c.size());
    	//}
    	//catch (Exception e) {
    	//	logger.info(e.getMessage());
    	//}
    }
    
    @SuppressWarnings({ "unchecked" })
    private void profileAthIsoforms() throws Exception
    {
    	logger.info("Profiling A.th. RGP identifiers for isoform designations");
    	try {
	    	/* iteration 0 - not sure what this was for
    		Collection<GKInstance> c_displayName = (dbAdaptor != null ? dbAdaptor : fileAdaptor)
	    		.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants._displayName, "LIKE", "%.%");
	    	logger.info("displayName size: " + c_displayName.size());
	    	for (GKInstance rgp : c_displayName) {
	        	String RGPid = rgp.getAttributeValue(ReactomeJavaConstants._displayName).toString();
	        	if (RGPid.startsWith("AT"))
	        		//logger.info(RGPid);
	            	System.out.println(RGPid); // TEST
	        }

	    	Collection<GKInstance> c = (dbAdaptor != null ? dbAdaptor : fileAdaptor)
	    		.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.identifier, "LIKE", "AT%");
	    	c.addAll((dbAdaptor != null ? dbAdaptor : fileAdaptor)
	    		.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct, ReactomeJavaConstants.identifier, "LIKE", "AT%"));
			*/
    		
    		/* iteration 1 - just the AT stuff    		
    		System.out.println("identifier\t_displayName\tnames"); // TEST

    		Collection<GKInstance> c = (dbAdaptor != null ? dbAdaptor : fileAdaptor).fetchInstancesByClass(fileAdaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceGeneProduct));
    		for (GKInstance rgp : c) {
	        	String RGP_displayName = rgp.getAttributeValue(ReactomeJavaConstants._displayName).toString();
	        	RGP_displayName = (RGP_displayName.matches("AT.G.*")) ? RGP_displayName : "";
	        	String RGPid = rgp.getAttributeValue(ReactomeJavaConstants.identifier).toString();
	        	RGPid = (RGPid.matches("AT.G.*")) ? RGPid : "";

	        	String RGPnames = rgp.getAttributeValuesList(ReactomeJavaConstants.name).toString();
        		RGPnames = RGPnames.replace('[',' ').replace(']',' ').trim(); 
        		String[] names = RGPnames.split(",");
	        	String newNames = "";
        		for (String name : names) {
    	        	if (name.matches("AT.G.*")) {
    	        		newNames += name;
    	        	}
        		}

	        	System.out.println(RGP_displayName + "\t" + RGPid + "\t" + newNames); // TEST
	        }
	    	logger.info("total size: " + c.size());
	    	*/

    		/*
    		// iteration 2 - only nonAT RGP displayNames
    		System.out.println("identifier\t_displayName"); // TEST

    		Collection<GKInstance> c = (dbAdaptor != null ? dbAdaptor : fileAdaptor).fetchInstancesByClass(fileAdaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceGeneProduct));
    		for (GKInstance rgp : c) {
	        	String RGP_displayName = rgp.getAttributeValue(ReactomeJavaConstants._displayName).toString();
	        	RGP_displayName = (!RGP_displayName.matches("(AT.G.*)")) ? RGP_displayName : "";
	        	if (RGP_displayName.isEmpty())
	        		continue;
	        	String RGPid = rgp.getAttributeValue(ReactomeJavaConstants.identifier).toString();
	        	System.out.println(RGP_displayName + "\t" + RGPid); // TEST
	        }
	    	logger.info("total size: " + c.size());
    		// end iteration 2
    		*/

    		// iteration 3 - only AT RGP displayNames
    		System.out.println("identifier\t_displayName"); // TEST

    		Collection<GKInstance> c = (dbAdaptor != null ? dbAdaptor : fileAdaptor).fetchInstancesByClass(fileAdaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceGeneProduct));
    		for (GKInstance rgp : c) {
	        	String RGP_displayName = rgp.getAttributeValue(ReactomeJavaConstants._displayName).toString();
	        	RGP_displayName = (RGP_displayName.matches("(AT.G.*)")) ? RGP_displayName : "";
	        	if (RGP_displayName.isEmpty())
	        		continue;
	        	String RGPid = rgp.getAttributeValue(ReactomeJavaConstants.identifier).toString();
	        	System.out.println(RGP_displayName + "\t" + RGPid); // TEST
	        }
	    	logger.info("total size: " + c.size());
    		// end iteration 3

    	}
    	catch (Exception e) {
    		logger.info(e.getMessage());
    	}
    }
    
    /**
     * Retrieve and set UniProt data for Reference Gene Products
     */
    @SuppressWarnings("unchecked")
	private void updateRGPsWithUniProtKBData() throws Exception
    {
    	logger.info("Running updateRGPsWithUniProtKBData()...");
    	
    	// read in mapping file with identifiers and Uniprot IDs
        FileUtility fu = new FileUtility();
        String fileName = this.map_path;
        Map<String, String> identifier_to_UniProt = fu.importMap(fileName);

    	logger.info("Number of loci to map: " + identifier_to_UniProt.size());

        /* Prepare to reset the species to the designated species (if provided). This prevents the introduction of 
         * undesirable taxa from UniProt (e.g. "Oryza sativa subs. japonica" instead of the preferred "Oryza sativa")  
         */
    	Collection<GKInstance> speciesColl = fileAdaptor.fetchInstanceByAttribute(
        		ReactomeJavaConstants.Species,
        		ReactomeJavaConstants.name,
        		"=", 
        		this.designatedSpecies);

        // there should only be one species for this identifier; let's make sure
        GKInstance speciesInstance = null;
        if (speciesColl.size() == 1) {
            speciesInstance = speciesColl.iterator().next();
        } else {
        	throw new Exception("Designated species identifier " + this.designatedSpecies + 
    			" does not exist in the database or project file.");
        }
        
        ReferencePeptideSequenceAutoFiller uniprotAutoFiller = new ReferencePeptideSequenceAutoFiller();
        //uniprotAutoFiller.setPersistenceAdaptor((dbAdaptor != null ? dbAdaptor : fileAdaptor));
        uniprotAutoFiller.setPersistenceAdaptor(dbAdaptor);
        
        // iterate over identifiers 
        for (String key : identifier_to_UniProt.keySet()) 
        {
            //System.out.println(entry.getKey() + ": " + entry.getValue()); // TEST
        	key = key.toUpperCase();
            String value = identifier_to_UniProt.get(key.toUpperCase());
        	try {
	        	// cross-ref identifiers to RGPs;
        		// add isomer if needed (required by db search in MySQLDBAdapter; "LIKE" clause only for use by XMLFileAdapter)
	            //Collection<GKInstance> c = (dbAdaptor != null ? dbAdaptor : fileAdaptor).fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct,
	            Collection<GKInstance> c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct,
	            		ReactomeJavaConstants.identifier,
	            		"=",
	            		(this.designatedSpecies == "Oryza sativa") ?
	            				(key.contains(".") ? key.toUpperCase() : key.toUpperCase() + ".1") // rice - isomer check
	            				: key); // ath - no isomer check required
	
	            // there should only be one RGP for this identifier; let's make sure
	            GKInstance rgp = null;
	            if (c.size() == 1) {
	                rgp = c.iterator().next();
	            } else {
		            if (c.size() == 0) {
		            	throw new Exception("RGP identifier " + key + 
		        			" does not have an entry in the database or project file.");
		            } else {
			            if (c.size() > 1) {
			            	throw new Exception("RGP identifier " + key + 
			        			" does not have a unique entry in the database or project file. There are " + c.size() + " entries.");
			            }
		            }
	            }
	            
	            // load all attributes so you don't overwrite; this is not necessary for file sources 
	            //fileAdaptor.loadInstanceAttributes(rgp);
	            //if (dbAdaptor != null)
	            	//dbAdaptor.fastLoadInstanceAttributeValues(rgp);

	            // make sure the current RGP is in your species filter
        		//boolean taxaMember = false;
        		
                /*for (GKInstance curr_species : target_taxa) {
            		logger.info("species filter: " + rgp.getAttributeValue(ReactomeJavaConstants.species) + "::" + curr_species); // TEST
                	if (((GKInstance)rgp.getAttributeValue(ReactomeJavaConstants.species)).equals(curr_species)) {
                		taxaMember = true;
                		break;
                	}
                }
            	if (!taxaMember) {
            		throw new Exception("RGP identifier " + key 
	            			+ " is not a member of the expected species list " + target_taxa.toString());
            	}*/

            	// preserve the old identifier minus any isomer (will be identical to the entry provided in the mapping file)
            	String old_identifier = key;
            	if (old_identifier.contains(".")) {
            		old_identifier = old_identifier.split("\\.")[0];
            	}
	            // set the identifier to the UniProt id
	            rgp.setAttributeValue(ReactomeJavaConstants.identifier, value);

	            // retrieve Uniprot data from the built-in web service call and 
	            // place UniProt data in appropriate attributes on RGPs 
	            logger.info("Retrieving Uniprot data for RGP " + rgp.getDisplayName() + "...");
	            uniprotAutoFiller.process(rgp);
	            logger.info("...retrieved Uniprot data for RGP" + rgp.getDisplayName());

	            // only overwrite the species if necessary
	            /*if (!(rgp.getAttributeValue(ReactomeJavaConstants.species) == speciesInstance)) {
	            	String uniProtSpecies = rgp.getAttributeValue(ReactomeJavaConstants.species).toString();
		            rgp.setAttributeValue(ReactomeJavaConstants.species, speciesInstance);
	            	String designatedSpecies = rgp.getAttributeValue(ReactomeJavaConstants.species).toString();
		            String new_comment_text = "Designated species " + designatedSpecies + " preserved instead of UniProt taxon "
		            	+ uniProtSpecies;
	            	rgp.addAttributeValue(ReactomeJavaConstants.comment,new_comment_text);
	            }*/
        	
	            // old identifier to gene name (only if not already present; also remove isoform suffix)
                List<String> geneNames = rgp.getAttributeValuesList(ReactomeJavaConstants.geneName);
                if (geneNames == null)
                    rgp.addAttributeValue(ReactomeJavaConstants.geneName, old_identifier);
            	// make sure identifier is first in the list
                else {
        			for (Iterator<String> it = geneNames.iterator(); it.hasNext();) {
        	            String currName = (String)it.next().toUpperCase();
        	            if (currName.equals(old_identifier))
        	            	it.remove();
        			}
                	geneNames.add(0, old_identifier);
                }
                InstanceDisplayNameGenerator.setDisplayName(rgp);
                
                changedInsts.add(rgp);
                
        	} catch(Exception e) {
        		logger.info(e.getMessage());
        	}
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public java.util.List getReferers(GKInstance instance) throws Exception {
        Set set = new HashSet();
        SchemaClass cls = instance.getSchemClass();
        Set referrerClasses = new HashSet();
        GKSchema schema = new GKSchema();
        java.util.List top = new ArrayList(1);
        for (Iterator it = cls.getReferers().iterator(); it.hasNext();) {
            GKSchemaAttribute att = (GKSchemaAttribute)it.next();
            GKSchemaClass origin = (GKSchemaClass) att.getOrigin();
            top.clear();
            top.add(origin);
            java.util.List classes = InstanceUtilities.getAllSchemaClasses(top);
            referrerClasses.addAll(classes);
        }
        for (Iterator it = referrerClasses.iterator(); it.hasNext();) {
            SchemaClass tmpCls = (SchemaClass) it.next();
            java.util.List instanceList = (java.util.List) clsMap.get(tmpCls); // TODO: your clsMap is null; populate it
            if (instanceList != null && instanceList.size() > 0) {
                for (Iterator it1 = instanceList.iterator(); it1.hasNext();) {
                    GKInstance tmpInstance = (GKInstance)it1.next();
                    for (Iterator it2 = tmpInstance.getSchemaAttributes().iterator(); it2.hasNext();) {
                        SchemaAttribute att = (SchemaAttribute) it2.next();
                        if (!att.isInstanceTypeAttribute())
                            continue;
                        if (!att.isValidValue(instance))
                            continue;
                        java.util.List values = tmpInstance.getAttributeValuesList(att);
                        if (values != null && values.size() > 0 & values.contains(instance))
                            set.add(tmpInstance);
                    }
                }
            }            
        }
        return new ArrayList(set);      
    }

    private Set<GKInstance> getReferrersForDBInstance(GKInstance instance) throws Exception {
        Map<String, Set<GKInstance>> map = new HashMap<String, Set<GKInstance>>();
        for (Iterator rai = instance.getSchemClass().getReferers().iterator(); rai.hasNext();) {
            GKSchemaAttribute att = (GKSchemaAttribute)rai.next();
            Collection r = instance.getReferers(att);
            // It is possible two attributes have same name but from different classes as in
            // pathwayDiagram in Edge and Vertex.
            if (r == null || r.size() == 0)
                continue;
            Set<GKInstance> set = map.get(att.getName());
            if (set == null) {
                set = new HashSet<GKInstance>();
                map.put(att.getName(), set);
            }
            set.addAll(r);
        }
        Set<GKInstance> rtn = new HashSet<GKInstance>();
        // Aggregate
        for (String attName : map.keySet()) {
            Set<GKInstance> set = map.get(attName);
        	rtn.addAll(set);
        }
        return rtn;
        
        // Do a sort
//        Map<String, List<GKInstance>> rtnMap = new HashMap<String, List<GKInstance>>();
//        for (String attName : map.keySet()) {
//            Set<GKInstance> set = map.get(attName);
//            List<GKInstance> list = new ArrayList<GKInstance>(set);
//            InstanceUtilities.sortInstances(list);
//            rtnMap.put(attName, list);
//        }
//        return rtnMap;
    }
    
    /**
     * Remove all data from a Reactome data source created in connection with a SINGLE InstanceEdit, being careful 
     * to not delete any instances shared by other Reactome data
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void deleteReactomeDataByInstanceEdit() throws Exception
    {
    	logger.info("Running deleteReactomeDataByInstanceEdit()...");
    	int del_count = 0;
    	int no_del_count = 0;
    	String log_string = new String();
    	this.target_instances.clear(); // clear out the autofilled instance edit referrers

		// gather all database objects CREATED as part of the target instance edit (configured value)
		Collection<GKInstance> targets = (Collection<GKInstance>)(uniAdaptor).fetchInstanceByAttribute(
				ReactomeJavaConstants.DatabaseObject,
				ReactomeJavaConstants.created,
				"=",
				this.target_instance_edit);

		logger.info("Target instance edit: " + this.target_instance_edit.getDisplayName());
		logger.info("Target instance edit DB_ID: " + this.target_instance_edit.getDBID());
		for (GKInstance curr_target : targets)
		{
			this.target_instances.add(curr_target);
		}
		logger.info("Num target instances: " + this.target_instances.size());

		for (GKInstance instance : this.target_instances) {
			log_string = instance.getAttributeValue(ReactomeJavaConstants.DB_ID) + ": " +
				(String)instance.getAttributeValue(ReactomeJavaConstants._displayName);
			
	    	// check each one for reference by other objects not in this collection
			//Collection<GKInstance> referrers = (Collection<GKInstance>) this.getReferers(instance);
			//Collection<GKInstance> referrers = null;
			Set<GKInstance> referrers = this.getReferrersForDBInstance(instance);

			referrers.removeAll(target_instances);
			if (referrers.size() == 0) {
	    		log_string += " - NO REFERRERS (OR ONLY INTERNAL REFERRERS); DELETE";
	    		try {
	    	    	this.dbAdaptor.txDeleteInstance(instance); // ** NOTE: this is the danger line; uncomment at your peril **
		    		del_count++;
	    		} catch (Exception e) {
	    			log_string += " *** DELETE FAILED: " + e.getMessage();
	    		}
			}
			else {
				log_string += " - HAS EXTERNAL REFERRERS; DO NOT DELETE; Referring classes: ";
				for (GKInstance curr_ref : referrers) {
					log_string += "[" + curr_ref.getDBID() + ":" + curr_ref.getDisplayName() + " (" + curr_ref.getSchemClass().getName() + ")] ";
					//log_string += "\n!" + curr_ref.getSchemClass().getName();
				}
				no_del_count++;
			}
    		logger.info(log_string);
    		
		}
		logger.info("Total num of instances to delete: " + del_count);
		logger.info("Total num of instances to keep: " + no_del_count);
    }
    
    /**
     * Test update: add a comment to all ReferenceGeneProducts for a specified species 
     * @throws Exception
     */
    private void testUpdate1() throws Exception
    {
		logger.info("Running test update 1...");
        for (GKInstance curr_species : target_taxa)
        {
			Collection<?> instances = dbAdaptor.fetchInstanceByAttribute(
	        		ReactomeJavaConstants.ReferenceGeneProduct,
					ReactomeJavaConstants.species,
					"=",
					curr_species);
	        Integer count = 0;
			for (Iterator<?> it = instances.iterator(); it.hasNext();) {
	            count++;
	            GKInstance inst = (GKInstance) it.next();
        		dbAdaptor.fastLoadInstanceAttributeValues(inst); // load all attributes so you don't overwrite
        		logger.info(count + ": " + inst.getAttributeValue(ReactomeJavaConstants._displayName)); // TEST
        		//List<GKInstance> comments = (List<GKInstance>) inst.getAttributeValuesList(ReactomeJavaConstants.comment);
        		//logger.info("--- has " + comments.size() + " comment(s)."); // TEST
	            // test update
        		String new_comment_text = "TEST: Update of O.sativa japonica RGPs.";
            	inst.addAttributeValue(ReactomeJavaConstants.comment,new_comment_text);
        		//List<GKInstance> comments_after = (List<GKInstance>) inst.getAttributeValuesList(ReactomeJavaConstants.comment);
        		//logger.info("--- has " + comments_after.size() + " comment(s)."); // TEST
            	changedInsts.add(inst);
	        }
        }
    }

    /**
     * Test update: add another comment to all ReferenceGeneProducts for a specified species
     * @throws Exception
     */
    private void testUpdate2(List<GKInstance> referrers) throws Exception
    {
		logger.info("Running test update 2...");
        //logger.info("Num of referrers (target instances): " + referrers.size()); // TEST   

		for (GKInstance curr_referrer : referrers)
		{
    		logger.info(curr_referrer); // TEST
    		List<GKInstance> comments = (List<GKInstance>) curr_referrer.getAttributeValuesList(ReactomeJavaConstants.comment);
    		logger.info("--- has " + comments.size() + " comment(s)."); // TEST
            // test update
    		String new_comment_text = "TEST 3: Another update of O.sativa japonica RGPs.";
    		curr_referrer.addAttributeValue(ReactomeJavaConstants.comment,new_comment_text);
    		List<GKInstance> comments_after = (List<GKInstance>) curr_referrer.getAttributeValuesList(ReactomeJavaConstants.comment);
    		logger.info("--- has " + comments_after.size() + " comment(s)."); // TEST
		
    		changedInsts.add(curr_referrer);
        }
    }

    private void listRegulationTypes() throws Exception {
        Collection<?> regTypes = uniAdaptor.fetchInstancesByClass(uniAdaptor.getSchema().getClassByName(ReactomeJavaConstants.RegulationType));
        for (Iterator<?> it = regTypes.iterator(); it.hasNext();) {
            GKInstance regType = (GKInstance) it.next();
    		logger.info(regType.getDisplayName());
        }
    }

    private void listPathways(boolean filterSpecies) throws Exception {
    	int count = 0;
        Collection<?> pathways = uniAdaptor.fetchInstancesByClass(uniAdaptor.getSchema().getClassByName(ReactomeJavaConstants.Pathway));
        for (Iterator<?> it = pathways.iterator(); it.hasNext();) {
            GKInstance pathway = (GKInstance) it.next();
            if (filterSpecies) {
	            if (target_taxa.contains(pathway.getAttributeValue(ReactomeJavaConstants.species))) {
	            	System.out.println(pathway.getDisplayName());
	            	count++;
	            }
            }
        }
		System.out.println("Pathway count: " + count);
    }

    private String printCompartments(GKInstance compartmentHolder) throws Exception {
    	String compStr = "";
        List comps = compartmentHolder.getAttributeValuesList(ReactomeJavaConstants.compartment);
        Integer count = 0;
        if (comps != null) {
            for (Iterator<?> itC = comps.iterator(); itC.hasNext();) {
                GKInstance curComp = (GKInstance) itC.next();
                compStr += (((count > 0) ? "|" : "") + curComp.getDisplayName());
                count++;
        	}
        }
        return compStr;
    }
    
    // analyze the compartment assignments present in EWAS, DefinedSets, CatalystActivities, Reactions, and SimpleEntities
    private void profileCompartmentDistribution() throws Exception {
        System.out.println("Reaction\tRxn Compartment(s)\tCatalyst Activity\tPhysical Entity <EWAS/Defined Set/Complex>\tPE Compartment(s)");
    	// get reactions
        Collection<?> reactions = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Reaction);
        for (Iterator<?> itR = reactions.iterator(); itR.hasNext();) {
            GKInstance curR = (GKInstance) itR.next();
            System.out.print(curR.getDisplayName() + "\t");
            System.out.print(printCompartments(curR) + "\t");
    		// get CA
            GKInstance ca = (GKInstance)curR.getAttributeValue(ReactomeJavaConstants.catalystActivity);
            if (ca != null) {
            	System.out.print(ca.getDisplayName() + "\t");
    			// get PE
                GKInstance pe = (GKInstance)ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                if (pe != null) {
                	System.out.print(pe.getDisplayName() + " <" + pe.getSchemClass().getName() + ">\t");
    				// if DefinedSet or Complex
                	Boolean DefinedSet_flag = pe.getSchemClass() == fileAdaptor.fetchSchemaClass(ReactomeJavaConstants.DefinedSet); 
                	Boolean Complex_flag = pe.getSchemClass() == fileAdaptor.fetchSchemaClass(ReactomeJavaConstants.Complex); 
                	if (DefinedSet_flag || Complex_flag) {
    					// get members (they will be EWAS)
                		List members = null;
                		if (DefinedSet_flag)
                			members = pe.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                		else // Complex_flag == true
                			members = pe.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
                		if (members != null) {
                			Integer count = 0;
                	        for (Iterator<?> itM = members.iterator(); itM.hasNext();) {
                	            GKInstance curMember = (GKInstance) itM.next();
                	            System.out.print(((count > 0) ? "|" : "") 
                	            		+ curMember.getDisplayName() 
                	            		+ (printCompartments(curMember).isEmpty() ? "" : (" *" + (printCompartments(curMember) + "*"))));
                	            count++;
                	        }
                		}
                	}
                	else // it's an EWAS or SimpleEntity
                    	printCompartments(pe);
                }
            }
        	System.out.print("\n");
        }
    }

    // utility method to isolate the AT # from a Defined Set's member EWAS. It will either be in the member's displayName or
    // in the member's referenceEntity attribute, or not there at all in the case of the SimpleEntities
    private String getMemberCanonicalGeneOrSimpleEntity(GKInstance member) throws Exception {
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

    // analyze the compartment assignments present in DefinedSets and collate with SUBA compartment data
    private void profileCompartmentDistributionWithSUBA() throws Exception {
        System.out.println("Defined Set\tPathway\tReaction\tEC Number\tMember\tAraCyc Compartment\tSUBA Compartment");

        // load SUBA data
        String SUBA_filepath = "/home/preecej/Documents/projects/plant_reactome/aracyc_to_reactome_conversion/compartment_assignment/AraCyc11_IDs_with_unique_SUBA_locations.txt";
        FileUtility fu = new FileUtility();
        String fileName = SUBA_filepath;
        Map<String, String> SUBA_Ath_to_compartment = fu.importMap(fileName);

        // get defined sets
        Collection<?> definedSets = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DefinedSet);
        for (Iterator<?> itDS = definedSets.iterator(); itDS.hasNext();) {
            GKInstance curDS = (GKInstance) itDS.next();
            Integer memCount = 0;
            String pathwayName = "";
            List members = curDS.getAttributeValuesList(ReactomeJavaConstants.hasMember);
    		if (members != null) {
                for (Iterator<?> itM = members.iterator(); itM.hasNext();) {
                    if (memCount == 0) {
                    	System.out.print(curDS.getDisplayName());
                    }
                   	System.out.print("\t");
                   	
                    // get parent reaction(s) and EC Number(s)
                    Collection<GKInstance> refs = curDS.getReferers(ReactomeJavaConstants.physicalEntity);
                    if (refs != null) {
        	            for (GKInstance ref : refs) {
        	            	if (ref.getSchemClass() == fileAdaptor.fetchSchemaClass(ReactomeJavaConstants.CatalystActivity)) {
        	                    Collection<GKInstance> reactions = ref.getReferers(ReactomeJavaConstants.catalystActivity);
        	                    if (reactions != null) {
        	        	            for (GKInstance reaction : reactions) {
        	        	            	// get parent pathway

        	                            Collection<GKInstance> rxnRefs = reaction.getReferers(ReactomeJavaConstants.hasEvent);
        	                            if (rxnRefs != null) {
        	                	            for (GKInstance rxnRef : rxnRefs) {
        	                	            	if (rxnRef.getSchemClass() == fileAdaptor.fetchSchemaClass(ReactomeJavaConstants.Pathway)) {
	                	        	            	pathwayName = rxnRef.getDisplayName(); // assumes only one
        	                	            	}
        	                	            }
        	                            }
        	        	            	
        	        	            	// get EC number
        	        	            	String ECNumber = "";
        	        	            	Collection<GKInstance> xrefs = reaction.getAttributeValuesList(ReactomeJavaConstants.crossReference);
        	        	            	for (GKInstance xref : xrefs) {
        	        	            		if (xref.getDisplayName().contains("EC-NUMBER:")) {
        	        	            			ECNumber = xref.getDisplayName().substring(10);
        	        	            			break;
        	        	            		}
        	        	            	}
        	                            if (memCount == 0) {
        	                            	System.out.print(pathwayName + "\t" + reaction.getDisplayName() + "\t" + ECNumber + "\t");
        	                            }
        	                            else {
        	                            	System.out.print("\t\t\t");
        	                            }
        	        	            	break; // only 1 for testing
        	        	            }
        	                    }
        	            	}
        	            	break; // only 1 for testing
        	            }
                    }
                   	                   	
                   	
    	            GKInstance curMember = (GKInstance) itM.next();
    	            System.out.print(curMember.getDisplayName() + "\t" + (printCompartments(curMember)) + "\t");

    	            // check SUBA data for gene id match
    	            String convertedDisplayName = ""; 
    	            try {
    	            	convertedDisplayName = getMemberCanonicalGeneOrSimpleEntity(curMember)
    	            		+ ".1"; // must account for default isoform designation in SUBA data
        	            if (SUBA_Ath_to_compartment.containsKey(convertedDisplayName)) { 
            				System.out.print(SUBA_Ath_to_compartment.get(convertedDisplayName) + "\t");
            			}
    	            }
    	            catch (Exception e) {}

    	            // TEST
//        			for (String key : SUBA_Ath_to_compartment.keySet()) 
//    	            {
//    	            	key = key.toUpperCase();
//    	                String value = SUBA_Ath_to_compartment.get(key);
//    	                System.out.println(key + ": " + value);
//    	            }

    	            System.out.print("\n");
    	    		memCount++;
    	        }
    		}
        }
    }
    
    // find all of the duplicate SEs; see if they need to be addressed prior to merging w/ gk_central
    private void profileDupeSEs() throws Exception {
        Collection<?> refMols = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceMolecule);
        Set<GKInstance> uniqRefMols = new HashSet<GKInstance>();
        for (Iterator<?> it = refMols.iterator(); it.hasNext();) {
            GKInstance curRefMol = (GKInstance) it.next();
            //logger.info(curRefMol);
            if (uniqRefMols.contains(curRefMol)) {
            	logger.info("Dupe: " + curRefMol);
            	continue;
            }
            else {
            	uniqRefMols.add(curRefMol);
            }
        }
        //logger.info(refMols.size());
    }

    // compare unmapped reference molecules (those with no ChEBI identifier) between gk_central and a file project
    private void compareRefMols() throws Exception {
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

        logger.info("db: " + gk_central_RefMols.size());
        logger.info("file: " + aracyc_RefMols.size());
        
        Set<String> overlap = new HashSet<String>();

        for (Iterator<?> it = aracyc_RefMols.iterator(); it.hasNext();) {
            GKInstance curARM = (GKInstance) it.next();
            List<String> names = curARM.getAttributeValuesList(ReactomeJavaConstants.name);
            overlap.add(names.get(0));
            //logger.info(names.get(0));
        }
        Integer count = 0;
        for (Iterator<?> it2 = gk_central_RefMols.iterator(); it2.hasNext();) {
            GKInstance curGKRM = (GKInstance) it2.next();
            List<String> names = curGKRM.getAttributeValuesList(ReactomeJavaConstants.name);
            String tmpName = names.get(0);
//            String tmpName = names.get(0).replaceFirst("^a ", "").replaceFirst("^an ", "");
//            if (names.get(0).startsWith("a ") || names.get(0).startsWith("an ")) {
//            	logger.info(names.get(0) + " changed to " + tmpName);
//            }
            if (overlap.contains(tmpName)) {
                logger.info("match: " + tmpName);
            	count++;
            }
        }
        logger.info("total overlap in ref mols, based on name: " + count);
    }

    private void listNewRefMols() throws Exception {
        System.out.println("DB_ID\tName");
    	Collection<?> aracyc_RefMols = uniAdaptor.fetchInstanceByAttribute(
        		ReactomeJavaConstants.ReferenceMolecule, 
        		ReactomeJavaConstants.identifier, 
        		"IS NULL", 
        		"");
        
        for (Iterator<?> it = aracyc_RefMols.iterator(); it.hasNext();) {
            GKInstance curARM = (GKInstance) it.next();
            if (curARM.getDBID() < 0) {
	            List<String> names = curARM.getAttributeValuesList(ReactomeJavaConstants.name);
	            System.out.println(curARM.getDBID() + "\t" + names.get(0));
            }
        }
    }

    private void grameneSolrExporter() throws Exception {
    	// pathway browser dump
    	/* For pathway_browser objects (used for every indexed, searchable pathway browser object):
    		Link Template: http://plantreactome.gramene.org/entitylevelview/PathwayBrowser.html#DB=<db_name>&FOCUS_SPECIES_ID=<plant_reactome_species_id>&FOCUS_PATHWAY_ID=<plant_reactome_pathway_id>&ID=<object_id (number at end of id field)>
    	*/
    	int count = 0;
    	StringBuilder sb = new StringBuilder();
    	String object_search_type = "plant_reactome_pathway";
    	String module = "reactome";
    	String pathwayName = ""; // used to provide pathway context for non-pathway instances in Solr  
    	String pathwayEntry = "";
    	String reactionEntry = "";
    	String catalystEntry = "";
    	String reactantEntry = "";
    	String subspExp = " subsp.*";

    	// header
        System.out.println("id\ttitle\tmodule\tobject\tspecies\ttaxonomy\tcontent");
        // set defaults
        String curSpeciesName = "";
        String curTaxonID = ""; // NCBI

        // get pathways
        Collection<?> pathways = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
        for (Iterator<?> itP = pathways.iterator(); itP.hasNext();) {
            GKInstance curP = (GKInstance) itP.next();
            count++;
            Long curPathwayID = curP.getDBID();
            String curPathwayName = curP.getDisplayName();
            Long curObjectID = curPathwayID;
            String curObjectName = curP.getDisplayName();
            pathwayName = curObjectName;
            
            GKInstance curSpecies = (GKInstance)curP.getAttributeValue(ReactomeJavaConstants.species);
            curSpeciesName = curSpecies.getDisplayName().replaceAll(subspExp, "");

            // NOTE: had to modify sliced db to make sure projected Species and DatabaseIdentifier exists in db and was assigned;
            // It may be better to hard-code those NCBI ids (or provide a config listing) in the future to avoid this problem.
            /*curTaxonID = ((GKInstance)((List<GKInstance>)curSpecies
            		.getAttributeValuesList(ReactomeJavaConstants.crossReference)).get(0))
            		.getAttributeValue(ReactomeJavaConstants.identifier).toString();*/
            // ...and so it is: NCBI ids for projected species now come from a hash via the curator config file
            curTaxonID = this.NCBI_map.get(curSpeciesName);  
		            
			GKInstance curGoPB = (GKInstance)curP.getAttributeValue(ReactomeJavaConstants.goBiologicalProcess);
			List<GKInstance> curLitRefs = (List<GKInstance>)curP.getAttributeValuesList(ReactomeJavaConstants.literatureReference);

            pathwayEntry = 
            		module + "/" + object_search_type + "/" 
        				+ curObjectID.toString() + "-" 
        				+ curPathwayID.toString() + "-" // plant_reactome_pathway_id
    					+ curSpecies.getDBID().toString() // plant_reactome_species_id
        				+ "\t" // Solr identifier
            		+ curSpeciesName + " pathway " + curObjectName + "\t" // title
            		+ module + "\t" // module
            		+ object_search_type + "\t" // object
    				+ curSpeciesName.toLowerCase().replace(' ', '_') + "\t" // species
    				+ curTaxonID + "\t" // taxonomy
    				// content field (all other human-readable and desired searchable attributes
            		// (GO bio process, litRefs, compartment, other names, basket of RGP ids), space-delimited)...
    				+ curObjectName + " " // current object name
    				+ curP.getSchemClass().getName() + " " // plant_reactome_class
    				;
					// compartment
    				if (curP.getAttributeValue(ReactomeJavaConstants.compartment) != null)
    					pathwayEntry += ((GKInstance)curP.getAttributeValue(ReactomeJavaConstants.compartment)).getDisplayName() + " ";

					// GO Bio Process
    				if (curGoPB != null)
    					pathwayEntry += curGoPB.getDisplayName() + " "
							+ "GO:" + curGoPB.getAttributeValue(ReactomeJavaConstants.accession) + " ";

    				// synonyms
    				List<String> otherNames = (List<String>)curP.getAttributeValuesList(ReactomeJavaConstants.name);
    				if (otherNames != null) {
    					count = 0;
    					for (String name : otherNames) {
    						count++;
    						if (count == 1) continue; // skip the first name (redundant)
    						if (!name.startsWith("pathway")) // hack to remove -Cyc internal ids
    							pathwayEntry += name + " ";
    					}
    				}

    				// gene ids
                	Set<GKInstance> allPEs = InstanceUtilities.grepRefPepSeqsFromPathway(curP);
                    if (allPEs != null) {
                    	for (GKInstance curPE : allPEs) {
                    		if (curPE.getAttributeValue(ReactomeJavaConstants.identifier) != null) {
    	                		String rgpIdentifier = curPE.getAttributeValue(ReactomeJavaConstants.identifier).toString();
    		            		if (rgpIdentifier != null) {
    		            			pathwayEntry += rgpIdentifier + " ";
    		            		}
                    		}
                    	}
                    }

    				if (curLitRefs != null)
    					// literature references
    					for (GKInstance litRef : curLitRefs)
    						pathwayEntry += litRef.getDisplayName() + " ";
			
			sb.append(pathwayEntry.trim() + "\n");
			
    		// get reactions
            Collection<GKInstance> events = curP.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if (events != null) {
            	// check to make sure this isn't a super-pathway; we don't want to generate sub-instance data (rxns, etc.) from those
            	boolean hasParent = false;
            	for (GKInstance curEvent : events) {
            		// if the pathway has a child pathway, it's a superpathway
            		if (curEvent.getSchemClass().getName().equals(ReactomeJavaConstants.Pathway)) {
            			hasParent = true;
            			break;
            		}
            	}
            	if (hasParent) continue; // don't bother getting reactions, etc from a superpathway

            	// flags for determining when to print a newline
            	boolean isReaction = false;
            	boolean isCatalyst = false;
            	
            	for (GKInstance curEvent : events) {
                    if (curEvent.getSchemClass().getName().equals(ReactomeJavaConstants.Reaction)) {
                    	isReaction = true;
                        curObjectID = curEvent.getDBID();
                        curObjectName = curEvent.getDisplayName();
                        
                        reactionEntry = 
                        		module + "/" + object_search_type + "/" 
			        				+ curObjectID.toString() + "-" 
			        				+ curPathwayID.toString() + "-" // plant_reactome_pathway_id
		        					+ curSpecies.getDBID().toString() // plant_reactome_species_id
			        				+ "\t" // Solr identifier
		        				+ curSpeciesName + " reaction " + curObjectName + "(pathway: " + pathwayName + ")\t" // title
                    			+ module + "\t" // module
                        		+ object_search_type + "\t" // object
			    				+ curSpeciesName.toLowerCase().replace(' ', '_') + "\t" // species
			    				+ curTaxonID + "\t" // taxonomy
		        				// content (all other human-readable attributes) // content
		        				+ curObjectName + " "
		        				+ curEvent.getSchemClass().getName() + " " // plant_reactome_class
		        				;
		    					// compartment
		        				if (curEvent.getAttributeValue(ReactomeJavaConstants.compartment) != null)
		        					reactionEntry += ((GKInstance)curEvent.getAttributeValue(ReactomeJavaConstants.compartment)).getDisplayName() + " ";
		
		        				// synonyms
		        				List<String> otherReactionNames = (List<String>)curEvent.getAttributeValuesList(ReactomeJavaConstants.name);
		        				if (otherReactionNames != null) {
		        					count = 0;
		        					for (String name : otherReactionNames) {
		        						count++;
		        						if (count == 1) continue; // skip the first name (redundant)
		        						if (!name.startsWith("biochemicalReaction")) // hack to remove -Cyc internal ids
		        							reactionEntry += name + " ";
		        					}
		        				}

        				Set<GKInstance> RGPs = null;

                    	// get EWAS (via Catalystactivity.PhysicalEntity)
	                    Collection<GKInstance> cas = curEvent.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
	                    if (cas != null) {
	                    	for (GKInstance curCA : cas) {
	                            GKInstance pe = (GKInstance)curCA.getAttributeValue(ReactomeJavaConstants.physicalEntity);
	                            if (pe != null) {
	                            	isCatalyst = true;
	                            	// get the RGPs for this catalyst 
	                            	RGPs = InstanceUtilities.grepRefPepSeqsFromPhysicalEntity(pe);
	                            	
	                                curObjectID = pe.getDBID();
	                                curObjectName = pe.getDisplayName();
	                                if (curObjectName.matches(".*]")) {
	                                	curObjectName = curObjectName.split("\\[")[0]; 
	                                }
	                                
	                                catalystEntry = 
	                                		module + "/" + object_search_type + "/" 
						        				+ curObjectID.toString() + "-" 
						        				+ curPathwayID.toString() + "-" // plant_reactome_pathway_id
					        					+ curSpecies.getDBID().toString() // plant_reactome_species_id
						        				+ "\t" // Solr identifier
	                                		+ curSpeciesName + " catalyst " + curObjectName + "(pathway: " + pathwayName + ")\t" // title
	                                		+ module + "\t" // module
	                                		+ object_search_type + "\t" // object
						    				+ curSpeciesName.toLowerCase().replace(' ', '_') + "\t" // species
						    				+ curTaxonID + "\t" // taxonomy
	                        				// + content (all other human-readable attributes) // content
			                				+ curObjectName + " "
			                				+ pe.getSchemClass().getName() + " " // plant_reactome_class
			                				;
	                            }
	                        }
	                    }
	                    // finally, append the reaction and catalystEntries, once it is determined whether RGPs are available
	                    if (RGPs != null) {
	                    	for (GKInstance RGP : RGPs) {
	                    		if (RGP.getAttributeValue(ReactomeJavaConstants.identifier) != null) {
	    	                		String rgpIdentifier = RGP.getAttributeValue(ReactomeJavaConstants.identifier).toString();
	    		            		if (rgpIdentifier != null) {
	    		            			reactionEntry += rgpIdentifier + " ";
	    		            			catalystEntry += rgpIdentifier + " ";
	    		            		}
	                    		}
	                    	}
	                    }
	                    if (isReaction)
	                    	sb.append(reactionEntry.trim() + "\n");
	                    if (isCatalyst)
	                    	sb.append(catalystEntry.trim() + "\n");
	                    
	                    // get SimpleEntities (or any and all Physical Entities acting as inputs and outputs)
	                    Collection<GKInstance> inputs = curEvent.getAttributeValuesList(ReactomeJavaConstants.input);
	                    Collection<GKInstance> outputs = curEvent.getAttributeValuesList(ReactomeJavaConstants.output);
	                    Collection<GKInstance> SEs = inputs;
	                    SEs.addAll(outputs);
	                    if (SEs!= null) {
		                    for (GKInstance entity : SEs) {
	                                curObjectID = entity.getDBID();
	                                curObjectName = entity.getDisplayName();
	                                if (curObjectName.matches(".*]")) {
	                                	curObjectName = curObjectName.split("\\[")[0]; 
	                                }

	                                reactantEntry = 
	                                		module + "/" + object_search_type + "/" 
						        				+ curObjectID.toString() + "-" 
						        				+ curPathwayID.toString() + "-" // plant_reactome_pathway_id
					        					+ curSpecies.getDBID().toString() // plant_reactome_species_id
						        				+ "\t" // Solr identifier
	                                		+ curSpeciesName + " reactant " + curObjectName + "(pathway: " + pathwayName + ")\t" // title
	                                		+ module + "\t" // module
	                                		+ object_search_type + "\t" // object
						    				+ curSpeciesName.toLowerCase().replace(' ', '_') + "\t" // species
						    				+ curTaxonID + "\t" // taxonomy
	                        				// + content (all other human-readable attributes) // content
			                				+ curObjectName + " "
			                				+ entity.getSchemClass().getName() + " " // plant_reactome_class
	                                		;
			                				// synonyms
			                				List<String> otherReactantNames = (List<String>)entity.getAttributeValuesList(ReactomeJavaConstants.name);
			                				if (otherReactantNames != null) {
			                					count = 0;
			                					for (String name : otherReactantNames) {
			                						count++;
			                						if (count == 1) continue; // skip the first name (redundant)
			                						if (!name.startsWith("phys-ent-participant")) // hack to remove -Cyc internal ids
			                							reactantEntry += name + " ";
			                					}
			                				}
	                                		
                            		sb.append(reactantEntry.trim() + "\n");
		                    }
	                    }
                    }
                }
            }
        }
        System.out.println(sb.toString());
        
    	// quick search dump
        /* For quick_search (used for every indexed, searchable plant reactome data instance of interest):
			Link Template: http://plantreactome.gramene.org/cgi-bin/search2?CATEGORY=everything&OPERATOR=all&QUERY=<final word in title field> 
         */
/*
        String instanceEntry = "";
    	object_search_type = "quick_search";
        // id title module object species taxonomy content
    	
        // NOTE: assumes dbAdaptor b/c you want to use fetchInstancesByClass()
    	// set up list of classes for which you want data
	    String[] classNames = new String[] {
	            ReactomeJavaConstants.CatalystActivity,
	            ReactomeJavaConstants.Pathway,
	            ReactomeJavaConstants.Reaction,
	            ReactomeJavaConstants.GO_BiologicalProcess,
	            ReactomeJavaConstants.GO_MolecularFunction,
	            ReactomeJavaConstants.EntityCompartment,
	            ReactomeJavaConstants.Complex,
	            ReactomeJavaConstants.DefinedSet,
	            ReactomeJavaConstants.EntityWithAccessionedSequence,
	            ReactomeJavaConstants.SimpleEntity,
	            ReactomeJavaConstants.LiteratureReference,
	            ReactomeJavaConstants.ReferenceMolecule,
	            ReactomeJavaConstants.ReferenceGeneProduct
	    };
    	// get objects by class
        for (String className : classNames) {
            Collection<?> instances = dbAdaptor.fetchInstancesByClass(className);
            for (Iterator<?> it = instances.iterator(); it.hasNext();) {
                GKInstance curI = (GKInstance) it.next();

                GKInstance curSpecies = null;
                String curSpeciesID = "";
                curSpeciesName = "";
                curTaxonID = "";
                Long curObjectID = curI.getDBID();
                String curObjectName = curI.getDisplayName();

                try { // get a species if you can
	                curSpecies = (GKInstance)curI.getAttributeValue(ReactomeJavaConstants.species);
	                curSpeciesName = curSpecies.getDisplayName().replaceAll(subspExp, "");
	                curSpeciesID = curSpecies.getDBID().toString();
	                // NOTE: had to modify sliced db to make sure projected Species and DatabaseIdentifier exists in db and was assigned;
	                // It may be better to hard-code those NCBI ids (or provide a config listing) in the future to avoid this problem.
	                curTaxonID = ((GKInstance)((List<GKInstance>)curSpecies
	                		.getAttributeValuesList(ReactomeJavaConstants.crossReference)).get(0))
	                		.getAttributeValue(ReactomeJavaConstants.identifier).toString();
                } catch (Exception e) {}

    			// make sure the current instance is in your species filter
        		boolean isTaxaMember = false;
                for (GKInstance targetSpecies : target_taxa) {
            		//logger.info("species filter: " + curSpecies + "::" + targetSpecies); // TEST
                	if (curSpecies == null || targetSpecies.equals(curSpecies)) {
                		isTaxaMember = true;
                		break;
                	}
                }
            	if (!isTaxaMember) {
            		//logger.info(curI.getSchemClass().getName() + "::" + curSpecies);
            		continue; // skip this instance
            	}

                instanceEntry = 
                		module + (curSpeciesName != "" ? "_" : "") + curSpeciesName.toLowerCase().replace(' ', '_') + "/" + object_search_type + "/" 
                				+ curObjectID.toString() + "\t" // Solr identifier
                    		+ curSpeciesName + " " + curI.getSchemClass().getName().toLowerCase() + " " + curObjectName + "\t" // title
                    		+ module + (curSpeciesName.length() > 0 ? "_" + curSpeciesName.toLowerCase().replace(' ', '_') : "") + "\t" // module
                    		+ object_search_type + "\t" // object
            				+ curSpeciesName.toLowerCase().replace(' ', '_') + "\t" // species
            				+ curTaxonID + "\t" // taxonomy
            				// content field (all other human-readable and desired searchable attributes
                    		// (GO bio process, litRefs, compartment, other names, basket of RGP ids), space-delimited)...
            				+ curObjectName + " " // current object name
            				+ curI.getSchemClass().getName() + " " // plant_reactome_class
                			;
        					// compartment
            				if (curI.getAttributeValueNoCheck(ReactomeJavaConstants.compartment) != null)
            					pathwayEntry += ((GKInstance)curI.getAttributeValue(ReactomeJavaConstants.compartment)).getDisplayName() + " ";

            				// synonyms
            				List<String> otherNames = (List<String>)curI.getAttributeValuesListNoCheck(ReactomeJavaConstants.name);
            				if (otherNames != null) {
            					count = 0;
            					for (String name : otherNames) {
            						count++;
            						if (count == 1) continue; // skip the first name (redundant)
            						if (!name.startsWith("pathway") 
            								|| !name.startsWith("biochemicalReaction")
            								|| !name.startsWith("phys-ent-participant")) // hack to remove -Cyc internal ids
            							pathwayEntry += name + " ";
            					}
            				}
                sb.append(instanceEntry.trim() + "\n");
            }
        }
        System.out.println(sb.toString());
  */  }
    
    private void dumpRGPsBinnedByPathwayOld() throws Exception {
    	int count = 0;
    	
    	// get pathways
        Collection<?> pathways = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
        for (Iterator<?> itP = pathways.iterator(); itP.hasNext();) {
            GKInstance curP = (GKInstance) itP.next();
            Long curPathwayID = curP.getDBID();
            String curPathwayName = curP.getDisplayName();
            Long curObjectID = curPathwayID;
            String curObjectName = curPathwayName;
    	    Long curMultiLevelPathwayParent = null;
    	    // check the parents
    	    Collection<GKInstance> parentRefs = curP.getReferers(ReactomeJavaConstants.hasEvent);
            //System.out.println(curPathwayName + "\t" + curPathwayID);

            Collection<GKInstance> events = curP.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if (events != null) {
            	// check to make sure this isn't a super-pathway; we don't want to generate sub-instance data (rxns, etc.) from those
            	boolean hasParent = false;
            	for (GKInstance curEvent : events) {
            		// if the pathway has a child pathway, it's a superpathway
            		if (curEvent.getSchemClass().getName().equals(ReactomeJavaConstants.Pathway)) {
            			hasParent = true;
            			break;
            		}
            	}
            	if (hasParent) continue; // don't bother getting reactions, etc from a superpathway
            	for (GKInstance curEvent : events) {
                    if (curEvent.getSchemClass().getName().equals(ReactomeJavaConstants.Reaction)) {
                        curObjectID = curEvent.getDBID();
                        curObjectName = curEvent.getDisplayName();
                        
                    	// get EWAS (via Catalystactivity.PhysicalEntity)
	                    Collection<GKInstance> cas = curEvent.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
	                    if (cas != null) {
	                    	for (GKInstance curCA : cas) {
	                            GKInstance pe = (GKInstance)curCA.getAttributeValue(ReactomeJavaConstants.physicalEntity);
	                            if (pe != null) {
	                                //System.out.println(pe.getAttributeValue(ReactomeJavaConstants.name) + "\t" + curPathwayID + "\t" + curPathwayName);

	                                curObjectID = pe.getDBID();
	                                curObjectName = pe.getDisplayName();
	                                
	                                // is EWAS? get refEntity (RGP) identifier (Uniprot) and stop
	                                if (pe.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
	                                	GKInstance re = (GKInstance)pe.getAttributeValue(ReactomeJavaConstants.referenceEntity);
	                                	if (re != null) {
	                                		String rgpIdentifier = re.getAttributeValue(ReactomeJavaConstants.identifier).toString();
	                                		if (rgpIdentifier != null) {
	        	                                System.out.println(rgpIdentifier + "\t" + curPathwayID + "\t" + curPathwayName);
	                                			count++;
	                                		}
	                                	}
	                                }
	                                // else, is DefinedSet?
	                                else {
		                                if (pe.getSchemClass().getName().equals(ReactomeJavaConstants.DefinedSet)) {
		                                	// get hasMember collection
		                                	List<GKInstance> members = (List<GKInstance>)pe.getAttributeValuesList(ReactomeJavaConstants.hasMember);
		                                	for (GKInstance member : members) {
		                                		// is EWAS? get refEntity (RGP) identifier (Uniprot)
		    	                                if (member.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
				                                	GKInstance re = (GKInstance)member.getAttributeValue(ReactomeJavaConstants.referenceEntity);
				                                	if (re != null) {
				                                		if (re.getAttributeValue(ReactomeJavaConstants.identifier) != null) {
					                                		String rgpIdentifier = re.getAttributeValue(ReactomeJavaConstants.identifier).toString();
					                                		if (rgpIdentifier != null) {
					        	                                System.out.println(rgpIdentifier + "\t" + curPathwayID + "\t" + curPathwayName);
					                                			count++;
					                                		}
				                                		}	
				                                	}
		    	                                // is it a Complex?
		    	                                } else {
		    	                                	// get the EWAS
		    		                                if (member.getSchemClass().getName().equals(ReactomeJavaConstants.Complex)) {
		    		                                	// get hasMember collection
		    		                                	List<GKInstance> cPXmembers = (List<GKInstance>)pe.getAttributeValuesList(ReactomeJavaConstants.hasMember);
		    		                                	for (GKInstance cPXmember : cPXmembers) {
		    		                                		// is EWAS? get refEntity (RGP) identifier (Uniprot)
		    		    	                                if (cPXmember.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
		    				                                	GKInstance re = (GKInstance)cPXmember.getAttributeValue(ReactomeJavaConstants.referenceEntity);
		    				                                	if (re != null) {
		    				                                		String rgpIdentifier = re.getAttributeValue(ReactomeJavaConstants.identifier).toString();
		    				                                		if (rgpIdentifier != null) {
		    				        	                                System.out.println(rgpIdentifier + "\t" + curPathwayID + "\t" + curPathwayName);
		    				                                			count++;
		    				                                		}
		    				                                	}
		    		    	                                }
		    		                                	}
		    		                                }
		                                		}
		                                	}
	                                	}
	                                }
	                            }
	                    	}
	                    }
                    }
            	}
            }
        }
        System.out.println("Total RGPs: " + count);
    }

    private void dumpRGPsBinnedByPathway() throws Exception {
    	int count = 0;

    	// get pathways
        Collection<?> pathways = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
        for (Iterator<?> itP = pathways.iterator(); itP.hasNext();) {
            GKInstance curP = (GKInstance) itP.next();
            Long curPathwayID = curP.getDBID();
			String curStableID = ((GKInstance)curP.getAttributeValue(ReactomeJavaConstants.stableIdentifier)).getDisplayName();
            String curPathwayName = curP.getDisplayName();
            String curPathwaySpeciesName = ((GKInstance)curP.getAttributeValue(ReactomeJavaConstants.species)).getDisplayName();
            Long curObjectID = curPathwayID;
            String curObjectName = curPathwayName;
    	    Long curMultiLevelPathwayParent = null;
    	    // check the+ parents
    	    Collection<GKInstance> parentRefs = curP.getReferers(ReactomeJavaConstants.hasEvent);
            //System.out.println(curPathwayName + "\t" + curPathwayID);

            Collection<GKInstance> events = curP.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if (events != null) {
            	// check to make sure this isn't a super-pathway; we don't want to generate sub-instance data (rxns, etc.) from those
            	boolean hasParent = false;
            	for (GKInstance curEvent : events) {
            		// if the pathway has a child pathway, it's a superpathway
            		if (curEvent.getSchemClass().getName().equals(ReactomeJavaConstants.Pathway)) {
            			hasParent = true;
            			break;
            		}
            	}
            	if (hasParent) continue; // don't bother getting reactions, etc from a superpathway
            	
            	//ArrayList<Long> pFilter = new ArrayList<Long>(); // JP used to grab a specific pathway's RGPs
            	//pFilter.add(3899351L);
            	//pFilter.add(5225756L);
            	//pFilter.add(5608118L);
            	//pFilter.add(5632095L);
            	
            	//if (pFilter.contains(curPathwayID)) {
	        		// get all PhysicalEntities from this pathway
	            	Set<GKInstance> allPEs = InstanceUtilities.grepRefPepSeqsFromPathway(curP);
	                if (allPEs != null) {
	                	for (GKInstance curPE : allPEs) {
	                		if (curPE.getAttributeValue(ReactomeJavaConstants.identifier) != null) {
		                		String rgpIdentifier = curPE.getAttributeValue(ReactomeJavaConstants.identifier).toString();
			            		if (rgpIdentifier != null) {
		            				//System.out.println(rgpIdentifier + "\t" + curPathwayID + "\t" + curPathwayName + "\t" + curPathwaySpeciesName);
		            				System.out.println(curPathwayID + "\t" + curPathwayName + "\t" + curPathwaySpeciesName + "\t" + rgpIdentifier);
									//System.out.println(((curStableID != null) ? curStableID : curPathwayID) + "\t" + curPathwayName + "\t" + curPathwaySpeciesName + "\t" + rgpIdentifier);
		            				//System.out.println(rgpIdentifier); // JP used to grep a specific pathway's RGPs
			            			count++;
			            		}
	                		}
	                	}
	                }
            	//}
            }
        }
        System.out.println("Total RGPs: " + count);
    }

    // generate a tab-del index list of Pathway, Reaction, EWAS, and SE names and IDs found in Pathway diagrams 
    // for the Gramene Solr search index
    private void dumpPathwayDiagramTermsForGrameneSearchIndex() throws Exception {
        // NOTE: assumes dbAdaptor b/c you want to use fetchInstancesByClass()
    	Integer count = 0;
    	String searchTerms = null;

    	// new approach: if pathway does not have at least one child pathway, then generate a row, otherwise: "next..."
    	
    	// special case for certain multi-level pathway ids (an inadvisable hack, btw)
	    List<Long> multiLevelPathwaysIDs  = new ArrayList<Long>();
	    multiLevelPathwaysIDs.add(1112967L);
	    multiLevelPathwaysIDs.add(3899368L);
	    multiLevelPathwaysIDs.add(5225808L);
        System.out.println("Pathway Name\tSearch Terms\tPathway ID\tObject ID");

        // get pathways
        Collection<?> pathways = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
        for (Iterator<?> itP = pathways.iterator(); itP.hasNext();) {
            GKInstance curP = (GKInstance) itP.next();
            count++;
            Long curPathwayID = curP.getDBID();
    	    Long curMultiLevelPathwayParent = null;
    	    // check the parents
    	    Collection<GKInstance> parentRefs = curP.getReferers(ReactomeJavaConstants.hasEvent);
            if (parentRefs != null) {
            	for (GKInstance ref : parentRefs) {
            		if (multiLevelPathwaysIDs.contains(ref.getDBID())) {
            			curMultiLevelPathwayParent = ref.getDBID();
            		}
            	}
            }
            if (curMultiLevelPathwayParent != null) {
            	System.out.println(curP.getDisplayName() + "\t" + curMultiLevelPathwayParent + "\t" + curPathwayID);
            }
            else {
                System.out.println(curP.getDisplayName() + "\t" + curPathwayID + "\t" + curPathwayID);
    		}
    		// get reactions
            Collection<GKInstance> events = curP.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if (events != null) {
            	for (GKInstance curEvent : events) {
                    if (curEvent.getSchemClass().getName().equals(ReactomeJavaConstants.Reaction)) {
	                    count++;
	                    if (curMultiLevelPathwayParent != null) {
	                    	System.out.println(curEvent.getDisplayName() + "\t" + curMultiLevelPathwayParent + "\t" + curEvent.getDBID());
	                    }
	                    else {
	                    	System.out.println(curEvent.getDisplayName() + "\t" + curPathwayID + "\t" + curEvent.getDBID());
	                    }
	            		// get EWAS (via Catalystactivity.PhysicalEntity)
	                    Collection<GKInstance> cas = curEvent.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
	                    if (cas != null) {
	                    	for (GKInstance curCA : cas) {
	                            GKInstance pe = (GKInstance)curCA.getAttributeValue(ReactomeJavaConstants.physicalEntity);
	                            if (pe != null) {
			                                count++;
			                                System.out.println(pe.getAttributeValue(ReactomeJavaConstants.name) + "\t" + curPathwayID + "\t" + pe.getDBID());
	                            }
	                        }
	                    }
	                    // get SimpleEntities (or any and all Physical Entities acting as inputs and outputs)
	                    Collection<GKInstance> inputs = curEvent.getAttributeValuesList(ReactomeJavaConstants.input);
	                    Collection<GKInstance> outputs = curEvent.getAttributeValuesList(ReactomeJavaConstants.output);
	                    Collection<GKInstance> SEs = inputs;
	                    SEs.addAll(outputs);
	                    if (SEs!= null) {
		                    for (GKInstance entity : SEs) {
	                                count++;
	                                System.out.println(entity.getAttributeValue(ReactomeJavaConstants.name) + "\t" + curPathwayID + "\t" + entity.getDBID());
		                    }
	                    }
                    }
                }
            }
            curMultiLevelPathwayParent = null;
            searchTerms = "";
        }
        logger.info("Number of indexed instances: " + count);
    }

    // generate a tab-del index list of all useful object names and IDs in Plant Reactome
    // for the Gramene Solr search index
    private void dumpQuickSearchTermsForGrameneSearchIndex() throws Exception {
        // NOTE: assumes dbAdaptor b/c you want to use fetchInstancesByClass()
    	// set up list of classes for which you want object names & ids
	    String[] classNames = new String[] {
	            ReactomeJavaConstants.CatalystActivity,
	            ReactomeJavaConstants.Pathway,
	            ReactomeJavaConstants.Reaction,
	            ReactomeJavaConstants.GO_BiologicalProcess,
	            ReactomeJavaConstants.GO_MolecularFunction,
	            ReactomeJavaConstants.EntityCompartment,
	            ReactomeJavaConstants.Complex,
	            ReactomeJavaConstants.DefinedSet,
	            ReactomeJavaConstants.EntityWithAccessionedSequence,
	            ReactomeJavaConstants.SimpleEntity,
	            ReactomeJavaConstants.LiteratureReference,
	            ReactomeJavaConstants.ReferenceMolecule,
	            ReactomeJavaConstants.ReferenceGeneProduct
	    };
    	Integer count = 0;
        System.out.println("Search Term\tContext");
    	// get objects by class
        for (String className : classNames) {
            Collection<?> instances = dbAdaptor.fetchInstancesByClass(className);
            for (Iterator<?> it = instances.iterator(); it.hasNext();) {
                GKInstance curI = (GKInstance) it.next();
                count++;
                System.out.println(curI.getDisplayName() + "\tPlant Reactome (" + curI.getSchemClass().getName() + ")");
            }
        }
        logger.info("Number of indexed instances: " + count);
    }
    
    // generate raw HTML table containing aggregate projection counts by species
    // format: Species Name, Pathways, Reactions, Gene Products
    private void dumpProjectionStats(boolean outputHTML) throws Exception {

    	class speciesNameComparator implements Comparator<GKInstance>{
    	    @Override
    	    public int compare(GKInstance s1, GKInstance s2) {
    	        return s1.getDisplayName().compareToIgnoreCase(s2.getDisplayName());
    	    }
    	}
    	
    	StringBuilder sb = new StringBuilder();
    	
    	if (outputHTML)
    		sb.append("<table border=1 cellpadding=3>"
    				+ "\n" + "<tr bgcolor=\"#C2D998\"><td><b>Species</b></td><td><b>Pathways</b></td><td><b>Reactions</b></td><td><b>Gene Products</b></td></tr>"
    				);
    	else
    		sb.append("Species\tPathways\tReactions\tGene Products");
    	
    	// get list of species, sort it, place Oryza at the top
        Collection<GKInstance> speciesColl = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Species);
        List<GKInstance> speciesList = new ArrayList();
        for (GKInstance speciesIns : speciesColl) {
        	//if (speciesIns.getDBID() != ""
        	speciesList.add(speciesIns);
        }
        Collections.sort(speciesList, new speciesNameComparator());

    	// iterate through species
        for (Iterator<?> itS = speciesList.iterator(); itS.hasNext();) {
            GKInstance curS = (GKInstance) itS.next();
    		// gather count of pathways
            Collection<?> pathways = dbAdaptor.fetchInstanceByAttribute(
            		ReactomeJavaConstants.Pathway, 
            		ReactomeJavaConstants.species, 
					"=",
					curS);
    		// gather count of reactions
            Collection<?> reactions = dbAdaptor.fetchInstanceByAttribute(
            		ReactomeJavaConstants.Reaction, 
            		ReactomeJavaConstants.species, 
					"=",
					curS);
    		// gather count of gene products (RGPs)
            Collection<?> RGPs = dbAdaptor.fetchInstanceByAttribute(
            		ReactomeJavaConstants.ReferenceGeneProduct, 
            		ReactomeJavaConstants.species, 
					"=",
					curS);
            // format this species' stats for printing
            if (pathways.size() > 0) 
            	if (outputHTML)
            		sb.append("\n" + "<tr><td>" + curS.getDisplayName() + "</td><td>" + pathways.size() + "</td><td>" + reactions.size() + "</td><td>" + RGPs.size() + "</td></tr>");
            	else
            		sb.append("\n" + curS.getDisplayName() + "\t" + pathways.size() + "\t" + reactions.size() + "\t" + RGPs.size());
        }
    	if (outputHTML)
    		sb.append("\n" + "</table>");
    	else
    		sb.append("\n");
    	System.out.println(sb.toString());
    }

	private class speciesNameComparator implements Comparator<GKInstance>{
		@Override
		public int compare(GKInstance s1, GKInstance s2) {
			return s1.getDisplayName().compareToIgnoreCase(s2.getDisplayName());
		}
	}

	// generate raw binary table containing presence/absence of reaction by species
    private void dumpRiceProjectionReactionTable() throws Exception {

    	StringBuilder sb = new StringBuilder();
    	sb.append("Reaction\tOryza sativa");

    	// build the list of rice species names
        Collection<GKInstance> speciesColl = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Species);
        List<GKInstance> speciesList = new ArrayList();
        for (GKInstance speciesIns : speciesColl) {
        	//if (target_taxa.contains(speciesIns)) {
	        	speciesList.add(speciesIns);
        	//}
        }
        Collections.sort(speciesList, new speciesNameComparator());

        for (GKInstance species : speciesList) {
        	sb.append("\t" + species.getDisplayName());
        }
        sb.append("\n");
    	
    	// iterate through O.sativa reactions
        GKInstance Osativa = dbAdaptor.fetchInstance(186860L);
        Collection<?> OSreactions = dbAdaptor.fetchInstanceByAttribute(
        		ReactomeJavaConstants.Reaction, 
        		ReactomeJavaConstants.species, 
				"=",
				Osativa);
        for (Iterator<?> itR = OSreactions.iterator(); itR.hasNext();) {
            GKInstance curR = (GKInstance) itR.next();
            sb.append(curR.getDisplayName() + "\t1");
            
            // get orthologousEvents for current Reaction, look for a species match in each one
            Collection<GKInstance> orthoEvents = curR.getAttributeValuesList(ReactomeJavaConstants.orthologousEvent);
            if (orthoEvents.size() > 0) {
                for (GKInstance curPS : speciesList) {
                	boolean isPresent = false;
		            for (Iterator<?> itOE = orthoEvents.iterator(); itOE.hasNext();) {
		                GKInstance curOE = (GKInstance) itOE.next();
		                GKInstance curOES = (GKInstance)curOE.getAttributeValue(ReactomeJavaConstants.species);
		                // look in each projected species for each orthoEvent
	    				if (curPS.equals(curOES))
	    					isPresent = true;
	                }
		            if (isPresent)
                    	sb.append("\t1");
                    else
                    	sb.append("\t0");
	            }
            }
            else { // fill out the non-projected species
                for (GKInstance curPS : speciesList)
                	sb.append("\t0");
            }
            sb.append("\n");
        }            
        sb.append("\n");
    	System.out.println(sb.toString());
    }

    private String buildProjectedReactionsRow(Collection<GKInstance> curPathways, String OsRXNname, Long OsRXNid,
    										Long PrjRxnID, 
    										String speciesName, String speciesID, GKInstance physEnt) throws Exception {
    	String refGeneDetails = "";

    	GKInstance re = (GKInstance)physEnt.getAttributeValue(ReactomeJavaConstants.referenceEntity);
    	if (re != null) {
			String refGeneName = "";
			String refRGPDBID = "";
    		// get ref species EWAS, RGP id, and Os id
    		GKInstance refEWAS = (GKInstance)physEnt.getAttributeValue(ReactomeJavaConstants.inferredFrom);
    		if (refEWAS != null) {
    			GKInstance refRGP = (GKInstance)refEWAS.getAttributeValue(ReactomeJavaConstants.referenceEntity);
    			if (refRGP != null) {
    				if (refRGP.getAttributeValue(ReactomeJavaConstants.geneName) != null) {
        				List<String> refGeneNames = (List<String>)refRGP.getAttributeValuesList(ReactomeJavaConstants.geneName);
        				if (refGeneNames != null) {
            				for (String rGN : refGeneNames) {
            					if (rGN.toUpperCase().startsWith("OS") && rGN.length() == 12) { 
            						refGeneName = rGN;
            						break;
            					}
            				}
            				if (refGeneName.isEmpty())
            					refGeneName = refGeneNames.get(0);
        				}
    				}
        			refRGPDBID = refRGP.getDBID().toString();
    			}
    		}
    		String rgpIdentifier = re.getAttributeValue(ReactomeJavaConstants.identifier).toString();

    		// loop through pathways to generate all pathway-reaction-gene correlations 
    		if (curPathways != null) { 
	            for (Iterator<?> itP = curPathways.iterator(); itP.hasNext();) {
	                GKInstance curP = (GKInstance) itP.next();
	                
	                if (curP != null) {
	                	String OsPwyName = (curP.getDisplayName() != null ? curP.getDisplayName() : "no name");
	                	String OsPwyID = (curP.getDBID() != null ? curP.getDBID().toString() : "no id");
		
			    		if (rgpIdentifier != null) {
			    			refGeneDetails = OsPwyName + "\t" + OsPwyID + "\t"
			    							+ OsRXNname + "\t" + OsRXNid + "\t" + PrjRxnID + "\t"
											+ speciesName + "\t" + speciesID + "\t"
											+ refGeneName + "\t" + refRGPDBID + "\t"
											+ rgpIdentifier + "\t" + re.getDBID() + "\n";
			    		}
	                }
	            }
            }
    	}
    	//System.out.print(refGeneDetails);
    	return refGeneDetails;
    }
    
    // generate table containing genes, RGP ids, projected reactions (IDs), and reference reactions (names and IDs)
    private void exportReactionProjectionTable() throws Exception {

    	class speciesNameComparator implements Comparator<GKInstance>{
    	    @Override
    	    public int compare(GKInstance s1, GKInstance s2) {
    	        return s1.getDisplayName().compareToIgnoreCase(s2.getDisplayName());
    	    }
    	}
    	
    	StringBuilder sb = new StringBuilder();
    	sb.append("Os Pathway Name\t"
    			+ "Os Pathway DB ID\t"
    			+ "Os Reaction Name\t"
				+ "Os Reaction DB ID\t"
				+ "Proj Reaction DB ID\t"
				+ "Proj Reaction Species\t"
				+ "Proj Reaction Species ID\t"
				+ "Os Gene Product Locus ID\t"
				+ "Os Gene Product DB ID\t"
				+ "Proj Gene Product Locus ID\t"
				+ "Proj Gene Product DB ID");

    	// build the list of rice species names
        Collection<GKInstance> speciesColl = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Species);
        List<GKInstance> speciesList = new ArrayList();
        for (GKInstance speciesIns : speciesColl) {
        	if (target_taxa.contains(speciesIns)) {
	        	speciesList.add(speciesIns);
        	}
        }
        Collections.sort(speciesList, new speciesNameComparator());

        //for (GKInstance species : speciesList) {
        //	sb.append("\t" + species.getDisplayName());
        //}
        sb.append("\n");
    	
    	// iterate through O.sativa reactions
        GKInstance Osativa = dbAdaptor.fetchInstance(186860L);
        Collection<?> OSreactions = dbAdaptor.fetchInstanceByAttribute(
        		ReactomeJavaConstants.Reaction, 
        		ReactomeJavaConstants.species, 
				"=",
				Osativa);
        for (Iterator<?> itR = OSreactions.iterator(); itR.hasNext();) {
            GKInstance curR = (GKInstance) itR.next();

            /*
             * Grab all pathways for this reaction, pass to all calls to buildProjectedReactionsRow().
             * This is designed for iterative efficiency, since we are starting with reactions and working in both
             * directions (up to pathways and down to gene products). 
             */
            Collection<GKInstance> curPathways = curR.getReferers(ReactomeJavaConstants.hasEvent);

       		// get orthologousEvents for current Reaction
            Collection<GKInstance> orthoEvents = curR.getAttributeValuesList(ReactomeJavaConstants.orthologousEvent);
            if (orthoEvents.size() > 0) {
	            for (Iterator<?> itOE = orthoEvents.iterator(); itOE.hasNext();) {
	                GKInstance curOE = (GKInstance) itOE.next();
	                GKInstance curOES = (GKInstance)curOE.getAttributeValue(ReactomeJavaConstants.species);

	                Long curObjectID;
	                String curObjectName;
	                //Integer count = 0;
	                
                	// get EWAS (via Catalystactivity.PhysicalEntity)
                    Collection<GKInstance> cas = curOE.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
                    if (cas != null) {
                    	for (GKInstance curCA : cas) {
                            GKInstance pe = (GKInstance)curCA.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                            if (pe != null) {
                                //System.out.println(pe.getAttributeValue(ReactomeJavaConstants.name) + "\t" + curPathwayID + "\t" + curPathwayName);

                                curObjectID = pe.getDBID();
                                curObjectName = pe.getDisplayName();
                                
                                // is EWAS? get refEntity (RGP) identifier (Uniprot) and stop
                                if (pe.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
                                	sb.append(
                               			buildProjectedReactionsRow(
                               				curPathways,curR.getDisplayName(),curR.getDBID(),curOE.getDBID(),curOES.getDisplayName(),curOES.getDBID().toString(),pe));
                                }
                                // else, is DefinedSet?
                                else {
	                                if (pe.getSchemClass().getName().equals(ReactomeJavaConstants.DefinedSet)) {
	                                	// get hasMember collection
	                                	List<GKInstance> members = (List<GKInstance>)pe.getAttributeValuesList(ReactomeJavaConstants.hasMember);
	                                	for (GKInstance member : members) {
	                                		// is EWAS? get refEntity (RGP) identifier (Uniprot)
	    	                                if (member.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
	    	                                	sb.append(
	                                				buildProjectedReactionsRow(
	                                						curPathways,curR.getDisplayName(),curR.getDBID(),curOE.getDBID(),curOES.getDisplayName(),curOES.getDBID().toString(),member));
	    	                                // is it a Complex?
	    	                                } else {
	    	                                	// get the EWAS
	    		                                if (member.getSchemClass().getName().equals(ReactomeJavaConstants.Complex)) {
	    		                                	// get hasMember collection
	    		                                	List<GKInstance> cPXmembers = (List<GKInstance>)pe.getAttributeValuesList(ReactomeJavaConstants.hasMember);
	    		                                	for (GKInstance cPXmember : cPXmembers) {
	    		                                		// is EWAS? get refEntity (RGP) identifier (Uniprot)
	    		    	                                if (cPXmember.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
	    		    	                                	sb.append(
		    		                                   			buildProjectedReactionsRow(
		    		                                   					curPathways,curR.getDisplayName(),curR.getDBID(),curOE.getDBID(),curOES.getDisplayName(),curOES.getDBID().toString(),cPXmember));
	    		    	                                }
	    		                                	}
	    		                                }
	                                		}
	                                	}
                                	}
                                }
                            }
                    	}
                    }
                }
            }
        }            
        //sb.append("\n");
    	System.out.println(sb.toString());
    }
    
    // for UniProt-identified entities, displayNames and other attributes have stale LOCs from old, pre-UniProt settings
    private void removeStaleLOCs() throws Exception {

    	int count = 0;
    	class speciesNameComparator implements Comparator<GKInstance>{
    	    @Override
    	    public int compare(GKInstance s1, GKInstance s2) {
    	        return s1.getDisplayName().compareToIgnoreCase(s2.getDisplayName());
    	    }
    	}
    	
    	// build the list of rice species names
        Collection<GKInstance> speciesColl = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Species);
        List<GKInstance> speciesList = new ArrayList();
        for (GKInstance speciesIns : speciesColl) {
        	if (target_taxa.contains(speciesIns)) {
	        	speciesList.add(speciesIns);
        	}
        }
        Collections.sort(speciesList, new speciesNameComparator());
        
        // iterate through each species
        for (GKInstance curSpecies : speciesList) { 
	    	// gather rice EWAS with LOC in displayName (Os, Osj, or Osi)
	        Collection<?> OsEWAS = dbAdaptor.fetchInstanceByAttribute(
	        		ReactomeJavaConstants.EntityWithAccessionedSequence, 
	        		ReactomeJavaConstants.species, 
					"=",
					curSpecies);
	        for (Iterator<?> itEWAS = OsEWAS.iterator(); itEWAS.hasNext();) {
	            GKInstance curEWAS = (GKInstance) itEWAS.next();
	        	if (curEWAS.getDisplayName().contains(" (LOC")) {
		        	System.out.println(curEWAS.getAttributeValue(ReactomeJavaConstants.species).toString() 
		        			+ " " + curEWAS.getDisplayName());
		        	//count++;
			    	// remove LOC from .name(0), place in new, secondary .name slot
		        	if (curEWAS.getAttributeValuesList(ReactomeJavaConstants.name) != null) {
		        		List<String> names = curEWAS.getAttributeValuesList(ReactomeJavaConstants.name);
		    			for (Iterator<String> it = names.iterator(); it.hasNext();) {
		    	            String currName = (String)it.next();
		    	            // grab the curated rice RGPs with LOC identifiers for mapping (majority)  
		    	            if (currName.contains(" (LOC")) {
		    	            	//System.out.println("\t" + currName.toString());
		    	            	
		    	            	String[] nameParts = currName.split(" ");
		    	            	String trimmedName = new String();
		    	            	int count_names = 0;
		    	            	for (String namePart : nameParts) {
		    	            		if (count_names < nameParts.length-1)
		    	            			trimmedName += " " + namePart;
		    	            		count_names++;
		    	            	}
		    	            	String LOCName = nameParts[nameParts.length-1].replace("(", "").replace(")", "");
				    	        //System.out.println("\tTrimmed name: " + trimmedName);
				    	        //System.out.println("\tLOC name: " + LOCName);
		    	            	names.remove(0);
		    	            	names.add(0, trimmedName.trim());
		    	            	names.add(LOCName);

			    	            // reset displayName
					        	curEWAS.setDisplayName(trimmedName 
					        			+ (curEWAS.getAttributeValue(ReactomeJavaConstants.compartment) != null ? " [" + ((GKInstance)curEWAS.getAttributeValue(ReactomeJavaConstants.compartment)).getDisplayName() + "]" : "")
					        			);
				    	        //InstanceDisplayNameGenerator.setDisplayName(curEWAS);

					        	count_names = 0;
		    	            	break;
		        			}
		        		}
		        	}
			    	// gather affected CAs and reset their displayName as well - HOLD
	            	changedInsts.add(curEWAS);
	        	}
	        }
        }
        //System.out.println("Count: " + count); 
    }

	// generate raw table containing species-specific gene counts binned by pathway
	private void dumpGeneCountsInPathwaysBySpecies() throws Exception {

        System.out.print("Pathway\tPathwayID\tOryza sativa");

		// build the list of rice species names
		Collection<GKInstance> speciesColl = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Species);
		List<GKInstance> speciesList = new ArrayList();
		for (GKInstance speciesIns : speciesColl) {
			//if (target_taxa.contains(speciesIns)) {
			speciesList.add(speciesIns);
			//}
		}
		Collections.sort(speciesList, new speciesNameComparator());

		for (GKInstance species : speciesList) {
            System.out.print("\t" + species.getDisplayName());
		}
        System.out.print("\n");

		// collect the O.sativa pathways
		GKInstance Osativa = dbAdaptor.fetchInstance(186860L);
		Collection<?> OSpathways = dbAdaptor.fetchInstanceByAttribute(
				ReactomeJavaConstants.Pathway,
				ReactomeJavaConstants.species,
				"=",
				Osativa);

        // iterate over the OS pathways; filtering for container pathways
        int count = 0;
        Set<GKInstance> nRGPs = null; // RGP container

		for (Iterator<?> itP = OSpathways.iterator(); itP.hasNext();) {
			GKInstance curP = (GKInstance) itP.next();

            boolean hasChildPath = false;
            // check for child pathways; if present, filter this pathway out. we only want terminal pathways
            Collection<GKInstance> childEvents = curP.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if (childEvents != null) {
                for (GKInstance event : childEvents) {
                    if (event.getSchemClass() == curP.getSchemClass()) {
                        hasChildPath = true;
                        break;
                    }
                }
            }
            if (!hasChildPath) {
                // write pathway info
                System.out.print(curP.getDisplayName() + "\t" + ((GKInstance) curP.getAttributeValue(ReactomeJavaConstants.stableIdentifier)).getDisplayName() + "\t");
                //count++;
                // gather and write Os genes for this pathway
                nRGPs = InstanceUtilities.grepRefPepSeqsFromPathway(curP);
                System.out.print((nRGPs != null ? nRGPs.size() : "0") + "\t");
                //System.out.print("\n");
                nRGPs = null; // reset for use with projected species


                // get orthologousEvents for current pathway, look for a species match in each one
                Collection<GKInstance> orthoEvents = curP.getAttributeValuesList(ReactomeJavaConstants.orthologousEvent);
                if (orthoEvents.size() > 0) {
                    for (GKInstance curPS : speciesList) {
                        boolean isPresent = false;
                        for (Iterator<?> itOE = orthoEvents.iterator(); itOE.hasNext();) {
                            isPresent = false;
                            nRGPs = null;
                            GKInstance curOE = (GKInstance) itOE.next();
                            GKInstance curOES = (GKInstance)curOE.getAttributeValue(ReactomeJavaConstants.species);
                            // look in each projected species for each orthoEvent
                            if (curPS.equals(curOES)) {
                                isPresent = true;
                                // generate count of gene products in this species for this reaction
                                nRGPs = InstanceUtilities.grepRefPepSeqsFromPathway(curOE);
                            }
                            if (isPresent)
                                System.out.print("\t" + (nRGPs != null ? nRGPs.size() : ""));
                        }
                    }
                    System.out.print("\n");
                }
                else { // placeholder for non-projected species
                    for (GKInstance curPS : speciesList)
                        System.out.print("\t0");
                    System.out.print("\n");
                }
            }
        }
		//sb.append("Os Pathway count: " + count + "\n");
	}

	/**
	 * Constructor: Establish logger and configs.
	 */
    public CuratorUtilities() throws Exception
    {
		logger.info("Initializing utilities...");
        this.loadConfigs("resources/CuratorUtilities.xml");
    }

	public static void main(String[] args) throws Exception
	{
        try
        {
    		PropertyConfigurator.configure("resources/log4j.properties"); // enable logging

	        CuratorUtilities run_utilities = new CuratorUtilities();

	        // call any or all of these utilities, depending on what you're trying to accomplish...
	        //run_utilities.testUpdate1();
	        //run_utilities.testUpdate2(run_utilities.target_instances);
	        //run_utilities.updateRGPsWithUniProtKBData();
	        //run_utilities.listRiceRGPs(true); // for PR data releases; pre-projection
	        //run_utilities.listAthRGPs();
	        //run_utilities.deleteReactomeDataByInstanceEdit();
	        //run_utilities.profileAthIsoforms();
	        //run_utilities.listCompartments();
	        //run_utilities.listRegulationTypes();
	        //run_utilities.listPathways(true);
	        //run_utilities.listAthEWASCompartments();
	        //run_utilities.profileCompartmentDistribution();
	        //run_utilities.profileCompartmentDistributionWithSUBA();
	        //run_utilities.profileDupeSEs();
	        //run_utilities.compareRefMols();
	        //run_utilities.listNewRefMols();
	        //run_utilities.grameneSolrExporter(); // for PR data releases - v2, obsolete
	        //run_utilities.dumpRGPsBinnedByPathwayOld();
	        //run_utilities.dumpRGPsBinnedByPathway(); // for PR data releases
	        //run_utilities.dumpPathwayDiagramTermsForGrameneSearchIndex();
	        //run_utilities.dumpQuickSearchTermsForGrameneSearchIndex();
	        //run_utilities.dumpProjectionStats(false); // for PR data releases - stats page
	        //run_utilities.exportReactionProjectionTable(); // for PR data releases - Gramoogle
	        //run_utilities.removeStaleLOCs();
			//run_utilities.dumpRiceProjectionReactionTable();
			run_utilities.dumpGeneCountsInPathwaysBySpecies();
	        // create and attach IE to changes; commit changes
    		//run_utilities.commitChanges();
        }
        catch(Exception e) {
            e.printStackTrace();
    		logger.info("Terminating prematurely.");
        }
		logger.info("Finished.");
	}
}
