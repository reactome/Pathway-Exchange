/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.simcd;

import org.sbml.jsbml.SBMLDocument;

/**
 * Extend JSBML to simulate some aspects of CellDesigner SBML.
 * 
 * @author David Croft
 *
 */
public class SimCDDocument extends SBMLDocument {
	public SimCDDocument() {
		super();
	}

	public SimCDDocument(int level, int version) {
		super(level, version);
	}

	public SimCDDocument(SBMLDocument sb) {
		super(sb);
	}
}
