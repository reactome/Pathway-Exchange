/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.CompartmentGlyph and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface CompartmentGlyph {
	public int setId(String id);
	public void setCompartmentId(String id);
	public String getId();
	void setBoundingBox(BoundingBox boundingBox);
}
