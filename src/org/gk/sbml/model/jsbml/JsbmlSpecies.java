/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.Species;


/**
 * Wraps org.sbml.jsbml.Species.
 * 
 * @author David Croft
 *
 */
public class JsbmlSpecies extends JsbmlSBase implements Species {
	private org.sbml.jsbml.Species sbmlElement = null;

	public JsbmlSpecies(org.sbml.jsbml.Species sbmlElement) {
		super(sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.jsbml.Species getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		sbmlElement.setId(id);
		return 0;
	}

	@Override
	public int setMetaId(String metaId) {
		sbmlElement.setMetaId(metaId);
		return 0;
	}

	@Override
	public int setName(String name) {
		sbmlElement.setName(name);
		return 0;
	}

	@Override
	public int addCVTerm(CVTerm cVTerm) {
		sbmlElement.addCVTerm(((JsbmlCVTerm)cVTerm).getSbmlElement());
		return 0;
	}

	@Override
	public int setCompartment(String compartmentId) {
		sbmlElement.setCompartment(compartmentId);
		return 0;
	}

	@Override
	public String getId() {
		return sbmlElement.getId();
	}
}
