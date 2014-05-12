/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps for keeping track of model components, such as reactions, species, etc.
 * 
 * @author David Croft
 *
 */
public class ModelComponentMaps {
	protected Map<String,Reaction> reactionHash = new HashMap<String,Reaction>();
	protected Map<String,Species> speciesHash = new HashMap<String,Species>();
	protected Map<String,Compartment> compartmentHash = new HashMap<String,Compartment>();

	public void addReaction(Reaction reaction) {
		String id = reaction.getId();
		if (!id.isEmpty() && !existsReaction(id))
			reactionHash.put(id, reaction);
	}
	
	public Reaction getReaction(String id) {
		Reaction reaction = null;
		if (!id.isEmpty() && reactionHash.containsKey(id))
			reaction = reactionHash.get(id);
		return reaction;
	}

	public boolean existsReaction(String id) {
		if (!id.isEmpty() && reactionHash.containsKey(id))
			return true;
		return false;
	}
	
	public void addSpecies(Species species) {
		String id = species.getId();
		if (!id.isEmpty() && !speciesHash.containsKey(id))
			speciesHash.put(id, species);
	}
	
	public Species getSpecies(String id) {
		Species species = null;
		if (!id.isEmpty() && speciesHash.containsKey(id))
			species = speciesHash.get(id);
		return species;
	}

	public void addCompartment(Compartment compartment) {
		String id = compartment.getId();
		if (!id.isEmpty() && !compartmentHash.containsKey(id))
			compartmentHash.put(id, compartment);
	}
	
	public Compartment getCompartment(String id) {
		Compartment compartment = null;
		if (!id.isEmpty() && compartmentHash.containsKey(id))
			compartment = compartmentHash.get(id);
		return compartment;
	}
}
