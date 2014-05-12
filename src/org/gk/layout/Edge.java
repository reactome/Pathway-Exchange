/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.layout;

/**
 * Edge layout info.
 * 
 * @author David Croft
 *
 */
public class Edge extends Glyph {
	private Vertex startVertex;
	private Vertex endVertex;
	private double startX;
	private double startY;
	private double endX;
	private double endY;
	private String role = null;
	private String sbmlSpeciesReferenceId = null;
	private EntityVertex entityVertexLayout = null;
	private ReactionVertex reactionVertexLayout = null;

	public Vertex getStartVertex() {
		return startVertex;
	}

	public void setStartVertex(Vertex startVertex) {
		this.startVertex = startVertex;
	}

	public Vertex getEndVertex() {
		return endVertex;
	}

	public void setEndVertex(Vertex endVertex) {
		this.endVertex = endVertex;
	}

	public double getStartX() {
		return startX;
	}

	public void setStartX(double startX) {
		this.startX = startX;
	}

	public double getStartY() {
		return startY;
	}

	public void setStartY(double startY) {
		this.startY = startY;
	}

	public double getEndX() {
		return endX;
	}

	public void setEndX(double endX) {
		this.endX = endX;
	}

	public double getEndY() {
		return endY;
	}

	public void setEndY(double endY) {
		this.endY = endY;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getSbmlSpeciesReferenceId() {
		return sbmlSpeciesReferenceId;
	}

	public void setSbmlSpeciesReferenceId(String sbmlSpeciesReferenceId) {
		this.sbmlSpeciesReferenceId = sbmlSpeciesReferenceId;
	}

	public void setEntityVertexLayout(EntityVertex entityVertexLayout) {
		this.entityVertexLayout = entityVertexLayout;
	}

	public EntityVertex getEntityVertexLayout() {
		return entityVertexLayout;
	}

	public ReactionVertex getReactionVertexLayout() {
		return reactionVertexLayout;
	}

	public void setReactionVertexLayout(ReactionVertex reactionVertexLayout) {
		this.reactionVertexLayout = reactionVertexLayout;
	}

	public static String getGlyphType() {
		return "edge";
	}
}
