package org.reactome.gsea;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * Reactome orthologous prediction doesn't put gene names for mouse ReferenceGeneProducts.
 * This class is used to load a map from UniProt to gene names for mouse based on a downloaded
 * Fasta file for the mouse proteome from this URL:
 * https://www.uniprot.org/uniprot/?query=proteome:UP000000589%20reviewed:yes
 * @author wug
 *
 */
public class MousePathwaysHelper {
    
    public MousePathwaysHelper() {
    }
    
    public Set<GKInstance> grepSignalTransductionPathways(MySQLAdaptor dba) throws Exception {
        Long dbId = 9777872L;
        GKInstance topic = dba.fetchInstance(dbId);
        Set<GKInstance> pathways = org.gk.model.InstanceUtilities.getContainedEvents(topic);
        pathways.add(topic);
        return pathways;
    }
    
    @Test
    public void testLoadMap() throws IOException {
        String fileName = "/Users/wug/datasets/UniProt/mouse_2020_01/";
        fileName += "uniprot-mouse-proteome.fasta";
        Map<String, String> idToMap = loadMap(fileName);
        System.out.println("Total map: " + idToMap.size());
        idToMap.forEach((id, gene) -> System.out.println(id + "\t" + gene));
    }
    
    public Map<String, String> loadMap() throws IOException {
        String fileName = "/Users/wug/datasets/UniProt/mouse_2020_01/";
        fileName += "uniprot-mouse-proteome.fasta";
        Map<String, String> idToMap = loadMap(fileName);
        return idToMap;
    }
    
    public Map<String, String> loadMap(String fileName) throws IOException {
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = null;
        Map<String, String> idToGene = new HashMap<>();
        while ((line = fu.readLine()) != null) {
            if (!line.startsWith(">"))
                continue;
            String[] tokens = line.split(" |\\|");
            for (int i = 2; i < tokens.length; i++) {
                if (tokens[i].startsWith("GN=")) {
                    String gene = tokens[i].split("=")[1];
                    idToGene.put(tokens[1], gene);
                    break;
                }
            }
        }
        fu.close();
        return idToGene;
    }
    
}
