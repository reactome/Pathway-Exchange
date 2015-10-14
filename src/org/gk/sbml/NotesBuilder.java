/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.gk.model.GKInstance;
import org.gk.sbml.model.elements.SBase;

/**
 * Methods for extracting notes from the summations of Reactome instances and inserting
 * them into SBML SBase objects.
 * 
 * @author David Croft
 *
 */
public class NotesBuilder {
	public static int MAX_NOTES_LENGTH = 2500;
	
	public static void appendInstanceSummationsToSBase(SBase sbase, GKInstance instance) {
		String notes = extractNotesFromInstance(instance);
		
		if (notes != null && !notes.isEmpty()) {
			String notesString = createNotesString(notes, false);
			if (notesString != null && !notesString.isEmpty()) {
				try {
					sbase.setNotes(notesString);
				} catch (Exception e) {
					System.err.println("NotesBuilder.appendInstanceSummationsToSBase: WARNING - problem setting notes, notesString=" + notesString);
					e.printStackTrace(System.err);
				}
			} else
				System.err.println("NotesBuilder.appendInstanceSummationsToSBase: WARNING - notesString is null or empty, notes=" + notes);
		}
	}
	
	public static void appendInstanceSummationsToSBase(SBase sbase, List<GKInstance> instances) {
		String notes = extractNotesFromInstances(instances);
		
		if (notes != null && !notes.isEmpty()) {
			String notesString = createNotesString(notes, false);
			if (notesString != null && !notesString.isEmpty()) {
				try {
					sbase.setNotes(notesString);
				} catch (Exception e) {
					System.err.println("NotesBuilder.appendInstanceSummationsToSBase: WARNING - problem setting notes, notesString=" + notesString);
					e.printStackTrace(System.err);
				}
			} else
				System.err.println("NotesBuilder.appendInstanceSummationsToSBase: WARNING - notesString is null or empty, notes=" + notes);
		}
	}
	
	public static void appendEntityInformationToSpecies(SBase species, GKInstance instance) {
		if (!instance.getSchemClass().isa("PhysicalEntity")) {
			System.err.println("NotesBuilder.appendInstanceSummationsToSBase: WARNING - instance is not a physical entity");
			return;
		}
		
		String notesString = "<p xmlns=\"http://www.w3.org/1999/xhtml\">Derived from a Reactome " + instance.getSchemClass().getName() + ".";
		if (instance.getSchemClass().isa("SimpleEntity")) {
			notesString += "  This is a small compound.";
		} else if (instance.getSchemClass().isa("EntityWithAccessionedSequence")) {
			notesString += "  This is a protein.";
		} else if (instance.getSchemClass().isa("Complex")) {
			String complexStructure = extractComplexStructure(instance);
			if (complexStructure == null)
				notesString += "  Reactome uses a nested structure for complexes, which cannot be fully represented in SBML.";
			else
				notesString += "  Here is Reactomes nested structure for this complex: " + complexStructure;
		} else if (instance.getSchemClass().isa("CandidateSet")) {
			notesString += "  A list of entities, one or more of which might perform the given function.";
		} else if (instance.getSchemClass().isa("DefinedSet")) {
			notesString += "  This is a list of alternative entities, any of which can perform the given function.";
		} else if (instance.getSchemClass().isa("OpenSet")) {
			notesString += "  A set of examples characterizing a very large but not explicitly enumerated set, e.g. mRNAs.";
		}
		notesString += "</p>";
		species.setNotes(notesString);
	}
	
	private static String extractComplexStructure(GKInstance complex) {
		if (!complex.getSchemClass().isa("Complex")) {
			System.err.println("NotesBuilder.appendInstanceSummationsToSBase: WARNING - complex is not actually a complex!");
			return null;
		}
		
		String complexStructure = "";
		try {
			List<GKInstance> components = complex.getAttributeValuesList("hasComponent");
			for (GKInstance component: components) {
				String componentStructure = null;
				if (component.getSchemClass().isa("SimpleEntity") || component.getSchemClass().isa("EntityWithAccessionedSequence")) {
					GKInstance referenceEntity = (GKInstance) component.getAttributeValue("referenceEntity");
					if (referenceEntity == null) {
						System.err.println("NotesBuilder.appendInstanceSummationsToSBase: WARNING - referenceEntity == null for component DB_ID=" + component.getDBID());
						return null;
					}
					componentStructure = (String) referenceEntity.getAttributeValue("identifier");
				} else if (component.getSchemClass().isa("Complex"))
					componentStructure = extractComplexStructure(component);
				if (componentStructure == null) {
					return null;
				} else {
					if (complexStructure.isEmpty())
						complexStructure = "(";
					else
						complexStructure += ",";
					complexStructure += componentStructure;
				}
			}
		} catch (Exception e) {
			System.err.println("NotesBuilder.appendInstanceSummationsToSBase: WARNING - problem getting information from complex components.");
			e.printStackTrace(System.err);
		}
		complexStructure += ")";
		
		return complexStructure;
	}
	
	private static String extractNotesFromInstances(List<GKInstance> instances) {
		String notes = "";
		
		for (GKInstance instance: instances) {
			if (instances.size() > 1) {
				String displayName = instance.getDisplayName();
				if (displayName != null && !displayName.isEmpty())
					notes += displayName + ": ";
			}
			notes += extractNotesFromInstance(instance);
		}
		
		return notes;
	}
	
	public static String extractNotesFromInstance(GKInstance instance) {
		String notes = "";
		if (instance.getSchemClass().isValidAttribute("summation")) {
			try {
				GKInstance summation = (GKInstance) instance.getAttributeValue("summation");
				if (summation != null) {
					String text = summation.getAttributeValue("text").toString();
					
					byte[] textBytes = text.getBytes();
					text = flattenToAscii(new String(textBytes, StandardCharsets.UTF_8));

					if (text != null)
						notes = text;
				}
			} catch (Exception e) {
				System.err.println("NotesBuilder.extractNotesFromInstance: WARNING - problem getting information from Reactome instance");
				e.printStackTrace(System.err);
			}
		}
		
		return notes;
	}

	public static String createNotesString(String notes) {
		return createNotesString(notes, true);
	}
	
	private static String createNotesString(String notes, boolean embedInXMLTag) {
		// libSBML doesn't like really long notes strings
		if (notes.length() > MAX_NOTES_LENGTH)
			notes = notes.substring(0, MAX_NOTES_LENGTH);
		
		String[] notesStrings = notes.split("\n+");
		String notesParagraphsString = "";
		for (String notesString: notesStrings)
			if (!notesString.isEmpty() && !notesString.matches("^[\t  ]+$")) {
				String newNotesParagraphsString = notesParagraphsString + "<p xmlns=\"http://www.w3.org/1999/xhtml\">" + cleanUpNotesString(notesString) + "</p>";
				if (newNotesParagraphsString.length() <= MAX_NOTES_LENGTH)
					notesParagraphsString = newNotesParagraphsString;
				else
					break;
			}
		
		return embedInXMLTag ? "<notes>" + notesParagraphsString + "</notes>" : notesParagraphsString;
	}

	public static String cleanUpNotesString(String notes) {
		notes = notes.replaceAll("\\p{Cntrl}+", "");
		notes = notes.replaceAll("\\cm+", "");
		notes = notes.replaceAll("</*[a-zA-Z][^>]*>", "");
		notes = notes.replaceAll("<>", " interconverts to ");
		notes = notes.replaceAll("\n+", "  ");
		notes = notes.replaceAll("&+", "  ");
		
		// libSBML doesn't like really long notes strings
		if (notes.length() > MAX_NOTES_LENGTH)
			notes = notes.substring(0, MAX_NOTES_LENGTH);
		
		return StringEscapeUtils.escapeXml(notes);
	}
	
	// Method taken from http://stackoverflow.com/questions/3322152/is-there-a-way-to-get-rid-of-accents-and-convert-a-whole-string-to-regular-lette/15191508#15191508
	public static String flattenToAscii(String text) {
		StringBuilder flattenedText = new StringBuilder(text.length());
		
		String normalizedText = Normalizer.normalize(text, Normalizer.Form.NFD);
		for (char c : normalizedText.toCharArray()) {
			if (c <= '\u007F')
				flattenedText.append(c);
		}
		
		return flattenedText.toString();
	}
}
