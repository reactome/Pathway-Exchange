/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.Species;
import org.gk.sbml.simcd.SimCDSBase;


/**
 * Wraps org.sbml.jsbml.Species.
 * 
 * @author David Croft
 *
 */
public class CellDesignerSpecies extends CellDesignerSBase implements Species {
	private org.gk.sbml.simcd.SimCDSpecies sbmlElement = null;

	public CellDesignerSpecies(org.gk.sbml.simcd.SimCDSpecies sbmlElement) {
		super((SimCDSBase) sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.jsbml.Species getSbmlElement() {
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
	public int setName(String name) {
		sbmlElement.setName(name);
		return 0;
	}

	@Override
	public int addCVTerm(CVTerm cVTerm) {
		sbmlElement.addCVTerm(((CellDesignerCVTerm)cVTerm).getSbmlElement());
		return 0;
	}

	@Override
	public int setCompartment(String compartmentId) {
		sbmlElement.setCompartment(compartmentId);
		return 0;
	}

	@Override
	public String getId() {
		return sbmlElement.getId();
	}
}
