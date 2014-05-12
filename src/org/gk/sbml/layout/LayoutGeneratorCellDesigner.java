/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout;

import java.util.HashMap;

import org.gk.layout.CompartmentVertex;
import org.gk.layout.Diagram;
import org.gk.layout.Edge;
import org.gk.layout.EntityVertex;
import org.gk.layout.ReactionVertex;
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
	
	public void run(Diagram diagram) {
		for (ReactionVertex reactionVertexLayout: diagram.getReactionVertexes()) {
			addReactionVertexLayout(reactionVertexLayout);
		}
		
		for (CompartmentVertex compartmentVertexLayout: diagram.getCompartmentVertexes())
			addCompartmentLayout(compartmentVertexLayout);
		
		addToModel();
	}
	
	private void addReactionVertexLayout(ReactionVertex reactionVertexLayout) {
		Glyph reactionGlyph = new Glyph();
		reactionGlyph.setClazz("process");
		reactionGlyph.setId(reactionVertexLayout.getId());
		
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
