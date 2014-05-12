/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.layout;

import java.util.ArrayList;
import java.util.List;

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
	private int currentPortNum = 1;

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
	
	public int getCurrentPortNum() {
		return currentPortNum++;
	}

	/**
	 * Get a list of the 4 mid-boundary points, in the order North, East,
	 * South, West.
	 * 
	 * @return
	 */
	public List<Point> getMidBoundaryPoints() {
		List<Point> midBoundaryPoints = new ArrayList<Point>();
		double halfWidth = width/2.0;
		double halfHeight = height/2.0;
		midBoundaryPoints.add(new Point(x + halfWidth, y)); // North
		midBoundaryPoints.add(new Point(x + width, y + halfHeight)); // East
		midBoundaryPoints.add(new Point(x + halfWidth, y + height)); // South
		midBoundaryPoints.add(new Point(x, y + halfHeight)); // West
		
		return midBoundaryPoints;
	}
}
