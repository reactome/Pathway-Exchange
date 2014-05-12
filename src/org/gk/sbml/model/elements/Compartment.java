/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.Compartment and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface Compartment extends SBase {
	public int setId(String id);
	public String getId();
	public int setName(String name);
}
