/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.BoundingBox;
import org.gk.sbml.model.elements.CompartmentGlyph;


/**
 * Wraps org.sbml.libsbml.CompartmentGlyph.
 * 
 * @author David Croft
 *
 */
public class LibsbmlCompartmentGlyph implements CompartmentGlyph {
	private org.sbml.libsbml.CompartmentGlyph sbmlElement = null;

	public LibsbmlCompartmentGlyph(org.sbml.libsbml.CompartmentGlyph sbmlCompartmentGlyph) {
		this.sbmlElement = sbmlCompartmentGlyph;
	}

	public org.sbml.libsbml.CompartmentGlyph getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		return sbmlElement.setId(id);
	}

	@Override
	public String getId() {
		return sbmlElement.getId();
	}

	@Override
	public void setCompartmentId(String id) {
		sbmlElement.setCompartmentId(id);
	}

	@Override
	public void setBoundingBox(BoundingBox boundingBox) {
		sbmlElement.setBoundingBox(((LibsbmlBoundingBox)boundingBox).getSbmlElement());
	}
}
