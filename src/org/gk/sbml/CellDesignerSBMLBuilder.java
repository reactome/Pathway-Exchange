/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import org.gk.sbml.model.celldesigner.CellDesignerDocumentSingleton;

/**
 * Build SBML from Reactome using CellDesigner SBML.
 * 
 * @author David Croft
 *
 */
public class CellDesignerSBMLBuilder extends SBMLBuilder {
	public CellDesignerSBMLBuilder() {
		super(new CellDesignerDocumentSingleton());
	}
}
