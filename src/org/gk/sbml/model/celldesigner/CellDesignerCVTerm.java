/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import org.gk.sbml.model.elements.CVTerm;


/**
 * Wraps org.sbml.jsbml.SBase.
 * 
 * @author David Croft
 *
 */
public class CellDesignerCVTerm implements CVTerm {
	private org.gk.sbml.simcd.SimCDCVTerm sbmlElement = null;

	public CellDesignerCVTerm() {
		sbmlElement = new org.gk.sbml.simcd.SimCDCVTerm();
	}

	public org.gk.sbml.simcd.SimCDCVTerm getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int addResource(String resource) {
		sbmlElement.addResource(resource);
		return 0;
	}

	@Override
	public int setQualifierTypeBiological() {
		sbmlElement.setQualifierType(org.gk.sbml.simcd.SimCDCVTerm.Type.BIOLOGICAL_QUALIFIER);
		return 0;
	}

	@Override
	public int setBiologicalQualifierTypeBqbIs() {
		sbmlElement.setBiologicalQualifierType(org.gk.sbml.simcd.SimCDCVTerm.Qualifier.BQB_IS);
		return 0;
	}

	@Override
	public int setBiologicalQualifierTypeBqbIsDescribedBy() {
		sbmlElement.setBiologicalQualifierType(org.gk.sbml.simcd.SimCDCVTerm.Qualifier.BQB_IS_DESCRIBED_BY);
		return 0;
	}

	@Override
	public int setBiologicalQualifierTypeBqbIsHomologTo() {
		sbmlElement.setBiologicalQualifierType(org.gk.sbml.simcd.SimCDCVTerm.Qualifier.BQB_IS_HOMOLOG_TO);
		return 0;
	}

	@Override
	public int setBiologicalQualifierTypeBqbHasPart() {
		sbmlElement.setBiologicalQualifierType(org.gk.sbml.simcd.SimCDCVTerm.Qualifier.BQB_HAS_PART);
		return 0;
	}

	@Override
	public int setBiologicalQualifierTypeBqbHasVersion() {
		sbmlElement.setBiologicalQualifierType(org.gk.sbml.simcd.SimCDCVTerm.Qualifier.BQB_HAS_VERSION);
		return 0;
	}
}
