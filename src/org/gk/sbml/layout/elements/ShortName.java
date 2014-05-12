/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.elements;

/**
 * Generate a short name
 * 
 * @author David Croft
 *
 */
public class ShortName {
	public static int DEFAULT_CUTOFF = 20;
	private static String[] stopTerms = {"also", "and", "are", "at", "be", "but", "by", "can", "for", "go", "gone", "had", "has", "have", "here", "in", "is", "it", "its", "now", "of", "on", "one", "or", "some", "that", "the", "their", "them", "then", "there", "these", "this", "those", "to", "us", "was", "we", "went", "were", "what", "when", "where", "which", "while", "why", "you"};

	public ShortName() {
		super();
	}
	
	public static String generate(String longName) {
		return generate(longName, DEFAULT_CUTOFF);
	} 

	public static String generate(String longName, int cutOff) {
		if (longName.isEmpty()) {
			System.err.println("ReactomeToSBMLConverter.generate: WARNING - longName is empty");
			return longName;
		}
		String shortName = longName.replaceAll(" *\\[[^\\]]+\\]", "");
		shortName = shortName.replaceAll("[^a-zA-Z0-9]+", "_");
		if (shortName.matches("^[0-9].*$"))
			shortName = "_" + shortName;
		shortName = removeStopTerms(shortName);
		shortName = removeShortWords(shortName);
		shortName = truncate(shortName, cutOff);

		if (shortName.isEmpty())
			shortName = truncate(longName, cutOff);
		
		if (shortName.isEmpty())
			System.err.println("ReactomeToSBMLConverter.generate: WARNING - shortName is empty for longName=" + longName);
		
		return shortName;
	}
	
	private static String truncate(String longName, int cutOff) {
		String shortName = longName;
		if (shortName.length() > cutOff) {
			if (shortName.substring(cutOff, cutOff + 1).matches("[a-zA-Z0-9]")) {
				int underlineCutOff = shortName.substring(0, cutOff).lastIndexOf("_");
				if (underlineCutOff > 3)
					cutOff = underlineCutOff;
			}
			shortName = shortName.substring(0, cutOff);
		}
		shortName = shortName.replaceAll("_+$", "");
		
		return shortName;
	}
	
	private static String removeShortWords(String prolix) {
		String concise = prolix;
		for (String stopTerm: stopTerms) {
			concise = concise.replaceAll("^[a-zA-Z0-9][^a-zA-Z0-9]+", "");
			concise = concise.replaceAll("[^a-zA-Z0-9][a-zA-Z0-9][^a-zA-Z0-9]+", " ");
			concise = concise.replaceAll("[^a-zA-Z0-9][a-zA-Z0-9]$", "");
		}
		return concise;
	}
	
	private static String removeStopTerms(String prolix) {
		String concise = prolix;
		for (String stopTerm: stopTerms) {
			concise = concise.replaceAll("^" + stopTerm + "[^a-zA-Z0-9]+", "");
			concise = concise.replaceAll("[^a-zA-Z0-9]" + stopTerm + "[^a-zA-Z0-9]+", " ");
			concise = concise.replaceAll("[^a-zA-Z0-9]" + stopTerm + "$", "");
		}
		return concise;
	}
}
