/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Model layout info.
 * 
 * @author David Croft
 *
 */
public class Diagram extends Glyph {
	private double width;
	private double height;
	private Map<String,CompartmentVertex> compartmentVertexHash = new HashMap<String,CompartmentVertex>();
	private Map<String,ReactionVertex> reactionVertexHash = new HashMap<String,ReactionVertex>();
	private Map<String,EntityVertex> entityVertexHash = new HashMap<String,EntityVertex>();
	private Map<String,Edge> edgeHash = new HashMap<String,Edge>();

	public Diagram() {
		super();
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	private String generateCompartmentVertexId(String id) {
		String internalId = id;
		if (id.matches("^[0-9].*$"))
			internalId = CompartmentVertex.getGlyphType() + "_" + id;
		return internalId;
	}

	public CompartmentVertex getCompartmentVertex(String id) {
		String internalId = generateCompartmentVertexId(id);
		CompartmentVertex vertex = compartmentVertexHash.get(internalId);
		return vertex;
	}

	public CompartmentVertex createCompartmentVertex(String id) {
		String internalId = generateCompartmentVertexId(id);
		CompartmentVertex vertex = new CompartmentVertex();
		vertex.setId(internalId);
		compartmentVertexHash.put(internalId, vertex);
		return vertex;
	}

	public List<CompartmentVertex> getCompartmentVertexes() {
		// Sort by size, so that the big ones get printed first.
		Collection<CompartmentVertex> compartmentVertexes = compartmentVertexHash.values();
		List<CompartmentVertex> sortedCompartmentVertexes =  new ArrayList<CompartmentVertex>(compartmentVertexes);
		Collections.sort(sortedCompartmentVertexes, new VertexAreaComparator());
		return sortedCompartmentVertexes;
	}

	private String generateReactionVertexId(String id) {
		String internalId = id;
		if (id.matches("^[0-9].*$"))
			internalId = ReactionVertex.getGlyphType() + "_" + id;
		return internalId;
	}

	public ReactionVertex getReactionVertex(String id) {
		String internalId = generateReactionVertexId(id);
		ReactionVertex vertex = reactionVertexHash.get(internalId);
		return vertex;
	}

	public ReactionVertex createReactionVertex(String id) {
		String internalId = generateReactionVertexId(id);
		ReactionVertex vertex = new ReactionVertex();
		vertex.setId(internalId);
		reactionVertexHash.put(internalId, vertex);
		return vertex;
	}

	public List<ReactionVertex> getReactionVertexes() {
		// Sort by DB_ID, so that the results are reproducuble.
		Set<String> ids = reactionVertexHash.keySet();
		List<String> sortedIds =  new ArrayList<String>(ids);
		Collections.sort(sortedIds);
		List<ReactionVertex> reactionVertexes = new ArrayList<ReactionVertex>();
		for (String id: sortedIds)
			reactionVertexes.add(reactionVertexHash.get(id));
		return reactionVertexes;
	}
	
	private String generateEntityVertexId(String id) {
		String internalId = id;
		if (id.matches("^[0-9].*$"))
			internalId = EntityVertex.getGlyphType() + "_" + id;
		return internalId;
	}

	public EntityVertex getEntityVertex(String id) {
		String internalId = generateEntityVertexId(id);
		EntityVertex entityVertex = entityVertexHash.get(internalId);
		
		return entityVertex;
	}

	public EntityVertex createEntityVertex(String id) {
		String internalId = generateEntityVertexId(id);
		EntityVertex entityVertex = new EntityVertex();
		entityVertex.setId(internalId);
		entityVertexHash.put(internalId, entityVertex);
		
		return entityVertex;
	}

	public List<EntityVertex> getEntityVertexes() {
		// Sort by DB_ID, so that the results are reproducuble.
		Set<String> ids = entityVertexHash.keySet();
		List<String> sortedIds =  new ArrayList<String>(ids);
		Collections.sort(sortedIds);
		List<EntityVertex> entityVertexes = new ArrayList<EntityVertex>();
		for (String id: sortedIds)
			entityVertexes.add(entityVertexHash.get(id));
		return entityVertexes;
	}

	private String generateEdgeId(String id) {
		String internalId = id;
		if (id.matches("^[0-9].*$"))
			internalId = Edge.getGlyphType() + "_" + id;
		return internalId;
	}

	public Edge getEdge(String id) {
		String internalId = generateEdgeId(id);
		Edge edge = edgeHash.get(internalId);
		return edge;
	}

	public Edge createEdge(String id) {
		String internalId = generateEdgeId(id);
		Edge edge = new Edge();
		edge.setId(internalId);
		edgeHash.put(internalId, edge);
		return edge;
	}

	public List<Edge> getEdges() {
		// Sort by DB_ID, so that the results are reproducuble.
		Set<String> ids = edgeHash.keySet();
		List<String> sortedIds =  new ArrayList<String>(ids);
		Collections.sort(sortedIds);
		List<Edge> edges = new ArrayList<Edge>();
		for (String id: sortedIds)
			edges.add(edgeHash.get(id));
		return edges;
	}
	
    class VertexAreaComparator implements Comparator<Vertex> {
        @Override
        public int compare(Vertex vertex1, Vertex vertex2) {
                double area1 = vertex1.getWidth() * vertex1.getHeight();
                double area2 = vertex2.getWidth() * vertex2.getHeight();
                if (area1 < area2)
                        return (-1);
                else if (area1 > area2)
                        return 1;

                return 0;
        }
}
}
