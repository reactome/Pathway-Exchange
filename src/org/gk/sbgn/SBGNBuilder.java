/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbgn;

import java.util.List;
import java.util.Map;

import org.gk.convert.common.DatabaseConnectionHandler;
import org.gk.convert.common.PathwayReactionHashHandler;
import org.gk.layout.Diagram;
import org.gk.layout.Extractor;
import org.gk.persistence.MySQLAdaptor;

/**
 * Export SBGN from Reactome.
 * 
 * @author David Croft
 *
 */
public class SBGNBuilder {
	protected PDExtractor pdExtractor = new PDExtractor();
	private DatabaseConnectionHandler databaseConnectionHandler = new DatabaseConnectionHandler();
	private PathwayReactionHashHandler pathwayReactionHashHandler = new PathwayReactionHashHandler();
	
	public PDExtractor getPDExtractor() {
		return pdExtractor;
	}

	public PathwayReactionHashHandler getPathwayReactionHashHandler() {
		return pathwayReactionHashHandler;
	}

	public DatabaseConnectionHandler getDatabaseConnectionHandler() {
		return databaseConnectionHandler;
	}

	/**
	 * This method will do conversion to SBGN using the settings accumulated.
	 */
	public void convertPathways() {
		MySQLAdaptor databaseAdaptor = getDatabaseConnectionHandler().getDatabaseAdaptor();
		
		// Make sure that everything that needs a dba has one before starting
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
	 * Convert Reactome pathways to SBGN.
	 * @param pathwayReactionHash keys are pathway DB_IDs, values are lists of reaction DB_IDs
	 */
	public void convertPathways(Map<Long,List<Long>> pathwayReactionHash) {
		if (pathwayReactionHash.size() > 0) {
			Extractor extractor = new Extractor();
			extractor.setDatabaseAdaptor(getDatabaseConnectionHandler().getDatabaseAdaptor());
			extractor.buildFromPathwayReactionHash(pathwayReactionHash);
			Diagram modelLayout = extractor.getDiagram();
			pdExtractor.extract(modelLayout);
		}
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
