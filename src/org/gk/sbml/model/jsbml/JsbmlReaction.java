/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.ModifierSpeciesReference;
import org.gk.sbml.model.elements.Reaction;
import org.gk.sbml.model.elements.SpeciesReference;


/**
 * Wraps org.sbml.jsbml.Reaction.
 * 
 * @author David Croft
 *
 */
public class JsbmlReaction extends JsbmlSBase implements Reaction {
	private org.sbml.jsbml.Reaction sbmlElement = null;

	public JsbmlReaction(org.sbml.jsbml.Reaction sbmlElement) {
		super(sbmlElement);
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.jsbml.Reaction getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		sbmlElement.setId(id);
		return 0;
	}

	@Override
	public String getId() {
		return sbmlElement.getId();
	}

	@Override
	public int setName(String name) {
		sbmlElement.setName(name);
		return 0;
	}

	@Override
	public int setReversible(boolean reversible) {
		sbmlElement.setReversible(reversible);
		return 0;
	}

	@Override
	public int setNotes(String notes) {
		sbmlElement.setNotes(notes);
		return 0;
	}

	@Override
	public SpeciesReference createReactant() {
		org.sbml.jsbml.SpeciesReference sbmlSpeciesReference = sbmlElement.createReactant();
		return new JsbmlSpeciesReference(sbmlSpeciesReference);
	}

	@Override
	public ModifierSpeciesReference createModifier() {
		org.sbml.jsbml.ModifierSpeciesReference sbmlModifierSpeciesReference = sbmlElement.createModifier();
		return new JsbmlModifierSpeciesReference(sbmlModifierSpeciesReference);
	}

	@Override
	public SpeciesReference createProduct() {
		org.sbml.jsbml.SpeciesReference sbmlSpeciesReference = sbmlElement.createProduct();
		return new JsbmlSpeciesReference(sbmlSpeciesReference);
	}
}
