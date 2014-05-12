/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.BoundingBox;
import org.gk.sbml.model.elements.TextGlyph;
import org.sbml.libsbml.libsbmlConstants;


/**
 * Wraps org.sbml.libsbml.TextGlyph.
 * 
 * @author David Croft
 *
 */
public class LibsbmlTextGlyph implements TextGlyph {
	private org.sbml.libsbml.TextGlyph sbmlElement = null;

	public LibsbmlTextGlyph(org.sbml.libsbml.TextGlyph sbmlTextGlyph) {
		this.sbmlElement = sbmlTextGlyph;
	}

	public org.sbml.libsbml.TextGlyph getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public void setBoundingBox(BoundingBox boundingBox) {
		sbmlElement.setBoundingBox(((LibsbmlBoundingBox)boundingBox).getSbmlElement());
	}

	@Override
	public void setText(String title) {
		sbmlElement.setText(title);
	}

	@Override
	public void setOriginOfTextId(String id) {
		sbmlElement.setOriginOfTextId(id);
	}

	@Override
	public void setGraphicalObjectId(String id) {
		sbmlElement.setGraphicalObjectId(id);
	}
}
