/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.SpeciesReferenceGlyph and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface SpeciesReferenceGlyph {
	public int setId(String id);
	public void setSpeciesReferenceId(String id);
	public void setSpeciesGlyphId(String id);
	public Curve getCurve();
	public void setSpeciesRoleUndefined();
	public void setSpeciesRoleSubstrate();
	public void setSpeciesRoleProduct();
	public void setSpeciesRoleModifier();
}
