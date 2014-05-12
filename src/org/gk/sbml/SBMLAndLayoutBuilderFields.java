/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gk.persistence.MySQLAdaptor;

/**
 * Export SBML and layout from Reactome.
 * 
 * Provides a field-based interface to this functionality, so that it can be
 * incorporated into the server end of an advanced SBML generator.
 * 
 * To use, call the addField method as many times as you need to build
 * up the parameter settings for the underlying SBMLAndLayoutBuilder object.
 * Then run the convertPathways method with no arguments to do the
 * conversion.
 * 
 * @author David Croft
 *
 */
public class SBMLAndLayoutBuilderFields extends SBMLAndLayoutBuilder {
	/**
	 * This method can be called multiple times, to cumulatively build up the underlying
	 * SBMLAndLayoutBuilder object.
	 * 
	 * @param fieldName
	 * @param queryLines
	 */
	public void addField(String fieldName, List<String> queryLines) {
		String lowerCaseFieldName = null;
		if (fieldName != null)
			lowerCaseFieldName = fieldName.toLowerCase();
		if (lowerCaseFieldName.equals("host")) {
			if (queryLines.size() > 0)
				getDatabaseConnectionHandler().setHostname(queryLines.get(0));
		} else if (lowerCaseFieldName.equals("db")) {
			if (queryLines.size() > 0)
				getDatabaseConnectionHandler().setDbName(queryLines.get(0));
		} else if (lowerCaseFieldName.equals("user")) {
			if (queryLines.size() > 0)
				getDatabaseConnectionHandler().setUsername(queryLines.get(0));
		} else if (lowerCaseFieldName.equals("pass")) {
			if (queryLines.size() > 0)
				getDatabaseConnectionHandler().setPassword(queryLines.get(0));
		} else if (lowerCaseFieldName.equals("port")) {
			if (queryLines.size() > 0)
				getDatabaseConnectionHandler().setPort(queryLines.get(0));
		} else if (lowerCaseFieldName == null || lowerCaseFieldName.equals("query") || lowerCaseFieldName.equals("file") || lowerCaseFieldName.equals("rid")) {
			if (queryLines.size() > 0) {
				for (String queryLine: queryLines)
					try {
						getPathwayReactionHashHandler().addReactionDbId(new Long(queryLine));
					} catch (NumberFormatException e) {
						System.err.println("SBMLAndLayoutBuilderFields.addField: WARNING - problem extracting a valid reaction DB_ID from string: " + queryLine);
						e.printStackTrace(System.err);
					}
			}
		} else if (lowerCaseFieldName.equals("id") || lowerCaseFieldName.equals("pid")) {
			if (queryLines.size() > 0) {
				String pathwayDbIdString = queryLines.get(0);
				try {
					getPathwayReactionHashHandler().addPathwayDbId(new Long(pathwayDbIdString));
				} catch (NumberFormatException e) {
					System.err.println("SBMLAndLayoutBuilderFields.addField: WARNING - problem extracting a valid pathway DB_ID from string: " + pathwayDbIdString);
					e.printStackTrace(System.err);
				}
			}
		} else if (lowerCaseFieldName.equals("level")) {
			if (queryLines.size() > 0)
				getSbmlBuilder().setLevel(Integer.parseInt(queryLines.get(0)));
		} else if (lowerCaseFieldName.equals("version")) {
			if (queryLines.size() > 0)
				getSbmlBuilder().setVersion(Integer.parseInt(queryLines.get(0)));
		} else if (lowerCaseFieldName.equals("engine")) {
			if (queryLines.size() > 0)
				setSbmlBuilder(queryLines.get(0));
		} else if (lowerCaseFieldName.equals("concat")) {
			if (queryLines.size() > 0 && queryLines.get(0).equals("1"))
				getPathwayReactionHashHandler().setConcatenateReactionFlag(true);
		} else if (lowerCaseFieldName.equals("squeeze")) {
			if (queryLines.size() > 0 && queryLines.get(0).equals("1"))
				setAutogenerateKineticFlag(true);
		} else if (lowerCaseFieldName.equals("squeezesvlt")) {
			if (queryLines.size() > 0)
				this.setAutogenerateKineticServletUrl(queryLines.get(0));
		} else if (lowerCaseFieldName.equals("layout")) {
//			System.err.println("SBMLAndLayoutBuilderFields.addField: doing layout");
			for (String queryLine: queryLines) {
				if (queryLine.toLowerCase().equals("extension"))
					addLayoutGenerator("Extension");
				else if (queryLine.toLowerCase().equals("sbgn"))
					addLayoutGenerator("SBGN");
				else if (queryLine.toLowerCase().equals("cell_designer"))
					addLayoutGenerator("CellDesigner");
				else
					System.err.println("SBMLAndLayoutBuilderFields.addField: WARNING - unknown layouter " + queryLine);
			}
		} else if (lowerCaseFieldName.equals("filter")) {
			if (filterQueryLineParser(queryLines).size() > 0)
				System.err.println("SBMLAndLayoutBuilderFields.addField: WARNING - extra terms in the filter specification");				
		} else
			System.err.println("SBMLAndLayoutBuilderFields.addField: WARNING - unknown field name " + lowerCaseFieldName);
	}
	
	/**
	 * Parses one or more filters from the supplied list of filter parameters.
	 * This is a repeating sequence of the form:
	 * 
	 * <filter type>, <Instance class>, <Attribute>, <Term1>, <Term2>, ....
	 * 
	 * E.g. "inc", "Pathway", "species", "Homo sapiens", "exc", "Reaction", "name", "Decarboxylation", "Electron transport", ....
	 * 
	 * Filter type can be either "inc" or "exc", standing for inclusion or exclusion,
	 * respectively.  Instance class should be a Reactome instance class, and Attribute
	 * should be an attribute appropriate for that instance class.  Any number of
	 * Terms can be specified, these are the values that are filtered against.
	 * 
	 * Note: "inc" and "exc" are reserved words in the filter parameter list, you
	 * should avoid using these words in Instance classes, Attributes or Terms.
	 * 
	 * @param queryLines
	 * @return
	 */
	private List<String> filterQueryLineParser(List<String> queryLines) {
		List<String> newQueryLines = null;
		List<String> filterTerms = new ArrayList<String>();
		
		// Grab the filter terms at the start of queryLines, if there
		// are any.
		for (String queryLine: queryLines)
			if (queryLine.equals("inc") || queryLine.equals("exc"))
				break;
			else
				filterTerms.add(queryLine);
		newQueryLines = queryLines.subList(filterTerms.size(), queryLines.size());
		
		// Recursion terminator
		if (newQueryLines.size() == 0) {
			return filterTerms;
		}
		
		if (newQueryLines.size() < 4) {
			System.err.println("SBMLAndLayoutBuilderFields.addField: WARNING - not enough elements left in newQueryLines to build a filter, will not continue parsing.");
			System.err.print("SBMLAndLayoutBuilderFields.addField: newQueryLines=[");
			for (String queryLine: newQueryLines)
				System.err.print(queryLine + ",");
			System.err.println("]");
			return filterTerms;
		}
		
		String filterType = newQueryLines.get(0).toLowerCase();
		String filteredInstanceClass = newQueryLines.get(1).toLowerCase();
		String attribute = newQueryLines.get(2).toLowerCase();

		if (filterType.equals("inc"))
			appendFilters("inclusionFilters", filteredInstanceClass, attribute, filterQueryLineParser(newQueryLines.subList(3, newQueryLines.size())));
		else if (filterType.equals("exc"))
			appendFilters("exclusionFilters", filteredInstanceClass, attribute, filterQueryLineParser(newQueryLines.subList(3, newQueryLines.size())));
		else
			System.err.println("SBMLAndLayoutBuilderFields.addField: WARNING - unknown filter type " + filterType + ", will not continue parsing.");
		
		return filterTerms;
	}

	private String getTrueInstanceClassName(MySQLAdaptor dbAdaptor, String tentativeInstanceClassName) {
		String trueInstanceClassName = capitalizeFirstLowerCaseRemainder(tentativeInstanceClassName);

		try {
			if (dbAdaptor.getSchema().isValidClass(trueInstanceClassName)) {
			} else {
				Collection<String> knownInstanceClassNames = dbAdaptor.getSchema().getClassNames();
				boolean foundTrueInstanceClassName = false;
				for (String knownInstanceClassName: knownInstanceClassNames) {
					if (knownInstanceClassName.equalsIgnoreCase(trueInstanceClassName)) {
						trueInstanceClassName = knownInstanceClassName;
						foundTrueInstanceClassName = true;
						break;
					}
				}
				if (!foundTrueInstanceClassName) {
					System.err.println("SBMLAndLayoutBuilderFields.getTrueInstanceClassName: WARNING - could not find an instance class matching " + trueInstanceClassName);
					return null;
				}
			}
		} catch (Exception e) {
			System.err.println("SBMLAndLayoutBuilderFields.getTrueInstanceClassName: WARNING - problem with instanceClassName=" + trueInstanceClassName);
			e.printStackTrace(System.err);
			return null;
		}
		
		return trueInstanceClassName;
	}
	
	private void appendFilters(String type, String filteringInstanceClassName, String attribute, List<String> queryLines) {
		String trueFilteredInstanceClassName = getTrueInstanceClassName(getDatabaseConnectionHandler().getDatabaseAdaptor(), filteringInstanceClassName);
		for (String queryLine: queryLines)
			if (type.equals("inclusionFilters"))
				getPathwayReactionHashHandler().getInstanceFilters().addInclusionFilter(trueFilteredInstanceClassName, attribute, queryLine);
			else
				getPathwayReactionHashHandler().getInstanceFilters().addExclusionFilter(trueFilteredInstanceClassName, attribute, queryLine);
	}
	
	public static String capitalizeFirstLowerCaseRemainder(String string) {
		String firstLetter = string.substring(0, 1);
		String remainder = string.substring(1);

		return firstLetter.toUpperCase() + remainder.toLowerCase();
	}
	
	public List<String> parseQueryLines(String query) {
		String trueQuery = query.replaceAll("\n+$", "");
		String[] trueQuerySplit;
		if (trueQuery.contains(","))
			trueQuerySplit = trueQuery.split(",");
		else
			trueQuerySplit = trueQuery.split("\n");
		List<String> queryLines = new ArrayList<String>();
		for (String queryLine: trueQuerySplit)
			queryLines.add(queryLine);
		
		return queryLines;
	}

	static public void main(String[] args) {
		SBMLAndLayoutBuilderFields sbmlAndLayoutBuilder = new SBMLAndLayoutBuilderFields();
		String outputFile = null;

		// Parse arguments
		String s;
		for (int i=0; i<args.length; i++) {
			s = args[i];
			if (s.matches("^[a-zA-Z_]+=")) {
				String[] argumentComponents = s.split("=");
				String fieldName = argumentComponents[0];
				String fieldValue = argumentComponents[1];
				sbmlAndLayoutBuilder.addField(fieldName, sbmlAndLayoutBuilder.parseQueryLines(fieldValue));
			}
			else if (s.equals("-o")) {
				i++;
				if (i<args.length)
					outputFile = args[i];
				else
					sbmlAndLayoutBuilder.handleError("missing argument after -o");
			}
			else if (s.equals("--help") || s.equals("-help"))
				sbmlAndLayoutBuilder.printHelp();
			else if (sbmlAndLayoutBuilder.getDatabaseConnectionHandler().parseDatabaseArgument(args, i))
				i++;// Command line db args
			else
				sbmlAndLayoutBuilder.handleError("Unknown argument" + args[i]);
		}
		
		// Do conversion
		sbmlAndLayoutBuilder.convertPathways();
		
		System.err.println("SBMLAndLayoutBuilderFields.main: sending document");

		sbmlAndLayoutBuilder.getSbmlBuilder().printDocument(outputFile);
		
		System.err.println("SBMLAndLayoutBuilderFields.main: done");
	}

	private void printHelp() {
		System.out.println("Usage:");
		System.out.println("");
		System.out.println("java org.reactome.sbml.SBMLAndLayoutBuilderFields <options>");
		System.out.println("");
		System.out.println("The following options are available:");
		System.out.println("");
		System.out.println(" <FIELD_NAME>=<value> set the given field to the given value");
		System.out.println(" -o filename supply the name of a file to dump the output into (if not specified, STDOUT will be used).");

		System.exit(0);
	}
}
