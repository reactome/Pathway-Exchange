/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.SpeciesReference;


/**
 * Wraps org.sbml.jsbml.SpeciesReference.
 * 
 * @author David Croft
 *
 */
public class JsbmlSpeciesReference extends JsbmlSBase implements SpeciesReference {
	private org.sbml.jsbml.SpeciesReference sbmlElement = null;

	public JsbmlSpeciesReference(org.sbml.jsbml.SpeciesReference sbmlElement) {
		super(sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.jsbml.SpeciesReference getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		sbmlElement.setId(id);
		return 0;
	}

	@Override
	public int setMetaId(String metaId) {
		sbmlElement.setMetaId(metaId);
		return 0;
	}

	@Override
	public int setSpecies(String speciesId) {
		sbmlElement.setSpecies(speciesId);
		return 0;
	}

	@Override
	public double getStoichiometry() {
		return sbmlElement.getStoichiometry();
	}

	@Override
	public int setStoichiometry(double stoichiometry) {
		sbmlElement.setStoichiometry(stoichiometry);
		return 0;
	}
}
