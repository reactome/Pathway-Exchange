/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.libsbml.Species and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface Species extends SBase {
	public int setId(String id);
	public String getId();
	public int setName(String name);
	public int setCompartment(String compartmentId);
}
