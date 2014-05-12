/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.LineSegment;
import org.gk.sbml.model.elements.Point;
import org.gk.sbml.model.elements.ReactionGlyph;

/**
 * Wraps org.sbml.jsbml.LineSegment.
 * 
 * The layout extension is not available in the current JSBML.
 * 
 * @author David Croft
 *
 */
public class JsbmlLineSegment extends LineSegment {
//	private org.sbml.jsbml.LineSegment sbmlElement = null;
//
//	public JsbmlLineSegment(org.sbml.jsbml.LineSegment sbmlElement) {
//		this.sbmlElement = sbmlElement;
//	}
//
//	public org.sbml.jsbml.LineSegment getSbmlElement() {
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
}
