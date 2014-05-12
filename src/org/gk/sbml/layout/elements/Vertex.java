/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.elements;

/**
 * Generic vertex layout info.
 * 
 * @author David Croft
 *
 */
public abstract class Vertex extends Glyph {
	private double x;
	private double y;
	private double width;
	private double height;

	public Vertex() {
		super();
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}
}
