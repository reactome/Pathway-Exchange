/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

/**
 * Instance cache for reaction concatenator.  This supplements the MySQLAdaptor, which
 * is not able to cache all of the things that we need, such as referrers.
 * 
 * @author David Croft
 *
 */
public class ReactionConcatenatorCache {
	private Map<GKInstance,GKInstance> reactionToVertexHash = new HashMap<GKInstance,GKInstance>();
	private Map<GKInstance,Collection> reactionVertexToTargetEdgeHash = new HashMap<GKInstance,Collection>();
	private Map<GKInstance,Collection> reactionVertexToSourceEdgeHash = new HashMap<GKInstance,Collection>();
	
	public void putReactionVertexForReactionlikeEvent(GKInstance reactionlikeEvent, GKInstance reactionVertex) {
		reactionToVertexHash.put(reactionlikeEvent, reactionVertex);
	}

	/**
	 * Find all ReactionVertex instances in the pathway diagrams that
	 * reference the given reaction.
	 * 
	 * @param reactionlikeEvent
	 * @return
	 */
	public Collection<GKInstance> getReactionVertexesFromReactionlikeEvent(GKInstance reactionlikeEvent) {
		Collection<GKInstance> reactionVerteces = null;
		try {
			reactionVerteces = reactionlikeEvent.getReferers("representedInstance");
		} catch (Exception e) {
			System.err.println("ReactionConcatenatorCache.getReactionVertexesFromReactionlikeEvent: WARNING - problem with dba");
			e.printStackTrace(System.err);
		}
		if (reactionVerteces == null || reactionVerteces.size() == 0) {
			GKInstance reactionVertex = reactionToVertexHash.get(reactionlikeEvent);
			if (reactionVertex == null) {
				System.err.println("ReactionConcatenatorCache.getReactionVertexesFromReactionlikeEvent: WARNING - no ReactionVertex instances found for reactionlikeEvent " + reactionlikeEvent.getDBID());
				return null;
			} else {
				reactionVerteces = new ArrayList<GKInstance>();
				reactionVerteces.add(reactionVertex);
			}
		}
		
		return reactionVerteces;
	}

	public void putTargetEdgesForReactionVertex(GKInstance reactionVertex, Collection targetEdges) {
		reactionVertexToTargetEdgeHash.put(reactionVertex, targetEdges);
	}

	/**
	 * Find all Edge instances in the pathway diagrams that
	 * reference the given reaction vertex.
	 * 
	 * @param reactionlikeEvent
	 * @return
	 */
	public Collection<GKInstance> getTargetEdgesFromReactionVertex(GKInstance reactionVertex) {
		Collection<GKInstance> targetEdges = null;
		try {
			targetEdges = reactionVertex.getReferers("sourceVertex");
		} catch (Exception e) {
			System.err.println("ReactionConcatenatorCache.getTargetEdgesFromReactionVertex: WARNING - problem with dba");
			e.printStackTrace(System.err);
		}
		if (targetEdges == null || targetEdges.size() == 0) {
			targetEdges = reactionVertexToTargetEdgeHash.get(reactionVertex);
			if (targetEdges == null) {
				System.err.println("ReactionConcatenatorCache.getTargetEdgesFromReactionVertex: WARNING - no ReactionVertex instances found for reactionlikeEvent " + reactionVertex.getDBID());
				return null;
			}
		}
		
		return targetEdges;
	}

	public void putSourceEdgesForReactionVertex(GKInstance reactionVertex, Collection sourceEdges) {
		reactionVertexToSourceEdgeHash.put(reactionVertex, sourceEdges);
	}

	/**
	 * Find all Edge instances in the pathway diagrams that
	 * reference the given reaction vertex.
	 * 
	 * @param reactionlikeEvent
	 * @return
	 */
	public Collection<GKInstance> getSourceEdgesFromReactionVertex(GKInstance reactionVertex) {
		Collection<GKInstance> sourceEdges = null;
		try {
			sourceEdges = reactionVertex.getReferers("targetVertex");
		} catch (Exception e) {
			System.err.println("ReactionConcatenatorCache.getSourceEdgesFromReactionVertex: WARNING - problem with dba");
			e.printStackTrace(System.err);
		}
		if (sourceEdges == null || sourceEdges.size() == 0) {
			sourceEdges = reactionVertexToSourceEdgeHash.get(reactionVertex);
			if (sourceEdges == null) {
				System.err.println("ReactionConcatenatorCache.getSourceEdgesFromReactionVertex: WARNING - no ReactionVertex instances found for reactionlikeEvent " + reactionVertex.getDBID());
				return null;
			}
		}
		
		return sourceEdges;
	}
}