/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.CVTerm;


/**
 * Wraps org.sbml.jsbml.CVTerm.
 * 
 * @author David Croft
 *
 */
public class JsbmlCVTerm implements CVTerm {
	private org.sbml.jsbml.CVTerm sbmlElement = null;

	public JsbmlCVTerm() {
		sbmlElement = new org.sbml.jsbml.CVTerm();
	}

	public org.sbml.jsbml.CVTerm getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int addResource(String resource) {
		sbmlElement.addResource(resource);
		return 0;
	}

	@Override
	public int setQualifierTypeBiological() {
		sbmlElement.setQualifierType(org.sbml.jsbml.CVTerm.Type.BIOLOGICAL_QUALIFIER);
		return 0;
	}

	@Override
	public int setBiologicalQualifierTypeBqbIs() {
		sbmlElement.setBiologicalQualifierType(org.sbml.jsbml.CVTerm.Qualifier.BQB_IS);
		return 0;
	}

	@Override
	public int setBiologicalQualifierTypeBqbIsDescribedBy() {
		sbmlElement.setBiologicalQualifierType(org.sbml.jsbml.CVTerm.Qualifier.BQB_IS_DESCRIBED_BY);
		return 0;
	}

	@Override
	public int setBiologicalQualifierTypeBqbIsHomologTo() {
		sbmlElement.setBiologicalQualifierType(org.sbml.jsbml.CVTerm.Qualifier.BQB_IS_HOMOLOG_TO);
		return 0;
	}

	@Override
	public int setBiologicalQualifierTypeBqbHasPart() {
		sbmlElement.setBiologicalQualifierType(org.sbml.jsbml.CVTerm.Qualifier.BQB_HAS_PART);
		return 0;
	}

	@Override
	public int setBiologicalQualifierTypeBqbHasVersion() {
		sbmlElement.setBiologicalQualifierType(org.sbml.jsbml.CVTerm.Qualifier.BQB_HAS_VERSION);
		return 0;
	}
}
