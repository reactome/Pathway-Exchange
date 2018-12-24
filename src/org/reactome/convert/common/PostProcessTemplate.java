/*
 * Created on Aug 16, 2006
 *
 */
package org.reactome.convert.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gk.database.SynchronizationManager;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.SchemaClass;

public abstract class PostProcessTemplate {
    protected static final Logger logger = Logger.getLogger(PostProcessTemplate.class);
    
    public void postProcess(MySQLAdaptor dbAdaptor, 
                            XMLFileAdaptor fileAdaptor) throws Exception {
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        // Database should be update first since it is used by others.
        logger.info("Start replacing ReferenceDatabase ...");
        processReferenceDatabase(dbAdaptor, fileAdaptor);
        logger.info("Start replacing Species ...");
        processSpecies(dbAdaptor, fileAdaptor);
        logger.info("Start processing EntityCompartment...");
        processEntityCompartment(dbAdaptor, fileAdaptor);
        logger.info("Start processing EWAS...");
        processEWAS(dbAdaptor, fileAdaptor);
        logger.info("Attach dataSource...");
        attachDataSource(dbAdaptor, fileAdaptor);
        logger.info("Reset displayNames for all instances...");
        setDisplayNames(fileAdaptor);
    }
    
    protected void setDisplayNames(XMLFileAdaptor fileAdaptor) throws Exception {
        Collection collection = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        GKInstance gkInstance = null;
        for (Iterator it = collection.iterator(); it.hasNext();) {
            gkInstance = (GKInstance) it.next();
            if (gkInstance.isShell())
                continue;
            // Escape ModifiedResidue since _displayName is used for information keeping
            if (gkInstance.getSchemClass().isa(ReactomeJavaConstants.ModifiedResidue))
                continue;
            InstanceDisplayNameGenerator.setDisplayName(gkInstance);
        }
    }
    
    protected void processReferenceDatabase(MySQLAdaptor dbAdaptor,
                                            XMLFileAdaptor fileAdaptor) throws Exception {
        PostProcessHelper.useDBInstances(ReactomeJavaConstants.ReferenceDatabase, dbAdaptor, fileAdaptor);
    }
    
    protected void processSpecies(MySQLAdaptor dbAdaptor,
                                  XMLFileAdaptor fileAdaptor) throws Exception {
        PostProcessHelper.useDBInstances(ReactomeJavaConstants.Species, dbAdaptor, fileAdaptor);
    }
    
    protected abstract void processEntityCompartment(MySQLAdaptor dbAdaptor,
                                                     XMLFileAdaptor fileAdaptor) throws Exception;
    
    protected void processEntityCompartment(String checkAttName,
                                            MySQLAdaptor dbAdaptor,
                                            XMLFileAdaptor fileAdaptor) throws Exception {
        // The Schema used for this project has been relaxed to use GO_CellularComponent
        // to accommodate these data sets.
        Collection localList = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Compartment);
        GKInstance gkInstance = null;
        String attValue = null;
        Map<GKInstance, GKInstance> local2dbMap = new HashMap<GKInstance, GKInstance>();
        for (Iterator it = localList.iterator(); it.hasNext();) {
            gkInstance = (GKInstance) it.next();
            attValue = (String) gkInstance.getAttributeValue(checkAttName);
            if (attValue == null)
                continue;
            Collection dbList = dbAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.GO_CellularComponent, 
                                                                   checkAttName,
                                                                   "=",
                                                                   attValue);
            if (dbList != null && dbList.size() > 0) {
                GKInstance dbInstance = (GKInstance) dbList.iterator().next();
                local2dbMap.put(gkInstance, dbInstance);
            }
        }
        // Set DB_ID first before update to avoid shell conflict: a shell instance
         // is checked out first (e.g. Cytoplasm) before the local instance, there
         // will be two instances (e.g. two Cytoplasm).
         for (Iterator<GKInstance> it = local2dbMap.keySet().iterator();
             it.hasNext();) {
             GKInstance local = it.next();
             GKInstance dbInstance = local2dbMap.get(local);
             Long oldId = local.getDBID();
             local.setDBID(dbInstance.getDBID());
             fileAdaptor.dbIDUpdated(oldId, local);
         }
         // Sometimes, two or more local instances might be mapped to the same
         // db instance. For example in Panther, Cell Membrane and Plasma Membrane
         // are used. However, both are mapped to plasma membrane.
         Map<GKInstance, List<GKInstance>> db2localMap = new HashMap<GKInstance, List<GKInstance>>();
         for (Iterator<GKInstance> it = local2dbMap.keySet().iterator(); it.hasNext();) {
             GKInstance local = it.next();
             GKInstance db = local2dbMap.get(local);
             List<GKInstance> list = db2localMap.get(db);
             if (list == null) {
                 list = new ArrayList<GKInstance>();
                 db2localMap.put(db, list);
             }
             list.add(local);
         }
         // Merge
         for (Iterator<List<GKInstance>> it = db2localMap.values().iterator(); it.hasNext();) {
             List<GKInstance> list = it.next();
             if (list.size() > 1)
                 mergeEntityCompartment(list, local2dbMap, fileAdaptor);
         }
         // Actual downloading
         SynchronizationManager manager = SynchronizationManager.getManager();
         for (Iterator<GKInstance> it = local2dbMap.keySet().iterator();
              it.hasNext();) {
             GKInstance local = it.next();
             GKInstance dbInstance = local2dbMap.get(local);
             manager.updateFromDB(local, dbInstance);
             local.setIsDirty(false);
         }        
    }
    
    private void mergeEntityCompartment(List<GKInstance> list,
                                        Map<GKInstance, GKInstance> local2dbMap,
                                        XMLFileAdaptor fileAdaptor) throws Exception {
        // The first one should be kept
        GKInstance first = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            GKInstance merged = list.get(i);
            // Change the referrers to deleting instance to instance1
            List referrers = fileAdaptor.getReferers(merged);
            if (referrers == null && referrers.size() == 0)
                continue;
            for (Iterator it = referrers.iterator(); it.hasNext();){
                GKInstance referrer = (GKInstance) it.next();
                // Find the referrer and replace it with keptInstance
                InstanceUtilities.replaceReference(referrer, merged, first);
            }
            // Deleting the deletingInstance
            fileAdaptor.deleteInstance(merged);
            // remove it from the map so that it will not be handled afterwards
            local2dbMap.remove(merged);
        }
    }

    protected abstract void processEWAS(MySQLAdaptor dbAdaptor,
                                        XMLFileAdaptor fileAdaptor) throws Exception;
    
    protected abstract void attachDataSource(MySQLAdaptor dbAdaptor,
                                             XMLFileAdaptor fileAdaptor) throws Exception;
    
    protected void attachDataSource(String dbName,
                                    String url,
                                    MySQLAdaptor dbAdaptor,
                                    XMLFileAdaptor fileAdaptor) throws Exception {
        // Need to make sure dataSource is a valid attribute
        SchemaClass databaseObject = dbAdaptor.getSchema().getClassByName(ReactomeJavaConstants.DatabaseObject);
        if (!databaseObject.isValidAttribute(ReactomeJavaConstants.dataSource))
            return;
        GKInstance dataSource = null;
        // Check the local project first.
        // Make sure matched by _displayName
        Collection c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase, 
                                                            ReactomeJavaConstants._displayName, 
                                                            "=",
                                                            dbName);
        if (c != null && c.size() > 0)
            dataSource = (GKInstance) c.iterator().next();
        else { // Check the database
            c = dbAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase,
                                                   ReactomeJavaConstants._displayName,
                                                   "=",
                                                   dbName);
            if (c != null && c.size() > 0) {
                dataSource = (GKInstance) c.iterator().next();
                dataSource = PostProcessHelper.downloadDBInstance(dataSource, fileAdaptor);
            }
        }
        if (dataSource == null) { // Create a new one
            // Create a ReferenceDatabase
            dataSource = fileAdaptor.createNewInstance(ReactomeJavaConstants.ReferenceDatabase);
            dataSource.addAttributeValue(ReactomeJavaConstants.name,
                                         dbName);
            dataSource.setAttributeValue(ReactomeJavaConstants.url, url);
            InstanceDisplayNameGenerator.setDisplayName(dataSource);
        }
        c = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        GKInstance gkInstance = null;
        for (Iterator it = c.iterator(); it.hasNext();) {
            gkInstance = (GKInstance) it.next();
            // Work for new instances only
            if (gkInstance.getDBID() > 0)
                continue;
            gkInstance.setAttributeValue(ReactomeJavaConstants.dataSource,
                                         dataSource);
        }
    }

    protected void cleanUpRefPepSeqs(XMLFileAdaptor fileAdaptor) throws Exception {
        Collection refPepSeqs = null;
        if (fileAdaptor.getSchema().isValidClass(ReactomeJavaConstants.ReferencePeptideSequence))
            refPepSeqs = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferencePeptideSequence);
        else
            refPepSeqs = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.ReferenceGeneProduct);
        // Delete any refPepSeq that have not be used any more
        List<GKInstance> toBeDeleted = new ArrayList<GKInstance>();
        for (Iterator it = refPepSeqs.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            // If its DB_ID is positive, it is downloaded, and should not be deleted
            if (inst.getDBID() > 0)
                continue;
            Collection referrers = inst.getReferers(ReactomeJavaConstants.referenceEntity);
            if (referrers == null || referrers.size() == 0)
                toBeDeleted.add(inst);
        }
        logger.info("RefPepSeq to be deleted: " + toBeDeleted.size());
        System.out.println("RefPepSeq to be deleted: " + toBeDeleted.size());
        // To deletion
        for (GKInstance inst : toBeDeleted)
            fileAdaptor.deleteInstance(inst);
    }
}
