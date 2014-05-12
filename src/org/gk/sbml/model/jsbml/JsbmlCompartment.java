/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.Compartment;


/**
 * Wraps org.sbml.jsbml.Compartment.
 * 
 * @author David Croft
 *
 */
public class JsbmlCompartment extends JsbmlSBase implements Compartment {
	private org.sbml.jsbml.Compartment sbmlElement = null;

	public JsbmlCompartment(org.sbml.jsbml.Compartment sbmlElement) {
		super(sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.jsbml.Compartment getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		sbmlElement.setId(id);
		return 0;
	}

	@Override
	public int setName(String name) {
		sbmlElement.setName(name);
		return 0;
	}

	@Override
	public String getId() {
		return sbmlElement.getId();
	}
}
