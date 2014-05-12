/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.Document;
import org.gk.sbml.model.elements.Lib;

/**
 * Wraps org.sbml.libsbml.Lib.
 * 
 * @author David Croft
 *
 */
public class LibsbmlLib extends Lib {
	@Override
	public String writeSBMLToString(Document document) {
		return org.sbml.libsbml.libsbml.writeSBMLToString(((LibsbmlDocument)document).getSbmlElement());
	}

	@Override
	public int writeSBMLToFile(Document document, String filename) {
		return org.sbml.libsbml.libsbml.writeSBMLToFile(((LibsbmlDocument)document).getSbmlElement(), filename);
	}
}
