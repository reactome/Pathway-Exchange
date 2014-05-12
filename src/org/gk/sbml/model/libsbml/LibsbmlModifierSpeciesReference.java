/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.ModifierSpeciesReference;


/**
 * Wraps org.sbml.libsbml.ModifierSpeciesReference.
 * 
 * @author David Croft
 *
 */
public class LibsbmlModifierSpeciesReference extends LibsbmlSBase implements ModifierSpeciesReference {
	private org.sbml.libsbml.ModifierSpeciesReference sbmlElement = null;

	public LibsbmlModifierSpeciesReference(org.sbml.libsbml.ModifierSpeciesReference sbmlElement) {
		super(sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.libsbml.ModifierSpeciesReference getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		return sbmlElement.setId(id);
	}

//	@Override
//	public int setMetaId(String metaId) {
//		return sbmlElement.setMetaId(metaId);
//	}

	@Override
	public int setNotes(String notes) {
		return sbmlElement.setNotes(notes);
	}

	@Override
	public int setSpecies(String speciesId) {
		return sbmlElement.setSpecies(speciesId);
	}
}
