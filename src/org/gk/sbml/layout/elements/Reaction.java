/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Reaction layout info.
 * 
 * @author David Croft
 *
 */
public class Reaction extends Glyph {
	private String sbmlReactionId;
	private List<ReactionVertex> reactionVertexLayouts = new ArrayList<ReactionVertex>();

	public Reaction() {
		super();
	}

	public String getSbmlReactionId() {
		return sbmlReactionId;
	}

	public void setSbmlReactionId(String sbmlReactionId) {
		this.sbmlReactionId = sbmlReactionId;
	}

	public List<ReactionVertex> getReactionVertexLayouts() {
		return reactionVertexLayouts;
	}

	public ReactionVertex createReactionVertexLayout() {
		ReactionVertex reactionVertexLayout = new ReactionVertex();
		reactionVertexLayouts.add(reactionVertexLayout);
		return reactionVertexLayout;
	}
}
