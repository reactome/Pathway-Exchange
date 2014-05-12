/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;


/**
 * Wraps org.sbml.libsbml.ModelCreator and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface ModelCreator {
	public int setEmail(String email);
	public int setFamilyName(String familyName);
	public int setGivenName(String givenName);
	public int setOrganisation(String organisation);
}
