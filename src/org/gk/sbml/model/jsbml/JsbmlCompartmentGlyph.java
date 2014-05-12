/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.BoundingBox;
import org.gk.sbml.model.elements.CompartmentGlyph;


/**
 * Wraps org.sbml.jsbml.CompartmentGlyph.
 * 
 * The layout extension is not available in the current JSBML.
 * 
 * @author David Croft
 *
 */
public class JsbmlCompartmentGlyph implements CompartmentGlyph {
//	private org.sbml.jsbml.CompartmentGlyph sbmlElement = null;

//	public JsbmlCompartmentGlyph(org.sbml.jsbml.CompartmentGlyph sbmlCompartmentGlyph) {
//		this.sbmlElement = sbmlCompartmentGlyph;
//	}
//
//	public org.sbml.jsbml.CompartmentGlyph getSbmlElement() {
//		return sbmlElement;
//	}

	@Override
	public int setId(String id) {
//		return sbmlElement.setId(id);
		return 0;
	}

	@Override
	public String getId() {
//		return sbmlElement.getId();
		return null;
	}

	@Override
	public void setCompartmentId(String id) {
//		sbmlElement.setCompartmentId(id);
	}

	@Override
	public void setBoundingBox(BoundingBox boundingBox) {
//		sbmlElement.setBoundingBox(((JsbmlBoundingBox)boundingBox).getSbmlElement());
	}
}
