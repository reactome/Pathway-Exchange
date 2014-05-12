/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.ModifierSpeciesReference;
import org.gk.sbml.model.elements.Reaction;
import org.gk.sbml.model.elements.SpeciesReference;


/**
 * Wraps org.sbml.libsbml.Reaction.
 * 
 * @author David Croft
 *
 */
public class LibsbmlReaction extends LibsbmlSBase implements Reaction {
	private org.sbml.libsbml.Reaction sbmlElement = null;

	public LibsbmlReaction(org.sbml.libsbml.Reaction sbmlElement) {
		super(sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.libsbml.Reaction getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		return sbmlElement.setId(id);
	}

	@Override
	public String getId() {
		return sbmlElement.getId();
	}

	@Override
	public int setMetaId(String metaId) {
		return sbmlElement.setMetaId(metaId);
	}

	@Override
	public int setName(String name) {
		return sbmlElement.setName(name);
	}

	@Override
	public int setReversible(boolean reversible) {
		return sbmlElement.setReversible(reversible);
	}

	@Override
	public int setNotes(String notes) {
		return sbmlElement.setNotes(notes);
	}

	@Override
	public int addCVTerm(CVTerm cVTerm) {
		org.sbml.libsbml.CVTerm nativeCVTerm = ((LibsbmlCVTerm)cVTerm).getSbmlElement();
		return sbmlElement.addCVTerm(nativeCVTerm);
	}

	@Override
	public SpeciesReference createReactant() {
		org.sbml.libsbml.SpeciesReference sbmlSpeciesReference = sbmlElement.createReactant();
		return new LibsbmlSpeciesReference(sbmlSpeciesReference);
	}

	@Override
	public ModifierSpeciesReference createModifier() {
		org.sbml.libsbml.ModifierSpeciesReference sbmlModifierSpeciesReference = sbmlElement.createModifier();
		return new LibsbmlModifierSpeciesReference(sbmlModifierSpeciesReference);
	}

	@Override
	public SpeciesReference createProduct() {
		org.sbml.libsbml.SpeciesReference sbmlSpeciesReference = sbmlElement.createProduct();
		return new LibsbmlSpeciesReference(sbmlSpeciesReference);
	}
}
