/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.Date;
import org.gk.sbml.model.elements.ModelCreator;
import org.gk.sbml.model.elements.ModelHistory;


/**
 * Wraps org.sbml.libsbml.ModelHistory. 
 * 
 * @author David Croft
 *
 */
public class JsbmlModelHistory implements ModelHistory {
	private org.sbml.jsbml.History sbmlElement = null;

	public JsbmlModelHistory(org.sbml.jsbml.History sbmlElement) {
		this.sbmlElement = sbmlElement;
	}

	@Override
	public int addCreator(ModelCreator modelCreator) {
		sbmlElement.addCreator(((JsbmlModelCreator)modelCreator).getSbmlElement());
		return 0;
	}

	@Override
	public int setModifiedDate(Date date) {
		sbmlElement.setModifiedDate(((JsbmlDate)date).getSbmlElement());
		return 0;
	}

	@Override
	public int setCreatedDate(Date date) {
		sbmlElement.setCreatedDate(((JsbmlDate)date).getSbmlElement());
		return 0;
	}

	public org.sbml.jsbml.History getSbmlElement() {
		return sbmlElement;
	}
}
