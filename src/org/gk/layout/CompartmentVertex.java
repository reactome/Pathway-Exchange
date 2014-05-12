/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.layout;

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

	public static String getGlyphType() {
		return "compartmentVertex";
	}
}
