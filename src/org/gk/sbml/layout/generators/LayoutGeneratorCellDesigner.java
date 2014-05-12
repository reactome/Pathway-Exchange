/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.generators;

import java.util.HashMap;

import org.gk.sbml.layout.elements.CompartmentVertex;
import org.gk.sbml.layout.elements.Diagram;
import org.gk.sbml.layout.elements.Edge;
import org.gk.sbml.layout.elements.EntityVertex;
import org.gk.sbml.layout.elements.Reaction;
import org.gk.sbml.layout.elements.ReactionVertex;
import org.gk.sbml.model.elements.Model;
import org.sbgn.bindings.Glyph;

/**
 * Layout generator using CellDesigner.  Somewhat experimental.
 * 
 * @author David Croft
 *
 */
public class LayoutGeneratorCellDesigner extends LayoutGenerator {
	private HashMap<Long,Glyph> entityVertexLayoutSeen = new HashMap<Long,Glyph>();
	
	public void setModel(Model model) {
		super.setModel(model);
	}
	
	public void run(Diagram modelLayout) {
		for (Reaction reactionLayout: modelLayout.getReactionLayouts())
			addReactionLayout(reactionLayout);
		
		for (CompartmentVertex compartmentVertexLayout: modelLayout.getCompartmentVertexLayouts())
			addCompartmentLayout(compartmentVertexLayout);
		
		addToModel();
	}
	
	private void addReactionLayout(Reaction reactionLayout) {
		String sbmlReactionId = reactionLayout.getSbmlReactionId();

		for (ReactionVertex reactionVertexLayout: reactionLayout.getReactionVertexLayouts()) {
			addReactionVertexLayout(reactionVertexLayout, sbmlReactionId);
		}
	}
	
	private void addReactionVertexLayout(ReactionVertex reactionVertexLayout, String sbmlReactionId) {
		Glyph reactionGlyph = new Glyph();
		reactionGlyph.setClazz("process");
		reactionGlyph.setId(newReactionGlyphId());
		
		for (Edge edgeLayout: reactionVertexLayout.getEdgeLayouts()) {
			EntityVertex entityVertexLayout = edgeLayout.getEntityVertexLayout();
			Glyph speciesGlyph = addEntityVertexLayout(entityVertexLayout);

			addEdgeLayout(edgeLayout, reactionGlyph, speciesGlyph);
		}
	}
	
	private Glyph addEntityVertexLayout(EntityVertex entityVertexLayout) {
		Long entityVertexDbId = entityVertexLayout.getEntityVertexDbId();
		Glyph speciesGlyph = entityVertexLayoutSeen.get(entityVertexDbId);
		
		return speciesGlyph;
	}

	private void addEdgeLayout(Edge edgeLayout, Glyph reactionGlyph, Glyph speciesGlyph) {
	}

	private void addCompartmentLayout(CompartmentVertex compartmentVertexLayout) {
	}
	
	public void addToModel() {
	}
}
