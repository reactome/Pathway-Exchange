/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.psimixml;

import java.util.ArrayList;
import java.util.List;

/**
 * Export PSIMI XML from Reactome.
 * 
 * Provides a command line interface to this functionality, so that it can be
 * incorporated into release scripts and used for testing.
 * 
 * @author David Croft
 *
 */
public class PSIMIBuilderCommandLine extends PSIMIBuilder {
	static public void main(String[] args) {
		PSIMIBuilderCommandLine builder = new PSIMIBuilderCommandLine();
		String outputFile = null;

		// Parse arguments
		String s;
		for (int i=0; i<args.length; i++) {
			s = args[i];
			if (s.equals("-sp")) {
				i++;
				if (i<args.length) {
					String[] species = args[i].split(",");
					for (String specie: species)
						builder.getPathwayReactionHashHandler().addSpeciesFilter(specie);
					
				} else {
					builder.handleError("missing argument after -sp, expected <species 1>[,<species 2>, ...]");
				}
			}
			else if (s.equals("-pid")) {
				i++;
				if (i<args.length) {
					String[] splits = args[i].split(",");
					for (int j=0; j<splits.length; j++)
						try {
							builder.getPathwayReactionHashHandler().addPathwayDbId(new Long(splits[j]));
						} catch (NumberFormatException e) {
							builder.handleError("not a valid DB_ID: " + splits[j]);
						}
				} else {
					builder.handleError("missing argument after -pid, expected <pathway DB_ID 1>[,<pathway DB_ID 2>, ...]");
				}
			}
			else if (s.equals("-prgroup")) {
				i++;
				if (i<args.length) {
					String[] splits1 = args[i].split(":");
					if (splits1.length != 2)
						builder.handleError("format of an rgroup is <pathway DB_ID>:<reaction DB_ID 1>[,<reaction DB_ID 2>, ...]");
					long pathwayId = (new Long(splits1[0])).longValue();
					String[] splits = splits1[1].split(",");
					long[] reactionIds = new long[splits.length];
					for (int j=0; j<splits.length; j++)
						try {
							reactionIds[j] = (new Long(splits[j])).longValue();
						} catch (NumberFormatException e) {
							builder.handleError("not a valid DB_ID: " + splits[j]);
						}
					List<Long> reactionDbIdList = new ArrayList<Long>();
					for (long reactionId: reactionIds)
						reactionDbIdList.add(new Long(reactionId));
					builder.getPathwayReactionHashHandler().addPathwayReactionHashElement(pathwayId, reactionDbIdList);
				} else {
					builder.handleError("missing argument after -rgroup, expected <pathway DB_ID>:<reaction DB_ID 1>[,<reaction DB_ID 2>, ...]");
				}
			}
			else if (s.equals("-rid")) {
				i++;
				if (i<args.length) {
					String[] splits = args[i].split(",");
					for (int j=0; j<splits.length; j++)
						try {
							builder.getPathwayReactionHashHandler().addReactionDbId(new Long(splits[j]));
						} catch (NumberFormatException e) {
							builder.handleError("not a valid DB_ID: " + splits[j]);
						}
				} else {
					builder.handleError("missing argument after -rlist, expected <reaction DB_ID 1>[,<reaction DB_ID 2>, ...]");
				}
			}
			else if (s.equals("-o")) {
				i++;
				if (i<args.length)
					outputFile = args[i];
				else
					builder.handleError("missing argument after -o");
			}
			else if (s.equals("--help") || s.equals("-help"))
				builder.printHelp();
			else if (builder.getDatabaseConnectionHandler().parseDatabaseArgument(args, i))
				i++;// Command line db args
			else
				builder.handleError("Unknown argument" + args[i]);
		}
		
		// Do conversion
		builder.convertPathways();
		
		System.err.println("PSIMIBuilderCommandLine.main: sending document");

		Dumper.dumpToFile(builder.getEntrySet(), outputFile);
		
		System.err.println("PSIMIBuilderCommandLine.main: done");
	}

	private void printHelp() {
		System.out.println("Usage:");
		System.out.println("");
		System.out.println("java org.reactome.psimixml.PSIMIXMLBuilderCommandLine <options>");
		System.out.println("");
		System.out.println("The following options are available:");
		System.out.println("");
		System.out.println(" -host <hostname> default hostname for all databases (e.g. picard.ebi.ac.uk)");
		System.out.println(" -user <username> default user name for all databases");
		System.out.println(" -port <port> default port for all databases");
		System.out.println(" -pass <password> default password for all databases");
		System.out.println(" -pid <pathway DB_ID 1>[,<pathway DB_ID 2>, ...] list of pathway DB_IDs");
		System.out.println(" -rgroup <pathway DB_ID>:<reaction DB_ID 1>[,<reaction DB_ID 2>, ...] group of reaction DB_IDs associated with a given pathway.  You may use this option more than once to specify multiple groups.");
		System.out.println(" -sp <species 1>[,<species 2>, ...] limit to list of species (use double quotes if you are using species names containing spaces).");
		System.out.println(" -o <filename> supply the name of a file to dump the output into (if not specified, STDOUT will be used).");

		System.exit(0);
	}
}
