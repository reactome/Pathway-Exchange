/*
 * Created on Sep 4, 2009
 *
 */
package org.reactome.gsea;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.junit.Test;


/**
 * This class is used to attach summary text to mart output to generate a file
 * requested by MsiDB.
 * @author wgm
 *
 */
public class SummaryTextForMartOutputForMsigDB {
    private String dirName = "/Users/wgm/Documents/gkteam/Robin/";
    protected String url = "https://reactome.org/content/detail/";
    
    public SummaryTextForMartOutputForMsigDB() {
    }
    
    @Test
    public void generateMsigFile() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_current_ver32",
                                            "root",
                                            "macmysql01",
                                            3306);
        Map<String, GKInstance> stableIdToPathway = loadStableIdToPathway(dba);
        // Load file
        //String input = dirName + "PathwayWithGeneIds.txt";
        String input = dirName + "release_30.txt";
        FileReader fileReader = new FileReader(input);
        BufferedReader reader = new BufferedReader(fileReader);
        String line = reader.readLine(); // Escape the first line
        Map<String, List<String>> stableIdToGeneIds = new HashMap<String, List<String>>();
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split("\t");
            String stableId = tokens[0];
            if (stableId.length() == 0)
                System.out.println(line);
            String geneId = tokens[2];
            List<String> list = stableIdToGeneIds.get(stableId);
            if (list == null) {
                list = new ArrayList<String>();
                stableIdToGeneIds.put(stableId, list);
            }
            list.add(geneId);
        }
        reader.close();
        fileReader.close();
        // Want to load summary text
        String output = dirName + "ReactomeForMSigDB_release_30.txt";
        FileWriter fileWriter = new FileWriter(output);
        PrintWriter writer = new PrintWriter(fileWriter);
        writer.println("Gene_Set_Name\tBrief_Description\tFull_Description\tExternal_Link\tGenes");
        List<String> stableIdList = new ArrayList<String>(stableIdToGeneIds.keySet());
        Collections.sort(stableIdList);
        StringBuilder builder = new StringBuilder();
        for (String stableId : stableIdList) {
            GKInstance pathway = stableIdToPathway.get(stableId);
            System.out.println(stableId + ": " + pathway);
            builder.append(pathway.getDisplayName()).append("\t");
            GKInstance summation = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.summation);
            if (summation == null) {
                builder.append("\t\t"); // Escape these two fields
            }
            else {
                String text = (String) summation.getAttributeValue(ReactomeJavaConstants.text);
                if (text == null || text.length() == 0) {
                    builder.append("\t\t");
                }
                else {
                    String brief = getFirstSentence(text);
                    builder.append(brief).append("\t");
                    builder.append(text).append("\t");
                }
            }
            // External link based on stable id
            builder.append(url).append(stableId).append("\t");
            List<String> genes = stableIdToGeneIds.get(stableId);
            for (Iterator<String> it = genes.iterator(); it.hasNext();) {
                builder.append(it.next());
                if (it.hasNext())
                    builder.append(", ");
            }
            writer.println(builder.toString());
            builder.setLength(0);
        }
        writer.close();
        fileWriter.close();
    }
    
    protected String getFirstSentence(String text) {
        int index = text.indexOf(".");
        if (index <= 0)
            return text;
        return text.substring(0, index);
    }

    private Map<String, GKInstance> loadStableIdToPathway(MySQLAdaptor dba) throws Exception {
        Collection collection = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Pathway,
                                                             ReactomeJavaConstants.species,
                                                             "=",
                                                             48887); // Human pathways only
        SchemaClass cls = dba.getSchema().getClassByName(ReactomeJavaConstants.Pathway);
        SchemaAttribute att = cls.getAttribute(ReactomeJavaConstants.stableIdentifier);
        dba.loadInstanceAttributeValues(collection, att);
        // To be used later
        att = cls.getAttribute(ReactomeJavaConstants.summation);
        dba.loadInstanceAttributeValues(collection, att);
        Map<String, GKInstance> stableIdToPathway = new HashMap<String, GKInstance>();
        for (Iterator it = collection.iterator(); it.hasNext();) {
            GKInstance pathway = (GKInstance) it.next();
            GKInstance stableId = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
            String identifier = (String) stableId.getAttributeValue(ReactomeJavaConstants.identifier);
            stableIdToPathway.put(identifier, pathway);
        }
        return stableIdToPathway;
    }
    
}
