/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Reaction vertex layout info.
 * 
 * @author David Croft
 *
 */
public class ReactionVertex extends Vertex {
	private List<Edge> edgeLayouts = new ArrayList<Edge>(); // edges from inputs, outputs and catalysts
	private String reactionType = null;

	public ReactionVertex() {
		super();
	}

	public List<Edge> getEdgeLayouts() {
		return edgeLayouts;
	}

	public String getReactionType() {
		return reactionType;
	}

	public void setReactionType(String reactionType) {
		this.reactionType = reactionType;
	}

	public Edge createEdgeLayout() {
		Edge edgeLayout = new Edge();
		edgeLayouts.add(edgeLayout);
		return edgeLayout;
	}
}
