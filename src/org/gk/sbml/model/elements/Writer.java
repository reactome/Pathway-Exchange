/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.SBMLWriter and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface Writer {
	public boolean writeSBML(Document document);
}
