/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.SBase and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface SBase {
	public int setMetaId(String metaId);
	public int addCVTerm(CVTerm cVTerm);
	public CVTerm createCVTerm();
	public int setNotes(String notes);
	public int setSBOTerm(int sboid);
}
