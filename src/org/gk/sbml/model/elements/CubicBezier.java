/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.CubicBezier and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public abstract class CubicBezier {
	public abstract void setStart(Point point) ;
	public abstract void setEnd(Point point) ;
	public abstract void setBasePoint1(Point point) ;
	public abstract void setBasePoint2(Point point) ;
}
