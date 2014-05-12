/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.CubicBezier;
import org.gk.sbml.model.elements.Curve;
import org.gk.sbml.model.elements.LineSegment;

/**
 * Wraps org.sbml.jsbml.Curve.
 * 
 * The layout extension is not available in the current JSBML.
 * 
 * @author David Croft
 *
 */
public class JsbmlCurve extends Curve {
//	private org.sbml.jsbml.Curve sbmlElement = null;
//
//	public JsbmlCurve(org.sbml.jsbml.Curve sbmlElement) {
//		this.sbmlElement = sbmlElement;
//	}
//
//	public org.sbml.jsbml.Curve getSbmlElement() {
//		return sbmlElement;
//	}

	@Override
	public LineSegment createLineSegment() {
//		org.sbml.jsbml.LineSegment sbmlElementLineSegment = sbmlElement.createLineSegment();
//		return new JsbmlLineSegment(sbmlElementLineSegment);
		return null;
	}

	@Override
	public CubicBezier createCubicBezier() {
//		org.sbml.jsbml.CubicBezier sbmlElementCubicBezier = sbmlElement.createCubicBezier();
//		return new JsbmlCubicBezier(sbmlElementCubicBezier);
		return null;
	}
}
