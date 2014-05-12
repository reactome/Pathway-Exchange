/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.SBase;


/**
 * Wraps org.sbml.jsbml.SBase.
 * 
 * @author David Croft
 *
 */
public class CellDesignerSBase implements SBase {
	private org.gk.sbml.simcd.SimCDSBase sbmlElement = null;

	public CellDesignerSBase(org.gk.sbml.simcd.SimCDSBase sbmlElement) {
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.jsbml.SBase getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setMetaId(String metaId) {
		sbmlElement.setMetaId(metaId);
		return 0;
	}

	@Override
	public int addCVTerm(CVTerm cVTerm) {
		sbmlElement.addCVTerm(((CellDesignerCVTerm)cVTerm).getSbmlElement());
		return 0;
	}

	@Override
	public CVTerm createCVTerm() {
		CellDesignerCVTerm cvTerm = new CellDesignerCVTerm();
		return cvTerm;
	}

	@Override
	public int setNotes(String notes) {
		sbmlElement.setNotes(notes);
		return 0;
	}

	@Override
	public int setSBOTerm(int sboid) {
		sbmlElement.setSBOTerm(sboid);
		return 0;
	}
}
