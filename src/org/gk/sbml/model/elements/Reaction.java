/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.Reaction and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface Reaction extends SBase {
	public int setId(String id);
	public String getId();
	public int setName(String name);
	public int setReversible(boolean reversible);
	public SpeciesReference createReactant();
	public ModifierSpeciesReference createModifier();
	public SpeciesReference createProduct();
}
