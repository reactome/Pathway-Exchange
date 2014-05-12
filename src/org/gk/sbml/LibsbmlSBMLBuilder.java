/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import org.gk.sbml.model.libsbml.LibsbmlDocumentSingleton;

/**
 * Build SBML from Reactome using libSBML.
 * 
 * @author David Croft
 *
 */
public class LibsbmlSBMLBuilder extends SBMLBuilder {
	public LibsbmlSBMLBuilder() {
		super(new LibsbmlDocumentSingleton());
	}
}
