/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.Document;
import org.gk.sbml.model.elements.Model;
import org.sbml.libsbml.SBMLDocument;

/**
 * Wraps org.sbml.libsbml.SBMLDocument.
 * 
 * @author David Croft
 *
 */
public class LibsbmlDocument implements Document {
	private SBMLDocument sbmlElement = null;
	
	public LibsbmlDocument(int level, int version) {
		sbmlElement = new SBMLDocument(level, version);
	}

	public org.sbml.libsbml.SBMLDocument getSbmlElement() {
		return sbmlElement;
	}

	public Model createModel() {
		org.sbml.libsbml.Model sbmlModel = sbmlElement.createModel();
		
		return new LibsbmlModel(sbmlModel);
	}
	
	public void setNotes(String notes) {
		sbmlElement.setNotes(notes);
	}
	
	public void setLevelAndVersion(int level, int version) {
//		sbmlElement.setLevelAndVersion(level, version);
		sbmlElement = new SBMLDocument(level, version); // TODO: potentially dangerous
	}
}
