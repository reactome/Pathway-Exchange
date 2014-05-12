/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.ModifierSpeciesReference;


/**
 * Wraps org.sbml.jsbml.ModifierSpeciesReference.
 * 
 * @author David Croft
 *
 */
public class JsbmlModifierSpeciesReference extends JsbmlSBase implements ModifierSpeciesReference {
	private org.sbml.jsbml.ModifierSpeciesReference sbmlElement = null;

	public JsbmlModifierSpeciesReference(org.sbml.jsbml.ModifierSpeciesReference sbmlElement) {
		super(sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.jsbml.ModifierSpeciesReference getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		sbmlElement.setId(id);
		return 0;
	}

	@Override
	public int setSpecies(String speciesId) {
		sbmlElement.setSpecies(speciesId);
		return 0;
	}
}
