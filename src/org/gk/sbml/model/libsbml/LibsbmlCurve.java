/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.CubicBezier;
import org.gk.sbml.model.elements.Curve;
import org.gk.sbml.model.elements.LineSegment;

/**
 * Wraps org.sbml.libsbml.Curve.
 * 
 * @author David Croft
 *
 */
public class LibsbmlCurve extends Curve {
	private org.sbml.libsbml.Curve sbmlElement = null;

	public LibsbmlCurve(org.sbml.libsbml.Curve sbmlElement) {
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.libsbml.Curve getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public LineSegment createLineSegment() {
		org.sbml.libsbml.LineSegment sbmlElementLineSegment = sbmlElement.createLineSegment();
		return new LibsbmlLineSegment(sbmlElementLineSegment);
	}

	@Override
	public CubicBezier createCubicBezier() {
		org.sbml.libsbml.CubicBezier sbmlElementCubicBezier = sbmlElement.createCubicBezier();
		return new LibsbmlCubicBezier(sbmlElementCubicBezier);
	}
}
