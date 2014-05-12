/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.layout;

/**
 * Two points.
 * 
 * @author David Croft
 *
 */
public class PointPair {
	public Point point1;
	public Point point2;
	
	public PointPair(Point point1, Point point2) {
		this.point1 = point1;
		this.point2 = point2;
	}
	
	public double separation() {
		double separationX = point1.x - point2.x;
		double separationY = point1.y - point2.y;
		return (separationX * separationX) + (separationY * separationY);
	}
}
