/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.Date;


/**
 * Wraps java.util.Date.
 * 
 * @author David Croft
 *
 */
public class JsbmlDate extends Date {
	private java.util.Date sbmlElement = null;

	public JsbmlDate(java.util.Date date) {
		super(date);		
		sbmlElement = date;
	}

	public java.util.Date getSbmlElement() {
		return sbmlElement;
	}
}
