/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import org.gk.sbml.model.elements.ModifierSpeciesReference;
import org.gk.sbml.model.elements.Reaction;
import org.gk.sbml.model.elements.SpeciesReference;
import org.gk.sbml.simcd.SimCDModifierSpeciesReference;
import org.gk.sbml.simcd.SimCDSBase;
import org.gk.sbml.simcd.SimCDSpeciesReference;


/**
 * Wraps org.sbml.jsbml.Reaction.
 * 
 * @author David Croft
 *
 */
public class CellDesignerReaction extends CellDesignerSBase implements Reaction {
	private org.gk.sbml.simcd.SimCDReaction sbmlElement = null;

	public CellDesignerReaction(org.gk.sbml.simcd.SimCDReaction sbmlElement) {
		super((SimCDSBase) sbmlElement);
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
		org.gk.sbml.simcd.SimCDSpeciesReference sbmlSpeciesReference = (SimCDSpeciesReference) sbmlElement.createReactant();
		return new CellDesignerSpeciesReference(sbmlSpeciesReference);
	}

	@Override
	public ModifierSpeciesReference createModifier() {
		org.gk.sbml.simcd.SimCDModifierSpeciesReference sbmlModifierSpeciesReference = (SimCDModifierSpeciesReference) sbmlElement.createModifier();
		return new CellDesignerModifierSpeciesReference(sbmlModifierSpeciesReference);
	}

	@Override
	public SpeciesReference createProduct() {
		org.gk.sbml.simcd.SimCDSpeciesReference sbmlSpeciesReference = (SimCDSpeciesReference) sbmlElement.createProduct();
		return new CellDesignerSpeciesReference(sbmlSpeciesReference);
	}
}
