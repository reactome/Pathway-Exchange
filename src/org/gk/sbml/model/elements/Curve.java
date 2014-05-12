/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.Curve and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public abstract class Curve {
	public abstract LineSegment createLineSegment() ;
	public abstract CubicBezier createCubicBezier() ;
}
