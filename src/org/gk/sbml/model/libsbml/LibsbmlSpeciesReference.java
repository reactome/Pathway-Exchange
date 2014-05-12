/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.SpeciesReference;


/**
 * Wraps org.sbml.libsbml.SpeciesReference.
 * 
 * @author David Croft
 *
 */
public class LibsbmlSpeciesReference extends LibsbmlSBase implements SpeciesReference {
	private org.sbml.libsbml.SpeciesReference sbmlElement = null;

	public LibsbmlSpeciesReference(org.sbml.libsbml.SpeciesReference sbmlElement) {
		super(sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.libsbml.SpeciesReference getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		return sbmlElement.setId(id);
	}

	@Override
	public int setNotes(String notes) {
		return sbmlElement.setNotes(notes);
	}

	@Override
	public int setSpecies(String speciesId) {
		return sbmlElement.setSpecies(speciesId);
	}

	@Override
	public double getStoichiometry() {
		return sbmlElement.getStoichiometry();
	}

	@Override
	public int setStoichiometry(double stoichiometry) {
		return sbmlElement.setStoichiometry(stoichiometry);
	}
}
