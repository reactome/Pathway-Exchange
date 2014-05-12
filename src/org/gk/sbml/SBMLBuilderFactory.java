/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates builders for SBML export.  Different builders are based on different
 * underlying SBML generation engines, e.g. libSBML, JSBML, etc.
 * 
 * @author David Croft
 *
 */
public class SBMLBuilderFactory {
	/**
	 * Prevent instances of this class from being created.
	 */
	private SBMLBuilderFactory() {
		super();
	}
	
	public static SBMLBuilder factory(String sbmlBuilderName) {
		if (sbmlBuilderName == null) {
			System.err.println("SBMLBuilderFactory.factory: sbmlBuilderName is null!");
			return null;
		}
		
		SBMLBuilder sbmlBuilder = null;
		if (sbmlBuilderName.equals("libSBML"))
			sbmlBuilder =  new LibsbmlSBMLBuilder();
		else if (sbmlBuilderName.equals("JSBML"))
			sbmlBuilder =  new JsbmlSBMLBuilder();
		else if (sbmlBuilderName.equals("CellDesigner"))
			sbmlBuilder =  new CellDesignerSBMLBuilder();
		
		if (sbmlBuilder == null)
			System.err.println("SBMLBuilderFactory.factory: unknown SBML builder name " + sbmlBuilderName);
		
		return sbmlBuilder;
	}
}
