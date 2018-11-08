/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.convert.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.ClassAttributeFollowingInstruction;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

/**
 * Maps various kinds of thing onto a pathway reaction hash.  This is a hash map,
 * where the keys are pathway IDs and the values are lists of reaction IDs.  The
 * reactions must be part of the pathway denoted by the key, but not all
 * reactions in the pathway need to be in the list, i.e. it may be a subset
 * of the full list of reactions present in a pathway.
 * 
 * This is basically a throwaway utility class.
 * 
 * @author David Croft
 *
 */
public class PathwayReactionHashHandler {
	private Map<Long,List<Long>> pathwayReactionHash = null;
	private List<Long> reactionDbIds = null;
	private List<Long> pathwayDbIds = null;
	private MySQLAdaptor databaseAdaptor = null;
	private InstanceFilters instanceFilters = new InstanceFilters();
	private boolean concatenateReactionFlag = false;
	
	public void setPathwayReactionHash(HashMap<Long, List<Long>> pathwayReactionHash) {
		this.pathwayReactionHash = pathwayReactionHash;
	}
	
	public void addPathwayReactionHashElement(Long pathwayDbId, List<Long> reactionDbIdList) {
		if (pathwayDbId == null || reactionDbIdList == null) {
			System.err.println("PathwayReactionHash.addReactionDbId: pathwayDbId or reactionDbIdList is null");
			return;
		}
		
		if (pathwayReactionHash == null)
			pathwayReactionHash = new HashMap<Long,List<Long>>();
		
		pathwayReactionHash.put(pathwayDbId, reactionDbIdList);
	}

	public void setReactionDbIds(List<Long> reactionDbIds) {
		this.reactionDbIds = reactionDbIds;
	}
	
	public void addReactionDbId(Long reactionDbId) {
		if (reactionDbId == null) {
			System.err.println("PathwayReactionHash.addReactionDbId: reactionDbId == null");
			return;
		}
		
		if (reactionDbIds == null)
			reactionDbIds = new ArrayList<Long>();
		
		reactionDbIds.add(reactionDbId);
	}

	public void setPathwayDbIds(List<Long> pathwayDbIds) {
		this.pathwayDbIds = pathwayDbIds;
	}
	
	public void addPathwayDbId(Long pathwayDbId) {
		if (pathwayDbId == null) {
			System.err.println("PathwayReactionHash.addPathwayDbId: pathwayDbId == null");
			return;
		}
		
		if (pathwayDbIds == null)
			pathwayDbIds = new ArrayList<Long>();
		
		pathwayDbIds.add(pathwayDbId);
	}

	public void setDbAdaptor(MySQLAdaptor dbAdaptor) {
		this.databaseAdaptor = dbAdaptor;
	}

	public void setConcatenateReactionFlag(boolean concatenateReactionFlag) {
		this.concatenateReactionFlag = concatenateReactionFlag;
	}

	public InstanceFilters getInstanceFilters() {
		return instanceFilters;
	}

	public void addSpeciesFilter(String species) {
		instanceFilters.addInclusionFilter("DatabaseObject", "species", species);
	}

	private HashMap<Long,List<Long>> deriveFromAllPathways() {
		if (databaseAdaptor == null) {
			System.err.println("PathwayReactionHash.deriveFromAllPathways: dbAdaptor == null");
			return null;
		}
		
		List<Long> pathwayDbIds = new ArrayList<Long>();
		try {
			Collection<GKInstance> pathways = databaseAdaptor.fetchInstancesByClass("Pathway");
			for (GKInstance pathway: pathways) {
				Collection<GKInstance> pathwayDiagrams = pathway.getReferers("representedPathway");
				if (pathwayDiagrams == null)
					continue;
				if (pathwayDiagrams.size() < 1)
					continue;
				pathwayDbIds.add(pathway.getDBID());
			}
		} catch (Exception e) {
			System.err.println("PathwayReactionHash.deriveFromAllPathways: WARNING - problem deriving hash from pathway IDs");
			e.printStackTrace(System.err);
		}
		
		return deriveFromPathwayIds(pathwayDbIds);
	}
	
	private HashMap<Long,List<Long>> deriveFromPathwayIds(List<Long> pathwayDbIds) {
		HashMap<Long,List<Long>> pathwayReactionHash = new HashMap<Long,List<Long>>();
		try {
			GKInstance pathway;
			for (Long pathwayDbID: pathwayDbIds) {
				pathway = databaseAdaptor.fetchInstance(pathwayDbID);
				if (pathway == null) {
					System.err.println("PathwayReactionHash.deriveFromPathwayIds: WARNING - no pathway found for DB_ID=" + pathwayDbID);
					continue;
				}
				Collection<GKInstance> reactionlikEvents = getReactionlikeEventsFromPathway(pathway);
				List<Long> reactionDbIds = new ArrayList<Long>();
				pathwayReactionHash.put(pathwayDbID, reactionDbIds);
				for (GKInstance reactionlikEvent: reactionlikEvents)
					reactionDbIds.add(reactionlikEvent.getDBID());
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		
		return pathwayReactionHash;
	}
	
	private HashMap<Long,List<Long>> deriveFromReactionIds(List<Long> instanceDbIds) {
		HashMap<Long,List<Long>> pathwayReactionHash = new HashMap<Long,List<Long>>();

		System.err.println("PathwayReactionHash.deriveFromReactionIds: entered");
		
		if (databaseAdaptor == null) {
			System.err.println("PathwayReactionHash.deriveFromReactionIds: WARNING - dbAdaptor == null, aborting!!");
			return pathwayReactionHash;
		}

		for (Long instanceDbId: instanceDbIds) {
			Long pathwayDbId = getTopLevelPathwayDbId(databaseAdaptor, instanceDbId);
			if (pathwayDbId == null) {
				System.err.println("PathwayReactionHash.deriveFromReactionIds: WARNING - pathwayDbId == null for instanceDbId=" + instanceDbId);
				continue;
			}

			System.err.println("PathwayReactionHash.deriveFromReactionIds: pathwayDbId="+pathwayDbId);

			List<Long> reactionDbIdList = pathwayReactionHash.get(pathwayDbId);
			if (reactionDbIdList == null)
				reactionDbIdList = new ArrayList<Long>();
			reactionDbIdList.add(instanceDbId);
			pathwayReactionHash.put(pathwayDbId, reactionDbIdList);
		}

		System.err.println("PathwayReactionHash.deriveFromReactionIds: done");
		
		return pathwayReactionHash;
	}
	
	public Map<Long,List<Long>> derive() {
		Map<Long,List<Long>> compositePathwayReactionHash = null;
		if (pathwayReactionHash == null && pathwayDbIds == null && reactionDbIds == null)
			compositePathwayReactionHash = deriveFromAllPathways();
		else {
			if (pathwayReactionHash != null)
				compositePathwayReactionHash = pathwayReactionHash;
			else
				compositePathwayReactionHash = new HashMap<Long,List<Long>>();
			if (pathwayDbIds != null)
				addPathwayReactionHash(compositePathwayReactionHash, deriveFromPathwayIds(pathwayDbIds));
			if (reactionDbIds != null)
				addPathwayReactionHash(compositePathwayReactionHash, deriveFromReactionIds(reactionDbIds));
		}
		
		pathwayReactionHash = filter(compositePathwayReactionHash);
		pathwayReactionHash = concatenateReactions(pathwayReactionHash);
		
		return pathwayReactionHash;
	}

	private void addPathwayReactionHash(Map<Long,List<Long>> pathwayReactionHash1, Map<Long,List<Long>> pathwayReactionHash2) {
		for (Long pathwayDbId2: pathwayReactionHash2.keySet()) {
			List<Long> reactionList2 = pathwayReactionHash2.get(pathwayDbId2);
			if (pathwayReactionHash1.containsKey(pathwayDbId2)) {
				for (Long reactionDbId2: reactionList2)
					if (!pathwayReactionHash1.get(pathwayDbId2).contains(reactionDbId2))
						pathwayReactionHash1.get(pathwayDbId2).add(reactionDbId2);
			} else
				pathwayReactionHash1.put(pathwayDbId2, reactionList2);
		}
	}

	private Map<Long, List<Long>> filter(Map<Long, List<Long>> pathwayReactionHash) {
		instanceFilters.setDatabaseAdaptor(databaseAdaptor);
		return instanceFilters.filterPathwayReactionHash(pathwayReactionHash);
	}

	private Map<Long, List<Long>> concatenateReactions(Map<Long, List<Long>> compositePathwayReactionHash) {
		if (concatenateReactionFlag) {
			ReactionConcatenator reactionConcatenator = new ReactionConcatenator(databaseAdaptor);
			pathwayReactionHash = reactionConcatenator.concatenatePathwayReactionHash(pathwayReactionHash);
		}
		
		return pathwayReactionHash;
	}
	
	private Collection<GKInstance> getReactionlikeEventsFromPathway(GKInstance pathway) {
		if (pathway == null) {
			System.err.println("PathwayReactionHash.getReactionlikeEventsFromPathway: pathway == null");
			return null;
		}
		Collection<GKInstance> reactionlikEvents = null;
		try {
			List<ClassAttributeFollowingInstruction> instructions = new ArrayList<ClassAttributeFollowingInstruction>();
			instructions.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.Pathway, new String[] {ReactomeJavaConstants.hasEvent}, null));
			instructions.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.Reaction, new String[] {ReactomeJavaConstants.reverseReaction}, null));
//			instructions.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.ReactionlikeEvent, new String[] {ReactomeJavaConstants.hasMember}, null)); // As of release 41, ReactionlikeEvent does not have a hasMember attribute
			// As of November, 2018, hasEvent is gone in BlackBoxEvent class
			//			instructions.add(new ClassAttributeFollowingInstruction(ReactomeJavaConstants.BlackBoxEvent, new String[] {ReactomeJavaConstants.hasEvent, "templateEvent"}, null));
			reactionlikEvents = InstanceUtilities.followInstanceAttributes(pathway, instructions, new String[]{ReactomeJavaConstants.ReactionlikeEvent});
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	
		return reactionlikEvents;
	}

	/*
	 * Given the dbId of an entity or event, return the DB_ID of the top
	 * level pathway containing it.
	 */
	private Long getTopLevelPathwayDbId(MySQLAdaptor dbAdaptor, Long dbId) {
		List<Long> topLevelPathwayDbIds = null;
		try {
			topLevelPathwayDbIds = getTopLevelPathwayDbIds(dbAdaptor, dbId);
		} catch (Exception e) {
			System.err.println("PathwayReactionHashHandler.getTopLevelPathwayDbId: WARNING - could not get top level pathways for dbId=" + dbId);
			e.printStackTrace(System.err);
		}
		
		if (topLevelPathwayDbIds == null || topLevelPathwayDbIds.size() == 0)
			return null;
		
		Collections.sort(topLevelPathwayDbIds);
		
		return topLevelPathwayDbIds.get(0);
	}
	
	/*
	 * Given a instance id, map it to a reaction if it not an event and find paths from that event
	 * to all top level pathways it is a part of.
	 * 
	 * @param instanceId The entity to find a path for. If its not a reaction, reactions in which it is an input or output for
	 * 	will be identified.
	 *  @param pathMap The map to collect paths in. Key is toplevel pathway id
	 */
	private List<Long> getTopLevelPathwayDbIds(MySQLAdaptor dbAdaptor, Long dbId) throws Exception {
		List<Long> topLevelPathwayDbIds = new ArrayList<Long>();
		GKInstance instance = dbAdaptor.fetchInstance(dbId);
 		if (instance == null) {
 			System.err.println("PathwayReactionHashHandler.getPathToTopLevel: WARNING - instance == null for dbId=" + dbId);
 			return topLevelPathwayDbIds;
 		}
		Collection<GKInstance> eventCollection = new ArrayList<GKInstance>();
		if (instance.getSchemClass().isa("Event"))
			eventCollection.add(instance);
		else
			eventCollection.addAll(getEventsFromPhysicalEntity(instance));
		
     	for (GKInstance event: eventCollection) {
     		if (event == null) {
     			System.err.println("PathwayReactionHashHandler.getPathToTopLevel: WARNING - event == null, ignoring");
     			continue;
     		}
 			
     		collectEventPathIdentifiers(event, topLevelPathwayDbIds);
     	}
     	
     	return topLevelPathwayDbIds;
	}
	
	private Set<GKInstance> getEventsFromPhysicalEntity(GKInstance instance) throws Exception{
		Set<GKInstance> events = new HashSet<GKInstance>();
		if (!(instance.getSchemClass().isa("PhysicalEntity"))) {
			System.err.println("PathwayReactionHashHandler.getEventsFromInstance: WARNING - instance with DB_ID=" + instance.getDBID() + " is not a PhysicalEntity, ignoring");
			return events;
		}

		List instructions = new ArrayList();
		instructions.add(new ClassAttributeFollowingInstruction("PhysicalEntity", null, new String[]{"physicalEntity", "input", "output"}));
		instructions.add(new ClassAttributeFollowingInstruction("CatalystActivity", null, new String[]{"catalystActivity"}));
		String[] classList = {"ReactionlikeEvent"};
		Set<GKInstance> entityEvents = InstanceUtilities.followInstanceAttributes(instance, instructions, classList);
		if (entityEvents != null)
			events.addAll(entityEvents);

		instructions = new ArrayList();
		instructions.add(new ClassAttributeFollowingInstruction("PhysicalEntity", null, new String[]{"regulator"}));
		instructions.add(new ClassAttributeFollowingInstruction("Regulation", new String[]{"regulatedEntity"}, null));
		Set<GKInstance> regulatedEvents = InstanceUtilities.followInstanceAttributes(instance, instructions, classList);
		if (regulatedEvents != null)
			events.addAll(regulatedEvents);

		return events;
	}
	
	/*
	 * Given an event, return a list of dbids going back to the top level.
	 * Used to find out which tree node to highlight, for example.
	 * 
	 * @param eventId Identifier of event in hierarchy to start back from.
	 * @param path Accumulates the path from event to top level as this function is called recursively.
	 * @param pathMap The map to collect paths in. Key is top level pathway id.
	 * path
	 */
	private void collectEventPathIdentifiers(GKInstance event, List<Long> topLevelPathwayDbIds) throws Exception {
		collectEventPathIdentifiers(event, topLevelPathwayDbIds, 0);
	}
	
	/*
	 * Given an event, return a list of dbids going back to the top level.
	 * Used to find out which tree node to highlight, for example.
	 * 
	 * @param eventId Identifier of event in hierarchy to start back from.
	 * @param path Accumulates the path from event to top level as this function is called recursively.
	 * @param pathMap The map to collect paths in. Key is top level pathway id.
	 * path
	 */
	private void collectEventPathIdentifiers(GKInstance event, List<Long> topLevelPathwayDbIds, int recursionDepth) throws Exception{
		if (recursionDepth > 100) {
			System.err.println("PathwayReactionHashHandler.collectEventPathIdentifiersDavid: WARNING - hit hard recursion depth limit, aborting!");
			return;
		}
		
		// Recursion termination
		if(event.getSchemClass().isa("Pathway") && hasDiagram(event)){
			topLevelPathwayDbIds.add(event.getDBID());
			return;
		}
		
		Collection<GKInstance> referers = event.getReferers("hasEvent");
		List<GKInstance> parentEvents = new ArrayList<GKInstance>();
		if (referers != null)
			for (GKInstance referer: referers)
				if (referer.getSchemClass().isa("Pathway"))
					parentEvents.add(referer);
		
		for (GKInstance parentEvent: parentEvents) {
			collectEventPathIdentifiers(parentEvent, topLevelPathwayDbIds, recursionDepth + 1);
		}
	}
	
	private boolean hasDiagram(GKInstance pathway) {
		try {
			Collection<GKInstance> pathwayDiagrams = pathway.getReferers("representedPathway");
			if (pathwayDiagrams == null)
				return false;
			if (pathwayDiagrams.size() < 1)
				return false;
		} catch (Exception e) {
			System.err.println("Utils.hasDiagram: WARNING - problem getting referrers to pathway");
			e.printStackTrace(System.err);
			return false;
		}
		
		return true;
	}
}
