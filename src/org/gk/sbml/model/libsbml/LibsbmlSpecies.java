/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.Species;

/**
 * Wraps org.sbml.libsbml.Species.
 * 
 * @author David Croft
 *
 */
public class LibsbmlSpecies extends LibsbmlSBase implements Species {
	private org.sbml.libsbml.Species sbmlElement = null;

	public LibsbmlSpecies(org.sbml.libsbml.Species sbmlElement) {
		super(sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.libsbml.Species getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		return sbmlElement.setId(id);
	}

	@Override
	public String getId() {
		return sbmlElement.getId();
	}

	@Override
	public int setName(String name) {
		return sbmlElement.setName(name);
	}

	@Override
	public int setCompartment(String compartmentId) {
		return sbmlElement.setCompartment(compartmentId);
	}

	@Override
	public int setNotes(String notes) {
		return sbmlElement.setNotes(notes);
	}
}
