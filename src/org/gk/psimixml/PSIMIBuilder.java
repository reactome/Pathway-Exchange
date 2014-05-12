/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.psimixml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.convert.common.DatabaseConnectionHandler;
import org.gk.convert.common.PathwayReactionHashHandler;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaClass;

import psidev.psi.mi.xml.PsimiXmlVersion;
import psidev.psi.mi.xml.model.Attribute;
import psidev.psi.mi.xml.model.BiologicalRole;
import psidev.psi.mi.xml.model.DbReference;
import psidev.psi.mi.xml.model.Entry;
import psidev.psi.mi.xml.model.EntrySet;
import psidev.psi.mi.xml.model.ExperimentDescription;
import psidev.psi.mi.xml.model.ExperimentalRole;
import psidev.psi.mi.xml.model.Interaction;
import psidev.psi.mi.xml.model.InteractionDetectionMethod;
import psidev.psi.mi.xml.model.InteractionType;
import psidev.psi.mi.xml.model.Interactor;
import psidev.psi.mi.xml.model.InteractorType;
import psidev.psi.mi.xml.model.Names;
import psidev.psi.mi.xml.model.Organism;
import psidev.psi.mi.xml.model.Participant;
import psidev.psi.mi.xml.model.ParticipantIdentificationMethod;
import psidev.psi.mi.xml.model.PsiFactory;
import psidev.psi.mi.xml.model.Xref;

/**
 * Export PSIMI XML from Reactome.
 * 
 * @author David Croft
 *
 */
// Note, MI terms obtained from OLS: http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MI
public class PSIMIBuilder {
	private DatabaseConnectionHandler databaseConnectionHandler = new DatabaseConnectionHandler();
	private PathwayReactionHashHandler pathwayReactionHashHandler = new PathwayReactionHashHandler();
	private InteractionDetectionMethod interactionDetMethod = PsiFactory.createInteractionDetectionMethod("MI:0364", "inferred by curator");
	private ParticipantIdentificationMethod participantIdentMethod = PsiFactory.createParticipantIdentificationMethod("MI:0396", "predetermined");
	private BiologicalRole biologicalRoleUnspecified = PsiFactory.createBiologicalRole("MI:0499", "unspecified role");
	private ExperimentalRole experimentalRoleNeutralComponent = PsiFactory.createExperimentalRole("MI:0497", "neutral component");
	private ExperimentDescription experimentDescription = PsiFactory.createExperiment("InferredFromReactomeData", "21082427", interactionDetMethod, participantIdentMethod, PsiFactory.createOrganismInVitro());
	private Organism human = PsiFactory.createOrganismHuman();
	private EntrySet entrySet = null;
	private static int MAX_PARTICIPANTS_LISTS_IN_SET = 100000;
	
	public PathwayReactionHashHandler getPathwayReactionHashHandler() {
		return pathwayReactionHashHandler;
	}

	public DatabaseConnectionHandler getDatabaseConnectionHandler() {
		return databaseConnectionHandler;
	}

	public EntrySet getEntrySet() {
		return entrySet;
	}

	/**
	 * This method will do conversion to PSIMI XML using the settings accumulated.
	 */
	public void convertPathways() {
		System.err.println(getClass().getCanonicalName() + ".convertPathways: entered");

		MySQLAdaptor databaseAdaptor = getDatabaseConnectionHandler().getDatabaseAdaptor();
		
		// Make sure that everything that needs a dba has one before starting
		pathwayReactionHashHandler.setDbAdaptor(databaseAdaptor);
		
		// We need to use the dba's internal caching, so turn it on for
		// the duration of the conversion.
		boolean originalUseCache = databaseAdaptor.isUseCache();
		databaseAdaptor.setUseCache(true);
		
		System.err.println(getClass().getCanonicalName() + ".convertPathways: running pathwayReactionHashHandler.derive");

		// Collect together all the pathway IDs and things that the
		// user has supplied to specify the construction of the
		// SBML.
		Map<Long,List<Long>> compositePathwayReactionHash = pathwayReactionHashHandler.derive();
		
		System.err.println(getClass().getCanonicalName() + ".convertPathways: running convertPathways");

		// Convert the specified things into SBML.
		convertPathways(compositePathwayReactionHash);
		
		databaseAdaptor.setUseCache(originalUseCache);
		
		System.err.println(getClass().getCanonicalName() + ".convertPathways: done");
	}
	
	/**
	 * Convert Reactome pathways to PSIMI XML.
	 * @param pathwayReactionHash keys are pathway DB_IDs, values are lists of reaction DB_IDs
	 */
	public void convertPathways(Map<Long,List<Long>> pathwayReactionHash) {
		System.err.println(getClass().getCanonicalName() + ".convertPathways: entered");

		if (pathwayReactionHash.size() == 0)
			return;
		if (databaseConnectionHandler == null) {
			System.err.println(getClass().getCanonicalName() + ".convertPathways: WARNING - databaseConnectionHandler == null");
			return;
		}
		MySQLAdaptor dba = databaseConnectionHandler.getDatabaseAdaptor();
		if (dba == null) {
			System.err.println(getClass().getCanonicalName() + ".convertPathways: WARNING - dba == null");
			return;
		}

		List<Participants> participanstList = new ArrayList<Participants>();

		// Sort by DB_ID, so that the results are reproducuble.
		Set<Long> pathwayDbIds = pathwayReactionHash.keySet();
		List<Long> sortedPathwayDbIds =  new ArrayList<Long>(pathwayDbIds);
		Collections.sort(sortedPathwayDbIds);
		int pathwayCount = sortedPathwayDbIds.size();
		int pathwayNum = 0;
		double pathwayPercent;
		for (Long pathwayDbId: sortedPathwayDbIds) {
			pathwayPercent = (100.0 * pathwayNum) / pathwayCount;
			System.err.println(getClass().getCanonicalName() + ".convertPathways: dealing with pathway, DB_ID=" + pathwayDbId + " (" + pathwayPercent + "%)");
			pathwayNum++;
			
			try {
				GKInstance pathway = dba.fetchInstance(pathwayDbId);
				String pathwayName = pathway.getDisplayName();
				String pathwayStableIdentifierString = null;
				try {
					GKInstance pathwayStableIdentifier = (GKInstance)pathway.getAttributeValue("stableIdentifier");
					if (pathwayStableIdentifier != null)
						pathwayStableIdentifierString = (String)pathwayStableIdentifier.getAttributeValue("identifier");
				} catch (Exception e) {
					System.err.println(getClass().getCanonicalName() + ".convertPathways: WARNING - problem getting stable ID from pathway, DB_ID=" + pathwayDbId);
					e.printStackTrace(System.err);
				}
				for (Long reactionDbId: pathwayReactionHash.get(pathwayDbId)) {
					GKInstance reaction = dba.fetchInstance(reactionDbId);
					List<Participants> reactionParticipanstList = createPSIMIParticipantsListFromReactomeReaction(reaction, pathwayStableIdentifierString, pathwayName);
					participanstList.addAll(reactionParticipanstList);
				}
				
				// Do some filtering on the participants list at this point, rather than
				// leaving it to later.
				List<Participants> mergedParticipanstList = mergeIdenticalComplexesFromParticipantsList(participanstList);
				participanstList = mergedParticipanstList;
				System.err.println(getClass().getCanonicalName() + ".convertPathways: participants list size: " + participanstList.size());
			} catch (Exception e) {
				System.err.println(getClass().getCanonicalName() + ".convertPathways: WARNING - could not get complexes for pathway DB_ID: " + pathwayDbId);
				e.printStackTrace(System.err);
			}
		}

		System.err.println(getClass().getCanonicalName() + ".convertPathways: FINAL participants list size: " + participanstList.size());

		System.err.println(getClass().getCanonicalName() + ".convertPathways: running convertParticipantsListToInteractions");

		// Turn each of the PSIMI participant lists into an interaction
		List<Interaction> interactions = convertParticipantsListToInteractions(participanstList);

		System.err.println(getClass().getCanonicalName() + ".convertPathways: running PsiFactory.createEntry");

        // we put the collection of interactions in an entry
        Entry entry = PsiFactory.createEntry(PsiFactory.createSource("Reactome"), interactions);

		System.err.println(getClass().getCanonicalName() + ".convertPathways: running PsiFactory.createEntrySet");

        // and finally we create the root object, the EntrySet, that contains the entries
        entrySet = PsiFactory.createEntrySet(PsimiXmlVersion.VERSION_254, entry);

		System.err.println(getClass().getCanonicalName() + ".convertPathways: done");
	}
	
	/**
	 * Take the given Reactome reaction, extract all of the complexes that it contains,
	 * convert them to PSIMI complexes, and return these as a list.
	 * 
	 * @param reaction
	 * @return
	 */
	private List<Participants> createPSIMIParticipantsListFromReactomeReaction(GKInstance reactionlikeEvent, String pathwayStableIdentifierString, String pathwayName) {
		if (reactionlikeEvent == null) {
			System.err.println(getClass().getCanonicalName() + ": WARNING - reaction is null");
			return null;
		}
		SchemaClass schemClass = reactionlikeEvent.getSchemClass();
		if (!schemClass.isa("ReactionlikeEvent")) {
			System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantsListFromReactomeReaction: WARNING - reaction is not a ReactionlikeEvent instance, it is a " + schemClass.getName());
			return null;
		}
		
		Long reactionDbId = reactionlikeEvent.getDBID();
		String reactionName = reactionlikeEvent.getDisplayName();
		String reactionStableIdentifierString = null;
		try {
			GKInstance reactionStableIdentifier = (GKInstance)reactionlikeEvent.getAttributeValue("stableIdentifier");
			if (reactionStableIdentifier != null)
				reactionStableIdentifierString = (String)reactionStableIdentifier.getAttributeValue("identifier");
		} catch (Exception e) {
			System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantsListFromReactomeReaction: WARNING - problem getting stable ID from reaction, DB_ID=" + reactionDbId);
			e.printStackTrace(System.err);
		}
		List<Participants> participanstList = new ArrayList<Participants>();
		try {
			// Add inputs
			List<GKInstance> inputs = reactionlikeEvent.getAttributeValuesList("input");
			if (inputs != null)
				for (GKInstance entity: inputs) {
					List<Participants> inputsParticipanstList = createPSIMIParticipantsListFromReactomeEntity(entity, reactionStableIdentifierString, reactionName, pathwayStableIdentifierString, pathwayName);
					participanstList.addAll(inputsParticipanstList);
				}

			// Add outputs
			List<GKInstance> outputs = reactionlikeEvent.getAttributeValuesList("output");
			if (outputs != null)
				for (GKInstance entity: outputs) {
					List<Participants> outputsParticipanstList = createPSIMIParticipantsListFromReactomeEntity(entity, reactionStableIdentifierString, reactionName, pathwayStableIdentifierString, pathwayName);
					participanstList.addAll(outputsParticipanstList);
				}

			// Catalysts
			List<GKInstance> catalystActivities = reactionlikeEvent.getAttributeValuesList("catalystActivity");
			if (catalystActivities != null)
				for (GKInstance catalystActivity: catalystActivities) {
					GKInstance entity = (GKInstance)catalystActivity.getAttributeValue("physicalEntity");
					if (entity != null) {
						List<Participants> catalystActivitiesParticipanstList = createPSIMIParticipantsListFromReactomeEntity(entity, reactionStableIdentifierString, reactionName, pathwayStableIdentifierString, pathwayName);
						participanstList.addAll(catalystActivitiesParticipanstList);
					}
				}
		} catch (Exception e) {
			System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantsListFromReactomeReaction: WARNING - reaction is not a ReactiolikeEvent instance, it is a " + schemClass.getName());
			e.printStackTrace(System.err);
		}
		
		// Do some filtering on the participants list at this point, rather than
		// leaving it to later.
		List<Participants> mergedParticipanstList = mergeIdenticalComplexesFromParticipantsList(participanstList);
		System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantsListFromReactomeReaction: participants list size: " + participanstList.size());

		return mergedParticipanstList;
	}
	
	private List<Participants> createPSIMIParticipantsListFromReactomeEntity(GKInstance entity, String reactionStableIdentifierString, String reactionName, String pathwayStableIdentifierString, String pathwayName) {
		// Generate PSIMI participant list(s) from the supplied entity.
		List<Participants> participanstList = new ArrayList<Participants>();
		String entityStableIdentifierString = null;
		try {
			GKInstance entityStableIdentifier = (GKInstance)entity.getAttributeValue("stableIdentifier");
			if (entityStableIdentifier != null)
				entityStableIdentifierString = (String)entityStableIdentifier.getAttributeValue("identifier");
		} catch (Exception e) {
			System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantsListFromReactomeEntity: WARNING - problem getting stable ID from entity");
			e.printStackTrace(System.err);
		}
		String entityName = entity.getDisplayName().replaceAll("\\[[^\\]]+\\]", "").replaceAll(" +$", "");
		
		try {
			createPSIMIParticipantListsFromReactomeEntity(entity, participanstList, 0, entityStableIdentifierString, entityName, reactionStableIdentifierString, reactionName, pathwayStableIdentifierString, pathwayName);
		} catch (TooManyParticipantsException e) {
			System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantsListFromReactomeEntity: TooManyParticipantsException: " + e.getMessage());
			System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantsListFromReactomeEntity: WARNING - ignoring entity " + entityName + " (" + entityStableIdentifierString + ") because it produces too many participants");
			participanstList = new ArrayList<Participants>();
		}
		
		// Do some filtering on the participants list at this point, rather than
		// leaving it to later.
		List<Participants> proteinatedParticipanstList = removeProteinlessComplexesFromParticipantsList(participanstList);
		List<Participants> mergedParticipanstList = mergeIdenticalComplexesFromParticipantsList(proteinatedParticipanstList);
//		System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantsListFromReactomeEntity: participants list size: " + participanstList.size());
//
		return mergedParticipanstList;
	}
	
	/**
	 * Filter out complexes that don't contain any proteins.  This was a request
	 * from Birgit.
	 * 
	 * @param participanstList
	 * @return list of Interaction objects
	 */
	private List<Participants> removeProteinlessComplexesFromParticipantsList(List<Participants> participanstList) {
		List<Participants> newParticipanstList = new ArrayList<Participants>();
		for (Participants participants: participanstList) {
			if (participants.isContainsProtein())
				newParticipanstList.add(participants);
		}
		
		return newParticipanstList;
	}
	
	/**
	 * Combine "identical" complexes.  Reactome distinguishes between many things
	 * that for IntAct are not important, such as proteins that have been post
	 * translationally modified.  This means that Reactome contains complexes that
	 * are, from IntAct's point of view, indistinguishable.
	 * 
	 * @param participanstList
	 * @return list of Interaction objects
	 */
	private List<Participants> mergeIdenticalComplexesFromParticipantsList(List<Participants> participanstList) {
		// First sort each participant list by identifier, to make it easier
		// to compare them.
		for (Participants participants: participanstList) {
			participants.sortParticipantList();
		}

		// Now throw away duplicates
		List<Participants> newParticipanstList = new ArrayList<Participants>();
		boolean foundIdenticalParticipants;
		for (int i=0; i<participanstList.size(); i++) {
			Participants participants = participanstList.get(i);
			foundIdenticalParticipants = false;
			for (int j=i+1; j<participanstList.size(); j++) {
				Participants followingParticipants = participanstList.get(j);
				if (isParticipantsEqual(participants, followingParticipants)) {
					foundIdenticalParticipants = true;
					followingParticipants.merge(participants);
					break;
				}
			}
			if (!foundIdenticalParticipants)
				newParticipanstList.add(participants);
		}

		return newParticipanstList;
	}
	
	private boolean isParticipantsEqual(Participants participants1, Participants participants2) {
		List<Participant> participantList1 = participants1.getParticipantList();
		List<Participant> participantList2 = participants2.getParticipantList();
		
		if (participantList1.size() != participantList2.size())
			return false;
		
		for (int i=0; i<participantList1.size(); i++) {
			Participant participant1 = participantList1.get(i);
			Participant participant2 = participantList2.get(i);
			
			if (!Participants.extractXrefStringFromParticipant(participant1).equals(Participants.extractXrefStringFromParticipant(participant2)))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Take each Participants object in the participanstList and convert it into an Interaction object.
	 * 
	 * @param participanstList
	 * @return list of Interaction objects
	 */
	private List<Interaction> convertParticipantsListToInteractions(List<Participants> participanstList) {
		// Do some preprocessing on the participants lists, to adapt from the
		// Reactome concept of a complex to the IntAct concept of a complex.
//		System.err.println(getClass().getCanonicalName() + ".convertParticipantsListToInteractions: removing proteinless participant lists");
//		List<Participants> proteinlessParticipanstList = removeProteinlessComplexesFromParticipantsList(participanstList);
//		System.err.println(getClass().getCanonicalName() + ".convertParticipantsListToInteractions: merging identical complexes");
//		List<Participants> mergedParticipanstList = mergeIdenticalComplexesFromParticipantsList(participanstList);
		
		List<Interaction> interactions = new ArrayList<Interaction>();
		int participantsNum = 0;
		int participantsCount = participanstList.size();
		double participantsPercent;
		for (Participants participants: participanstList) {
	        InteractionType interactionType = PsiFactory.createInteractionType("MI:1110", "predicted interaction");
	        String entityName = "Complex " + participantsNum;
	        List<String> complexNames = participants.getComplexNames();
	        if (complexNames.size() > 0)
	        	entityName = complexNames.get(0);
			if (participantsNum % 200 == 0) {
				participantsPercent = (100.0 * participantsNum) / participantsCount;
				System.err.println(getClass().getCanonicalName() + ".convertParticipantsListToInteractions: participantsNum=" + participantsNum + "(" + participantsPercent + "%), entityName=" + entityName);
			}
	        Interaction interaction = PsiFactory.createInteraction(entityName, experimentDescription, interactionType, participants.getParticipantList());
	        participantsNum++;
	        
	        // Set complex stable ID as primary cross reference, pathway and
	        // reaction stable IDs as secondary cross references.
	        Xref xref = new Xref();
			Collection<DbReference> secondaryRefs = xref.getSecondaryRef();
	        List<String> entityStableIdentifiers = participants.getComplexStableIdentifiers();
	        for (int i=0; i<entityStableIdentifiers.size(); i++) {
		        String entityStableIdentifier = entityStableIdentifiers.get(i);
		        DbReference complexRef = createDbReference(entityStableIdentifier, "Complex");
		        if (complexRef != null) {
		        	complexRef.setRefType("identity");
		        	if (i == 0)
		        		// arbitrarily set the first complex reference as "primary".
		        		xref.setPrimaryRef(complexRef );
		        	else
						secondaryRefs.add(complexRef);
		        }
	        }
			if (secondaryRefs != null) {
				List<String> pathwayStableIdentifiers = participants.getPathwayStableIdentifiers();
				for (String pathwayStableIdentifier: pathwayStableIdentifiers) {
					DbReference pathwayRef = createDbReference(pathwayStableIdentifier, "Pathway");
					if (pathwayRef != null) {
						pathwayRef.setRefType("process");
						secondaryRefs.add(pathwayRef);
					}
				}
				List<String> reactionStableIdentifiers = participants.getReactionStableIdentifiers();
				for (String reactionStableIdentifier: reactionStableIdentifiers) {
					DbReference reactionRef = createDbReference(reactionStableIdentifier, "Reaction");
					if (reactionRef != null) {
						reactionRef.setRefType("process");
						secondaryRefs.add(reactionRef);
					}
				}
				List<String> pubMedIds = participants.getPubMedIds();
				for (String pubMedId: pubMedIds) {
					DbReference reactionRef = createDbReference("pubmed", "MI:0446", pubMedId, null);
					if (reactionRef != null) {
						reactionRef.setRefType("see-also");
						secondaryRefs.add(reactionRef);
					}
				}
			}
			interaction.setXref(xref);
			
			// Add some extra pieces of potentially useful information as name/value
			// pairs.
			Collection<Attribute> attributes = interaction.getAttributes();
			if (attributes != null) {
				List<String> pathwayNames = participants.getPathwayNames();
				for (String pathwayName: pathwayNames) {
					Attribute pathwayNameAttribute = createAttribute("complex-synonym", pathwayName);
					if (pathwayNameAttribute != null)
						attributes.add(pathwayNameAttribute);
				}
				List<String> reactionNames = participants.getReactionNames();
				for (String reactionName: reactionNames) {
					Attribute reactionNameAttribute = createAttribute("curated-complex", reactionName);
					if (reactionNameAttribute != null)
						attributes.add(reactionNameAttribute);
				}
			}
			
	        interactions.add(interaction);
		}
        
        return interactions;
	}
	
	private DbReference createDbReference(String id, String type) {
		return createDbReference("reactome complex", "MI:0244", id, type);
	}
	
	private DbReference createDbReference(String db, String dbAc, String id, String type) {
		DbReference dbReference = null;
		
		if (id != null) {
	        dbReference = new DbReference();
	        dbReference.setDb(db);
	        dbReference.setDbAc(dbAc);
	        dbReference.setId(id);
	        if (type != null)
	        	dbReference.getAttributes().add(new Attribute("ReactomeType", type));
		}
		
		return dbReference;
	}
	
	private Attribute createAttribute(String name, String value) {
		Attribute attribute = null;
		
		if (value != null) {
			attribute = new Attribute();
			attribute.setName(name);
			attribute.setValue(value);
		}
		
		return attribute;
	}
	
	/**
	 * A list of participants contains the proteins or small molecules taking part in
	 * a complex.  A single entity can actually produce multiple lists of participants,
	 * because Reactome sets cannot be represented in IntAct, so we generate one
	 * participant list per set member.  This can and does lead to combinatorial
	 * explosions, so the code contains some hacks to keep these under control.
	 * 
	 * @param entity
	 * @param participanstList
	 * @param complexDepth
	 * @param complexStableIdentifierString
	 * @param complexName
	 * @param reactionStableIdentifierString
	 * @param reactionName
	 * @param pathwayStableIdentifierString
	 * @param pathwayName
	 * @throws TooManyParticipantsException
	 */
	private void createPSIMIParticipantListsFromReactomeEntity(GKInstance entity, List<Participants> participanstList, int complexDepth, String complexStableIdentifierString, String complexName, String reactionStableIdentifierString, String reactionName, String pathwayStableIdentifierString, String pathwayName) throws TooManyParticipantsException {
		if (entity == null) {
			System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantListsFromReactomeEntity: WARNING - entity is null");
			return;
		}

		SchemaClass schemClass = entity.getSchemClass();
		if (schemClass.isa("Complex"))
			// Only count a sub-complex as being a level in the complex
			createPSIMIParticipantListsFromReactomeComplex(entity, participanstList, complexDepth + 1, complexStableIdentifierString, complexName, reactionStableIdentifierString, reactionName, pathwayStableIdentifierString, pathwayName);
		else if (schemClass.isa("EntitySet"))
			createPSIMIParticipantListsFromReactomeSet(entity, participanstList, complexDepth, complexStableIdentifierString, complexName, reactionStableIdentifierString, reactionName, pathwayStableIdentifierString, pathwayName);
		else if (schemClass.isa("EntityWithAccessionedSequence") || schemClass.isa("SimpleEntity")) {
			if (complexDepth > 0) {
				// A scalar entity in, say, the input of a reaction is not a complex,
				// so skip it.  A scalar entity at a deeper level in the hierarchy
				// of a complex needs to be added to the complex.
				addReactomeScalarEntityToPSIMIParticipantLists(entity, participanstList, complexStableIdentifierString, complexName, reactionStableIdentifierString, reactionName, pathwayStableIdentifierString, pathwayName);
			}
		} // ignore Polymer and OtherEntity
	}
	
	private void createPSIMIParticipantListsFromReactomeComplex(GKInstance complex, List<Participants> participanstList, int complexDepth, String complexStableIdentifierString, String complexName, String reactionStableIdentifierString, String reactionName, String pathwayStableIdentifierString, String pathwayName) throws TooManyParticipantsException {
		List<GKInstance> components = null;
		try {
			components = complex.getAttributeValuesList("hasComponent");
		} catch (Exception e) {
			System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantListsFromReactomeComplex: WARNING - problem getting complex components");
			e.printStackTrace(System.err);
		}
		if (components != null)
			for (GKInstance component: components)
				createPSIMIParticipantListsFromReactomeEntity(component, participanstList, complexDepth, complexStableIdentifierString, complexName, reactionStableIdentifierString, reactionName, pathwayStableIdentifierString, pathwayName);
		
		if (complexDepth == 1)
			addComplexLiteratureReferencesToParticipanstList(complex, participanstList);
	}
	
	/**]
	 * If the supplied complex has an associated literature reference, add it to
	 * all of the participants in the participants list.
	 * 
	 * @param complex
	 * @param participanstList
	 */
	private void addComplexLiteratureReferencesToParticipanstList(GKInstance complex, List<Participants> participanstList) {
		try {
			List<GKInstance> literatureReferences = complex.getAttributeValuesList("literatureReference");
			List<String> pubMedIds = new ArrayList<String>();
			if (literatureReferences != null)
				for (GKInstance literatureReference: literatureReferences)
					if (literatureReference != null && literatureReference.getSchemClass().isa("LiteratureReference")) {
						Integer pubMedIdentifier = (Integer)literatureReference.getAttributeValue("pubMedIdentifier");
						if (pubMedIdentifier != null)
							pubMedIds.add(pubMedIdentifier.toString());
					}
			for (String pubMedId: pubMedIds)
				for (Participants participant: participanstList)
					participant.addPubMedId(pubMedId);
		} catch (Exception e) {
			System.err.println(getClass().getCanonicalName() + ".addComplexLiteratureReferencesToParticipanstList: WARNING - problem getting literature references from complex");
			e.printStackTrace(System.err);
		}
	}

	private void createPSIMIParticipantListsFromReactomeSet(GKInstance set, List<Participants> participanstList, int complexDepth, String complexStableIdentifierString, String complexName, String reactionStableIdentifierString, String reactionName, String pathwayStableIdentifierString, String pathwayName) throws TooManyParticipantsException {
		List<GKInstance> members = null;
		try {
			members = set.getAttributeValuesList("hasMember");
		} catch (Exception e) {
			System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantListsFromReactomeSet: WARNING - problem getting set members");
			e.printStackTrace(System.err);
		}
		if (members != null) {
			List<Participants> newParticipanstList = new ArrayList<Participants>();
			for (GKInstance member: members) {
				List<Participants> clonedParticipanstList = new ArrayList<Participants>();
				try {
					for (Participants participants: participanstList)
						clonedParticipanstList.add((Participants) participants.clone());
				} catch (CloneNotSupportedException e) {
					System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantListsFromReactomeSet: WARNING - cant clone participants");
					e.printStackTrace(System.err);
					break;
				}
				createPSIMIParticipantListsFromReactomeEntity(member, clonedParticipanstList, complexDepth, complexStableIdentifierString, complexName, reactionStableIdentifierString, reactionName, pathwayStableIdentifierString, pathwayName);
				newParticipanstList.addAll(clonedParticipanstList);

				if (newParticipanstList.size() > MAX_PARTICIPANTS_LISTS_IN_SET)
					throw(new TooManyParticipantsException("the number of participant lists (" + newParticipanstList.size() + ") exceeds " + MAX_PARTICIPANTS_LISTS_IN_SET + ", may cause out-of-memory errors.  Complex stable ID: " + complexStableIdentifierString + ", complex name: " + complexName));
			}
			participanstList.clear();
			for (Participants participants: newParticipanstList)
				participanstList.add(participants);
		}
		//		
		//		System.err.println(getClass().getCanonicalName() + ".createPSIMIParticipantListsFromReactomeSet: complexDepth=" + complexDepth + ", participants list size: " + participanstList.size());
	}
	
	/**
	 * A "scalar" entity is either a EntityWithAccessionedSequence or a SimpleEntity.  In this
	 * method, this entity is assumed to be a component of a complex.  The participanstList
	 * contains zero or more complexes that are in the process of being
	 * constructed.  The entity is added to each of the complexes in the list.
	 * If there are no complexes in the list, then one is created, and the entity
	 * will be added to it, as the first element.
	 * 
	 * @param entity
	 * @param participanstList
	 * @param reactionDbId
	 * @param reactionName
	 * @param pathwayDbId
	 * @param pathwayName
	 */
	private void addReactomeScalarEntityToPSIMIParticipantLists(GKInstance entity, List<Participants> participanstList, String complexStableIdentifierString, String complexName, String reactionStableIdentifierString, String reactionName, String pathwayStableIdentifierString, String pathwayName) {
		Participant participant = createPSIMIParticipantFromReactomeScalarEntity(entity);
		boolean containsProtein = entity.getSchemClass().isa("EntityWithAccessionedSequence");
		if (participanstList.size() < 1) {
			Participants participants = new Participants(complexStableIdentifierString, complexName, reactionStableIdentifierString, reactionName, pathwayStableIdentifierString, pathwayName);
			participanstList.add(participants);
		}
		for (Participants participants: participanstList) {
			participants.setContainsProtein(containsProtein);
			participants.addParticipant(participant);
		}
	}

	/**
	 * A "scalar" entity is either a EntityWithAccessionedSequence or a SimpleEntity.  Creates
	 * a PSMI participant corresponding to the Reactome entity and returns is.
	 * 
	 * @param entity
	 * @param reactionDbId
	 * @param reactionName
	 * @param pathwayDbId
	 * @param pathwayName
	 */
	private Participant createPSIMIParticipantFromReactomeScalarEntity(GKInstance entity) {
		String referenceDatabaseName = null;
		String referenceIdentifier = null;
		String referenceClass = null;
		try {
			GKInstance referenceEntity = (GKInstance)entity.getAttributeValue("referenceEntity");
			if (referenceEntity != null) {
				GKInstance referenceDatabase = (GKInstance)referenceEntity.getAttributeValue("referenceDatabase");
				if (referenceDatabase != null)
					referenceDatabaseName = (String)referenceDatabase.getAttributeValue("name");
				referenceIdentifier = (String)referenceEntity.getAttributeValue("identifier");
				referenceClass = referenceEntity.getSchemClass().getName();
			}
		} catch (Exception e) {
			System.err.println(getClass().getCanonicalName() + ".getReferenceDatabaseAndIdentifierFromScalarEntity: WARNING - problem getting reference entity information from entity");
			e.printStackTrace(System.err);
		}
		
		String referenceDatabaseAccession = "MI:0329"; // unknown database.  TODO: is this the right MI number?
		if (referenceDatabaseName.equals("UniProt"))
			referenceDatabaseAccession = "MI:0486";
		else if (referenceDatabaseName.equals("ChEBI"))
			referenceDatabaseAccession = "MI:0474";
		
		InteractorType interactorType = createPSIMIInteractorType(referenceClass);
		Interactor interactor = PsiFactory.createInteractor(referenceIdentifier, referenceDatabaseAccession, interactorType, human);
        Participant participant = PsiFactory.createParticipant(interactor, biologicalRoleUnspecified, experimentalRoleNeutralComponent);
        
        return participant;
	}
	
	private InteractorType createPSIMIInteractorType(String referenceClass) {
		Names names = new Names();
		Xref xref = new Xref();
		DbReference dbReference = new DbReference();
		
		String interactorTypeName = "unknown participant";
		String interactorTypeId = "MI:0329";
		if (referenceClass.equals("ReferenceMolecule")) {
			interactorTypeName = "small molecule";
			interactorTypeId = "MI:0328";
		} else if (referenceClass.equals("ReferenceGeneProduct")) {
			interactorTypeName = "protein";
			interactorTypeId = "MI:0326";
		}
		names.setFullName(interactorTypeName);
		names.setShortLabel(interactorTypeId);
//		dbReference.setId(referenceIdentifier);
		dbReference.setDb("psi-mi");
		xref.setPrimaryRef(dbReference);
//		InteractorType interactorType = new InteractorType();
		InteractorType interactorType = PsiFactory.createInteractorType(interactorTypeId, interactorTypeName);
//		interactorType.setNames(names);
//		interactorType.setXref(xref);

		return interactorType;
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
