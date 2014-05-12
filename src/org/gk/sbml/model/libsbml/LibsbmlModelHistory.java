/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.Date;
import org.gk.sbml.model.elements.ModelCreator;
import org.gk.sbml.model.elements.ModelHistory;
import org.gk.sbml.model.jsbml.JsbmlDate;


/**
 * Wraps org.sbml.libsbml.ModelHistory.
 * 
 * @author David Croft
 *
 */
public class LibsbmlModelHistory implements ModelHistory {
	private org.sbml.libsbml.ModelHistory sbmlElement = null;

	public LibsbmlModelHistory(org.sbml.libsbml.ModelHistory sbmlElement) {
		this.sbmlElement = sbmlElement;
	}

	@Override
	public int addCreator(ModelCreator modelCreator) {
		return sbmlElement.addCreator(((LibsbmlModelCreator)modelCreator).getSbmlElement());
	}

	@Override
	public int setModifiedDate(Date date) {
		sbmlElement.setModifiedDate(((LibsbmlDate)date).getSbmlElement());
		return 0;
	}

	@Override
	public int setCreatedDate(Date date) {
		sbmlElement.setCreatedDate(((LibsbmlDate)date).getSbmlElement());
		return 0;
	}

	public org.sbml.libsbml.ModelHistory getSbmlElement() {
		return sbmlElement;
	}
}
