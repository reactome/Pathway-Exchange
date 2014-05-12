/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.layout;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.render.HyperEdge;
import org.gk.sbml.Utils;
import org.gk.schema.SchemaClass;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * Extract layout from Reactome into an intermediate format suitable for
 * generating SBML layout extension, SBGN, etc.
 * 
 * @author David Croft
 *
 */
public class Extractor {
	private MySQLAdaptor databaseAdaptor;
	private Diagram diagram;
	private int nullRoleCounter = 0;
	private static final double DEFAULT_REACTION_WIDTH = 12.0;
	private static final double DEFAULT_REACTION_HEIGHT = 12.0;
	private static final double MIN_ENTITY_WIDTH = 30.0;
	private static final double MIN_ENTITY_HEIGHT = 12.0;
	private Map<Long,GKInstance> pathwayDiagramtHash = new HashMap<Long,GKInstance>();
	private Map<Long,Double> pathwayDiagramXOffsetHash = new HashMap<Long,Double>();

	public Extractor() {
		super();
	}

	public void setDatabaseAdaptor(MySQLAdaptor databaseAdaptor) {
		this.databaseAdaptor = databaseAdaptor;
	}

	public MySQLAdaptor getDatabaseAdaptor() {
		return this.databaseAdaptor;
	}

	public Diagram getDiagram() {
		return diagram;
	}

	public void buildFromPathwayReactionHash(Map<Long,List<Long>> pathwayReactionHash) {
		diagram = new Diagram();
		try {
			String title = "";
			double totalWidth = 0.0;
			double maxHeight = 0.0;
			
			// Sort pathways by DB_ID, so that the results are reproducible.
			Set<Long> pathwayDbIds = pathwayReactionHash.keySet();
			List<Long> sortedPathwayDbIds =  new ArrayList<Long>(pathwayDbIds);
			Collections.sort(sortedPathwayDbIds);
			
			for (Long pathwayDbId: sortedPathwayDbIds) {
				GKInstance pathway = databaseAdaptor.fetchInstance(pathwayDbId);
				if (pathway == null) {
					System.err.println("Extractor.buildFromPathwayReactionHash: WARNING - no pathway found with DB_ID=" + pathwayDbId);
					continue;
				}

				if (!title.isEmpty())
					title += ", ";
				title += pathway.getDisplayName();

				// Layout
				Collection<GKInstance> pathwayDiagrams = pathway.getReferers("representedPathway");
				GKInstance pathwayDiagram = null;
				if (pathwayDiagrams != null && pathwayDiagrams.size() > 0) {
					pathwayDiagram = pathwayDiagrams.iterator().next();
					double width = ((Integer)pathwayDiagram.getAttributeValue("width")).doubleValue();
					double height = ((Integer)pathwayDiagram.getAttributeValue("height")).doubleValue();
					totalWidth += width;
					if (height > maxHeight)
						maxHeight = height;
				}
			}
			
			diagram.setTitle(title);
			diagram.setWidth(totalWidth);
			diagram.setHeight(maxHeight);
			
			double xOffset = 0.0;
			List<Long> reactionDbIDs;
			for (Long pathwayDbId: sortedPathwayDbIds) {
//				System.err.println("Extractor.buildFromPathwayReactionHash: pathway DB_ID=" + pathwayDbId);
				
				GKInstance pathway = databaseAdaptor.fetchInstance(pathwayDbId);
				if (pathway == null) {
					System.err.println("Extractor.buildFromPathwayReactionHash: WARNING - no pathway found with DB_ID=" + pathwayDbId);
					continue;
				}

				reactionDbIDs = pathwayReactionHash.get(pathwayDbId);
				for (Long reactionDbId: reactionDbIDs) {
					GKInstance reactionlikEvent = databaseAdaptor.fetchInstance(reactionDbId);
					if (reactionlikEvent == null) {
						System.err.println("Extractor.buildFromPathwayReactionHash: WARNING - no reaction found with DB_ID=" + reactionDbId);
						continue;
					}
					createReactionVerteces(reactionlikEvent, pathway);
				}
			}
		} catch (Exception e) {
			System.err.println("Extractor.buildFromPathwayReactionHash: WARNING - problem with Reactome instance");
			e.printStackTrace(System.err);
		}
		
		if (nullRoleCounter > 0)
			System.err.println("Extractor.buildFromPathwayReactionHash: nullRoleCounter=" + nullRoleCounter);
	}
	
	/**
	 *  Find all verteces corresponding to the given reaction, and work out which
	 *  ones are found in "pathway".  Create vertices for them, and create the
	 *  appropriate layout for reactants, products and catalysts.  There may be
	 *  more than one reaction vertex in the diagram corresponding to a single reaction.
	 * 
	 * @param reactionlikEvent
	 * @param pathway
	 * @param xOffset
	 * @param entityRoleHash
	 */
	private void createReactionVerteces(GKInstance reactionlikEvent, GKInstance pathway) {
		String sbmlReactionId = Utils.getReactionIdFromReaction(reactionlikEvent);
		try {
			boolean foundRepresentedPathway = false;
			@SuppressWarnings("unchecked")
			Collection<GKInstance> reactionVerteces = reactionlikEvent.getReferers("representedInstance");
			if (reactionVerteces != null)
				for (GKInstance reactionVertex: reactionVerteces)
					if (createReactionVertex(reactionlikEvent, pathway, reactionVertex, sbmlReactionId))
						foundRepresentedPathway = true;
			if (!foundRepresentedPathway)
				System.err.println("Extractor.createReactionVerteces: WARNING - no represented pathways for reactionDbId=" + reactionlikEvent.getDBID() + ", pathwayDbId=" + pathway.getDBID() + ", representedPathwayDbIds: ");
		} catch (Exception e) {
			System.err.println("Extractor.createReactionVerteces: WARNING - problem with Reactome instance");
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 *  Deals with a vertex corresponding to the given reaction, and works out if
	 *  it is found in "pathway".  Create a vertex for it, and creates the
	 *  appropriate layout for reactants, products and catalysts.  Returns
	 *  true only if the vertex actually is found in "pathway".
	 * 
	 * @param reactionlikEvent
	 * @param pathway
	 * @param reactionVertex
	 * @param reactionLayout
	 * @param entityRoleHash
	 * @param sbmlReactionId 
	 * @param xOffset
	 * @return
	 */
	private boolean createReactionVertex(GKInstance reactionlikEvent, GKInstance pathway, GKInstance reactionVertex, String sbmlReactionId) {
		Long reactionDbId = reactionlikEvent.getDBID();
		boolean foundRepresentedPathway = false;

		try {
			ReactionVertex reactionVertexLayout = diagram.getReactionVertex(reactionVertex.getDBID().toString());
			if (reactionVertexLayout != null)
				return true;
			reactionVertexLayout = diagram.createReactionVertex(reactionVertex.getDBID().toString());
			
			GKInstance reactionVertexPathwayDiagram = (GKInstance)reactionVertex.getAttributeValue("pathwayDiagram");
			GKInstance representedPathway = (GKInstance)reactionVertexPathwayDiagram.getAttributeValue("representedPathway");
			
			// Find an appropriate x offset value.  We are trying to put all
			// pathway diagrams into a row, with none of them overlapping.
			// So, we need to calculate an x offset value that is as wide
			// as all of the pathways that have been drawn up to now.  If
			// the diagram has already been encountered, use the x offset
			// that was used the last time the diagram was used.
			double xOffset = 0.0;
			Double xOffsetDouble = pathwayDiagramXOffsetHash.get(reactionVertexPathwayDiagram.getDBID());
			if (xOffsetDouble == null) {
				for (Long pathwayDiagramDbId: pathwayDiagramXOffsetHash.keySet()) {
					GKInstance pathwayDiagram = pathwayDiagramtHash.get(pathwayDiagramDbId);
					xOffset += ((Integer)pathwayDiagram.getAttributeValue("width")).doubleValue();
				}

				pathwayDiagramtHash.put(reactionVertexPathwayDiagram.getDBID(), reactionVertexPathwayDiagram);
				pathwayDiagramXOffsetHash.put(reactionVertexPathwayDiagram.getDBID(), new Double(xOffset));
				extractCompartments(reactionVertexPathwayDiagram, xOffset);
			} else
				xOffset = xOffsetDouble.doubleValue();

			// We don't know a priori whether "pathway" (which comes from
			// the pathway-reaction hash) is above or below the represented
			// pathway (which comes from the diagram) in the event hierarchy.
			// So, we need to search both upwards and downwards.  Start
			// with downwards, because there will be less of the tree to
			// explore, so it will run faster.
			if (isDescendentEventOf(representedPathway, pathway) || isDescendentEventOf(pathway, representedPathway)) {
				foundRepresentedPathway = true;
				double reactionX = xOffset + ((Integer)reactionVertex.getAttributeValue("x")).doubleValue();
				double reactionY = ((Integer)reactionVertex.getAttributeValue("y")).doubleValue();
				Integer reactionWidthInteger = (Integer)(reactionVertex.getAttributeValue("width"));
				double reactionWidth = DEFAULT_REACTION_WIDTH;
				if (reactionWidthInteger != null)
					reactionWidth = reactionWidthInteger.doubleValue();
				Integer reactionHeightInteger = (Integer)(reactionVertex.getAttributeValue("height"));
				double reactionHeight = DEFAULT_REACTION_HEIGHT;
				if (reactionHeightInteger != null)
					reactionHeight = reactionHeightInteger.doubleValue();
				
				// Create the central reaction vertex
				reactionVertexLayout.setX(reactionX);
				reactionVertexLayout.setY(reactionY);
				reactionVertexLayout.setWidth(reactionWidth);
				reactionVertexLayout.setHeight(reactionHeight);
				reactionVertexLayout.setTitle(reactionlikEvent.getDisplayName());
				reactionVertexLayout.setReactionType(null);
				reactionVertexLayout.setSbmlReactionId(sbmlReactionId);
				
				// Create the edges leading to and from the reaction vertex, and the
				// verteces of the entities that they attach to.
				createReactionEdgeAndEntityLayouts(reactionVertex, reactionDbId, reactionVertexLayout, xOffset, true);
				createReactionEdgeAndEntityLayouts(reactionVertex, reactionDbId, reactionVertexLayout, xOffset, false);
			}
		} catch (Exception e) {
			System.err.println("Extractor.createReactionVertex: WARNING - problem with Reactome instance");
			e.printStackTrace(System.err);
		}

		return foundRepresentedPathway;
	}
	
	/**
	 * For the given reaction vertex in the diagram, create either the incoming or outgoing
	 * edges, plus associated entities.  Set isSource to "true" in order to generate the
	 * outgoing edges, "false" to generate the incoming edges.
	 * 
	 * @param reactionVertex
	 * @param reactionDbId
	 * @param reactionVertexLayout
	 * @param entityRoleHash
	 * @param xOffset
	 * @param isSource
	 */
	private void createReactionEdgeAndEntityLayouts(GKInstance reactionVertex, Long reactionDbId, ReactionVertex reactionVertexLayout, double xOffset, boolean isSource) {
		String referrerAttributeName = "sourceVertex";
		String vertexType = "targetVertex";
		if (isSource) {
			referrerAttributeName = "targetVertex";
			vertexType = "sourceVertex";			
		}
		try {
			Collection<GKInstance> edges = reactionVertex.getReferers(referrerAttributeName);
			if (edges == null || edges.size() == 0) {
				System.err.println("Extractor.createReactionEdgeAndEntityLayouts: WARNING - no " + referrerAttributeName + "es for reaction DB_ID=" + reactionDbId);
				return;
			}
			for (GKInstance edge: edges) 
				createReactionEdgeAndEntityLayout(edge, vertexType, reactionDbId, reactionVertexLayout, xOffset);
		} catch (Exception e) {
			System.err.println("Extractor.createReactionEdgeAndEntityLayouts: WARNING - problem with Reactome instance");
			e.printStackTrace(System.err);
		}
	}

	private void createReactionEdgeAndEntityLayout(GKInstance edge, String vertexType, Long reactionDbId, ReactionVertex reactionVertexLayout, double xOffset) {
		if (!edge.getSchemClass().isa("Edge")) {
			System.err.println("Extractor.createReactionEdgeLayout: WARNING - edge is actually a " + edge.getSchemClass().getName() + "!!");
			return;
		}
		Edge edgeLayout = diagram.getEdge(edge.getDBID().toString());
		if (edgeLayout != null)
			return;
		edgeLayout = diagram.createEdge(edge.getDBID().toString());
		String role = determineRoleForEdge(edge);
		reactionVertexLayout.addEdgeLayout(edgeLayout);
		edgeLayout.setRole(role);
		try {
			GKInstance entityVertex = (GKInstance)edge.getAttributeValue(vertexType);
			if (entityVertex == null) {
				System.err.println("Extractor.createReactionEdgeLayout: WARNING - entityVertex is null!!");
				return;
			}
			if (!entityVertex.getSchemClass().isa("EntityVertex")) {
				System.err.println("Extractor.createReactionEdgeLayout: WARNING - entityVertex is actually a " + entityVertex.getSchemClass().getName() + "!!");
				return;
			}
			EntityVertex entityVertexLayout = createEntityLayout(entityVertex, xOffset);
			if (entityVertexLayout == null) {
				System.err.println("Extractor.createReactionEdgeLayout: WARNING - entityVertexLayout is null!!");
				return;
			}
			GKInstance entity = (GKInstance)(entityVertex.getAttributeValue("representedInstance"));
			edgeLayout.setSbmlSpeciesReferenceId(Utils.getSpeciesReferenceId(reactionDbId.toString(), role, entity.getDBID().toString()));
		
			edgeLayout.setEntityVertexLayout(entityVertexLayout);
			edgeLayout.setReactionVertexLayout(reactionVertexLayout);
			
			// Decide which direction the edge should go.  To help SBGN,
			// make sure that the reaction is never the starting point
			// for a catalysis arc, and also ensure that inputs point
			// towards the reaction, and outputs away from it.
//			if ((isSource && !role.equals("catalyst") && !role.equals("input")) || role.equals("output"))
			if (role.equals("output"))
				connectGlyphs(reactionVertexLayout, entityVertexLayout, edgeLayout);
			else
				connectGlyphs(entityVertexLayout, reactionVertexLayout, edgeLayout);
		} catch (Exception e) {
			System.err.println("Extractor.createReactionEdgeLayout: WARNING - problem with Reactome instance");
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Given two vertices and an edge to connect them, calculate the best positioning
	 * of the edge to connect the vertices, and insert the appropriate start and end
	 * coordinates into the edge.
	 * 
	 * @param reactionVertexLayout
	 * @param entityVertexLayout
	 * @param edgeLayout
	 */
	private void connectGlyphs(Vertex vertex1, Vertex vertex2, Edge edge) {
		List<Point> midBoundaryPoints1 = vertex1.getMidBoundaryPoints();
		List<Point> midBoundaryPoints2 = vertex2.getMidBoundaryPoints();
		PointPair closestMidBoundaryPointPair = null;
		double minimumMidBoundaryPointSeparation = Double.MAX_VALUE;
		for (Point midBoundaryPoint1: midBoundaryPoints1)
			for (Point midBoundaryPoint2: midBoundaryPoints2) {
				PointPair pointPair = new PointPair(midBoundaryPoint1, midBoundaryPoint2);
				double midBoundaryPointSeparation = pointPair.separation();
				if (midBoundaryPointSeparation < minimumMidBoundaryPointSeparation) {
					minimumMidBoundaryPointSeparation = midBoundaryPointSeparation;
					closestMidBoundaryPointPair = pointPair;
				}
			}
		if (closestMidBoundaryPointPair == null)
			return;
		edge.setStartVertex(vertex1);
		edge.setEndVertex(vertex2);
		
//		System.err.println("Extractor.connectGlyphs: edge: " + edge.getId() + ", role: " + edge.getRole() + ", start: " + edge.getStartVertex().getId() + ", end: " + edge.getEndVertex().getId());
		
		edge.setStartX(closestMidBoundaryPointPair.point1.x);
		edge.setStartY(closestMidBoundaryPointPair.point1.y);
		edge.setEndX(closestMidBoundaryPointPair.point2.x);
		edge.setEndY(closestMidBoundaryPointPair.point2.y);
	}

	private EntityVertex createEntityLayout(GKInstance entityVertex, double xOffset) {
		Long entityVertexDbId = entityVertex.getDBID();
		EntityVertex entityVertexLayout = diagram.getEntityVertex(entityVertexDbId.toString());
		if (entityVertexLayout != null)
			return entityVertexLayout;
		entityVertexLayout = diagram.createEntityVertex(entityVertexDbId.toString());
		
		if (!entityVertex.getSchemClass().isa(ReactomeJavaConstants.EntityVertex)) {
			System.err.println("Extractor.createEntityLayout: WARNING - entityVertex is not of type EntityVertex, it is a: " + entityVertex.getSchemClass().getName());
			return null;
		}
		
		try {
			GKInstance entity = (GKInstance)(entityVertex.getAttributeValue("representedInstance"));
			double entityX = xOffset + ((Integer)entityVertex.getAttributeValue("x")).doubleValue();
			double entityY = ((Integer)entityVertex.getAttributeValue("y")).doubleValue();
			// For some reason, entities look better if you shrink them a bit
			double entityWidth = ((Integer)entityVertex.getAttributeValue("width")).doubleValue() * 0.5;
			double entityHeight = ((Integer)entityVertex.getAttributeValue("height")).doubleValue() * 0.3;
			if (entityWidth < MIN_ENTITY_WIDTH)
				entityWidth = MIN_ENTITY_WIDTH;
			if (entityHeight < MIN_ENTITY_HEIGHT)
				entityHeight = MIN_ENTITY_HEIGHT;
			entityVertexLayout.setX(entityX);
			entityVertexLayout.setY(entityY);
			entityVertexLayout.setWidth(entityWidth);
			entityVertexLayout.setHeight(entityHeight);
			entityVertexLayout.setEntityVertexDbId(entityVertexDbId);
			entityVertexLayout.setSbmlSpeciesId(Utils.getSpeciesIdFromEntity((GKInstance)entityVertex.getAttributeValue("representedInstance")));
			entityVertexLayout.setTitle(entity.getDisplayName());
			SchemaClass schemaClass = entity.getSchemClass();
			if (schemaClass.isa("Complex")) {
				entityVertexLayout.setType("complex");
				List<GKInstance> components = entity.getAttributeValuesList("hasComponent");
				boolean isMultimer = true;
				Long previousDbId = null;
				for (GKInstance component: components) {
					Long dbId = component.getDBID();
					if (previousDbId != null && !previousDbId.equals(dbId))
						isMultimer = false;
					previousDbId = dbId;
					entityVertexLayout.addComponentName(component.getDisplayName());
				}
				if (isMultimer)
					entityVertexLayout.setSubType("multimer");
			} else if (schemaClass.isa("EntityWithAccessionedSequence")) {
				GKInstance referenceEntity = (GKInstance) entity.getAttributeValue("referenceEntity");
				if (referenceEntity == null)
					entityVertexLayout.setType("protein");
				else {
					if (referenceEntity.getSchemClass().isa("ReferenceDNASequence"))
						entityVertexLayout.setType("dna");
					else if (referenceEntity.getSchemClass().isa("ReferenceRNASequence"))
						entityVertexLayout.setType("rna");
					else
						entityVertexLayout.setType("protein");
				}
			} else if (schemaClass.isa("SimpleEntity"))
				entityVertexLayout.setType("compound");
			else if (schemaClass.isa("Polymer"))
				entityVertexLayout.setType("polymer");
			else if (schemaClass.isa("Set")) {
				entityVertexLayout.setType("set");
				List<GKInstance> components = entity.getAttributeValuesList("hasMember");
				for (GKInstance component: components)
					entityVertexLayout.addComponentName(component.getDisplayName());
				if (schemaClass.isa("CandidateSet")) {
					entityVertexLayout.setSubType("candidate");
					components = entity.getAttributeValuesList("hasCandidate");
					for (GKInstance component: components)
						entityVertexLayout.addComponentName(component.getDisplayName());
				} else if (schemaClass.isa("DefinedSet"))
					entityVertexLayout.setSubType("defined");
				else if (schemaClass.isa("OpenSet"))
					entityVertexLayout.setSubType("open");
			} else
				entityVertexLayout.setType("unknown");
			
			return entityVertexLayout;
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		
		return null;
	}
	
	private String determineRoleForEdge(GKInstance edge) {
		int edgeType = (-1);
		try {
			edgeType = ((Integer)(edge.getAttributeValue("edgeType"))).intValue();
		} catch (Exception e) {
			System.err.println("Extractor.determineRoleForEdge: WARNING - problem extracting edgeType attribute from Edge");
			e.printStackTrace(System.err);
		}
		String role = "unknown";
		switch (edgeType) {
			case HyperEdge.INPUT :
				role = "input";
				break;
			case HyperEdge.OUTPUT :
				role = "output";
				break;
			case HyperEdge.CATALYST :
				role = "catalyst";
				break;
			case HyperEdge.INHIBITOR :
				role = "inhibitor";
				break;
			case HyperEdge.ACTIVATOR :
				role = "activator";
				break;
		}
		
		return role;
	}
	
	private void extractCompartments(GKInstance pathwayDiagram, double xOffset) {
		try {
			String xml = (String) pathwayDiagram.getAttributeValue(ReactomeJavaConstants.storedATXML);
			if (xml == null || xml.isEmpty())
				return;
			Reader sReader = new StringReader(xml);
			SAXBuilder builder = new SAXBuilder();
			org.jdom.Document document = builder.build(sReader);
			org.jdom.Element root = document.getRootElement();
			List<Element> children = root.getChildren();
			
			for (Element child: children) {
				String name = child.getName();
				if (name.equals("Nodes")) {
					List<Element> grandChildren = child.getChildren();
					for (Element grandChild: grandChildren) {
						name = grandChild.getName();
						if (name.matches("^.*RenderableCompartment.*$")) {
							Attribute reactomeId = grandChild.getAttribute("reactomeId");
							if (reactomeId == null) {
								System.err.println("Extractor.getCompartments: WARNING - reactomeId == null");
								continue;
							}
							String reactomeIdValue = reactomeId.getValue();
							if (reactomeIdValue == null || reactomeIdValue.isEmpty()) {
								System.err.println("Extractor.getCompartments: WARNING - could not find a suitable reactomeIdValue");
								continue;
							}
							Long compartmentDbId = new Long(reactomeIdValue);
							CompartmentVertex compartmentVertexLayout = diagram.getCompartmentVertex(compartmentDbId.toString() + "_" + pathwayDiagram.getDBID().toString());
							if (compartmentVertexLayout != null) {
								// This compartment has been defined already, so we can skip the rest
								continue;
							}
							compartmentVertexLayout = diagram.createCompartmentVertex(compartmentDbId.toString() + "_" + pathwayDiagram.getDBID().toString());
							
							GKInstance compartment = databaseAdaptor.fetchInstance(compartmentDbId);
							if (compartment == null) {
								System.err.println("Extractor.getCompartments: WARNING - compartment == null");
								continue;
							}
							Attribute bounds = grandChild.getAttribute("bounds");
							if (bounds == null) {
								System.err.println("Extractor.getCompartments: WARNING - bounds == null");
								continue;
							}
							String boundsValue = bounds.getValue();
							if (boundsValue == null || boundsValue.isEmpty()) {
								System.err.println("Extractor.getCompartments: WARNING - could not find a suitable boundsValue");
								continue;
							}
							Attribute textPosition = grandChild.getAttribute("textPosition");
							if (textPosition == null) {
								System.err.println("Extractor.getCompartments: WARNING - textPosition == null");
								continue;
							}
							String textPositionValue = textPosition.getValue();
							if (textPositionValue == null || textPositionValue.isEmpty()) {
								System.err.println("Extractor.getCompartments: WARNING - could not find a suitable textPositionValue");
								continue;
							}
							String compartmentName = compartment.getDisplayName();
							String[] boundsValues = boundsValue.split(" +");
							if (boundsValues.length != 4) {
								System.err.println("Extractor.getCompartments: WARNING - expected 4 bounds values, found: " + boundsValues.length);
								continue;
							}
							String[] textPositionValues = textPositionValue.split(" +");
							if (textPositionValues.length != 2) {
								System.err.println("Extractor.getCompartments: WARNING - expected 2 text position values, found: " + textPositionValues.length);
								continue;
							}
							double x = (new Double(boundsValues[0])).doubleValue() + xOffset;
							double y = (new Double(boundsValues[1])).doubleValue();
							double width = (new Double(boundsValues[2])).doubleValue();
							double height = (new Double(boundsValues[3])).doubleValue();
							double textXOffset = width * 0.08;
							if (textXOffset < MIN_ENTITY_WIDTH * 0.5)
								textXOffset = MIN_ENTITY_WIDTH * 0.5;
							double textX = x + textXOffset;
							if (textX > x + width)
								textX = x;
							double textYOffset = height * 0.03;
							if (textYOffset < MIN_ENTITY_HEIGHT * 2.0)
								textYOffset = MIN_ENTITY_HEIGHT * 2.0;
							double textY = y + textYOffset;
							if (textY > y + height)
								textY = y;
							compartmentVertexLayout.setTitle(compartmentName);
							compartmentVertexLayout.setX(x);
							compartmentVertexLayout.setY(y);
							compartmentVertexLayout.setWidth(width);
							compartmentVertexLayout.setHeight(height);
							compartmentVertexLayout.setCompartmentDbId(compartmentDbId);
							compartmentVertexLayout.setTextX(textX);
							compartmentVertexLayout.setTextY(textY);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
//	/**
//	 * Find a pathway diagram corresponding to the given pathway.  Returns null
//	 * if no diagram could be found.
//	 * 
//	 * @param pathway
//	 * @return
//	 */
//	private GKInstance findPathwayDiagram(GKInstance pathway) {
//		Collection<GKInstance> pathwayDiagrams = null;
//		try {
//			pathwayDiagrams = pathway.getReferers("representedPathway");
//		} catch (Exception e) {
//			System.err.println("Extractor.findPathwayDiagram: WARNING - problem with Reactome instance");
//			e.printStackTrace(System.err);
//		}
//		
//		if (pathwayDiagrams != null && pathwayDiagrams.size() > 0)
//			return pathwayDiagrams.iterator().next();
//
//		Collection<GKInstance> superPathways = null;
//		try {
//			superPathways = pathway.getReferers("hasEvent");
//		} catch (Exception e) {
//			System.err.println("Extractor.findPathwayDiagram: WARNING - problem with Reactome instance");
//			e.printStackTrace(System.err);
//		}
//		
//		if (superPathways != null) {
//			while (superPathways.iterator().hasNext()) {
//				GKInstance superPathway = superPathways.iterator().next();
//				GKInstance superPathwayDiagram = findPathwayDiagram(superPathway);
//				if (superPathwayDiagram != null)
//					return superPathwayDiagram;
//			}
//		}
//		
//		return null;
//	}
	
	/**
	 * A utility method to check if subEvent is a descendant of another event
	 * in a hierarchical structure.
	 * 
	 * Recursive method, in contrast to the one in InstanceUtilities.
	 * 
	 * @param subEvent the instance to be checked for
	 * @param event the instance to be checked against
	 * @return
	 */
	private boolean isDescendentEventOf(GKInstance subEvent, GKInstance event) {
		return isDescendentEventOf(subEvent, event, 0);
	}
	
	private static final int MAX_RECURSION_DEPTH = 100;
	private boolean isDescendentEventOf(GKInstance subEvent, GKInstance event, int depth) {
		// Deal with easy cases.  All of these will terminate the recursion.
		if (subEvent == event) // also true if both are null
			return true;
		if (subEvent == null) {
			System.err.println("Extractor.isDescendentOf: subEvent == null!!");
			return false;
		}
		if (event == null) {
			System.err.println("Extractor.isDescendentOf: event == null!!");
			return false;
		}
		Long subEventDbId = subEvent.getDBID();
		Long eventDbId = event.getDBID();
		if (eventDbId.equals(subEventDbId))
			return true;
		if (depth == MAX_RECURSION_DEPTH) {
			System.err.println("Extractor.isDescendentOf: hit soft recursion depth limit: " + depth);
			return false;
		}
		
		// Recurse further if no match found at the current level in the
		// hierarchy.
		try {
			// The recursion only works on pathways.  If we have hit a reaction,
			// we have arrived at a leaf node and can go no deeper.
			if (!event.getSchemClass().isa("Pathway"))
				return false;
			
			// Find child events and test each one to see if it matches.
			List<GKInstance> childEvents = event.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
			if (childEvents == null)
				return false;
			int newDepth = depth + 1;
			for (GKInstance childEvent: childEvents)
				if (isDescendentEventOf(subEvent, childEvent, newDepth))
					return true;
		}
		catch (Exception e) {
			System.err.println("Extractor.isDescendentOf: problem recursing to child events");
			e.printStackTrace(System.err);
		}

		return false;
	}
}
