/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.ModelCreator;


/**
 * Wraps org.sbml.libsbml.ModelCreator.
 * 
 * @author David Croft
 *
 */
public class JsbmlModelCreator implements ModelCreator {
	private org.sbml.jsbml.Creator sbmlElement = null;

	public JsbmlModelCreator() {
		sbmlElement = new org.sbml.jsbml.Creator();
	}

	public org.sbml.jsbml.Creator getSbmlElement() {
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
		sbmlElement.setOrganisation(organisation);
		return 0;
	}
}
