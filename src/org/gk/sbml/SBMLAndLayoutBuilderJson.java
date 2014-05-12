/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Export SBML and layout from Reactome.
 * 
 * Provides a Json interface to this functionality, so that it can be
 * incorporated into the server end of an advanced SBML generator.
 * 
 * @author David Croft
 *
 */
public class SBMLAndLayoutBuilderJson extends SBMLAndLayoutBuilder {
	/**
	 * Convert Reactome pathways to SBML.
	 * @param jsonString reactions to add to SBML, plus metadata.
	 * @return SBML
	 */
	public void convertPathwaysJson(String jsonString) {
		try {
			JSONObject json = new JSONObject(jsonString);
			
			// Get SBML engine, if supplied.  Values accepted are: "libSBML", "JSBML", possibly others.
			if (!json.isNull("engine")) {
				String sbmlBuilderName = json.getString("engine");
				setSbmlBuilder(sbmlBuilderName);
			}
			
			// Get explicitly-specified SBML level and version, if supplied.
			if (!json.isNull("level")) {
				int level = json.getInt("level");
				getSbmlBuilder().setLevel(level);
			}
			if (!json.isNull("version")) {
				int version = json.getInt("version");
				getSbmlBuilder().setVersion(version);
			}
			if (!json.isNull("concat")) {
				boolean concat = json.getBoolean("concat");
				getPathwayReactionHashHandler().setConcatenateReactionFlag(concat);
			}
			if (!json.isNull("squeeze")) {
				boolean squeeze = json.getBoolean("squeeze");
				setAutogenerateKineticFlag(squeeze);
			}
			if (!json.isNull("squeezesvlt")) {
				String squeezesvlt = json.getString("squeezesvlt");
				this.setAutogenerateKineticServletUrl(squeezesvlt);
			}
			
			// Get layout generators, if any.
			if (!json.isNull("layouter")) {
				layoutGenerators.setModel(sbmlBuilder.getModel());
				JSONArray layoutGeneratorsJson = json.getJSONArray("layouter");
				for (int i=0; i<layoutGeneratorsJson.length(); i++) {
					String layoutGeneratorName = layoutGeneratorsJson.getString(i);
					layoutGenerators.add(layoutGeneratorName);
				}
			}
			
			// Filter instances
			addFiltersFromJson(json, "infilter");
			addFiltersFromJson(json, "exfilter");
			
			// Collect instances for building model
			if (!json.isNull("prgroup")) {
				JSONObject pathwayReactionHashJson = json.getJSONObject("prgroup");
				Iterator keys = pathwayReactionHashJson.keys();
				while (keys.hasNext()) {
					String key = keys.next().toString();
					JSONArray reactionDbIds = pathwayReactionHashJson.getJSONArray(key);
					List<Long> reactionDbIdList = new ArrayList<Long>();
					for (int i=0; i<reactionDbIds.length(); i++)
						reactionDbIdList.add(new Long(reactionDbIds.getString(i)));
					Long pathwayDbId = new Long(key);
					getPathwayReactionHashHandler().addPathwayReactionHashElement(pathwayDbId, reactionDbIdList);
				}
			}
			if (!json.isNull("rid")) {
				JSONArray jsonReactionDbIds = json.getJSONArray("rid");
				for (int i=0; i < jsonReactionDbIds.length(); i++) {
					Long reactionDbId = jsonReactionDbIds.getLong(i);
					getPathwayReactionHashHandler().addReactionDbId(reactionDbId);
				}
			}
			if (!json.isNull("pid")) {
				JSONArray jsonPathwayDbIds = json.getJSONArray("pid");
				for (int i=0; i < jsonPathwayDbIds.length(); i++) {
					Long pathwayDbId = jsonPathwayDbIds.getLong(i);
					getPathwayReactionHashHandler().addPathwayDbId(pathwayDbId);
				}
			}
		} catch (Exception e) {
			System.err.println("SBMLAndLayoutBuilderJson.convertPathwaysJson: WARNING - problem parsing JSON");
			e.printStackTrace(System.err);
		}
		
		convertPathways();

		System.err.println("SBMLAndLayoutBuilderJson.convertPathwaysJson: done");
	}

	public void addFiltersFromJson(JSONObject json, String type) {
		try {
			if (!json.isNull(type)) {
				JSONObject instanceFiltersJson = json.getJSONObject(type);
				Iterator filteringInstanceClassNames = instanceFiltersJson.keys();

				while (filteringInstanceClassNames.hasNext()) {
					String filteringInstanceClassName = filteringInstanceClassNames.next().toString();

					JSONObject filteringInstanceClassFilters = instanceFiltersJson.getJSONObject(filteringInstanceClassName);
					Iterator instanceClassNames = filteringInstanceClassFilters.keys();

					while (instanceClassNames.hasNext()) {
						String attribute = instanceClassNames.next().toString();
						JSONArray values = filteringInstanceClassFilters.getJSONArray(attribute);

						for (int i=0; i<values.length(); i++) {
							if (type.equals("infilter"))
								getPathwayReactionHashHandler().getInstanceFilters().addInclusionFilter(filteringInstanceClassName, attribute, values.getString(i));
							else
								getPathwayReactionHashHandler().getInstanceFilters().addExclusionFilter(filteringInstanceClassName, attribute, values.getString(i));
						}
					}
				}
			}
		} catch (JSONException e) {
			System.err.println("InstanceFilters.addFiltersFromJson: WARNING - problem parsing JSON");
			e.printStackTrace(System.err);
		}
	}

	static public void main(String[] args) {
		SBMLAndLayoutBuilderJson sbmlAndLayoutBuilder = new SBMLAndLayoutBuilderJson();
		String jsonFilename = null;
		String outputFile = null;

		// Parse arguments
		String s;
		for (int i=0; i<args.length; i++) {
			s = args[i];
			if (s.equals("-o")) {
				i++;
				if (i<args.length)
					outputFile = args[i];
				else
					sbmlAndLayoutBuilder.handleError("missing argument after -o");
			}
			else if (s.equals("-json")) {
				i++;
				if (i<args.length)
					jsonFilename = args[i];
				else
					sbmlAndLayoutBuilder.handleError("missing argument after -json, expecting the name of a JSON file");
			}
			else if (s.equals("--help") || s.equals("-help"))
				sbmlAndLayoutBuilder.printHelp();
			else if (sbmlAndLayoutBuilder.getDatabaseConnectionHandler().parseDatabaseArgument(args, i))
				i++;// Command line db args
			else
				sbmlAndLayoutBuilder.handleError("Unknown argument" + args[i]);
		}
		
		// Do conversion
		if (jsonFilename != null) {
			String jsonString = "";
			try {
				FileReader fileReader = new FileReader(jsonFilename);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String line;
				while ((line = bufferedReader.readLine()) != null)
					jsonString += line;
				bufferedReader.close();
			} catch (IOException e) {
				sbmlAndLayoutBuilder.handleError("problem reading from file:\n" + e.getMessage());
			}
			sbmlAndLayoutBuilder.convertPathwaysJson(jsonString);
		} else
			sbmlAndLayoutBuilder.handleError("JSON filename is null!");
		
		System.err.println("SBMLAndLayoutBuilderJson.main: sending document");

		sbmlAndLayoutBuilder.getSbmlBuilder().printDocument(outputFile);
		
		System.err.println("SBMLAndLayoutBuilderJson.main: done");
	}

	private void printHelp() {
		System.out.println("Usage:");
		System.out.println("");
		System.out.println("java org.reactome.sbml.SBMLAndLayoutBuilderJson <options>");
		System.out.println("");
		System.out.println("The following options are available:");
		System.out.println("");
		System.out.println(" -host <hostname> default hostname for all databases (e.g. picard.ebi.ac.uk)");
		System.out.println(" -user <username> default user name for all databases");
		System.out.println(" -port <port> default port for all databases");
		System.out.println(" -pass <password> default password for all databases");
		System.out.println(" -o filename supply the name of a file to dump the output into (if not specified, STDOUT will be used).");
		System.out.println(" -json filename supply a file containing JSON directoves for SBML generation.");

		System.exit(0);
	}
}
