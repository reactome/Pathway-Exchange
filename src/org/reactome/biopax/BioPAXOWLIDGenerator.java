/*
 * Created on Feb 1, 2011
 *
 */
package org.reactome.biopax;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;

/**
 * This class is used to generate OWL IDs for BioPAX export. It is required that all OWL ids should be
 * unique across the whole name spaces for level 2 and level 3.
 * @author wgm
 *
 */
public class BioPAXOWLIDGenerator {
    // Used to limit the id search to speed up the performance
    private GKInstance species;
    private Map<String, Long> clsToLargestId;
    
    public BioPAXOWLIDGenerator() {
        clsToLargestId = new HashMap<>();
    }
    
    public void setSpecies(GKInstance species) {
        this.species = species;
    }
    
    public GKInstance getSpecies() {
        return this.species;
    }
    
    /**
     * Reset all cached ids to empty.
     */
    public void reset() {
        clsToLargestId.clear();
    }
    
    /**
     * Generate a unique id based on a template.
     * @param idTemplate the id template
     * @return
     */
    public String generateOWLID(String owlClsName) {
        Long id = clsToLargestId.get(owlClsName);
        if (id == null)
            id = 1l;
        else
            id += 1;
        clsToLargestId.put(owlClsName, id);
        return owlClsName + id;
    }
    
//    private String getSpeciesAbbreviation() {
//        if (species == null)
//            return null;
//        String name = species.getDisplayName();
//        String[] tokens = name.split(" ");
//        if (tokens.length == 1) {
//            // Get the first two letters
//            return tokens[0].substring(0, 2).toLowerCase();
//        }
//        else {
//            StringBuilder builder = new StringBuilder();
//            for (String token : tokens) {
//                builder.append(token.substring(0, 1).toLowerCase());
//            }
//            return builder.toString();
//        }
//    }
    
}
