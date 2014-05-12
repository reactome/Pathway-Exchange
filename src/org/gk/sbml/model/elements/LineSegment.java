/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.LineSegment and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public abstract class LineSegment {
	public abstract void setStart(Point createPoint) ;
	public abstract void setEnd(Point createPoint) ;
}
