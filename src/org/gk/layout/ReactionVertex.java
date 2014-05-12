/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.layout;

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
	private String sbmlReactionId = null;

	public List<Edge> getEdgeLayouts() {
		return edgeLayouts;
	}

	public void addEdgeLayout(Edge edgeLayout) {
		edgeLayouts.add(edgeLayout);
	}

	public String getReactionType() {
		return reactionType;
	}

	public void setReactionType(String reactionType) {
		this.reactionType = reactionType;
	}
	
	public static String getGlyphType() {
		return "reactionVertex";
	}

	public String getSbmlReactionId() {
		return sbmlReactionId;
	}

	public void setSbmlReactionId(String sbmlReactionId) {
		this.sbmlReactionId = sbmlReactionId;
	}
}
