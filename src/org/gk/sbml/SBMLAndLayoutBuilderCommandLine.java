/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.util.ArrayList;
import java.util.List;

/**
 * Export SBML and layout from Reactome.
 * 
 * Provides a command line interface to this functionality, so that it can be
 * incorporated into release scripts and used for testing.
 * 
 * @author David Croft
 *
 */
public class SBMLAndLayoutBuilderCommandLine extends SBMLAndLayoutBuilder {
	static public void main(String[] args) {
		SBMLAndLayoutBuilderCommandLine sbmlAndLayoutBuilder = new SBMLAndLayoutBuilderCommandLine();
		String outputFile = null;

		// Parse arguments
		String s;
		for (int i=0; i<args.length; i++) {
			s = args[i];
			if (s.equals("-level")) {
				i++;
				if (i<args.length)
					sbmlAndLayoutBuilder.getSbmlBuilder().setLevel(new Integer(args[i]));
				else
					sbmlAndLayoutBuilder.handleError("missing argument after -level");
			}
			else if (s.equals("-version")) {
				i++;
				if (i<args.length)
					sbmlAndLayoutBuilder.getSbmlBuilder().setVersion(new Integer(args[i]));
				else
					sbmlAndLayoutBuilder.handleError("missing argument after -version");
			}
			else if (s.equals("-layouter")) {
				i++;
				if (i<args.length) {
					String[] layouters = args[i].split(",");
					for (String layouter: layouters)
						sbmlAndLayoutBuilder.addLayoutGenerator(layouter);
				} else {
					sbmlAndLayoutBuilder.handleError("missing argument after -layouter, expected <layouter 1>[,<layouter 2>, ...]");
				}
			}
			else if (s.equals("-sp")) {
				i++;
				if (i<args.length) {
					String[] species = args[i].split(",");
					for (String specie: species)
						sbmlAndLayoutBuilder.getPathwayReactionHashHandler().addSpeciesFilter(specie);
					
				} else {
					sbmlAndLayoutBuilder.handleError("missing argument after -layouter, expected <layouter 1>[,<layouter 2>, ...]");
				}
			}
			else if (s.equals("-pid")) {
				i++;
				if (i<args.length) {
					String[] splits = args[i].split(",");
					for (int j=0; j<splits.length; j++)
						try {
							sbmlAndLayoutBuilder.getPathwayReactionHashHandler().addPathwayDbId(new Long(splits[j]));
						} catch (NumberFormatException e) {
							sbmlAndLayoutBuilder.handleError("not a valid DB_ID: " + splits[j]);
						}
				} else {
					sbmlAndLayoutBuilder.handleError("missing argument after -pid, expected <pathway DB_ID 1>[,<pathway DB_ID 2>, ...]");
				}
			}
			else if (s.equals("-prgroup")) {
				i++;
				if (i<args.length) {
					String[] splits1 = args[i].split(":");
					if (splits1.length != 2)
						sbmlAndLayoutBuilder.handleError("format of an rgroup is <pathway DB_ID>:<reaction DB_ID 1>[,<reaction DB_ID 2>, ...]");
					long pathwayId = (new Long(splits1[0])).longValue();
					String[] splits = splits1[1].split(",");
					long[] reactionIds = new long[splits.length];
					for (int j=0; j<splits.length; j++)
						try {
							reactionIds[j] = (new Long(splits[j])).longValue();
						} catch (NumberFormatException e) {
							sbmlAndLayoutBuilder.handleError("not a valid DB_ID: " + splits[j]);
						}
					List<Long> reactionDbIdList = new ArrayList<Long>();
					for (long reactionId: reactionIds)
						reactionDbIdList.add(new Long(reactionId));
					sbmlAndLayoutBuilder.getPathwayReactionHashHandler().addPathwayReactionHashElement(pathwayId, reactionDbIdList);
				} else {
					sbmlAndLayoutBuilder.handleError("missing argument after -rgroup, expected <pathway DB_ID>:<reaction DB_ID 1>[,<reaction DB_ID 2>, ...]");
				}
			}
			else if (s.equals("-rid")) {
				i++;
				if (i<args.length) {
					String[] splits = args[i].split(",");
					for (int j=0; j<splits.length; j++)
						try {
							sbmlAndLayoutBuilder.getPathwayReactionHashHandler().addReactionDbId(new Long(splits[j]));
						} catch (NumberFormatException e) {
							sbmlAndLayoutBuilder.handleError("not a valid DB_ID: " + splits[j]);
						}
				} else {
					sbmlAndLayoutBuilder.handleError("missing argument after -rlist, expected <reaction DB_ID 1>[,<reaction DB_ID 2>, ...]");
				}
			}
			else if (s.equals("-engine")) {
				i++;
				if (i<args.length)
					sbmlAndLayoutBuilder.setSbmlBuilder(args[i]);
				else
					sbmlAndLayoutBuilder.handleError("missing argument after -engine");
			}
			else if (s.equals("-concat"))
				sbmlAndLayoutBuilder.getPathwayReactionHashHandler().setConcatenateReactionFlag(true);
			else if (s.equals("-squeeze"))
				sbmlAndLayoutBuilder.setAutogenerateKineticFlag(true);
			else if (s.equals("-squeezesvlt")) {
				i++;
				if (i<args.length)
					sbmlAndLayoutBuilder.setAutogenerateKineticServletUrl(args[i]);
				else
					sbmlAndLayoutBuilder.handleError("missing argument after -squeezesvlt");
			} else if (s.equals("-o")) {
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
		
		System.err.println("SBMLAndLayoutBuilderCommandLine.main: sending document");

		sbmlAndLayoutBuilder.getSbmlBuilder().printDocument(outputFile);
		
		System.err.println("SBMLAndLayoutBuilderCommandLine.main: done");
	}

	private void printHelp() {
		System.out.println("Usage:");
		System.out.println("");
		System.out.println("java org.reactome.sbml.SBMLAndLayoutBuilderCommandLine <options>");
		System.out.println("");
		System.out.println("The following options are available:");
		System.out.println("");
		System.out.println(" -host <hostname> default hostname for all databases (e.g. picard.ebi.ac.uk)");
		System.out.println(" -user <username> default user name for all databases");
		System.out.println(" -port <port> default port for all databases");
		System.out.println(" -pass <password> default password for all databases");
		System.out.println(" -level <SBML level> integer value, 1, 2 or 3. Defaults to 2");
		System.out.println(" -version <SBML version> integer value greater than 0. Defaults to 3");
		System.out.println(" -pid <pathway DB_ID 1>[,<pathway DB_ID 2>, ...] list of pathway DB_IDs");
		System.out.println(" -rgroup <pathway DB_ID>:<reaction DB_ID 1>[,<reaction DB_ID 2>, ...] group of reaction DB_IDs associated with a given pathway.  You may use this option more than once to specify multiple groups.");
		System.out.println(" -layouter <layouter 1>[,<layouter 2>, ...] list of layouters");
		System.out.println(" -sp <species 1>[,<species 2>, ...] limit to list of species (use double quotes if you are using species names containing spaces).");
		System.out.println(" -o <filename> supply the name of a file to dump the output into (if not specified, STDOUT will be used).");
		System.out.println(" -engine <SBML engine> e.g. libSBML, JSBML, etc. (if not specified, JSBML will be used).");
		System.out.println(" -concat flag to indicate that reactions should be concatenated (defaults to false).");

		System.exit(0);
	}
}
