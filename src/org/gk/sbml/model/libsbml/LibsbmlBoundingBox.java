/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.BoundingBox;

/**
 * Wraps org.sbml.libsbml.Dimensions.
 * 
 * @author David Croft
 *
 */
public class LibsbmlBoundingBox extends BoundingBox {
	private org.sbml.libsbml.BoundingBox sbmlElement = null;

	public LibsbmlBoundingBox(String id, double x, double y, double width, double height) {
		sbmlElement = new org.sbml.libsbml.BoundingBox(id, x, y, width, height);
	}

	public org.sbml.libsbml.BoundingBox getSbmlElement() {
		return sbmlElement;
	}
}
