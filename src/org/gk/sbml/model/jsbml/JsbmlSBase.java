/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.SBase;


/**
 * Wraps org.sbml.jsbml.SBase.
 * 
 * @author David Croft
 *
 */
public class JsbmlSBase implements SBase {
	private org.sbml.jsbml.SBase sbmlElement = null;

	public JsbmlSBase(org.sbml.jsbml.SBase sbmlElement) {
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
		sbmlElement.addCVTerm(((JsbmlCVTerm)cVTerm).getSbmlElement());
		return 0;
	}

	@Override
	public CVTerm createCVTerm() {
		JsbmlCVTerm cvTerm = new JsbmlCVTerm();
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
