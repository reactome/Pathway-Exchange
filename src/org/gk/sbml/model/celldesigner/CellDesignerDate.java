/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import org.gk.sbml.model.elements.Date;


/**
 * Wraps java.util.Date.
 * 
 * @author David Croft
 *
 */
public class CellDesignerDate extends Date {
	private java.util.Date sbmlElement = null;

	public CellDesignerDate(java.util.Date date) {
		super(date);		
		sbmlElement = date;
	}

	public java.util.Date getSbmlElement() {
		return sbmlElement;
	}
}
