/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.Curve;
import org.gk.sbml.model.elements.SpeciesReferenceGlyph;


/**
 * Wraps org.sbml.jsbml.SpeciesReferenceGlyph.
 * 
 * The layout extension is not available in the current JSBML.
 * 
 * @author David Croft
 *
 */
public class JsbmlSpeciesReferenceGlyph implements SpeciesReferenceGlyph {
//	private org.sbml.jsbml.SpeciesReferenceGlyph sbmlElement = null;
//
//	public JsbmlSpeciesReferenceGlyph(org.sbml.jsbml.SpeciesReferenceGlyph sbmlSpeciesReferenceGlyph) {
//		this.sbmlElement = sbmlSpeciesReferenceGlyph;
//	}
//
//	public org.sbml.jsbml.SpeciesReferenceGlyph getSbmlElement() {
//		return sbmlElement;
//	}

	@Override
	public int setId(String id) {
//		return sbmlElement.setId(id);
		return 0;
	}

	@Override
	public void setSpeciesReferenceId(String id) {
//		sbmlElement.setSpeciesReferenceId(id);
	}

	@Override
	public void setSpeciesGlyphId(String id) {
//		sbmlElement.setSpeciesGlyphId(id);
	}

	@Override
	public Curve getCurve() {
//		org.sbml.jsbml.Curve sbmlElementCurve = sbmlElement.getCurve();
//		return new JsbmlCurve(sbmlElementCurve);
		return null;
	}

	@Override
	public void setSpeciesRoleUndefined() {
//		sbmlElement.setRole(jsbmlConstants.SPECIES_ROLE_UNDEFINED);
	}

	@Override
	public void setSpeciesRoleSubstrate() {
//		sbmlElement.setRole(jsbmlConstants.SPECIES_ROLE_SUBSTRATE);
	}

	@Override
	public void setSpeciesRoleProduct() {
//		sbmlElement.setRole(jsbmlConstants.SPECIES_ROLE_PRODUCT);
	}

	@Override
	public void setSpeciesRoleModifier() {
//		sbmlElement.setRole(jsbmlConstants.SPECIES_ROLE_MODIFIER);
	}
}
