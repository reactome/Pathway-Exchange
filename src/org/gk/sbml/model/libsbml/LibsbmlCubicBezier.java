/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.CubicBezier;
import org.gk.sbml.model.elements.Point;

/**
 * Wraps org.sbml.libsbml.CubicBezier.
 * 
 * @author David Croft
 *
 */
public class LibsbmlCubicBezier extends CubicBezier {
	private org.sbml.libsbml.CubicBezier sbmlElement = null;

	public LibsbmlCubicBezier(org.sbml.libsbml.CubicBezier sbmlElement) {
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.libsbml.CubicBezier getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public void setStart(Point point) {
		sbmlElement.setStart(((LibsbmlPoint)point).getSbmlElement());
	}

	@Override
	public void setEnd(Point point) {
		sbmlElement.setEnd(((LibsbmlPoint)point).getSbmlElement());
	}

	@Override
	public void setBasePoint1(Point point) {
		sbmlElement.setBasePoint1(((LibsbmlPoint)point).getSbmlElement());
	}

	@Override
	public void setBasePoint2(Point point) {
		sbmlElement.setBasePoint2(((LibsbmlPoint)point).getSbmlElement());
	}
}
