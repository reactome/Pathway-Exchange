/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.BoundingBox;
import org.gk.sbml.model.elements.SpeciesGlyph;


/**
 * Wraps org.sbml.libsbml.SpeciesGlyph.
 * 
 * @author David Croft
 *
 */
public class LibsbmlSpeciesGlyph implements SpeciesGlyph {
	private org.sbml.libsbml.SpeciesGlyph sbmlElement = null;

	public LibsbmlSpeciesGlyph(org.sbml.libsbml.SpeciesGlyph sbmlSpeciesGlyph) {
		this.sbmlElement = sbmlSpeciesGlyph;
	}

	public org.sbml.libsbml.SpeciesGlyph getSbmlElement() {
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
	public void setSpeciesId(String id) {
		sbmlElement.setSpeciesId(id);
	}

	@Override
	public void setBoundingBox(BoundingBox boundingBox) {
		sbmlElement.setBoundingBox(((LibsbmlBoundingBox)boundingBox).getSbmlElement());
	}
}
