/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.util.List;
import java.util.Map;

import org.gk.convert.common.DatabaseConnectionHandler;
import org.gk.convert.common.PathwayReactionHashHandler;
import org.gk.layout.Extractor;
import org.gk.persistence.MySQLAdaptor;
import org.gk.sbml.layout.LayoutGenerators;

/**
 * Export SBML and layout from Reactome.
 * 
 * This implementation uses generic builders for SBML and layout,
 * enabling it to work with libSBML, JSBML and potentially other
 * SBML generation packages (e.g. CellDesigner).
 * 
 * @author David Croft
 *
 */
public class SBMLAndLayoutBuilder {
	protected SBMLBuilder sbmlBuilder = SBMLBuilderFactory.factory("JSBML"); // default
	protected Extractor layoutExtractor = new Extractor();
	protected LayoutGenerators layoutGenerators = new LayoutGenerators();
	protected boolean autogenerateKineticFlag = false;
	protected String autogenerateKineticServletUrl = null;
	private DatabaseConnectionHandler databaseConnectionHandler = new DatabaseConnectionHandler();
	private PathwayReactionHashHandler pathwayReactionHashHandler = new PathwayReactionHashHandler();
	
	public PathwayReactionHashHandler getPathwayReactionHashHandler() {
		return pathwayReactionHashHandler;
	}

	public DatabaseConnectionHandler getDatabaseConnectionHandler() {
		return databaseConnectionHandler;
	}

	public void setAutogenerateKineticFlag(boolean autogenerateKineticFlag) {
		this.autogenerateKineticFlag = autogenerateKineticFlag;
	}

	protected void setAutogenerateKineticServletUrl(String autogenerateKineticServletUrl) {
		this.autogenerateKineticServletUrl = autogenerateKineticServletUrl;
	}

	public void setSbmlBuilder(String sbmlBuilderName) {
		sbmlBuilder = SBMLBuilderFactory.factory(sbmlBuilderName);
	}

	public SBMLBuilder getSbmlBuilder() {
		return sbmlBuilder;
	}

	/**
	 * This method will do conversion to SBML using the settings accumulated.
	 */
	public void convertPathways() {
		MySQLAdaptor databaseAdaptor = getDatabaseConnectionHandler().getDatabaseAdaptor();
		
		// Make sure that everything that needs a dba has one before starting
		sbmlBuilder.setDatabaseAdaptor(databaseAdaptor);
		layoutExtractor.setDatabaseAdaptor(databaseAdaptor);
		pathwayReactionHashHandler.setDbAdaptor(databaseAdaptor);
		
		// We need to use the dba's internal caching, so turn it on for
		// the duration of the conversion.
		boolean originalUseCache = databaseAdaptor.isUseCache();
		databaseAdaptor.setUseCache(true);
		
		// Collect together all the pathway IDs and things that the
		// user has supplied to specify the construction of the
		// SBML.
		Map<Long,List<Long>> compositePathwayReactionHash = pathwayReactionHashHandler.derive();
		
		// Convert the specified things into SBML.
		convertPathways(compositePathwayReactionHash);
		
		databaseAdaptor.setUseCache(originalUseCache);
	}
	
	/**
	 * Convert Reactome pathways to SBML.
	 * @param pathwayReactionHash keys are pathway DB_IDs, values are lists of reaction DB_IDs
	 */
	public void convertPathways(Map<Long,List<Long>> pathwayReactionHash) {
//		System.err.println("SBMLAndLayoutBuilder.convertPathways: entered");
		
		if (pathwayReactionHash.size() > 0) {
//			System.err.println("SBMLAndLayoutBuilder.convertPathways: about to build SBML");

			sbmlBuilder.buildFromPathwayReactionHash(pathwayReactionHash);

			if (autogenerateKineticFlag)
				sbmlBuilder.autogenerateKinetics(autogenerateKineticServletUrl);
			
			if (layoutGenerators.isGeneratorsAvailable()) {
//				System.err.println("SBMLAndLayoutBuilder.convertPathways: layout generators are available");
				
				try {
					layoutGenerators.setModel(sbmlBuilder.getModel());
					layoutExtractor.buildFromPathwayReactionHash(pathwayReactionHash);
					layoutGenerators.run(layoutExtractor.getDiagram());
				} catch (Exception e) {
					System.err.println("SBMLAndLayoutBuilder.convertPathways: WARNING - something went wrong during layout");
					e.printStackTrace(System.err);
				}
			} else
				System.err.println("SBMLAndLayoutBuilder.convertPathways: no layout generator is available");
		}

//		System.err.println("SBMLAndLayoutBuilder.convertPathways: done");
	}
	
	public void addLayoutGenerator(String layoutGeneratorName) {
//		System.err.println("SBMLAndLayoutBuilder.addLayoutGenerator: layoutGeneratorName=" + layoutGeneratorName);
		
		layoutGenerators.add(layoutGeneratorName);
	}

	/**
	 * Used by subclasses to bail out with a message if something goes wrong.
	 * 
	 * @param text
	 */
	protected void handleError(String text) {
		System.err.println(this.getClass().getCanonicalName() + ": " + text);
		System.exit(1);
	}
}
