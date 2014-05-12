/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.ReactionGlyph and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface ReactionGlyph {
	public int setId(String id);
	public void setReactionId(String reactionId);
	public Curve getCurve();
	public SpeciesReferenceGlyph createSpeciesReferenceGlyph() ;
}
