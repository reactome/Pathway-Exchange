/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout;

import org.gk.layout.Diagram;
import org.gk.sbgn.Dumper;
import org.gk.sbgn.PDExtractor;

/**
 * Layout generator producing SBGN PD.
 * 
 * This is nice and simple, because most of the work is already done in the PDExtractor.
 * 
 * @author David Croft
 *
 */
public class LayoutGeneratorSBGNPD extends LayoutGenerator {
	public void run(Diagram modelLayout) {
		PDExtractor pdExtractor = new PDExtractor();
		pdExtractor.extract(modelLayout); 

		String sbgnPdString = Dumper.dumpToString(pdExtractor.getSbgn());
		if (sbgnPdString == null)
			System.err.println("LayoutGeneratorSBGNPD.run: WARNING - sbgnPdString is null");
		else
			model.appendAnnotation(sbgnPdString);
	}
}
