/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.convert.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.MySQLAdaptor;
import org.gk.sbml.NotesBuilder;
import org.gk.sbml.ReactionConcatenatorCache;

/**
 * Concatenates reactions, so that linear chans of reactions without any
 * branches become single reactions.  Uses the connectivity information in
 * pathway diagrams to find out which reactions are connected to each other.
 * 
 * @author David Croft
 *
 */
public class ReactionConcatenator {
	private MySQLAdaptor dbAdaptor;
	private long maxDbId = (-1);

	public ReactionConcatenator(MySQLAdaptor dbAdaptor) {
		this.dbAdaptor = dbAdaptor;
		maxDbId = dbAdaptor.fetchMaxDbId();
	}

	public Map<Long,List<Long>> concatenatePathwayReactionHash(Map<Long,List<Long>> pathwayReactionHash) {
		Set<Long> pathwayDbIDs = pathwayReactionHash.keySet();
		Map<Long,List<Long>> newPathwayReactionHash = new HashMap<Long,List<Long>>();
		try {
			for (Long pathwayDbID: pathwayDbIDs) {
				if (pathwayDbID == null) {
					System.err.println("ReactionConcatenator.concatenatePathwayReactionHash: WARNING - pathwayDbID is null, skipping!");
					continue;
				}
				GKInstance pathway = dbAdaptor.fetchInstance(pathwayDbID);
				if (pathway == null) {
					System.err.println("ReactionConcatenator.concatenatePathwayReactionHash: WARNING - no pathway found for DB_ID=" + pathwayDbID);
					continue;
				}
	
				ReactionConcatenatorCache cache = new ReactionConcatenatorCache();
				List<Long> reactionDbIds = pathwayReactionHash.get(pathwayDbID);
				List<Long> contatenatedReactionDbIds = concatenateReactions(pathway, cache, reactionDbIds);
				if (contatenatedReactionDbIds.size() > 0)
					newPathwayReactionHash.put(pathwayDbID, contatenatedReactionDbIds);
			}
		} catch (Exception e) {
			System.err.println("ReactionConcatenator.concatenateReactions: WARNING - Problem with DBAdaptor");
			e.printStackTrace(System.err);
		}
		
		return newPathwayReactionHash;
	}

	/**
	 * This is a recursive procedure that takes a list of ReactionlikeEvents and returns a modified
	 * list of ReactionlikeEvents, in which unbranching chains of reactions have been replaced with
	 * single, composite reactions.  If there are no unbranching chains, then the list returned
	 * will contain exactly the same reactions as the list you submitted.  If there are unbranching
	 * chains, the returned list will be shorter than the submitted list.  It will also contain
	 * newly created reactions, which are composites of some of the reactions in the originally
	 * submitted list.
	 * 
	 * @param pathway limit all computations to within the scope of this pathway
	 * @param reactionlikeEvents submitted reaction list
	 * @return
	 */
	private List<Long> concatenateReactions(GKInstance pathway, ReactionConcatenatorCache cache, List<Long> reactionDbIds) {
		List<GKInstance> reactionlikeEvents = getInstancesFromDbIds(reactionDbIds);
		List<GKInstance> contatenatedReactions = concatenateReactions(0, pathway, cache, reactionlikeEvents);
		List<GKInstance> newContatenatedReactions = concatenateReactions(0, pathway, cache, contatenatedReactions);
		while (newContatenatedReactions.size() < contatenatedReactions.size()) {
			contatenatedReactions = newContatenatedReactions;
			newContatenatedReactions = concatenateReactions(0, pathway, cache, contatenatedReactions);
		}
		return getDbIdsFromInstances(newContatenatedReactions);
	}
	
	/**
	 * This is a recursive procedure that takes a list of ReactionlikeEvents and returns a modified
	 * list of ReactionlikeEvents, in which unbranching chains of reactions have been replaced with
	 * single, composite reactions.  If there are no unbranching chains, then the list returned
	 * will contain exactly the same reactions as the list you submitted.  If there are unbranching
	 * chains, the returned list will be shorter than the submitted list.  It will also contain
	 * newly created reactions, which are composites of some of the reactions in the originally
	 * submitted list.
	 * 
	 * @param reactionNum the position of the reaction to be processed in the submitted reaction list
	 * @param pathway limit all computations to within the scope of this pathway
	 * @param reactionlikeEvents submitted reaction list
	 * @return
	 */
	private List<GKInstance> concatenateReactions(int reactionNum, GKInstance pathway, ReactionConcatenatorCache cache, List<GKInstance> reactionlikeEvents) {
		// Recursion termination condition
		if (reactionlikeEvents.size() == reactionNum)
			return reactionlikeEvents;
		if (reactionlikeEvents.size() < reactionNum) {
			System.err.println("ReactionConcatenator.concatenateReactions: WARNING - reactions.size() (" + reactionlikeEvents.size() + ") is less than reactionNum (" + reactionNum + ") for pathway " + pathway.getDBID() + " !!");
			return reactionlikeEvents;
		}
		
		GKInstance reactionlikeEvent = reactionlikeEvents.get(reactionNum);
		int newReactionNum = reactionNum + 1;
		List<GKInstance> newReactionlikeEvents = new ArrayList<GKInstance>();
		for (GKInstance newReactionlikeEvent: reactionlikeEvents)
			newReactionlikeEvents.add(newReactionlikeEvent);
		
		// Look to see if this reaction is the start of an unbranching chain
		// of at least one reaction long, and if so, combine it with the 
		// subsequent reaction in the chain.
		GKInstance nextReactionlikeEvent = getNextReactionIfOnlyOne(pathway, cache, reactionlikeEvent);
		if (nextReactionlikeEvent != null && reactionlikeEvents.contains(nextReactionlikeEvent)) {
			// Somewhat convoluted logic here, but if the next reaction
			// has multiple reactions feeding into it (one of which will
			// be reactionlikeEvent), then reactionlikeEvent is not a
			// suitable candidate for a chain starting point.
			List<GKInstance> previousReactionlikeEvents = getPreviousReactions(pathway, cache, nextReactionlikeEvent);
			if (previousReactionlikeEvents.size() < 2) {
				// TODO: we need to put cycle detection here, to avoid infinite loops.
				System.err.println("ReactionConcatenator.concatenateReactions: reaction DB_ID=" + reactionlikeEvent.getDBID() + ", name=" + reactionlikeEvent.getDisplayName());
				System.err.println("ReactionConcatenator.concatenateReactions: next reaction DB_ID=" + nextReactionlikeEvent.getDBID() + ", name=" + nextReactionlikeEvent.getDisplayName());
				GKInstance compositeReactionlikeEvent = combineReactionsWithinPathway(pathway, cache, reactionlikeEvent, nextReactionlikeEvent);
				System.err.println("ReactionConcatenator.concatenateReactions: composite reaction DB_ID=" + compositeReactionlikeEvent.getDBID() + ", name=" + compositeReactionlikeEvent.getDisplayName());
	
				if (newReactionlikeEvents.indexOf(nextReactionlikeEvent) < reactionNum)
					newReactionNum--;
	
				newReactionlikeEvents.remove(reactionlikeEvent);
				newReactionlikeEvents.remove(nextReactionlikeEvent);
				newReactionlikeEvents.add(compositeReactionlikeEvent);
			}
		}

		return concatenateReactions(newReactionNum, pathway, cache, newReactionlikeEvents);
	}
	
	/**
	 * Convert a list of DB_IDs into a list of instances.
	 * 
	 * @param dbIds
	 * @return
	 */
	private List<GKInstance> getInstancesFromDbIds(List<Long> dbIds) {
		List<GKInstance> instances = new ArrayList<GKInstance>();
		try {
			for (Long dbId: dbIds) {
				GKInstance instance = dbAdaptor.fetchInstance(dbId);
				if (instance == null) {
					System.err.println("ReactionConcatenator.getInstancesFromDbIds: WARNING - no instance found for DB_ID=" + dbId);
					continue;
				}
				
				instances.add(instance);
			}
		} catch (Exception e) {
			System.err.println("ReactionConcatenator.getInstancesFromDbIds: WARNING - Problem with DBAdaptor");
			e.printStackTrace(System.err);
		}

		return instances;
	}
	
	/**
	 * Convert a list of instances into a list of DB_IDs.
	 * 
	 * @param instances
	 * @return
	 */
	private List<Long> getDbIdsFromInstances(List<GKInstance> instances) {
		List<Long> dbIds = new ArrayList<Long>();
		try {
			for (GKInstance instance: instances) {
				Long dbId = instance.getDBID().longValue();
				dbIds.add(dbId);
			}
		} catch (Exception e) {
			System.err.println("ReactionConcatenator.getDbIdsFromInstances: WARNING - Problem with DBAdaptor");
			e.printStackTrace(System.err);
		}

		return dbIds;
	}
	
	/**
	 * Takes the given reactions, removes them from the given pathway, and
	 * substitutes a single reaction that is a composite of the two original reactions.
	 * 
	 * @param pathway
	 * @param reactionlikEvent1
	 * @param reactionlikEvent2
	 */
	private GKInstance combineReactionsWithinPathway(GKInstance pathway, ReactionConcatenatorCache cache, GKInstance reactionlikEvent1, GKInstance reactionlikEvent2) {
		GKInstance compositeReaction = createCompositeReaction(pathway, cache, reactionlikEvent1, reactionlikEvent2);
		if (compositeReaction == null)
			return null;
		
		try {
			GKInstance subpathway1 = findSubpathwayContainingReaction(pathway, reactionlikEvent1);
			GKInstance subpathway2 = findSubpathwayContainingReaction(pathway, reactionlikEvent2);
			if (subpathway1 == null)
				System.err.println("ReactionConcatenator.combineReactionsWithinPathway: WARNING - subpathway1 == null!!");
			else {
				subpathway1.removeAttributeValueNoCheck("hasEvent", reactionlikEvent1);
				subpathway1.addAttributeValue("hasEvent", compositeReaction); // arbitrarily put this in subpathway1
			}
			if (subpathway2 == null)
				System.err.println("ReactionConcatenator.combineReactionsWithinPathway: WARNING - subpathway2 == null!!");
			else {
				subpathway2.removeAttributeValueNoCheck("hasEvent", reactionlikEvent2);
			}
		} catch (Exception e) {
			System.err.println("ReactionConcatenator.combineReactionsWithinPathway: WARNING - Problem with DBAdaptor");
			e.printStackTrace(System.err);
		}
		
		return compositeReaction;
	}

	/**
	 * Descends the pathway hierarchy, starting with the given pathway as the root, and
	 * looks for the given reaction.  Returns the subpathway within which the reaction
	 * was found.  Returns null if no reaction was found.  This is a recursive method,
	 * it won't like it if you have loops in the pathway hierarchy.
	 * 
	 * @param pathway
	 * @param reactionlikeEvent
	 * @return
	 */
	private GKInstance findSubpathwayContainingReaction(GKInstance pathway, GKInstance reactionlikeEvent) {
		GKInstance foundSubpathway = null;
		try {
			List<GKInstance> events = pathway.getAttributeValuesList("hasEvent");
			for (GKInstance event: events) {
				if (event.equals(reactionlikeEvent)) {
					foundSubpathway = pathway;
					break;
				}
			}
			if (foundSubpathway == null) {
				for (GKInstance event: events) {
					if (event.getSchemClass().isa("Pathway")) {
						foundSubpathway = findSubpathwayContainingReaction(event, reactionlikeEvent);
						if (foundSubpathway != null)
							break;
					}
				}
			}
		} catch (Exception e) {
			System.err.println("ReactionConcatenator.findSubpathwayContainingReaction: WARNING - Problem with DBAdaptor");
			e.printStackTrace(System.err);
		}
		
		return foundSubpathway;
	}
	/**
	 * Create a new reaction from the two supplied reactions, by using the inputs of
	 * reactionlikeEvent1, the outputs of reactionlikeEvent2 and the catalysts of both of
	 * them.
	 * 
	 * @param reactionlikeEvent1
	 * @param reactionlikeEvent2
	 */
	private GKInstance createCompositeReaction(GKInstance pathway, ReactionConcatenatorCache cache, GKInstance reactionlikeEvent1, GKInstance reactionlikeEvent2) {
		if (reactionlikeEvent1 == null) {
			System.err.println("ReactionConcatenator.createCompositeReaction: WARNING - reactionlikEvent1 == null");
			return null;
		}
		if (reactionlikeEvent2 == null) {
			System.err.println("ReactionConcatenator.createCompositeReaction: WARNING - reactionlikEvent2 == null");
			return null;
		}
		
		GKInstance compositeReaction = null;
		try {
//			compositeReaction = new GKInstance();
//			compositeReaction.setSchemaClass(dbAdaptor.getSchema().getClassByName("ReactionlikeEvent"));
//			compositeReaction.setDbAdaptor(dbAdaptor);
//			compositeReaction.setDefaultValues();
//			compositeReaction.setIsInflated(true);
//			compositeReaction.setDBID(++maxDbId);
			compositeReaction = (GKInstance) dbAdaptor.getInstance("ReactionlikeEvent", ++maxDbId);
			String reactionlikEvent1DbId = reactionlikeEvent1.getDBID().toString();
			String reactionlikEvent2DbId = reactionlikeEvent2.getDBID().toString();
			String name1 = (String) reactionlikeEvent1.getAttributeValue("name");
			String name2 = (String) reactionlikeEvent2.getAttributeValue("name");
			String combinedName;
			if (name1 != null && name1.matches("^[0-9 +]+$"))
				combinedName = name1;
			else
				combinedName = reactionlikEvent1DbId;
			combinedName += " + ";
			if (name2 != null && name2.matches("^[0-9 +]+$"))
				combinedName += name2;
			else
				combinedName += reactionlikEvent2DbId;
			compositeReaction.setAttributeValue("name", combinedName);
			compositeReaction.setAttributeValue("_displayName", combinedName);
			
			List<GKInstance> inputs = reactionlikeEvent1.getAttributeValuesList("input");
			if (inputs != null)
				for (GKInstance input: inputs)
					compositeReaction.addAttributeValue("input", input);
			List<GKInstance> outputs = reactionlikeEvent1.getAttributeValuesList("output");
			if (outputs != null)
				for (GKInstance output: outputs)
					compositeReaction.addAttributeValue("output", output);
			List<GKInstance> catalystActivitys = reactionlikeEvent1.getAttributeValuesList("catalystActivity");
			if (catalystActivitys != null)
				for (GKInstance catalystActivity: catalystActivitys)
					compositeReaction.addAttributeValue("catalystActivity", catalystActivity);
			
			String notes1 = NotesBuilder.extractNotesFromInstance(reactionlikeEvent1);
			String notes2 = NotesBuilder.extractNotesFromInstance(reactionlikeEvent2);
			String compositeNotes = notes1;
			if (!compositeNotes.isEmpty() && !notes2.isEmpty())
				compositeNotes += "\n";
			compositeNotes += notes2;
			if (!compositeNotes.isEmpty()) {
				GKInstance summation = (GKInstance) dbAdaptor.getInstance("Summation", ++maxDbId);
				summation.setAttributeValue("text", compositeNotes);
				compositeReaction.setAttributeValue("summation", summation);
			}
		} catch (Exception e) {
			System.err.println("ReactionConcatenator.createCompositeReaction: WARNING - Problem with DBAdaptor");
			e.printStackTrace(System.err);
		}
		
		// Merge the corresponding ReactionVertex as well, so that layout looks sensible.
		GKInstance reactionVertex1 = getReactionVertexFromReaction(pathway, cache, reactionlikeEvent1);
		GKInstance reactionVertex2 = getReactionVertexFromReaction(pathway, cache, reactionlikeEvent2);
		if (reactionVertex1 == null)
			System.err.println("ReactionConcatenator.createCompositeReaction: WARNING - reactionlikeEvent1 (" + reactionlikeEvent1.getDBID() + ", " + reactionlikeEvent1.getDisplayName() + ") has no corresponding vertex");
		else if (reactionVertex2 == null)
			System.err.println("ReactionConcatenator.createCompositeReaction: WARNING - reactionlikeEvent2 (" + reactionlikeEvent2.getDBID() + ", " + reactionlikeEvent2.getDisplayName() + ") has no corresponding vertex");
		else
			createCompositeReactionVertex(cache, compositeReaction, reactionVertex1, reactionVertex2);
		
		return compositeReaction;
	}

	/**
	 * Takes 2 ReactionVertex and combines them.  Returns the combined vertex.  Uses the input
	 * edges from reactionVertex1 and the output edges from reactionVertex2;
	 * 
	 * @param reactionVertex1
	 * @param reactionVertex2
	 * @return
	 */
	private GKInstance createCompositeReactionVertex(ReactionConcatenatorCache cache, GKInstance compositeReaction, GKInstance reactionVertex1, GKInstance reactionVertex2) {
		GKInstance compositeReactionVertex = null;
		
		try {
//			compositeReactionVertex = new GKInstance();
//			compositeReactionVertex.setSchemaClass(dbAdaptor.getSchema().getClassByName("ReactionVertex"));
//			compositeReactionVertex.setDbAdaptor(dbAdaptor);
//			compositeReactionVertex.setDefaultValues();
//			compositeReactionVertex.setIsInflated(true); 
//			compositeReactionVertex.setDBID(++maxDbId);
			compositeReactionVertex = (GKInstance) dbAdaptor.getInstance("ReactionVertex", ++maxDbId);
			String reactionVertex1DbId = reactionVertex1.getDBID().toString();
			String reactionVertex2DbId = reactionVertex2.getDBID().toString();
			String compositeReactionVertexName = reactionVertex1DbId + " + " + reactionVertex2DbId;
			compositeReactionVertex.setAttributeValue("_displayName", compositeReactionVertexName);
			compositeReactionVertex.setAttributeValue("pathwayDiagram", (GKInstance)reactionVertex1.getAttributeValue("pathwayDiagram"));
			compositeReactionVertex.setAttributeValue("representedInstance", compositeReaction);
			cache.putReactionVertexForReactionlikeEvent(compositeReaction, compositeReactionVertex);
			
			// Use the coordinates of the first reaction vertex
			String pointCoordinates1 = (String) reactionVertex1.getAttributeValue("pointCoordinates");
			Integer x1 = (Integer) reactionVertex1.getAttributeValue("x");
			Integer y1 = (Integer) reactionVertex1.getAttributeValue("y");
			compositeReactionVertex.setAttributeValue("pointCoordinates", pointCoordinates1);
			compositeReactionVertex.setAttributeValue("x", x1);
			compositeReactionVertex.setAttributeValue("y", y1);
			
			Collection<GKInstance> sourceEdges = cache.getSourceEdgesFromReactionVertex(reactionVertex1);
			if (sourceEdges != null) {
				for (GKInstance sourceEdge: sourceEdges) {
					sourceEdge.removeAttributeValueNoCheck("targetVertex", reactionVertex1);
					sourceEdge.addAttributeValue("targetVertex", compositeReactionVertex);
				}
				cache.putSourceEdgesForReactionVertex(compositeReactionVertex, sourceEdges);
			}
			
			Collection<GKInstance> targetEdges = cache.getTargetEdgesFromReactionVertex(reactionVertex2);
			if (targetEdges != null) {
				for (GKInstance targetEdge: targetEdges) {
					targetEdge.removeAttributeValueNoCheck("sourceVertex", reactionVertex2);
					targetEdge.addAttributeValue("sourceVertex", compositeReactionVertex);
				}
				cache.putTargetEdgesForReactionVertex(compositeReactionVertex, targetEdges);
			}
		} catch (Exception e) {
			System.err.println("ReactionConcatenator.createCompositeReactionVertex: WARNING - Problem with DBAdaptor");
			e.printStackTrace(System.err);
		}
		
		return compositeReactionVertex;
	}
	
	/**
	 * For a given ReactionlikeEvent, in a given Pathway, find out what the preceding reactions
	 * are, and return them as a list.
	 * 
	 * @param pathway
	 * @param reactionlikEvent
	 * @return
	 */
	private List<GKInstance> getPreviousReactions(GKInstance pathway, ReactionConcatenatorCache cache, GKInstance reactionlikEvent) {
		List<GKInstance> previousReactions = new ArrayList<GKInstance>();
		try {
			GKInstance foundReactionVertex = getReactionVertexFromReaction(pathway, cache, reactionlikEvent);
//			Collection<GKInstance> reactionVerteces = cache.getReactionVertexesFromReactionlikeEvent(reactionlikEvent);
//			if (reactionVerteces == null || reactionVerteces.size() == 0)
//				return new ArrayList<GKInstance>();
//			GKInstance foundReactionVertex = reactionVerteces.iterator().next();;

			// Find all of the edges leading into the ReactionVertex.  Things
			// are very easy in the case where there are no incoming edges.
			Collection<GKInstance> sourceEdges = cache.getSourceEdgesFromReactionVertex(foundReactionVertex);
			if (sourceEdges == null || sourceEdges.size() == 0)
				return new ArrayList<GKInstance>();
			
			// For each incoming edge, look for the reactions that they
			// ultimately lead to.
			for (GKInstance sourceEdge: sourceEdges) {
				GKInstance entityVertex = (GKInstance)sourceEdge.getAttributeValue("sourceVertex");
				
				// Examine the type of the vertex that the edge ends
				// in.  Only look at those that end in entities.
				if (!entityVertex.getSchemClass().isa("EntityVertex"))
					continue;
				
				// Find all of the edges leading into the EntityVertex.
				Collection<GKInstance> entitySourceEdges = entityVertex.getReferers("targetVertex");
				if (entitySourceEdges == null)
					continue;
				
				// Loop over the incoming edges (there will probably only be one).
				for (GKInstance entitySourceEdge: entitySourceEdges) {
					// Get the vertex that the edge starts with.  We are only interested
					// in it if this is a reaction.
					GKInstance sourceReactionVertex = (GKInstance)entitySourceEdge.getAttributeValue("sourceVertex");
					if (sourceReactionVertex == null || !sourceReactionVertex.getSchemClass().isa("ReactionVertex"))
						continue;
					
					GKInstance previousReaction = (GKInstance)sourceReactionVertex.getAttributeValue("representedInstance");
					if (previousReaction != null && !previousReactions.contains(previousReaction))
						previousReactions.add(previousReaction);
				}
			}
		} catch (Exception e) {
			System.err.println("ReactionConcatenator.getPreviousReactionIfOnlyOne: WARNING - Problem with DBAdaptor");
			e.printStackTrace(System.err);
		}
		
		return previousReactions;
	}
	
	/**
	 * For a given ReactionlikeEvent, in a given Pathway, find out what the following reaction
	 * is, if there is only one of them.  If the pathway either terminates or branches at
	 * the given ReactionlikeEvent, return null, otherwise return the following ReactionlikeEvent.
	 * 
	 * @param pathway
	 * @param reactionlikEvent
	 * @return
	 */
	private GKInstance getNextReactionIfOnlyOne(GKInstance pathway, ReactionConcatenatorCache cache, GKInstance reactionlikEvent) {
		GKInstance nextReaction = null;
		
		try {
			GKInstance foundReactionVertex = getReactionVertexFromReaction(pathway, cache, reactionlikEvent);
			if (foundReactionVertex == null)
				return null;

			// Find all of the edges leading out of the ReactionVertex
			Collection<GKInstance> targetEdges = cache.getTargetEdgesFromReactionVertex(foundReactionVertex);
			if (targetEdges == null || targetEdges.size() == 0) {
				System.err.println("ReactionConcatenator.getNextReactionIfOnlyOne: WARNING - no target edges for reaction DB_ID=" + reactionlikEvent.getDBID());
				return null;
			}
			
			// For each outgoing edge, look for the reactions that they
			// ultimately lead to.  If there is only one single reaction, then
			// we are onto a good thing.
			GKInstance nextReactionVertex = null;
			for (GKInstance targetEdge: targetEdges) {
				GKInstance entityVertex = (GKInstance)targetEdge.getAttributeValue("targetVertex");
				
				// Examine the type of the vertex that the edge ends
				// in.  Only look at those that end in entities.
				if (!entityVertex.getSchemClass().isa("EntityVertex"))
					continue;
				
				// Find all of the edges leading out of the EntityVertex.
				Collection<GKInstance> entityTargetEdges = entityVertex.getReferers("sourceVertex");
				if (entityTargetEdges == null || entityTargetEdges.size() != 1)
					continue;
				
				// If there is more than one outgoing edge, make the assumption that this
				// reaction connects to multiple other reactions (this might
				// be wrong).
				if (entityTargetEdges == null || entityTargetEdges.size() != 1)
					return null;
				
				// Get the one and only outgoing edge
				GKInstance entityTargetEdge = entityTargetEdges.iterator().next();
				
				// Get the vertex that the edge terminates in.  We are only interested
				// in it if this is a reaction.
				GKInstance targetReactionVertex = (GKInstance)entityTargetEdge.getAttributeValue("targetVertex");
				if (targetReactionVertex == null || !targetReactionVertex.getSchemClass().isa("ReactionVertex"))
					continue;
				
				if (nextReactionVertex == null)
					// This is the first ReactionVertex we have found
					nextReactionVertex = targetReactionVertex;
				else if (targetReactionVertex != nextReactionVertex)
					// There is more than one different ReactionVertex
					return null;
			}
			
			if (nextReactionVertex != null)
				nextReaction = (GKInstance)nextReactionVertex.getAttributeValue("representedInstance");
		} catch (Exception e) {
			System.err.println("ReactionConcatenator.getNextReactionIfOnlyOne: WARNING - Problem with DBAdaptor");
			e.printStackTrace(System.err);
		}
		
		return nextReaction;
	}
	
	/**
	 * Get the ReactionVertex instance corresponding to the given ReactionlikeEvent.  A couple
	 * of restrictions are imposed on possible candidates:
	 * 
	 * 1. The vertex must lie within the diagram for the supplied pathway;
	 * 2. There may only be one single vertex representing that reaction within the pathway.
	 * 
	 * @param pathway
	 * @param reactionlikeEvent
	 * @return
	 */
	private GKInstance getReactionVertexFromReaction(GKInstance pathway, ReactionConcatenatorCache cache, GKInstance reactionlikeEvent) {
		GKInstance foundReactionVertex = null;
		
		try {
			Collection<GKInstance> reactionVerteces = cache.getReactionVertexesFromReactionlikeEvent(reactionlikeEvent);
			
			// To make things easy on ourselves, only deal with the case where
			// the given reaction has exactly one vertex in the diagram for
			// the given pathway.
			for (GKInstance reactionVertex: reactionVerteces) {
				if (isReactionVertexInPathway(reactionVertex,  pathway)) {
					if (foundReactionVertex == null)
						// The first ReactionVertex, keep a note of it.
						foundReactionVertex = reactionVertex;
					else if (foundReactionVertex != reactionVertex)
						// Two different ReactionVertex represent the same ReactionlikEvent
						return null;
				}
			}
		} catch (Exception e) {
			System.err.println("ReactionConcatenator.getReactionVertexFromReaction: WARNING - Problem with DBAdaptor");
			e.printStackTrace(System.err);
		}
		
		return foundReactionVertex;
	}
		
	/**
	 * Given a ReactionVertex instance, looks to see if the PathwayDiagram that
	 * it belongs to is a representation of the given pathway.
	 * 
	 * @param reactionVertex
	 * @param pathway
	 * @return
	 */
	private boolean isReactionVertexInPathway(GKInstance reactionVertex, GKInstance pathway) {
		try {
			GKInstance pathwayDiagram = (GKInstance)reactionVertex.getAttributeValue("pathwayDiagram");
			GKInstance representedPathway = (GKInstance)pathwayDiagram.getAttributeValue("representedPathway");
			if (representedPathway.getDBID() == pathway.getDBID() || InstanceUtilities.isDescendentOf(pathway, representedPathway))
				return true;
		} catch (Exception e) {
			System.err.println("ReactionConcatenator.isReactionVertexInPathway: WARNING - Problem with DBAdaptor");
			e.printStackTrace(System.err);
		}
		
		return false;
		
	}
}
