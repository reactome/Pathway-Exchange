/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.BoundingBox;
import org.gk.sbml.model.elements.SpeciesGlyph;


/**
 * Wraps org.sbml.jsbml.SpeciesGlyph.
 * 
 * The layout extension is not available in the current JSBML.
 * 
 * @author David Croft
 *
 */
public class JsbmlSpeciesGlyph implements SpeciesGlyph {
//	private org.sbml.jsbml.SpeciesGlyph sbmlElement = null;
//
//	public JsbmlSpeciesGlyph(org.sbml.jsbml.SpeciesGlyph sbmlSpeciesGlyph) {
//		this.sbmlElement = sbmlSpeciesGlyph;
//	}
//
//	public org.sbml.jsbml.SpeciesGlyph getSbmlElement() {
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
	public void setSpeciesId(String id) {
//		sbmlElement.setSpeciesId(id);
	}

	@Override
	public void setBoundingBox(BoundingBox boundingBox) {
//		sbmlElement.setBoundingBox(((JsbmlBoundingBox)boundingBox).getSbmlElement());
	}
}
