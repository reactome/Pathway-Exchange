/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.CVTerm.CVTerm and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface CVTerm {
	public int addResource(String resource);
	public int setQualifierTypeBiological();
	public int setBiologicalQualifierTypeBqbIs();
	public int setBiologicalQualifierTypeBqbIsDescribedBy();
	public int setBiologicalQualifierTypeBqbIsHomologTo();
	public int setBiologicalQualifierTypeBqbHasPart();
	public int setBiologicalQualifierTypeBqbHasVersion();
}
