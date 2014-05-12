/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import org.gk.sbml.model.elements.Date;
import org.gk.sbml.model.elements.ModelCreator;
import org.gk.sbml.model.elements.ModelHistory;


/**
 * Wraps org.sbml.libsbml.ModelHistory. 
 * 
 * @author David Croft
 *
 */
public class CellDesignerModelHistory implements ModelHistory {
	private org.gk.sbml.simcd.SimCDHistory sbmlElement = null;

	public CellDesignerModelHistory(org.gk.sbml.simcd.SimCDHistory sbmlElement) {
		this.sbmlElement = sbmlElement;
	}

	@Override
	public int addCreator(ModelCreator modelCreator) {
		sbmlElement.addCreator(((CellDesignerModelCreator)modelCreator).getSbmlElement());
		return 0;
	}

	@Override
	public int setModifiedDate(Date date) {
		sbmlElement.setModifiedDate(((CellDesignerDate)date).getSbmlElement());
		return 0;
	}

	@Override
	public int setCreatedDate(Date date) {
		sbmlElement.setCreatedDate(((CellDesignerDate)date).getSbmlElement());
		return 0;
	}

	public org.sbml.jsbml.History getSbmlElement() {
		return sbmlElement;
	}
}
