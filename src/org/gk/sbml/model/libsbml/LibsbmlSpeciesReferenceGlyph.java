/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.Curve;
import org.gk.sbml.model.elements.SpeciesReferenceGlyph;
import org.sbml.libsbml.libsbmlConstants;


/**
 * Wraps org.sbml.libsbml.SpeciesReferenceGlyph.
 * 
 * @author David Croft
 *
 */
public class LibsbmlSpeciesReferenceGlyph implements SpeciesReferenceGlyph {
	private org.sbml.libsbml.SpeciesReferenceGlyph sbmlElement = null;

	public LibsbmlSpeciesReferenceGlyph(org.sbml.libsbml.SpeciesReferenceGlyph sbmlSpeciesReferenceGlyph) {
		this.sbmlElement = sbmlSpeciesReferenceGlyph;
	}

	public org.sbml.libsbml.SpeciesReferenceGlyph getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		return sbmlElement.setId(id);
	}

	@Override
	public void setSpeciesReferenceId(String id) {
		sbmlElement.setSpeciesReferenceId(id);
	}

	@Override
	public void setSpeciesGlyphId(String id) {
		sbmlElement.setSpeciesGlyphId(id);
	}

	@Override
	public Curve getCurve() {
		org.sbml.libsbml.Curve sbmlElementCurve = sbmlElement.getCurve();
		return new LibsbmlCurve(sbmlElementCurve);
	}

	@Override
	public void setSpeciesRoleUndefined() {
		sbmlElement.setRole(libsbmlConstants.SPECIES_ROLE_UNDEFINED);
	}

	@Override
	public void setSpeciesRoleSubstrate() {
		sbmlElement.setRole(libsbmlConstants.SPECIES_ROLE_SUBSTRATE);
	}

	@Override
	public void setSpeciesRoleProduct() {
		sbmlElement.setRole(libsbmlConstants.SPECIES_ROLE_PRODUCT);
	}

	@Override
	public void setSpeciesRoleModifier() {
		sbmlElement.setRole(libsbmlConstants.SPECIES_ROLE_MODIFIER);
	}
}
