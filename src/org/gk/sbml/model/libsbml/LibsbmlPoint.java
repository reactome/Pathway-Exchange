/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.Point;

/**
 * Wraps org.sbml.libsbml.Point.
 * 
 * @author David Croft
 *
 */
public class LibsbmlPoint extends Point {
	private org.sbml.libsbml.Point sbmlElement = null;

	public LibsbmlPoint(double x, double y) {
		sbmlElement = new org.sbml.libsbml.Point(x, y);
	}

	public org.sbml.libsbml.Point getSbmlElement() {
		return sbmlElement;
	}
}
