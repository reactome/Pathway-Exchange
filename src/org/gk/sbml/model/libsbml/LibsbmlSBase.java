/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.SBase;

/**
 * Wraps org.sbml.libsbml.SBase.
 * 
 * @author David Croft
 *
 */
public abstract class LibsbmlSBase implements SBase {
	private org.sbml.libsbml.SBase sbmlElement = null;

	public LibsbmlSBase(org.sbml.libsbml.SBase sbmlElement) {
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.libsbml.SBase getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setMetaId(String metaId) {
		return sbmlElement.setMetaId(metaId);
	}

	@Override
	public int addCVTerm(CVTerm cVTerm) {
		return sbmlElement.addCVTerm(((LibsbmlCVTerm)cVTerm).getSbmlElement());
	}

	@Override
	public CVTerm createCVTerm() {
		LibsbmlCVTerm cvTerm = new LibsbmlCVTerm();
		return cvTerm;
	}

	@Override
	public int setSBOTerm(int sboid) {
		return sbmlElement.setSBOTerm(sboid);
	}
}
