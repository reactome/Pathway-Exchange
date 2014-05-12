/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.Curve;
import org.gk.sbml.model.elements.ModifierSpeciesReference;
import org.gk.sbml.model.elements.ReactionGlyph;
import org.gk.sbml.model.elements.SpeciesReference;
import org.gk.sbml.model.elements.SpeciesReferenceGlyph;
import org.sbml.libsbml.libsbmlConstants;


/**
 * Wraps org.sbml.libsbml.ReactionGlyph.
 * 
 * @author David Croft
 *
 */
public class LibsbmlReactionGlyph implements ReactionGlyph {
	private org.sbml.libsbml.ReactionGlyph sbmlElement = null;

	public LibsbmlReactionGlyph(org.sbml.libsbml.ReactionGlyph sbmlReactionGlyph) {
		this.sbmlElement = sbmlReactionGlyph;
	}

	public org.sbml.libsbml.ReactionGlyph getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		return sbmlElement.setId(id);
	}

	@Override
	public void setReactionId(String reactionId) {
		sbmlElement.setReactionId(reactionId);
	}

	@Override
	public Curve getCurve() {
		org.sbml.libsbml.Curve sbmlElementCurve = sbmlElement.getCurve();
		return new LibsbmlCurve(sbmlElementCurve);
	}

	@Override
	public SpeciesReferenceGlyph createSpeciesReferenceGlyph() {
		org.sbml.libsbml.SpeciesReferenceGlyph sbmlElementSpeciesReferenceGlyph = sbmlElement.createSpeciesReferenceGlyph();
		return new LibsbmlSpeciesReferenceGlyph(sbmlElementSpeciesReferenceGlyph);
	}
}
