/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.libsbml and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public abstract class Lib {
	public abstract String writeSBMLToString(Document document);
	public abstract int writeSBMLToFile(Document document, String filename);
}
