/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.ModelCreator;


/**
 * Wraps org.sbml.libsbml.ModelCreator.
 * 
 * @author David Croft
 *
 */
public class LibsbmlModelCreator implements ModelCreator {
	private org.sbml.libsbml.ModelCreator sbmlElement = null;

	public LibsbmlModelCreator() {
		sbmlElement = new org.sbml.libsbml.ModelCreator();
	}

	public org.sbml.libsbml.ModelCreator getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setEmail(String email) {
		return sbmlElement.setEmail(email);
	}

	@Override
	public int setFamilyName(String familyName) {
		return sbmlElement.setFamilyName(familyName);
	}

	@Override
	public int setGivenName(String givenName) {
		return sbmlElement.setGivenName(givenName);
	}

	@Override
	public int setOrganisation(String organisation) {
		return sbmlElement.setOrganisation(organisation);
	}
}
