/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.Document;
import org.gk.sbml.model.elements.Writer;
import org.sbml.libsbml.SBMLWriter;

/**
 * Wraps org.sbml.libsbml.Writer.
 * 
 * @author David Croft
 *
 */
public class LibsbmlWriter implements Writer {
	private org.sbml.libsbml.SBMLWriter sbmlElement = null;

	public LibsbmlWriter() {
		super();
		sbmlElement = new SBMLWriter();
	}

	@Override
	public boolean writeSBML(Document document) {
		LibsbmlOStream libsbmlOStream = new LibsbmlOStream();
		return sbmlElement.writeSBML(((LibsbmlDocument)document).getSbmlElement(), libsbmlOStream.getSbmlElement());
	}
}
