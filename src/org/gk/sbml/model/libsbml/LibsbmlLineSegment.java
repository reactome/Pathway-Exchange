/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.LineSegment;
import org.gk.sbml.model.elements.Point;
import org.gk.sbml.model.elements.ReactionGlyph;

/**
 * Wraps org.sbml.libsbml.LineSegment.
 * 
 * @author David Croft
 *
 */
public class LibsbmlLineSegment extends LineSegment {
	private org.sbml.libsbml.LineSegment sbmlElement = null;

	public LibsbmlLineSegment(org.sbml.libsbml.LineSegment sbmlElement) {
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.libsbml.LineSegment getSbmlElement() {
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
}
