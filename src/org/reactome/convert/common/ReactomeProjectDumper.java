/*
 * Created on Aug 25, 2006
 *
 */
package org.reactome.convert.common;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.gk.database.DefaultInstanceEditHelper;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.slicing.SlicingEngine;
import org.gk.util.GKApplicationUtilities;

/**
 * This class is used to dump one or more rtpj projects into the database.
 * @author guanming
 *
 */
public class ReactomeProjectDumper {
    private static Logger logger = Logger.getLogger(ReactomeProjectDumper.class);
    
    private Long maxId;
    private MySQLAdaptor dbAdaptor;
    private Long defaultPersonId;
    
    public ReactomeProjectDumper() {
        setUp();
    }
    
    protected void setUp() {
        PropertyConfigurator.configure("resources/log4j.properties");
    }
    
    public void setMySQLAdaptor(MySQLAdaptor dba) {
        this.dbAdaptor = dba;
    }
    
    /**
     * All ids should be higher than this passed id.
     * @param maxId
     */
    public void setMaxID(Long maxId) {
    }
    
    public void setDefaultPersonId(Long dbId) {
        this.defaultPersonId = dbId;
    }
    
    public void dumpToDB(String[] projectFileNames) throws Exception {
        if (dbAdaptor == null)
            throw new IllegalStateException("Database adaptor is not set!");
        SlicingEngine slicingEngine = new SlicingEngine();
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        Long maxId = null;
        List<GKInstance> toBeStored = new ArrayList<GKInstance>();
        for (String fileName : projectFileNames) {
            long time1 = System.currentTimeMillis();
            logger.info("Starting dumping " + fileName + "...");
            toBeStored.clear();
            fileAdaptor.setSource(fileName);
            // Get the maximum DBID used in the database
            maxId = getMaximumDBID();
            logger.info("Max DB_ID: " + maxId);
            // Reset the DB_ID for newly created GKInstances
            preProcessInstances(fileAdaptor, 
                                maxId, 
                                toBeStored);
            // Store these newly created GKInstances
            storeInstances(toBeStored, slicingEngine);
            logger.info("Done!");
            long time2 = System.currentTimeMillis();
            logger.info("Time used for dumping: " + (time2 - time1));
            // Keep a copy of the project file after dump
            storeDumpedProject(fileAdaptor, toBeStored);
        }
    }
    
    private void storeDumpedProject(XMLFileAdaptor fileAdaptor,
                                    List<GKInstance> stored) throws Exception {
        // Get a file name
        String fileName = fileAdaptor.getSourceName();
        int index = fileName.lastIndexOf(".");
        fileName = fileName.substring(0, index) + "_Dumped.rtpj";
        // Dirty flag
        for (GKInstance inst : stored) {
            inst.setIsDirty(false);
        }
        fileAdaptor.save(fileName);
    }
    
    public void storeInstances(List<GKInstance> toBeStored,
                               SlicingEngine slicingEngine) throws Exception {
        boolean isTnSupported = dbAdaptor.supportsTransactions();
        if (isTnSupported)
            dbAdaptor.startTransaction();
        try {
            for (GKInstance instance : toBeStored) {
                logger.info("Store instance: " + instance);
                slicingEngine.storeInstance(instance, dbAdaptor);
            }
            if (isTnSupported)
                dbAdaptor.commit();
        }
        catch (Exception e) {
            dbAdaptor.rollback();
            throw e;
        }
    }
    
    private void preProcessInstances(XMLFileAdaptor fileAdaptor,
                                     Long maxId,
                                     List<GKInstance> toBeStored) throws Exception {
        Collection collection = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        GKInstance gkInstance = null;
        Long oldMaxId = maxId;
        for (Iterator it = collection.iterator(); it.hasNext();) {
            gkInstance = (GKInstance) it.next();
            if (gkInstance.getDBID() > 0) {
                // Check if this old instance is in the database
                GKInstance dbInstance = dbAdaptor.fetchInstance(gkInstance.getDBID());
                if (dbInstance == null)
                    toBeStored.add(gkInstance);
                continue; // Old GKInstances copied from the database
            }
            maxId ++;
            gkInstance.setDBID(maxId);
            toBeStored.add(gkInstance);
        }
        // Attach IEs for all instances to be stored
        if (defaultPersonId != null) {
            DefaultInstanceEditHelper ieHelper = new DefaultInstanceEditHelper();
            ieHelper.setDefaultPerson(defaultPersonId);
            PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
            PersistenceManager.getManager().setActiveMySQLAdaptor(dbAdaptor);
            GKInstance ie = ieHelper.getDefaultInstanceEdit(null);
            ie.addAttributeValue(ReactomeJavaConstants.dateTime, 
                                 GKApplicationUtilities.getDateTime());
            InstanceDisplayNameGenerator.setDisplayName(ie);
            // Attach ie
            for (GKInstance inst : toBeStored) {
                GKInstance createdIE = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.created);
                if (inst.getDBID() > oldMaxId) { // These should be new instances
                    if (createdIE != null) // This should never occur
                        inst.addAttributeValue(ReactomeJavaConstants.modified, ie); 
                    else
                        inst.setAttributeValue(ReactomeJavaConstants.created, ie);
                }
                else {
                    // These should be old instances
                    inst.addAttributeValue(ReactomeJavaConstants.modified, ie);
                }
            }
            // Don't forget to add this ie to the list
            maxId ++;
            ie.setDBID(maxId);
            toBeStored.add(ie);
        }
    }
    
    public Long getMaximumDBID() throws SQLException {
        Connection connection = dbAdaptor.getConnection();
        Statement stat = connection.createStatement();
        String query = "SELECT MAX(DB_ID) FROM DatabaseObject";
        ResultSet resultSet = stat.executeQuery(query);
        resultSet.next();
        Long id = resultSet.getLong(1);
        // Add a check so that a manual max id can be set
        if (maxId != null && id < maxId)
            return maxId;
        return id;
    }
    
    private static String[] getFileNames() {
        // Keep the order
        String[] fileNames = new String[] {
//                R3Constants.PANTHER_DIR + "Panther_2_5.rtpj",
//                R3Constants.NCI_NATURE_DIR + "NCI-Nature_Curated.rtpj",
//                R3Constants.KEGG_DIR + "KEGG.rtpj",
//                "datasets/cellmap_may_2006/CellMap.rtpj",
//                R3Constants.NATURE_PID_DIR + "BioCarta.rtpj",
//                R3Constants.INTACT_DIR + "IntAct.rtpj",
//                R3Constants.BIOGRID_DIR + "BioGrid.rtpj",
//                R3Constants.HPRD_DIR + "HPRD.rtpj"
//                R3Constants.TRED_DIR + "TRED.rtpj"
        };
        // Make sure all file existing
        for (String fileName : fileNames) {
            File file = new File(fileName);
            if (!file.exists())
                throw new IllegalStateException(fileName + " doesn't exist!");
        }
        return fileNames;
    }
    
    private static MySQLAdaptor getMySQLAdaptor() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "reactome_28_plus_i",
                                            "root",
                                            "macmysql01",
                                            3306);
        return dba;
    }
    
    public static void main(String[] args) {
        try {
            ReactomeProjectDumper dumper = new ReactomeProjectDumper();
//            MySQLAdaptor dbAdaptor = new MySQLAdaptor("localhost",
//                                                      //"reactome_plus_i_v2",
//                                                      "gk_central_121108_pid",
//                                                      "root",
//                                                      "macmysql01",
//                                                      3306);
            MySQLAdaptor dba = getMySQLAdaptor();
            dumper.setMySQLAdaptor(dba);
            // MaxID is fetched from the original gk_central database just before release
            // so that the maximum id can be used, since DB_IDs used in the file may be bigger
            // than the used database (a released one).
            dumper.setMaxID(397934L);
            String[] fileNames = getFileNames();
//            String dirName = "datasets/";
            // Keep this order!!!
//            String[] fileNames = new String[] {
//                    dirName + "Panther/Version1.3/Panther_1_3.rtpj",
//                    dirName + "cellmap_may_2006/CellMap.rtpj",
//                    dirName + "INOH/INOH.rtpj",
//                    // Note: not the latest file is used!
//                    dirName + "HPRD/PSI-MI/HPRD060106.rtpj",
//                    dirName + "BIND/BIND.rtpj",
//                    dirName + "IntAct/IntAct.rtpj",
//                    dirName + "NCI-Pathways/NCI-Nature_Curated.rtpj",
//                    dirName + "NCI-Pathways/BioCarta.rtpj",
//                    dirName + "KEGG/hsa/kegg.rtpj"
//                    dirName + "NCI-Pathways/121808/NCI-Nature_Curated.rtpj",
//            };
//            String dirName = "/Users/wgm/Documents/Manuel/";
//            String[] fileNames = new String[] {
//                    dirName + "Manuel_2.rtpj"
//            };
            dumper.dumpToDB(fileNames);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}
