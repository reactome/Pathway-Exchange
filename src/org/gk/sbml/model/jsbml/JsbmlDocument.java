/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.Document;
import org.gk.sbml.model.elements.Model;
import org.sbml.jsbml.SBMLDocument;

/**
 * Wraps org.sbml.jsbml.SBMLDocument.
 * 
 * @author David Croft
 *
 */
public class JsbmlDocument implements Document {
	private SBMLDocument sbmlElement = null;
	private int modelCounter = 0;
	
	public JsbmlDocument(int level, int version) {
		sbmlElement = new SBMLDocument(level, version);
	}

	public org.sbml.jsbml.SBMLDocument getSbmlElement() {
		return sbmlElement;
	}

	public Model createModel() {
		org.sbml.jsbml.Model sbmlModel = sbmlElement.createModel("Model_" + modelCounter);
		modelCounter++;
		
		return new JsbmlModel(sbmlModel);
	}
	
	public void setNotes(String notes) {
		sbmlElement.setNotes(notes);
	}
	
	public void setLevelAndVersion(int level, int version) {
//		sbmlElement.setLevelAndVersion(level, version);
		sbmlElement = new SBMLDocument(level, version); // TODO: potentially dangerous
	}
}
