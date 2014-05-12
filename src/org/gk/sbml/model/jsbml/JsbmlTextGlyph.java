/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.BoundingBox;
import org.gk.sbml.model.elements.TextGlyph;


/**
 * Wraps org.sbml.jsbml.TextGlyph.
 * 
 * The layout extension is not available in the current JSBML.
 * 
 * @author David Croft
 *
 */
public class JsbmlTextGlyph implements TextGlyph {
//	private org.sbml.jsbml.TextGlyph sbmlElement = null;
//
//	public JsbmlTextGlyph(org.sbml.jsbml.TextGlyph sbmlTextGlyph) {
//		this.sbmlElement = sbmlTextGlyph;
//	}
//
//	public org.sbml.jsbml.TextGlyph getSbmlElement() {
//		return sbmlElement;
//	}

	@Override
	public void setBoundingBox(BoundingBox boundingBox) {
//		sbmlElement.setBoundingBox(((JsbmlBoundingBox)boundingBox).getSbmlElement());
	}

	@Override
	public void setText(String title) {
//		sbmlElement.setText(title);
	}

	@Override
	public void setOriginOfTextId(String id) {
//		sbmlElement.setOriginOfTextId(id);
	}

	@Override
	public void setGraphicalObjectId(String id) {
//		sbmlElement.setGraphicalObjectId(id);
	}
}
