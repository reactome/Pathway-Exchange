/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.SpeciesReference and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface SpeciesReference extends SBase {
	public int setSpecies(String speciesId);
	public int setId(String id);
	public double getStoichiometry();
	public int setStoichiometry(double stoichiometry);
}
