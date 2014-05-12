/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.generators;

import java.util.HashMap;

import org.gk.sbml.Utils;
import org.gk.sbml.layout.elements.CompartmentVertex;
import org.gk.sbml.layout.elements.Diagram;
import org.gk.sbml.layout.elements.Edge;
import org.gk.sbml.layout.elements.EntityVertex;
import org.gk.sbml.layout.elements.Reaction;
import org.gk.sbml.layout.elements.ReactionVertex;
import org.gk.sbml.model.elements.CompartmentGlyph;
import org.gk.sbml.model.elements.CubicBezier;
import org.gk.sbml.model.elements.Curve;
import org.gk.sbml.model.elements.Layout;
import org.gk.sbml.model.elements.LineSegment;
import org.gk.sbml.model.elements.ReactionGlyph;
import org.gk.sbml.model.elements.SpeciesGlyph;
import org.gk.sbml.model.elements.SpeciesReferenceGlyph;
import org.gk.sbml.model.elements.TextGlyph;

/**
 * Layout generator using libSBML extension
 * 
 * @author David Croft
 *
 */
public class LayoutGeneratorExtension extends LayoutGenerator {
	private Layout layout;
	private HashMap<Long,String> entityVertexLayoutSeen = new HashMap<Long,String>();
	
	public void run(Diagram modelLayout) {
		layout = model.createLayout();
		if (layout == null) {
			System.err.println("LayoutGeneratorExtension.modelLayout: WARNING - layout extension not available, skipping");
			return;
		}
		layout.setId("Layout_1");

		String title = modelLayout.getTitle();

		layout.setDimensions(layout.createDimensions(modelLayout.getWidth(), modelLayout.getHeight()));

		TextGlyph textGlyph=layout.createTextGlyph();
		textGlyph.setBoundingBox(layout.createBoundingBox(newBoundingBoxId(), 50.0, 15.0, 150.0, 70.0));
		textGlyph.setText(title);

		for (Reaction reactionLayout: modelLayout.getReactionLayouts())
			addReactionLayout(reactionLayout);
		
		for (CompartmentVertex compartmentVertexLayout: modelLayout.getCompartmentVertexLayouts())
			addCompartmentLayout(compartmentVertexLayout);
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
		String reactionTitle = reactionVertexLayout.getShortTitle();
		ReactionGlyph reactionGlyph = layout.createReactionGlyph();
		reactionGlyph.setId(sbmlReactionId);
		reactionGlyph.setReactionId(sbmlReactionId);
		Curve reactionCurve = reactionGlyph.getCurve();
		LineSegment ls = reactionCurve.createLineSegment();
		if (reactionX < 0.0 || reactionY < 0.0) {
			ls.setStart(layout.createPoint(5.0, 0.0));
			ls.setEnd(layout.createPoint(5.0, 10.0));
		} else {
			double yStart = reactionY - 10.0;
			if (yStart < 5.0)
				yStart = 5.0;
			double yEnd = yStart + 10.0;
			ls.setStart(layout.createPoint(reactionX, yStart));
			ls.setEnd(layout.createPoint(reactionX, yEnd));
		}
		TextGlyph textGlyph=layout.createTextGlyph();
		textGlyph.setBoundingBox(layout.createBoundingBox(newBoundingBoxId(), reactionX, reactionY, 0.0, 0.0));
		textGlyph.setOriginOfTextId(sbmlReactionId);
		textGlyph.setGraphicalObjectId(sbmlReactionId);
		textGlyph.setText(reactionTitle);
		
		for (Edge edgeLayout: reactionVertexLayout.getEdgeLayouts()) {
			EntityVertex entityVertexLayout = edgeLayout.getEntityVertexLayout();
			String speciesGlyphId = addEntityVertexLayout(entityVertexLayout);

			SpeciesReferenceGlyph speciesReferenceGlyph = reactionGlyph.createSpeciesReferenceGlyph();
			addEdgeLayout(edgeLayout, speciesReferenceGlyph, speciesGlyphId);
		}
	}
	
	private String addEntityVertexLayout(EntityVertex entityVertexLayout) {
		Long entityVertexDbId = entityVertexLayout.getEntityVertexDbId();
		String speciesGlyphId = entityVertexLayoutSeen.get(entityVertexDbId);
		if (speciesGlyphId != null)
			return speciesGlyphId;
		
		String sbmlSpeciesId = entityVertexLayout.getSbmlSpeciesId();
		String title = entityVertexLayout.getShortTitle();
		double x = entityVertexLayout.getX();
		double y = entityVertexLayout.getY();
		double width = entityVertexLayout.getWidth();
		double height = entityVertexLayout.getHeight();
		double speciesX = x - 12.0;
		if (speciesX < 0.0)
			speciesX = 0.0;
		SpeciesGlyph speciesGlyph = layout.createSpeciesGlyph();
		speciesGlyph.setSpeciesId(sbmlSpeciesId);
		speciesGlyphId = newSpeciesGlyphId();
		entityVertexLayoutSeen.put(entityVertexDbId, speciesGlyphId);
		speciesGlyph.setId(speciesGlyphId);
		speciesGlyph.setBoundingBox(layout.createBoundingBox(newBoundingBoxId(), speciesX, y, width + 12.0, height));
		TextGlyph textGlyph=layout.createTextGlyph();
		textGlyph.setBoundingBox(layout.createBoundingBox(newBoundingBoxId(), x, y, width, height));
		textGlyph.setOriginOfTextId(speciesGlyph.getId());
		textGlyph.setGraphicalObjectId(speciesGlyph.getId());
		textGlyph.setText(title);
		
		return speciesGlyphId;
	}

	private void addEdgeLayout(Edge edgeLayout, SpeciesReferenceGlyph speciesReferenceGlyph, String speciesGlyphId) {
		double startX = edgeLayout.getStartX();
		double startY = edgeLayout.getStartY();
		double entityX = edgeLayout.getEndX();
		double entityY = edgeLayout.getEndY();
		String role = edgeLayout.getRole();
		speciesReferenceGlyph.setId(newSpeciesReferenceGlyphId());
		speciesReferenceGlyph.setSpeciesGlyphId(speciesGlyphId);
		speciesReferenceGlyph.setSpeciesReferenceId(edgeLayout.getSbmlSpeciesReferenceId());
		if (role == null)
			speciesReferenceGlyph.setSpeciesRoleUndefined();
		else if (role.equals("input"))
			speciesReferenceGlyph.setSpeciesRoleSubstrate();
		else if (role.equals("output"))
			speciesReferenceGlyph.setSpeciesRoleProduct();
		else
			speciesReferenceGlyph.setSpeciesRoleModifier();
		Curve speciesReferenceCurve1=speciesReferenceGlyph.getCurve();
		CubicBezier cb=speciesReferenceCurve1.createCubicBezier();
		cb.setStart(layout.createPoint(entityX, entityY));
		cb.setBasePoint1(layout.createPoint(entityX, (entityY + startY) / 2.0));
		cb.setBasePoint2(layout.createPoint(entityX, (entityY + startY) / 2.0));
		cb.setEnd(layout.createPoint(startX, startY));
	}

	private void addCompartmentLayout(CompartmentVertex compartmentVertexLayout) {
		double x = compartmentVertexLayout.getX();
		double y = compartmentVertexLayout.getY();
		double width = compartmentVertexLayout.getWidth();
		double height = compartmentVertexLayout.getHeight();
		double textX = compartmentVertexLayout.getTextX();
		double textY = compartmentVertexLayout.getTextY();
		String title = compartmentVertexLayout.getTitle();
		Long compartmentDbId = compartmentVertexLayout.getCompartmentDbId();
		String compartmentId = Utils.getCompartmentIdFromCompartmentDbID(compartmentDbId);
		CompartmentGlyph compartmentGlyph=layout.createCompartmentGlyph();
		compartmentGlyph.setId(newCompartmentGlyphId());
		compartmentGlyph.setCompartmentId(compartmentId);
		compartmentGlyph.setBoundingBox(layout.createBoundingBox(newBoundingBoxId(), x, y, width, height));
		TextGlyph textGlyph=layout.createTextGlyph();
		textGlyph.setBoundingBox(layout.createBoundingBox(newBoundingBoxId(), textX, textY, 12.0, 12.0));
		textGlyph.setText(title);
	}
}
