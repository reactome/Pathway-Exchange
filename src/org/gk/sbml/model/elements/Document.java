/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.SBMLDocument and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface Document {
	public abstract Model createModel();
	public abstract void setNotes(String notes);
	public abstract void setLevelAndVersion(int level, int version);
}
