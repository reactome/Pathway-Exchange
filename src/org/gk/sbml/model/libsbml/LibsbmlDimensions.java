/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.Dimensions;

/**
 * Wraps org.sbml.libsbml.Dimensions.
 * 
 * @author David Croft
 *
 */
public class LibsbmlDimensions extends Dimensions {
	private org.sbml.libsbml.Dimensions sbmlElement = null;

	public LibsbmlDimensions(double width, double height) {
		sbmlElement = new org.sbml.libsbml.Dimensions(width, height);
	}

	public org.sbml.libsbml.Dimensions getSbmlElement() {
		return sbmlElement;
	}
}
