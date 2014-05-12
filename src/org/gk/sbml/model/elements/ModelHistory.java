/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;


/**
 * Wraps org.sbml.libsbml.ModelHistory and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface ModelHistory {
	public int addCreator(ModelCreator modelCreator);
	public int setModifiedDate(Date date);
	public int setCreatedDate(Date date);
}
