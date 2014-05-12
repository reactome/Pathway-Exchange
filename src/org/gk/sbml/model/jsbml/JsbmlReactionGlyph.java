/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.Curve;
import org.gk.sbml.model.elements.ModifierSpeciesReference;
import org.gk.sbml.model.elements.ReactionGlyph;
import org.gk.sbml.model.elements.SpeciesReference;
import org.gk.sbml.model.elements.SpeciesReferenceGlyph;


/**
 * Wraps org.sbml.jsbml.ReactionGlyph.
 * 
 * The layout extension is not available in the current JSBML.
 * 
 * @author David Croft
 *
 */
public class JsbmlReactionGlyph implements ReactionGlyph {
//	private org.sbml.jsbml.ReactionGlyph sbmlElement = null;
//
//	public JsbmlReactionGlyph(org.sbml.jsbml.ReactionGlyph sbmlReactionGlyph) {
//		this.sbmlElement = sbmlReactionGlyph;
//	}
//
//	public org.sbml.jsbml.ReactionGlyph getSbmlElement() {
//		return sbmlElement;
//	}

	@Override
	public int setId(String id) {
//		return sbmlElement.setId(id);
		return 0;
	}

	@Override
	public void setReactionId(String reactionId) {
//		sbmlElement.setReactionId(reactionId);
	}

	@Override
	public Curve getCurve() {
//		org.sbml.jsbml.Curve sbmlElementCurve = sbmlElement.getCurve();
//		return new JsbmlCurve(sbmlElementCurve);
		return null;
	}

	@Override
	public SpeciesReferenceGlyph createSpeciesReferenceGlyph() {
//		org.sbml.jsbml.SpeciesReferenceGlyph sbmlElementSpeciesReferenceGlyph = sbmlElement.createSpeciesReferenceGlyph();
//		return new JsbmlSpeciesReferenceGlyph(sbmlElementSpeciesReferenceGlyph);
		return null;
	}
}
