/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;


/**
 * Wraps org.sbml.libsbml.Layout and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface Layout {
	public String getId();
	public int setId(String id);
	public int setName(String name);
	public int setMetaId(String metaId);
	public int addCVTerm(CVTerm cVTerm);
	public TextGlyph createTextGlyph();
	public void setDimensions(Dimensions dimensions);
	public Dimensions createDimensions(double width, double height);
	public BoundingBox createBoundingBox(String id, double x, double y, double width, double height);
	public Point createPoint(double x, double y);
	public ReactionGlyph createReactionGlyph();
	public SpeciesGlyph createSpeciesGlyph();
	public CompartmentGlyph createCompartmentGlyph();
}
