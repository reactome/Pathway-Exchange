/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.CubicBezier;
import org.gk.sbml.model.elements.Point;

/**
 * Wraps org.sbml.jsbml.CubicBezier.
 * 
 * The layout extension is not available in the current JSBML.
 * 
 * @author David Croft
 *
 */
public class JsbmlCubicBezier extends CubicBezier {
//	private org.sbml.jsbml.CubicBezier sbmlElement = null;
//
//	public JsbmlCubicBezier(org.sbml.jsbml.CubicBezier sbmlElement) {
//		this.sbmlElement = sbmlElement;
//	}
//
//	public org.sbml.jsbml.CubicBezier getSbmlElement() {
//		return sbmlElement;
//	}

	@Override
	public void setStart(Point point) {
//		sbmlElement.setStart(((JsbmlPoint)point).getSbmlElement());
	}

	@Override
	public void setEnd(Point point) {
//		sbmlElement.setEnd(((JsbmlPoint)point).getSbmlElement());
	}

	@Override
	public void setBasePoint1(Point point) {
//		sbmlElement.setBasePoint1(((JsbmlPoint)point).getSbmlElement());
	}

	@Override
	public void setBasePoint2(Point point) {
//		sbmlElement.setBasePoint2(((JsbmlPoint)point).getSbmlElement());
	}
}
