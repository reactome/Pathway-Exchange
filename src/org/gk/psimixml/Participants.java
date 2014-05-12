/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.psimixml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import psidev.psi.mi.xml.model.DbReference;
import psidev.psi.mi.xml.model.Interactor;
import psidev.psi.mi.xml.model.Participant;
import psidev.psi.mi.xml.model.Xref;

/**
 * Helper class that contains a list of PSIMI participants plus some additional
 * information extracted from Reactome.
 * 
 * @author David Croft
 *
 */
public class Participants implements Cloneable {
	private List<Participant> participantList = new ArrayList<Participant>();
	private boolean containsProtein = false;
	private List<String> complexStableIdentifiers = new ArrayList<String>();
	private List<String> complexNames = new ArrayList<String>();
	private List<String> reactionStableIdentifiers = new ArrayList<String>();
	private List<String> reactionNames = new ArrayList<String>();
	private List<String> pathwayStableIdentifiers = new ArrayList<String>();
	private List<String> pathwayNames = new ArrayList<String>();
	private List<String> pubMedIds = new ArrayList<String>();
	
	public Participants() {
	}
	
	public Participants(String complexStableIdentifierString, String complexName, String reactionStableIdentifier, String reactionName, String pathwayStableIdentifier, String pathwayName) {
		this.complexStableIdentifiers.add(complexStableIdentifierString);
		this.complexNames.add(complexName);
		this.reactionStableIdentifiers.add(reactionStableIdentifier);
		this.reactionNames.add(reactionName);
		this.pathwayStableIdentifiers.add(pathwayStableIdentifier);
		this.pathwayNames.add(pathwayName);
	}

	public List<Participant> getParticipantList() {
		return participantList;
	}

	public void setParticipantList(List<Participant> participantList) {
		this.participantList = participantList;
	}

	public List<String> getComplexStableIdentifiers() {
		return complexStableIdentifiers;
	}

	public void setComplexStableIdentifiers(List<String> complexStableIdentifiers) {
		this.complexStableIdentifiers = complexStableIdentifiers;
	}

	public List<String> getComplexNames() {
		return complexNames;
	}

	public void setComplexNames(List<String> complexNames) {
		this.complexNames = complexNames;
	}

	public List<String> getReactionStableIdentifiers() {
		return reactionStableIdentifiers;
	}

	public void setReactionStableIdentifiers(List<String> reactionStableIdentifiers) {
		this.reactionStableIdentifiers = reactionStableIdentifiers;
	}

	public List<String> getReactionNames() {
		return reactionNames;
	}

	public void setReactionNames(List<String> reactionNames) {
		this.reactionNames = reactionNames;
	}

	public List<String> getPathwayStableIdentifiers() {
		return pathwayStableIdentifiers;
	}

	public void setPathwayStableIdentifiers(List<String> pathwayStableIdentifiers) {
		this.pathwayStableIdentifiers = pathwayStableIdentifiers;
	}

	public List<String> getPathwayNames() {
		return pathwayNames;
	}

	public void setPathwayNames(List<String> pathwayNames) {
		this.pathwayNames = pathwayNames;
	}

	public List<String> getPubMedIds() {
		return pubMedIds;
	}

	public void addPubMedId(String pubMedId) {
		this.pubMedIds.add(pubMedId);
	}

	public boolean isContainsProtein() {
		return containsProtein;
	}

	public void setContainsProtein(boolean containsProtein) {
		this.containsProtein = containsProtein;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		Participants participants = new Participants();
		
		participants.setComplexStableIdentifiers(getComplexStableIdentifiers());
		participants.setComplexNames(getComplexNames());
		participants.setReactionStableIdentifiers(getReactionStableIdentifiers());
		participants.setReactionNames(getReactionNames());
		participants.setPathwayStableIdentifiers(getPathwayStableIdentifiers());
		participants.setPathwayNames(getPathwayNames());
		
		List<Participant> participantList = new ArrayList<Participant>();
		for (Participant participant: getParticipantList())
			participantList.add(participant);
		participants.setParticipantList(participantList);
		participants.setContainsProtein(isContainsProtein());
		
		return participants;
	}

	public void addParticipant(Participant participant) {
		participantList.add(participant);
	}
	
	/**
	 * Sort the participant list by participant ID and database.
	 */
	public void sortParticipantList() {
		// Copy participant list into a list of SortableParticipant objects
		List<SortableParticipant> sortableParticipantList = new ArrayList<SortableParticipant>();
		for (Participant participant: participantList) {
			SortableParticipant sortableParticipant = new SortableParticipant(participant);
			sortableParticipantList.add(sortableParticipant);
		}
		
		// Do the sorting on the list of SortableParticipant objects
		Collections.sort(sortableParticipantList);
		
		// Copy sorted list back into original participant list
		participantList = new ArrayList<Participant>();
		for (SortableParticipant sortableParticipant: sortableParticipantList) {
			participantList.add(sortableParticipant.getParticipant());
		}
	}
	
	public static String extractXrefStringFromParticipant(Participant participant) {
		Interactor interactor = participant.getInteractor();
		Xref xref = interactor.getXref();
		DbReference primaryRef = xref.getPrimaryRef();
		String id = primaryRef.getId();
		String db = primaryRef.getDb();
		
		return db + ":" + id;
	}
	
	/**
	 * Take the information from the supplied Participants object and merge it with
	 * "this".
	 * 
	 * @param participants
	 */
	public void merge(Participants participants) {
		setComplexStableIdentifiers(mergeStringLists(getComplexStableIdentifiers(), participants.getComplexStableIdentifiers()));
		setComplexNames(mergeStringLists(getComplexNames(), participants.getComplexNames()));
		setReactionStableIdentifiers(mergeStringLists(getReactionStableIdentifiers(), participants.getReactionStableIdentifiers()));
		setReactionNames(mergeStringLists(getReactionNames(), participants.getReactionNames()));
		setPathwayStableIdentifiers(mergeStringLists(getPathwayStableIdentifiers(), participants.getPathwayStableIdentifiers()));
		setPathwayNames(mergeStringLists(getPathwayNames(), participants.getPathwayNames()));
	}
	
	private List<String> mergeStringLists(List<String> list1, List<String> list2) {
		List<String> mergedList = new ArrayList<String>();
		
		for (String string: list1)
			mergedList.add(string);
		
		for (String string: list2)
			if (!list1.contains(string))
				mergedList.add(string);
		
		return mergedList;
	}

	class SortableParticipant implements Comparable<SortableParticipant> {
		Participant participant;

		public SortableParticipant(Participant participant) {
			this.participant = participant;
		}

		@Override
		public int compareTo(SortableParticipant sortableParticipant) {
			String xrefString = extractXrefStringFromParticipant(participant);
			String comparisonXrefString = extractXrefStringFromParticipant(sortableParticipant.getParticipant());
			
			return xrefString.compareTo(comparisonXrefString);
		}
		
		public Participant getParticipant() {
			return participant;
		}
	}
}
