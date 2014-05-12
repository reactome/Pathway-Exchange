/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

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
 * Wraps org.sbml.libsbml.Compartment.
 * 
 * @author David Croft
 *
 */
public class LibsbmlLayout implements Layout {
	private org.sbml.libsbml.Layout sbmlElement = null;

	public LibsbmlLayout(org.sbml.libsbml.Layout sbmlElement) {
		this.sbmlElement = sbmlElement;
	}

	public org.sbml.libsbml.Layout getSbmlElement() {
		return sbmlElement;
	}

	@Override
	public int setId(String id) {
		return sbmlElement.setId(id);
	}

	@Override
	public int setMetaId(String metaId) {
		return sbmlElement.setMetaId(metaId);
	}

	@Override
	public int setName(String name) {
		return sbmlElement.setName(name);
	}

	@Override
	public int addCVTerm(CVTerm cVTerm) {
		return sbmlElement.addCVTerm(((LibsbmlCVTerm)cVTerm).getSbmlElement());
	}

	@Override
	public String getId() {
		return sbmlElement.getId();
	}

	@Override
	public TextGlyph createTextGlyph() {
		return new LibsbmlTextGlyph(sbmlElement.createTextGlyph());
	}

	@Override
	public void setDimensions(Dimensions dimensions) {
		org.sbml.libsbml.Dimensions sbmlElementDimensions = ((LibsbmlDimensions)dimensions).getSbmlElement();
		sbmlElement.setDimensions(sbmlElementDimensions);
	}

	@Override
	public Dimensions createDimensions(double width, double height) {
		return new LibsbmlDimensions(width, height);
	}

	@Override
	public Point createPoint(double x, double y) {
		return new LibsbmlPoint(x, y);
	}

	@Override
	public BoundingBox createBoundingBox(String id, double x, double y, double width, double height) {
		return new LibsbmlBoundingBox(id, x, y, width, height);
	}

	@Override
	public ReactionGlyph createReactionGlyph() {
		org.sbml.libsbml.ReactionGlyph sbmlElementReactionGlyph = sbmlElement.createReactionGlyph();
		return new LibsbmlReactionGlyph(sbmlElementReactionGlyph);
	}

	@Override
	public SpeciesGlyph createSpeciesGlyph() {
		org.sbml.libsbml.SpeciesGlyph sbmlElementSpeciesGlyph = sbmlElement.createSpeciesGlyph();
		return new LibsbmlSpeciesGlyph(sbmlElementSpeciesGlyph);
	}

	@Override
	public CompartmentGlyph createCompartmentGlyph() {
		org.sbml.libsbml.CompartmentGlyph sbmlElementCompartmentGlyph = sbmlElement.createCompartmentGlyph();
		return new LibsbmlCompartmentGlyph(sbmlElementCompartmentGlyph);
	}
}
