/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import org.gk.sbml.model.elements.ModelCreator;


/**
 * Wraps org.sbml.libsbml.ModelCreator.
 * 
 * @author David Croft
 *
 */
public class CellDesignerModelCreator implements ModelCreator {
	private org.gk.sbml.simcd.SimCDCreator sbmlElement = null;

	public CellDesignerModelCreator() {
		sbmlElement = new org.gk.sbml.simcd.SimCDCreator();
	}

	public org.gk.sbml.simcd.SimCDCreator getSbmlElement() {
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
