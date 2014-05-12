/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import org.gk.sbml.model.elements.BoundingBox;
import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.CompartmentGlyph;
import org.gk.sbml.model.elements.Dimensions;
import org.gk.sbml.model.elements.Point;
import org.gk.sbml.model.elements.Layout;
import org.gk.sbml.model.elements.ReactionGlyph;
import org.gk.sbml.model.elements.SpeciesGlyph;
import org.gk.sbml.model.elements.TextGlyph;


/**
 * Wraps org.sbml.jsbml.Compartment.
 * 
 * The layout extension is not available in the current JSBML.
 * 
 * @author David Croft
 *
 */
public class JsbmlLayout implements Layout {
//	private org.sbml.jsbml.Layout sbmlElement = null;
//
//	public JsbmlLayout(org.sbml.jsbml.Layout sbmlElement) {
//		this.sbmlElement = sbmlElement;
//	}
//
//	public org.sbml.jsbml.Layout getSbmlElement() {
//		return sbmlElement;
//	}

	@Override
	public int setId(String id) {
//		return sbmlElement.setId(id);
		return 0;
	}

	@Override
	public int setMetaId(String metaId) {
//		return sbmlElement.setMetaId(metaId);
		return 0;
	}

	@Override
	public int setName(String name) {
//		return sbmlElement.setName(name);
		return 0;
	}

	@Override
	public int addCVTerm(CVTerm cVTerm) {
//		return sbmlElement.addCVTerm(((JsbmlCVTerm)cVTerm).getSbmlElement());
		return 0;
	}

	@Override
	public String getId() {
//		return sbmlElement.getId();
		return null;
	}

	@Override
	public TextGlyph createTextGlyph() {
//		return new JsbmlTextGlyph(sbmlElement.createTextGlyph());
		return null;
	}

	@Override
	public void setDimensions(Dimensions dimensions) {
//		org.sbml.jsbml.Dimensions sbmlElementDimensions = ((JsbmlDimensions)dimensions).getSbmlElement();
//		sbmlElement.setDimensions(sbmlElementDimensions);
	}

	@Override
	public Dimensions createDimensions(double width, double height) {
//		return new JsbmlDimensions(width, height);
		return null;
	}

	@Override
	public Point createPoint(double x, double y) {
//		return new JsbmlPoint(x, y);
		return null;
	}

	@Override
	public BoundingBox createBoundingBox(String id, double x, double y, double width, double height) {
//		return new JsbmlBoundingBox(id, x, y, width, height);
		return null;
	}

	@Override
	public ReactionGlyph createReactionGlyph() {
//		org.sbml.jsbml.ReactionGlyph sbmlElementReactionGlyph = sbmlElement.createReactionGlyph();
//		return new JsbmlReactionGlyph(sbmlElementReactionGlyph);
		return null;
	}

	@Override
	public SpeciesGlyph createSpeciesGlyph() {
//		org.sbml.jsbml.SpeciesGlyph sbmlElementSpeciesGlyph = sbmlElement.createSpeciesGlyph();
//		return new JsbmlSpeciesGlyph(sbmlElementSpeciesGlyph);
		return null;
	}

	@Override
	public CompartmentGlyph createCompartmentGlyph() {
//		org.sbml.jsbml.CompartmentGlyph sbmlElementCompartmentGlyph = sbmlElement.createCompartmentGlyph();
//		return new JsbmlCompartmentGlyph(sbmlElementCompartmentGlyph);
		return null;
	}
}
