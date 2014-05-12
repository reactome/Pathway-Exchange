/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.OStream;

/**
 * Wraps org.sbml.libsbml.OStream.
 * 
 * @author David Croft
 *
 */
public class LibsbmlOStream extends OStream {
	private org.sbml.libsbml.OStream sbmlElement = null;

	public LibsbmlOStream() {
		super();
		sbmlElement = new org.sbml.libsbml.OStream(org.sbml.libsbml.OStream.COUT);
	}

	public org.sbml.libsbml.OStream getSbmlElement() {
		return sbmlElement;
	}
}
