/*
 * Created on Mar 29, 2010
 *
 */
package org.reactome.gsea;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.junit.Test;
import org.reactome.convert.common.PathwayReferenceEntityHelper;


/**
 * This class is used to export all human Reactome pathways to MsigDB formation to be used by GSEA.
 * @author wgm
 *
 */
public class ReactomeToMsigDBExport {
    private int SIZE_CUTOFF = 10;
    private MySQLAdaptor dba;
    private boolean isForGMT;
    private Long speciesId = null;
    // Check for ReferenceGeneProduct instances that have no gene names and cannot be exported
    private Set<GKInstance> failedInstances = null;
    // A flag to control if UniProt should be used for output
    private boolean useUniProt = false;
    
    public ReactomeToMsigDBExport() {
    }
    
    private MySQLAdaptor getDBA() throws Exception {
        if (dba != null)
            return dba;
        dba = new MySQLAdaptor("localhost",
                               "gk_current_ver32", 
                               "root",
                               "macmysql01");
        return dba;
    }
    
    public void setIsForGMT(boolean value) {
        this.isForGMT = value;
        if (isForGMT)
            SIZE_CUTOFF = 5;
    }
    
    public void setUseUniProt(boolean useUniProt)  {
        this.useUniProt = useUniProt;
    }
    
    public void setSpeciesId(Long speciesId) {
        this.speciesId = speciesId;
    }
    
    public Long getSpeciesId() {
        return this.speciesId;
    }
    
    public void setDBA(MySQLAdaptor dba) {
        this.dba = dba;
    }
    
    public void export(String fileName) throws Exception {
        export(new FileOutputStream(fileName));
    }
    
    @Test
    public void exportMouseGMT() throws Exception {
        dba = new MySQLAdaptor("localhost",
                               "gk_current_ver73",
                               "",
                               "");
        speciesId = 48892L;
        isForGMT = true;
        useUniProt = false;
        FileOutputStream fos = new FileOutputStream("ReactomeMousePathways_Rel73.gmt");
        export(fos);
    }
    
    public void export(OutputStream os) throws Exception {
        MySQLAdaptor dba = getDBA();
        // Use all human pathways
        if (speciesId == null)
            speciesId = 48887L;
        Collection pathways = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Pathway, 
                                                           ReactomeJavaConstants.species,
                                                           "=",
                                                           speciesId);
//        
//        Set<GKInstance> stPathways = new MousePathwaysHelper().grepSignalTransductionPathways(dba);
//        pathways.retainAll(stPathways);
//        
        // Load some attributes
        SchemaClass cls = dba.getSchema().getClassByName(ReactomeJavaConstants.Pathway);
        SchemaAttribute att = cls.getAttribute(ReactomeJavaConstants.stableIdentifier);
        dba.loadInstanceAttributeValues(pathways, att);
        // To be used later
        att = cls.getAttribute(ReactomeJavaConstants.summation);
        dba.loadInstanceAttributeValues(pathways, att);
        // Create pathway to gene ids map
        if (isForGMT) {
            exportInGMT(pathways, os);
//            if (failedInstances.size() > 0) {
//                System.out.println("ReferenceSequence instances don't have gene names: " + failedInstances.size());
//                for (GKInstance inst : failedInstances)
//                    System.out.println(inst);
//            }
        }
        else { // Export for MSigDB
            if (failedInstances == null)
                failedInstances = new HashSet<GKInstance>();
            else
                failedInstances.clear();
            Map<GKInstance, List<String>> pathwayToGeneIds = generatePathwayToGeneIdsMap(pathways);
            export(pathways,
                   pathwayToGeneIds,
                   os);
        }
    }
    
    /**
     * Method to export a collection of pathways into an outputstream in the GMT format.
     * @param pathways
     * @param os
     * @throws Exception
     */
    public void exportInGMT(Collection<GKInstance> pathways, OutputStream os) throws Exception {
        if (failedInstances == null)
            failedInstances = new HashSet<GKInstance>();
        else
            failedInstances.clear();
        Map<GKInstance, List<String>> pathwayToNames = generatePathwayToGeneNamesMap(pathways);
        exportInGMT(pathways,
                    pathwayToNames,
                    os);
    }
    
    private Map<String, GKInstance> loadStableIdToPathway(Collection pathways) throws Exception{
        Map<String, GKInstance> stableIdToPathway = new HashMap<String, GKInstance>();
        for (Iterator it = pathways.iterator(); it.hasNext();) {
            GKInstance pathway = (GKInstance) it.next();
            GKInstance stableId = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
            String identifier = (String) stableId.getAttributeValue(ReactomeJavaConstants.identifier);
            stableIdToPathway.put(identifier, pathway);
        }
        return stableIdToPathway;
    }
    
    private void exportInGMT(Collection<GKInstance> pathways,
                        Map<GKInstance, List<String>> pathwayToNames,
                        OutputStream os) throws Exception {
//        System.out.println("Total pathways: " + pathways.size());
        List<GKInstance> pathwayList = new ArrayList<GKInstance>(pathways);
        InstanceUtilities.sortInstances(pathwayList);
        OutputStreamWriter osWriter = new OutputStreamWriter(os);
        PrintWriter writer = new PrintWriter(osWriter);
        StringBuilder builder = new StringBuilder();
        // Use this set to make sure only one pathway name should be exported: required by GMT
        Set<String> exported = new HashSet<String>();
        int count = 0;
        for (GKInstance pathway : pathwayList) {
        	if (exported.contains(pathway.getDisplayName())) {
                System.out.println(pathway + " has been exported already!");
                continue;
            }
            List<String> names = pathwayToNames.get(pathway);
            if (names == null || names.size() == 0)
                continue;

            GKInstance stableId = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
            String identifier = (String) stableId.getAttributeValue(ReactomeJavaConstants.identifier);    
            builder.append(pathway.getDisplayName().trim() + "\t" + identifier); // As description  
            
            for (String name : names)
                builder.append("\t").append(name);
//            System.out.println(pathway.getDisplayName() + ": " + names.size());
            writer.println(builder.toString());
            builder.setLength(0);
            count ++;
            exported.add(pathway.getDisplayName());
        }
        writer.close();
        osWriter.close();
//        System.out.println("Total exported pathways: " + exported.size());
//        System.out.println("Count lines: " + count);
    }
    
    private void export(Collection pathways,
                        Map<GKInstance, List<String>> pathwayToGeneIds,
                        OutputStream os) throws Exception {
        // Use stable ids to sort pathways to create a fixed order
        Map<String, GKInstance> stableIdToPathway = loadStableIdToPathway(pathways);
        List<String> stableIdList = new ArrayList<String>(stableIdToPathway.keySet());
        Collections.sort(stableIdList); // Sorted based on stable ids.
        SummaryTextForMartOutputForMsigDB summaryHelper = new SummaryTextForMartOutputForMsigDB();
        
        OutputStreamWriter osWriter = new OutputStreamWriter(os);
        PrintWriter writer = new PrintWriter(osWriter);
        writer.println("Gene_Set_Name\tBrief_Description\tExternal_Link\tNCBI Gene IDs");
        StringBuilder builder = new StringBuilder();
        for (String stableId : stableIdList) {
            GKInstance pathway = stableIdToPathway.get(stableId);
            //System.out.println(stableId + ": " + pathway);
            List<String> genes = pathwayToGeneIds.get(pathway);
            if (genes.size() < SIZE_CUTOFF)
                continue; // Don't export pathways with less than 10 genes
            String displayName = pathway.getDisplayName();
            builder.append(processDisplayName(displayName)).append("\t");
            // A simple version from MSigDB as of May 21, 2010
            builder.append("Genes involved in ").append(displayName).append("\t");
//            GKInstance summation = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.summation);
//            if (summation == null) {
//                builder.append("\t\t"); // Escape these two fields
//            }
//            else {
//                String text = (String) summation.getAttributeValue(ReactomeJavaConstants.text);
//                if (text == null || text.length() == 0) {
//                    builder.append("\t\t");
//                }
//                else {
//                    String brief = summaryHelper.getFirstSentence(text);
//                    builder.append(brief).append("\t");
//                    builder.append(text).append("\t");
//                }
//            }
            // External link based on stable id
            builder.append(summaryHelper.url).append(stableId).append("\t");
            builder.append("\"");
            for (Iterator<String> it = genes.iterator(); it.hasNext();) {
                builder.append(it.next());
                if (it.hasNext())
                    builder.append(", ");
            }
            builder.append("\"");
            writer.println(builder.toString());
            builder.setLength(0);
        }
        writer.close();
        os.close();
    }
    
    private String processDisplayName(String displayName) {
        String rtn = displayName.toUpperCase();
        rtn = rtn.replaceAll(" ", "_");
        rtn = rtn.replaceAll("\\(|\\)", "");
        return "REACTOME_" + rtn;
    }
    
    private Map<GKInstance, List<String>> generatePathwayToGeneIdsMap(Collection pathways) throws Exception {
        Map<GKInstance, List<String>> pathwayToIds = new HashMap<GKInstance, List<String>>();
        PathwayReferenceEntityHelper helper = new PathwayReferenceEntityHelper();
        for (Iterator it = pathways.iterator(); it.hasNext();) {
            GKInstance pathway = (GKInstance) it.next();
            Set<GKInstance> referenceEntities = helper.grepReferenceEntitiesInPathway(pathway);
            Set<String> geneIds = new HashSet<String>();
            for (GKInstance refEntity : referenceEntities) {
                if (!refEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceGene))
                    continue;
                List values = refEntity.getAttributeValuesList(ReactomeJavaConstants.referenceGene);
                for (Iterator it1 = values.iterator(); it1.hasNext();) {
                    GKInstance refGene = (GKInstance) it1.next();
                    if (refGene.getDisplayName().startsWith("Entrez Gene:") ||
                        refGene.getDisplayName().startsWith("NCBI Gene:")) { // As of June 26, 2012, NCBI has changed gene name to NCBI gene
                        String geneId = (String) refGene.getAttributeValue(ReactomeJavaConstants.identifier);
                        if (geneId != null)
                            geneIds.add(geneId);
                    }
                }
            }
            List<String> idList = new ArrayList<String>(geneIds);
            Collections.sort(idList);
            pathwayToIds.put(pathway, idList);
        }
        return pathwayToIds;
    }
    
    private Map<GKInstance, List<String>> generatePathwayToGeneNamesMap(Collection<GKInstance> pathways) throws Exception {
        Map<GKInstance, List<String>> pathwayToIds = new HashMap<GKInstance, List<String>>();
        PathwayReferenceEntityHelper helper = new PathwayReferenceEntityHelper();
        for (Iterator it = pathways.iterator(); it.hasNext();) {
            GKInstance pathway = (GKInstance) it.next();
            Set<GKInstance> referenceEntities = helper.grepReferenceEntitiesInPathway(pathway);
            Set<String> geneNames = new HashSet<String>();
            for (GKInstance refEntity : referenceEntities) {
                if (refEntity.getSchemClass().isa(ReactomeJavaConstants.ReferenceSequence)) {
                    if (useUniProt) {
                        GKInstance refDb = (GKInstance) refEntity.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
                        if (refDb.getDisplayName().equals("UniProt")) {
                            String identifier = (String) refEntity.getAttributeValue(ReactomeJavaConstants.identifier);
                            if (identifier != null)
                                geneNames.add(identifier);
                        }
                    }
                    else {
                        String geneName = (String) refEntity.getAttributeValue(ReactomeJavaConstants.geneName);
                        if (geneName == null) {
                            System.err.println(refEntity + " has no gene name!");
                            failedInstances.add(refEntity);
                        }
                        else
                            geneNames.add(geneName);
                    }
                }
            }
            List<String> geneNameList = new ArrayList<String>(geneNames);
            Collections.sort(geneNameList);
            pathwayToIds.put(pathway, geneNameList);
        }
        return pathwayToIds;
    }
    
    /**
     * Set the minimum size of pathways for export.
     * @param cutoff
     */
    public void setSizeCutoff(int cutoff) {
        this.SIZE_CUTOFF = cutoff;
    }
    
    public static void main(String[] args) {
        try {
            if (args.length < 6) {
                System.err.println("Usage java org.gk.gsea.ReactomeToMsigDBExport dbHost dbName dbUser dbPwd dbPort fileName (speciesId) (isForMsigDB)\n" +
                		           "Note: speciesId and isForMsigDB are optional. If no speciesId is provided, 48887 for homo sapiens will be used. The " +
                		           "value of isForMsigDB is true or false. The default value is false for the GMT format.");
                System.exit(1);
            }
            ReactomeToMsigDBExport exporter = new ReactomeToMsigDBExport();
            if (args.length == 8)
                exporter.setIsForGMT(!new Boolean(args[7]));
            else
                exporter.setIsForGMT(true);
            MySQLAdaptor dba = new MySQLAdaptor(args[0],
                                                args[1],
                                                args[2],
                                                args[3],
                                                new Integer(args[4]));
            exporter.setDBA(dba);
            if (args.length == 7) {
                exporter.setSpeciesId(new Long(args[6]));
                // For other species, only UniProt can be exported
                exporter.setUseUniProt(true);
            }
            exporter.export(args[5]);
        }
        catch(Exception e) {
            System.err.println("ReactomeToMsigDBExporter.main(): " + e);
            e.printStackTrace();
        }
    }
    
}
