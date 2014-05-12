/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.BoundingBox;

/**
 * Wraps org.sbml.jsbml.BoundingBox.
 * 
 * The layout extension is not available in the current JSBML.
 * 
 * @author David Croft
 *
 */
public class JsbmlBoundingBox extends BoundingBox {
//	private org.sbml.jsbml.BoundingBox sbmlElement = null;

	public JsbmlBoundingBox(String id, double x, double y, double width, double height) {
//		sbmlElement = new org.sbml.jsbml.BoundingBox(id, x, y, width, height);
	}

//	public org.sbml.jsbml.BoundingBox getSbmlElement() {
//		return sbmlElement;
//	}
}
