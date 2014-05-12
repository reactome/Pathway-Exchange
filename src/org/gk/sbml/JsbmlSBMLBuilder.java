/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import org.gk.sbml.model.jsbml.JsbmlDocumentSingleton;

/**
 * Build SBML from Reactome using JSBML.
 * 
 * @author David Croft
 *
 */
public class JsbmlSBMLBuilder extends SBMLBuilder {
	public JsbmlSBMLBuilder() {
		super(new JsbmlDocumentSingleton());
	}
}
