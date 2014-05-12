/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.SpeciesGlyph and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface SpeciesGlyph {
	public int setId(String id);
	public void setSpeciesId(String id);
	public void setBoundingBox(BoundingBox boundingBox);
	public String getId();
}
