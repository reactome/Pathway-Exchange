/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.elements;

/**
 * Edge layout info.
 * 
 * @author David Croft
 *
 */
public class Edge {
	private double startX;
	private double startY;
	private double endX;
	private double endY;
	private String role;
	private String sbmlSpeciesReferenceId;
	private EntityVertex entityVertexLayout = null;

	public Edge() {
		super();
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

	public EntityVertex createEntityVertexLayout() {
		if (entityVertexLayout != null)
			return entityVertexLayout;
		entityVertexLayout = new EntityVertex();
		return entityVertexLayout;
	}
}
