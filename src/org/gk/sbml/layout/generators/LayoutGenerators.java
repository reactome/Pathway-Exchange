/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.generators;

import java.util.ArrayList;
import java.util.List;

import org.gk.sbml.layout.elements.Diagram;
import org.gk.sbml.model.elements.Model;

/**
 * Generates and runs layout generators for you.
 * 
 * @author David Croft
 *
 */
public class LayoutGenerators {
	private List<LayoutGenerator> layoutGenerators = new ArrayList<LayoutGenerator>();
	private Model model;

	private LayoutGenerator factory(String layoutGeneratorName) {
		LayoutGenerator layoutGenerator = null;
		if (layoutGeneratorName.equals("SBGN")) {
			layoutGenerator = new LayoutGeneratorSBGNPD();
		} else if (layoutGeneratorName.equals("Extension")) {
			layoutGenerator = new LayoutGeneratorExtension();
		} else if (layoutGeneratorName.equals("CellDesigner")) {
			layoutGenerator = new LayoutGeneratorCellDesigner();
		} else
			System.err.println("ReactomeToSBML.addLayoutGenerator: WARNING - unknown layout generator: " + layoutGeneratorName);
	
		return layoutGenerator;
	}
	
	public void setModel(Model model) {
		this.model = model;
	}

	public boolean isGeneratorsAvailable() {
		return layoutGenerators.size() > 0;
	}
	
	public void add(String layoutGeneratorName) {
		LayoutGenerator layoutGenerator = factory(layoutGeneratorName);
		if (layoutGenerator != null)
			layoutGenerators.add(layoutGenerator);
	}

	public void run(Diagram diagram) {
		if (model == null)
			System.err.println("ReactomeToSBML.addLayoutGenerator: WARNING - model is null");
		
		for (LayoutGenerator layoutGenerator: layoutGenerators) {
			layoutGenerator.setModel(model);
//			System.err.println("ReactomeToSBML.runLayoutGenerators: running " + layoutGenerator.toString());
			layoutGenerator.run(diagram);
		}
	}
}
