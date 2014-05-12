/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.CVTerm;
import org.sbml.libsbml.libsbmlConstants;


/**
 * Wraps org.sbml.libsbml.CVTerm.
 * 
 * @author David Croft
 *
 */
public class LibsbmlCVTerm implements CVTerm {
	private org.sbml.libsbml.CVTerm sbmlElement = null;

	public LibsbmlCVTerm() {
		sbmlElement = new org.sbml.libsbml.CVTerm();
	}

	public org.sbml.libsbml.CVTerm getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int addResource(String resource) {
		return sbmlElement.addResource(resource);
	}

	@Override
	public int setQualifierTypeBiological() {
		return sbmlElement.setQualifierType(libsbmlConstants.BIOLOGICAL_QUALIFIER);
	}

	@Override
	public int setBiologicalQualifierTypeBqbIs() {
		return sbmlElement.setBiologicalQualifierType(libsbmlConstants.BQB_IS);
	}

	@Override
	public int setBiologicalQualifierTypeBqbIsDescribedBy() {
		return sbmlElement.setBiologicalQualifierType(libsbmlConstants.BQB_IS_DESCRIBED_BY);
	}

	@Override
	public int setBiologicalQualifierTypeBqbIsHomologTo() {
		return sbmlElement.setBiologicalQualifierType(libsbmlConstants.BQB_IS_HOMOLOG_TO);
	}

	@Override
	public int setBiologicalQualifierTypeBqbHasPart() {
		return sbmlElement.setBiologicalQualifierType(libsbmlConstants.BQB_HAS_PART);
	}

	@Override
	public int setBiologicalQualifierTypeBqbHasVersion() {
		return sbmlElement.setBiologicalQualifierType(libsbmlConstants.BQB_HAS_VERSION);
	}
}
