/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.Date;


/**
 * Wraps org.sbml.libsbml.Date.
 * 
 * @author David Croft
 *
 */
public class LibsbmlDate extends Date {
	private org.sbml.libsbml.Date sbmlElement = null;

	public LibsbmlDate(java.util.Date date) {
		super(date);
		sbmlElement = new org.sbml.libsbml.Date();
		sbmlElement.setYear(date.getYear() + 1900);
		sbmlElement.setMonth(date.getMonth());
		sbmlElement.setDay(date.getDate());
		sbmlElement.setHour(date.getHours());
		sbmlElement.setMinute(date.getMinutes());
		sbmlElement.setSecond(date.getSeconds());
	}

	public org.sbml.libsbml.Date getSbmlElement() {
		return sbmlElement;
	}
}
