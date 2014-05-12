/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.CVTerm.CVTerm and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface TextGlyph {
	void setBoundingBox(BoundingBox createBoundingBox);
	void setText(String title);
	void setOriginOfTextId(String sbmlReactionId);
	void setGraphicalObjectId(String sbmlReactionId);
}
