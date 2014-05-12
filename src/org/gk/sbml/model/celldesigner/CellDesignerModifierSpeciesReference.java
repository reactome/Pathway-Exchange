/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import org.gk.sbml.model.elements.ModifierSpeciesReference;
import org.gk.sbml.simcd.SimCDSBase;


/**
 * Wraps org.sbml.jsbml.ModifierSpeciesReference.
 * 
 * @author David Croft
 *
 */
public class CellDesignerModifierSpeciesReference extends CellDesignerSBase implements ModifierSpeciesReference {
	private org.gk.sbml.simcd.SimCDModifierSpeciesReference sbmlElement = null;

	public CellDesignerModifierSpeciesReference(org.gk.sbml.simcd.SimCDModifierSpeciesReference sbmlElement) {
		super((SimCDSBase) sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.gk.sbml.simcd.SimCDModifierSpeciesReference getSbmlElement() {
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
