/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.Compartment;


/**
 * Wraps org.sbml.libsbml.Compartment.
 * 
 * @author David Croft
 *
 */
public class LibsbmlCompartment extends LibsbmlSBase implements Compartment {
	private org.sbml.libsbml.Compartment sbmlElement = null;

	public LibsbmlCompartment(org.sbml.libsbml.Compartment sbmlElement) {
		super(sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.libsbml.Compartment getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		return sbmlElement.setId(id);
	}

	@Override
	public int setName(String name) {
		return sbmlElement.setName(name);
	}

	@Override
	public String getId() {
		return sbmlElement.getId();
	}

	@Override
	public int setNotes(String notes) {
		return sbmlElement.setNotes(notes);
	}
}
