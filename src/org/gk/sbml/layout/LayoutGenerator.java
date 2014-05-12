/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout;

import org.gk.layout.Diagram;
import org.gk.sbml.model.elements.Model;

/**
 * Layout generator base class
 * 
 * @author David Croft
 *
 */
public abstract class LayoutGenerator {
	protected Model model;

	public abstract void run(Diagram modelLayout);

	public void setModel(Model model) {
		this.model = model;
	}
}
