/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.generators;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.gk.sbml.layout.elements.CompartmentVertex;
import org.gk.sbml.layout.elements.Diagram;
import org.gk.sbml.layout.elements.Edge;
import org.gk.sbml.layout.elements.EntityVertex;
import org.gk.sbml.layout.elements.Reaction;
import org.gk.sbml.layout.elements.ReactionVertex;
import org.sbgn.ArcClazz;
import org.sbgn.GlyphClazz;
import org.sbgn.bindings.Arc;
import org.sbgn.bindings.Bbox;
import org.sbgn.bindings.Glyph;
import org.sbgn.bindings.Label;
import org.sbgn.bindings.Map;
import org.sbgn.bindings.Sbgn;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Layout generator using libSBGN
 * 
 * @author David Croft
 *
 */
public class LayoutGeneratorSBGNPD extends LayoutGenerator {
	private Sbgn sbgn;
	private Map map;
	private HashMap<Long,Glyph> entityVertexLayoutSeen = new HashMap<Long,Glyph>();
	
	public void run(Diagram modelLayout) {
		sbgn = new Sbgn();
		map = new Map();
		
		// This may break if SBGN changes the cardinality of Map to allow
		// multiple maps.  Then you will probably need to use an "addMap"
		// method.
		sbgn.setMap(map);

		for (Reaction reactionLayout: modelLayout.getReactionLayouts())
			addReactionLayout(reactionLayout);
		
		for (CompartmentVertex compartmentVertexLayout: modelLayout.getCompartmentVertexLayouts())
			addCompartmentLayout(compartmentVertexLayout);
		
		addToModel();
	}
	
	private void addReactionLayout(Reaction reactionLayout) {
		String sbmlReactionId = reactionLayout.getSbmlReactionId();

		for (ReactionVertex reactionVertexLayout: reactionLayout.getReactionVertexLayouts()) {
			addReactionVertexLayout(reactionVertexLayout, sbmlReactionId);
		}
	}
	
	private void addReactionVertexLayout(ReactionVertex reactionVertexLayout, String sbmlReactionId) {
		double reactionX = reactionVertexLayout.getX();
		double reactionY = reactionVertexLayout.getY();
		double width = 12.0;
		double height = 12.0;
		Glyph reactionGlyph = new Glyph();
		String reactionType = reactionVertexLayout.getReactionType();
		reactionGlyph.setClazz(GlyphClazz.PROCESS.toString());
		// TODO: Reactome knows more about process type than just "PROCESS", we can represent this in SBGN!
//		GlyphClazz.ASSOCIATION;// complex formation
		reactionGlyph.setId(newReactionGlyphId());
		
		Bbox bb = new Bbox();
		bb.setX((float) reactionX);
		bb.setY((float) reactionY);
		bb.setW((float) width);
		bb.setH((float) height);
		reactionGlyph.setBbox(bb);
		
		map.getGlyph().add(reactionGlyph);

		for (Edge edgeLayout: reactionVertexLayout.getEdgeLayouts()) {
			EntityVertex entityVertexLayout = edgeLayout.getEntityVertexLayout();
			Glyph speciesGlyph = addEntityVertexLayout(entityVertexLayout);

			addEdgeLayout(edgeLayout, reactionGlyph, speciesGlyph);
		}
	}
	
	private Glyph addEntityVertexLayout(EntityVertex entityVertexLayout) {
		Long entityVertexDbId = entityVertexLayout.getEntityVertexDbId();
		Glyph speciesGlyph = entityVertexLayoutSeen.get(entityVertexDbId);
		if (speciesGlyph != null)
			return speciesGlyph;
		
		String title = entityVertexLayout.getShortTitle();
		double x = entityVertexLayout.getX();
		double y = entityVertexLayout.getY();
		double width = entityVertexLayout.getWidth();
		double height = entityVertexLayout.getHeight();
		double speciesX = x - 12.0;
		if (speciesX < 0.0)
			speciesX = 0.0;
		speciesGlyph = new Glyph();
		String entityVertexType = entityVertexLayout.getType();
		String entityVertexSubType = entityVertexLayout.getSubType();
		String annotationText = null;
		String materialTypeLabelText = null;
		String conceptualTypeLabelText = null;
		if (entityVertexType.equals("complex")) {
			if (entityVertexSubType != null && entityVertexSubType.equals("multimer"))
				speciesGlyph.setClazz(GlyphClazz.COMPLEX_MULTIMER.toString());
			else
				speciesGlyph.setClazz(GlyphClazz.COMPLEX.toString());
			annotationText = stringListToString(entityVertexLayout.getComponentNames());
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
			annotationText = stringListToString(entityVertexLayout.getComponentNames());
		} else
			speciesGlyph.setClazz(GlyphClazz.UNSPECIFIED_ENTITY.toString());
		
		if (annotationText != null)
			speciesGlyph.getGlyph().add(createAnnotation(annotationText, width, height, x, y));
		if (materialTypeLabelText != null) 
			speciesGlyph.getGlyph().add(createMaterialTypeDecorator(materialTypeLabelText, width, height, x, y));
		if (conceptualTypeLabelText != null) 
			speciesGlyph.getGlyph().add(createConceptualTypeDecorator(conceptualTypeLabelText, width, height, x, y));
		
		Bbox bb = new Bbox();
		bb.setX((float) x);
		bb.setY((float) y);
		bb.setW((float) width);
		bb.setH((float) height);
		speciesGlyph.setBbox(bb);
		String speciesGlyphId = newSpeciesGlyphId();
		speciesGlyph.setId(speciesGlyphId);
		
		Label label = new Label();
		label.setText(title);
		speciesGlyph.setLabel(label);
		
		map.getGlyph().add(speciesGlyph);
		
		return speciesGlyph;
	}
	
	/**
	 * Add an annotation bubble to the glyph
	 * 
	 * @param labelText
	 */
	private Glyph createAnnotation(String labelText, double width, double height, double x, double y) {
		Label decoratorLabel = new Label();
		decoratorLabel.setText(labelText);
		Glyph annotation = new Glyph();
		annotation.setClazz(GlyphClazz.ANNOTATION.toString());
		annotation.setLabel(decoratorLabel);
		double annotationWidth = width/3.0;
		double annotationHeight = height/5.0;
		double annotationY = y + 1.1 * height;
		double annotationX = x + 1.1 * width;
		Bbox annotationBoundingBox = new Bbox();
		annotationBoundingBox.setX((float) annotationX);
		annotationBoundingBox.setY((float) annotationY);
		annotationBoundingBox.setW((float) annotationWidth);
		annotationBoundingBox.setH((float) annotationHeight);
		annotation.setBbox(annotationBoundingBox);
		return annotation;
	}
	
	/**
	 * Add unit of information to entities to get more specific types, e.g. MACROMOLECULE -> protein
	 * 
	 * @param labelText
	 */
	private Glyph createConceptualTypeDecorator(String labelText, double width, double height, double x, double y) {
		double decoratorX = x + 0.1 * width;
		return appendDecorator(labelText, width, height, decoratorX, y);
	}
	
	/**
	 * Add unit of information to entities to get more specific types, e.g. MACROMOLECULE -> protein
	 * 
	 * @param labelText
	 */
	private Glyph createMaterialTypeDecorator(String labelText, double width, double height, double x, double y) {
		double decoratorX = x + 0.7 * width;
		return appendDecorator(labelText, width, height, decoratorX, y);
	}
	
	/**
	 * Add unit of information to entities to get more specific types, e.g. MACROMOLECULE -> protein
	 * 
	 * @param labelText
	 */
	private Glyph appendDecorator(String labelText, double width, double height, double decoratorX, double y) {
		Label decoratorLabel = new Label();
		decoratorLabel.setText(labelText);
		Glyph decorator = new Glyph();
		decorator.setClazz(GlyphClazz.UNIT_OF_INFORMATION.toString());
		decorator.setLabel(decoratorLabel);
		double decoratorWidth = width/3.0;
		double decoratorHeight = height/5.0;
		double materialTypeY = y + height - (decoratorHeight * 0.5);
		Bbox decoratorBoundingBox = new Bbox();
		decoratorBoundingBox.setX((float) decoratorX);
		decoratorBoundingBox.setY((float) materialTypeY);
		decoratorBoundingBox.setW((float) decoratorWidth);
		decoratorBoundingBox.setH((float) decoratorHeight);
		decorator.setBbox(decoratorBoundingBox);
		return decorator;
	}

	private void addEdgeLayout(Edge edgeLayout, Glyph reactionGlyph, Glyph speciesGlyph) {
		String role = edgeLayout.getRole();
		Arc arc = new Arc();

		if (role == null) {
			System.err.println("LayoutGeneratorSBGN.addEdgeLayout: WARNING - role == null!");
			arc.setClazz(ArcClazz.UNKNOWN_INFLUENCE.toString());
		} else if (role.equals("input"))
			arc.setClazz(ArcClazz.CONSUMPTION.toString());
		else if (role.equals("output"))
			arc.setClazz(ArcClazz.PRODUCTION.toString());
		else
			arc.setClazz(ArcClazz.CATALYSIS.toString());
		
		String arcId = newArcId();
		arc.setId(arcId);

		arc.setSource(reactionGlyph);
		arc.setTarget(speciesGlyph);
		
		map.getArc().add(arc);
	}

	private void addCompartmentLayout(CompartmentVertex compartmentVertexLayout) {
		double x = compartmentVertexLayout.getX();
		double y = compartmentVertexLayout.getY();
		double width = compartmentVertexLayout.getWidth();
		double height = compartmentVertexLayout.getHeight();
		String title = compartmentVertexLayout.getTitle();
		
		Glyph compartmentGlyph = new Glyph();
		compartmentGlyph.setClazz(GlyphClazz.COMPARTMENT.toString());
		Bbox bb = new Bbox();
		bb.setX((float) x);
		bb.setY((float) y);
		bb.setW((float) width);
		bb.setH((float) height);
		compartmentGlyph.setBbox(bb);
		String compartmentGlyphId = newCompartmentGlyphId();
		compartmentGlyph.setId(compartmentGlyphId);
		
		// Add compartment ID as reference to entities
		Label label = new Label();
		label.setText(title);
		Bbox labelBoundingBox = new Bbox();
		labelBoundingBox.setX((float) (x + width * 0.1));
		labelBoundingBox.setY((float) (y + height * 0.9));
		labelBoundingBox.setW((float) (width * 0.3));
		labelBoundingBox.setH((float) (height * 0.2));
		label.setBbox(labelBoundingBox);
		compartmentGlyph.setLabel(label);
		
		map.getGlyph().add(compartmentGlyph);
	}
	
	public void dumpToFile() {
		dumpToFile("sbgn.xml");
	}
	
	public void dumpToFile(String filename) {
		dumpToFile(new File(filename));
	}
	
	public void dumpToFile(File file) {
		try {
			JAXBContext context = JAXBContext.newInstance("org.sbgn.bindings");
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(sbgn, file);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	public void addToModel() {
		try {
			JAXBContext context = JAXBContext.newInstance("org.sbgn.bindings");
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			marshaller.marshal(sbgn, doc);			

		    NodeList nodes = doc.getElementsByTagName("sbgn");
		    if (nodes == null)
	    		System.err.println("LayoutGeneratorSBGNPD.dumpToFile: WARNING - nodes is null");
		    else if (nodes.getLength() < 1)
	    		System.err.println("LayoutGeneratorSBGNPD.dumpToFile: WARNING - there are no nodes in the doc");
		    else {
		    	Node node = nodes.item(0);
		    	if (node == null)
		    		System.err.println("LayoutGeneratorSBGNPD.dumpToFile: WARNING - node is null");
		    	else {
			    	String nodeString = nodeToString(node);
			    	if (nodeString == null)
			    		System.err.println("LayoutGeneratorSBGNPD.dumpToFile: WARNING - nodeString is null");
			    	else if (nodeString.isEmpty())
			    		System.err.println("LayoutGeneratorSBGNPD.dumpToFile: WARNING - nodeString is empty");
			    	else
			    		model.appendAnnotation(nodeString);
		    	}
		    }
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private String nodeToString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException e) {
			e.printStackTrace(System.err);
		}
		return sw.toString();
	}
	
	public String stringListToString(List<String> stringList) {
		if (stringList == null)
			return null;
		String outputString = "";
		for (String string: stringList) {
			if (!outputString.isEmpty())
				outputString += "\n";
			outputString += "<p xmlns=\"http://www.w3.org/1999/xhtml\">" + string + "</p>";
		}
		
		return outputString;
	}
}
