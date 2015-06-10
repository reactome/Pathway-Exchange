/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbgn;

import java.util.HashMap;
import java.util.List;

import org.gk.layout.CompartmentVertex;
import org.gk.layout.Diagram;
import org.gk.layout.Edge;
import org.gk.layout.EntityVertex;
import org.gk.layout.ReactionVertex;
import org.gk.layout.Vertex;
import org.sbgn.ArcClazz;
import org.sbgn.GlyphClazz;
import org.sbgn.Language;
import org.sbgn.bindings.Arc;
import org.sbgn.bindings.Arc.End;
import org.sbgn.bindings.Arc.Start;
import org.sbgn.bindings.Bbox;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Glyph.Callout;
import org.sbgn.bindings.Label;
import org.sbgn.bindings.Map;
import org.sbgn.bindings.Point;
import org.sbgn.bindings.Port;
import org.sbgn.bindings.Sbgn;

/**
 * Layout extractor producing SBGN PD diagrams.
 * 
 * @author David Croft
 *
 */
public class PDExtractor {
	private Sbgn sbgn = new Sbgn();
	private static final double MAGNIFICATION = 3.0;
	private static final double FONT_WIDTH = 8.0;
	private static final double FONT_HEIGHT = 12.0;
	private java.util.Map<String,Glyph> reactionHash = new HashMap<String,Glyph>();
	private HashMap<String,Glyph> entityGlyphHash = new HashMap<String,Glyph>();
	private java.util.Map<String,Glyph> annotationGlyphHash = new HashMap<String,Glyph>();
	private java.util.Map<String,Arc> arcHash = new HashMap<String,Arc>();
	
	public Sbgn getSbgn() {
		return sbgn;
	}

	public void extract(Diagram diagram) {
		Map map = new Map();
		map.setLanguage(Language.PD.getName());
		
		// This may break if SBGN changes the cardinality of Map to allow
		// multiple maps.  Then you will probably need to use an "addMap"
		// method.
		sbgn.setMap(map);
		
		for (CompartmentVertex compartmentVertex: diagram.getCompartmentVertexes())
			addCompartmentLayout(map, compartmentVertex);

		for (EntityVertex entityVertex: diagram.getEntityVertexes())
			addEntityVertexLayout(map, entityVertex);

		for (ReactionVertex reactionVertex: diagram.getReactionVertexes())
			addReactionVertexLayout(map, reactionVertex);

		for (Edge edge: diagram.getEdges())
			addEdge(map, edge);
	}
	
	private void addReactionVertexLayout(Map map, ReactionVertex reactionVertex) {
		Glyph reactionGlyph = convertReactionVertexToSBGNGlyph(reactionVertex);
		map.getGlyph().add(reactionGlyph);
	}
	
	private Glyph convertReactionVertexToSBGNGlyph(ReactionVertex reactionVertex) {
		String reactionVertexId = reactionVertex.getId();
		Glyph reactionGlyph = reactionHash.get(reactionVertexId);
		if (reactionGlyph != null)
			return reactionGlyph;
		
		double reactionX = reactionVertex.getX() * MAGNIFICATION;
		double reactionY = reactionVertex.getY() * MAGNIFICATION;
		double width = reactionVertex.getWidth() * MAGNIFICATION;
		double height = reactionVertex.getHeight() * MAGNIFICATION;
		reactionGlyph = new Glyph();
		String reactionType = reactionVertex.getReactionType();
		reactionGlyph.setClazz(GlyphClazz.PROCESS.toString());
		// TODO: Reactome knows more about process type than just "PROCESS", we can represent this in SBGN!
//		GlyphClazz.ASSOCIATION;// complex formation
		reactionGlyph.setId(reactionVertexId);
		
		Bbox bb = new Bbox();
		bb.setX((float) reactionX);
		bb.setY((float) reactionY);
		bb.setW((float) width);
		bb.setH((float) height);
		reactionGlyph.setBbox(bb);
		
		reactionHash.put(reactionVertexId, reactionGlyph);
		
		return reactionGlyph;
	}
	
	private void addEntityVertexLayout(Map map, EntityVertex entityVertex) {
		Glyph speciesGlyph = convertEntityVertexToSBGNGlyph(entityVertex);
		map.getGlyph().add(speciesGlyph);
	}
	
	private Glyph convertEntityVertexToSBGNGlyph(EntityVertex entityVertex) {
		String entityVertexId = entityVertex.getId();
		Glyph speciesGlyph = entityGlyphHash.get(entityVertexId);
		if (speciesGlyph != null)
			return speciesGlyph;
		
		String title = entityVertex.getShortTitle();
		double x = entityVertex.getX() * MAGNIFICATION;
		double y = entityVertex.getY() * MAGNIFICATION;
		double width = entityVertex.getWidth() * MAGNIFICATION;
		double height = entityVertex.getHeight() * MAGNIFICATION;
		double speciesX = x - 12.0;
		if (speciesX < 0.0)
			speciesX = 0.0;
		speciesGlyph = new Glyph();
		String entityVertexType = entityVertex.getType();
		String entityVertexSubType = entityVertex.getSubType();
		String annotationText = null;
		String materialTypeLabelText = null;
		String conceptualTypeLabelText = null;
		if (entityVertexType.equals("complex")) {
			if (entityVertexSubType != null && entityVertexSubType.equals("multimer"))
				speciesGlyph.setClazz(GlyphClazz.COMPLEX_MULTIMER.toString());
			else
				speciesGlyph.setClazz(GlyphClazz.COMPLEX.toString());
			annotationText = stringListToString(entityVertex.getComponentNames());
		} else if (entityVertexType.equals("protein")) {
			speciesGlyph.setClazz(GlyphClazz.MACROMOLECULE.toString());
			materialTypeLabelText = "mt:prot";
		} else if (entityVertexType.equals("dna")) {
			speciesGlyph.setClazz(GlyphClazz.MACROMOLECULE.toString());
			materialTypeLabelText = "mt:dna";
		} else if (entityVertexType.equals("rna")) {
			speciesGlyph.setClazz(GlyphClazz.MACROMOLECULE.toString());
			materialTypeLabelText = "mt:rna";
		} else if (entityVertexType.equals("compound"))
			speciesGlyph.setClazz(GlyphClazz.SIMPLE_CHEMICAL.toString());
		else if (entityVertexType.equals("set")) {
			if (entityVertexSubType != null) {
				materialTypeLabelText = "mt:set";
				if (entityVertexSubType.equals("candidate"))
					conceptualTypeLabelText = "ct:cand";
				else if (entityVertexSubType.equals("defined"))
					conceptualTypeLabelText = "ct:def";
				else if (entityVertexSubType.equals("open"))
					conceptualTypeLabelText = "ct:open";
			}
			annotationText = stringListToString(entityVertex.getComponentNames());
		} else
			speciesGlyph.setClazz(GlyphClazz.UNSPECIFIED_ENTITY.toString());
		
		String speciesGlyphId = entityVertex.getId();
		Bbox bb = new Bbox();
		bb.setX((float) x);
		bb.setY((float) y);
		bb.setW((float) width);
		bb.setH((float) height);
		speciesGlyph.setBbox(bb);
		speciesGlyph.setId(speciesGlyphId);
		
		if (annotationText != null) {
			Glyph annotation = createAnnotation(annotationText, speciesGlyph);
			Callout callout = new Callout();
			Point point = new Point();
			point.setX((float) (x + width));
			point.setY((float) (y + height));
			callout.setPoint(point);
			callout.setTarget(speciesGlyph);
			annotation.setCallout(callout);
			speciesGlyph.getGlyph().add(annotation);
		}
		if (materialTypeLabelText != null) 
			speciesGlyph.getGlyph().add(createMaterialTypeDecorator(materialTypeLabelText, speciesGlyph));
		if (conceptualTypeLabelText != null) 
			speciesGlyph.getGlyph().add(createConceptualTypeDecorator(conceptualTypeLabelText, speciesGlyph));
		
		Label label = new Label();
		label.setText(title);
		speciesGlyph.setLabel(label);
		
		entityGlyphHash.put(entityVertexId, speciesGlyph);
		
		return speciesGlyph;
	}
	
	/**
	 * Add an annotation bubble to the glyph
	 * 
	 * @param labelText
	 */
	private Glyph createAnnotation(String labelText, Glyph speciesGlyph) {
		String speciesGlyphId = speciesGlyph.getId();
		Glyph annotation = annotationGlyphHash.get(speciesGlyphId);
		if (annotation != null)
			return annotation;
		
		Label decoratorLabel = new Label();
		decoratorLabel.setText(labelText);
		annotation = new Glyph();
		annotation.setClazz(GlyphClazz.ANNOTATION.toString());
		annotation.setLabel(decoratorLabel);
		annotation.setId(speciesGlyphId + "_annotation");
		double annotationWidth = labelText.length() * FONT_WIDTH;
		double annotationHeight = FONT_HEIGHT * 2.0;
		double annotationY = speciesGlyph.getBbox().getY() + speciesGlyph.getBbox().getH() + (FONT_HEIGHT * 2.0);
		double annotationX = speciesGlyph.getBbox().getX() + speciesGlyph.getBbox().getW() + (FONT_WIDTH * 2.0);
		Bbox annotationBoundingBox = new Bbox();
		annotationBoundingBox.setX((float) annotationX);
		annotationBoundingBox.setY((float) annotationY);
		annotationBoundingBox.setW((float) annotationWidth);
		annotationBoundingBox.setH((float) annotationHeight);
		annotation.setBbox(annotationBoundingBox);
		annotationGlyphHash.put(speciesGlyphId, annotation);

		return annotation;
	}
	
	/**
	 * Add unit of information to entities to get more specific types, e.g. MACROMOLECULE -> protein
	 * 
	 * @param labelText
	 */
	private Glyph createConceptualTypeDecorator(String labelText, Glyph speciesGlyph) {
		Glyph decorator =   appendDecorator(labelText, speciesGlyph, false);
		decorator.setId(speciesGlyph.getId() + "_ct");
		return decorator;
	}
	
	/**
	 * Add unit of information to entities to get more specific types, e.g. MACROMOLECULE -> protein
	 * 
	 * @param labelText
	 */
	private Glyph createMaterialTypeDecorator(String labelText, Glyph speciesGlyph) {
		Glyph decorator =  appendDecorator(labelText, speciesGlyph, true);
		decorator.setId(speciesGlyph.getId() + "_mt");
		return decorator;
	}
	
	/**
	 * Add unit of information to entities to get more specific types, e.g. MACROMOLECULE -> protein
	 * 
	 * @param labelText
	 */
	private Glyph appendDecorator(String labelText, Glyph speciesGlyph, boolean left) {
		Label decoratorLabel = new Label();
		decoratorLabel.setText(labelText);
		Glyph decorator = new Glyph();
		decorator.setClazz(GlyphClazz.UNIT_OF_INFORMATION.toString());
		decorator.setLabel(decoratorLabel);
		double x = speciesGlyph.getBbox().getX();
		double y = speciesGlyph.getBbox().getY();
		double decoratorWidth = labelText.length() * FONT_WIDTH;
		double decoratorHeight = FONT_HEIGHT * 2.0;
		double decoratorX = x + (FONT_WIDTH * 2.0);
		if (!left)
			decoratorX = x + decoratorWidth - (FONT_WIDTH * 2.0);
		if (decoratorX < x)
			decoratorX = x;
		if (decoratorX > x + decoratorWidth)
			decoratorX = x + decoratorWidth;
		double decoratorY = y - (decoratorHeight * 0.5);
		Bbox decoratorBoundingBox = new Bbox();
		decoratorBoundingBox.setX((float) decoratorX);
		decoratorBoundingBox.setY((float) decoratorY);
		decoratorBoundingBox.setW((float) decoratorWidth);
		decoratorBoundingBox.setH((float) decoratorHeight);
		decorator.setBbox(decoratorBoundingBox);
		return decorator;
	}

	private void addEdge(Map map, Edge edge) {
		Arc arc = convertEdgeToSBGNArc(edge);
		map.getArc().add(arc);
	}
	
	private Arc convertEdgeToSBGNArc(Edge edge) {
		String edgeId = edge.getId();
		Arc arc = arcHash.get(edgeId);
		if (arc != null)
			return arc;
		
		arc = new Arc();
		arc.setId(edgeId);

		String role = edge.getRole();
		if (role == null) {
			System.err.println("LayoutGeneratorSBGN.addEdgeLayout: WARNING - role == null!");
			arc.setClazz(ArcClazz.UNKNOWN_INFLUENCE.toString());
		} else if (role.equals("input"))
			arc.setClazz(ArcClazz.CONSUMPTION.toString());
		else if (role.equals("output"))
			arc.setClazz(ArcClazz.PRODUCTION.toString());
		else if (role.equals("catalyst"))
			arc.setClazz(ArcClazz.CATALYSIS.toString());
		else if (role.equals("activator"))
			arc.setClazz(ArcClazz.STIMULATION.toString());
		else if (role.equals("inhibitor"))
			arc.setClazz(ArcClazz.INHIBITION.toString());
		else
			arc.setClazz("Reactome_" + role);
		
		float startX = (float) (edge.getStartX() * MAGNIFICATION);
		float startY = (float) (edge.getStartY() * MAGNIFICATION);
		float endX = (float) (edge.getEndX() * MAGNIFICATION);
		float endY = (float) (edge.getEndY() * MAGNIFICATION);

		Start start = new Start();
		start.setX(startX);
		start.setY(startY);
		arc.setStart(start);
		End end = new End();
		end.setX(endX);
		end.setY(endY);
		arc.setEnd(end);
		
		Vertex startVertex = edge.getStartVertex();
		Vertex endVertex = edge.getEndVertex();
		Glyph startGlyph = null;
		Glyph endGlyph = null;
		if (startVertex == null || endVertex == null)
			System.err.println("LayoutGeneratorSBGNPD.addEdgeLayout: WARNING - start or end vertex is null");
		else if (startVertex.getClass().isAssignableFrom(org.gk.layout.ReactionVertex.class) && endVertex.getClass().isAssignableFrom(org.gk.layout.EntityVertex.class)) {
			startGlyph = convertReactionVertexToSBGNGlyph((ReactionVertex) startVertex);
			endGlyph = convertEntityVertexToSBGNGlyph((EntityVertex) endVertex);
		} else if (startVertex.getClass().isAssignableFrom(org.gk.layout.EntityVertex.class) && endVertex.getClass().isAssignableFrom(org.gk.layout.ReactionVertex.class)) {
			startGlyph = convertEntityVertexToSBGNGlyph((EntityVertex) startVertex);
			endGlyph = convertReactionVertexToSBGNGlyph((ReactionVertex) endVertex);
		} else
			System.err.println("LayoutGeneratorSBGNPD.addEdgeLayout: WARNING - start and end vertexes do not constitute a reaction/entity pair");
		if (startGlyph != null && endGlyph != null) {
			if (role.equals("output")) {
				Port startPort = addPort(startGlyph, startX, startY, startVertex.getCurrentPortNum());
				arc.setSource(startPort);
			} else
				arc.setSource(startGlyph);
			arc.setTarget(endGlyph);
			arcHash.put(edgeId, arc);
		}
		
		return arc;
	}

	private void addCompartmentLayout(Map map, CompartmentVertex compartmentVertexLayout) {
		double x = compartmentVertexLayout.getX() * MAGNIFICATION;
		double y = compartmentVertexLayout.getY() * MAGNIFICATION;
		double width = compartmentVertexLayout.getWidth() * MAGNIFICATION;
		double height = compartmentVertexLayout.getHeight() * MAGNIFICATION;
		String title = compartmentVertexLayout.getTitle() != null ? 
					   compartmentVertexLayout.getTitle() :
					   "";

		Glyph compartmentGlyph = new Glyph();
		compartmentGlyph.setClazz(GlyphClazz.COMPARTMENT.toString());
		Bbox bb = new Bbox();
		bb.setX((float) x);
		bb.setY((float) y);
		bb.setW((float) width);
		bb.setH((float) height);
		compartmentGlyph.setBbox(bb);
		String compartmentGlyphId = compartmentVertexLayout.getId();
		compartmentGlyph.setId(compartmentGlyphId);
		
		if (title.toLowerCase().matches("^.*extracellular.*$"))
			compartmentGlyph.setCompartmentOrder((float) 1);
		else if (title.toLowerCase().matches("^.*cytosol.*$"))
			compartmentGlyph.setCompartmentOrder((float) 2);
		else if (title.toLowerCase().matches("^.*outer.*$"))
			compartmentGlyph.setCompartmentOrder((float) 3);
		else
			compartmentGlyph.setCompartmentOrder((float) 10);
		
		// Add compartment ID as reference to entities
		double labelX = compartmentVertexLayout.getTextX() * MAGNIFICATION;
		double labelY = compartmentVertexLayout.getTextY() * MAGNIFICATION;
		double labelWidth = title.length() * FONT_WIDTH;
		double labelHeight = FONT_HEIGHT * 2.0;
		Label label = new Label();
		label.setText(title);
		Bbox labelBoundingBox = new Bbox();
		labelBoundingBox.setX((float) labelX);
		labelBoundingBox.setY((float) labelY);
		labelBoundingBox.setW((float) labelWidth);
		labelBoundingBox.setH((float) labelHeight);
		label.setBbox(labelBoundingBox);
		compartmentGlyph.setLabel(label);
		
		map.getGlyph().add(compartmentGlyph);
	}
	
	private String stringListToString(List<String> stringList) {
		if (stringList == null)
			return null;
		String outputString = "";
		java.util.Map<String,String> stringHash = new HashMap<String,String>();
		for (String string: stringList) {
			if (stringHash.containsKey(string))
				continue;
			if (!outputString.isEmpty())
				outputString += ", ";
			outputString += string.replaceAll(" *\\[[^\\]]*\\]$", "");
			stringHash.put(string, string);
		}
		
		return outputString;
	}
	
	/**
	 * Adds a port to the glyph.
	 *
	 * @param g
	 * @param x
	 * @param y
	 * @param portNum
	 */
	private Port addPort(Glyph g, float x, float y, int portNum) {
		Port port = new Port();
		port.setId(g.getId() + "." + portNum);
		port.setX(x);
		port.setY(y);
		g.getPort().add(port);
		
		return port;
	}
}
