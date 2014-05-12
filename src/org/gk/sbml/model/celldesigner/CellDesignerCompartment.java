/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.Compartment;
import org.gk.sbml.simcd.SimCDSBase;


/**
 * Wraps org.sbml.jsbml.Compartment.
 * 
 * @author David Croft
 *
 */
public class CellDesignerCompartment extends CellDesignerSBase implements Compartment {
	private org.gk.sbml.simcd.SimCDCompartment sbmlElement = null;

	public CellDesignerCompartment(org.gk.sbml.simcd.SimCDCompartment sbmlElement) {
		super((SimCDSBase) sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.gk.sbml.simcd.SimCDCompartment getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		sbmlElement.setId(id);
		return 0;
	}


	@Override
	public int setName(String name) {
		sbmlElement.setName(name);
		return 0;
	}


	@Override
	public String getId() {
		return sbmlElement.getId();
	}
}
