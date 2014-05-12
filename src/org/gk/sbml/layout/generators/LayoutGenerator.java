/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.generators;

import org.gk.sbml.layout.elements.Diagram;
import org.gk.sbml.model.elements.Model;

/**
 * Layout generator base class
 * 
 * @author David Croft
 *
 */
public abstract class LayoutGenerator {
	protected Model model;
	private int boundingBoxNumber = 0;
	private int speciesGlyphNumber = 0;
	private int speciesReferenceGlyphNumber = 0;
	private int reactionGlyphNumber = 0;
	private int compartmentGlyphNumber = 0;

	public abstract void run(Diagram modelLayout);

	public void setModel(Model model) {
		this.model = model;
	}

	protected String newBoundingBoxId() {
		return "bb" + boundingBoxNumber++;
	}

	protected String newSpeciesGlyphId() {
		return "SpeciesGlyph_" + speciesGlyphNumber++;
	}

	protected String newSpeciesReferenceGlyphId() {
		return "SpeciesReferenceGlyph_" + speciesReferenceGlyphNumber++;
	}

	protected String newReactionGlyphId() {
		return "ReactionGlyph_" + reactionGlyphNumber++;
	}

	protected String newArcId() {
		return "Arc_" + reactionGlyphNumber++;
	}

	protected String newCompartmentGlyphId() {
		return "CompartmentGlyph_" + compartmentGlyphNumber++;
	}
}
