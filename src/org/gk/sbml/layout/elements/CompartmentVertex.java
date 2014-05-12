/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.elements;

/**
 * Compartment vertex layout info.
 * 
 * @author David Croft
 *
 */
public class CompartmentVertex extends Vertex {
	private Long compartmentDbId;
	double textX;
	double textY;
	
	public CompartmentVertex() {
		super();
	}

	public Long getCompartmentDbId() {
		return compartmentDbId;
	}

	public void setCompartmentDbId(Long compartmentDbId) {
		this.compartmentDbId = compartmentDbId;
	}

	public double getTextX() {
		return textX;
	}

	public void setTextX(double textX) {
		this.textX = textX;
	}

	public double getTextY() {
		return textY;
	}

	public void setTextY(double textY) {
		this.textY = textY;
	}
}
