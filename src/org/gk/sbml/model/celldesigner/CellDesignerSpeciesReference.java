/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import org.gk.sbml.model.elements.SpeciesReference;
import org.gk.sbml.simcd.SimCDSBase;


/**
 * Wraps org.sbml.jsbml.SpeciesReference.
 * 
 * @author David Croft
 *
 */
public class CellDesignerSpeciesReference extends CellDesignerSBase implements SpeciesReference {
	private org.gk.sbml.simcd.SimCDSpeciesReference sbmlElement = null;

	public CellDesignerSpeciesReference(org.gk.sbml.simcd.SimCDSpeciesReference sbmlElement) {
		super((SimCDSBase) sbmlElement);
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
