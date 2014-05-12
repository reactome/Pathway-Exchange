/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.Compartment;
import org.gk.sbml.model.elements.Date;
import org.gk.sbml.model.elements.Layout;
import org.gk.sbml.model.elements.Lib;
import org.gk.sbml.model.elements.Model;
import org.gk.sbml.model.elements.ModelComponentMaps;
import org.gk.sbml.model.elements.ModelCreator;
import org.gk.sbml.model.elements.ModelHistory;
import org.gk.sbml.model.elements.Reaction;
import org.gk.sbml.model.elements.Species;
import org.gk.sbml.model.elements.Writer;
import org.sbml.libsbml.XMLNode;

/**
 * Wraps org.sbml.libsbml.Model.
 * 
 * @author David Croft
 *
 */
public class LibsbmlModel extends LibsbmlSBase implements Model {
	private org.sbml.libsbml.Model sbmlElement = null;
	private ModelHistory modelHistory = null;
	public ModelComponentMaps modelComponentMaps; // multiple inheritance avoidance tactic

	public LibsbmlModel(org.sbml.libsbml.Model sbmlElement) {
		super(sbmlElement);
		this.sbmlElement = sbmlElement;
		modelComponentMaps = new ModelComponentMaps();
	}

	@Override
	public ModelComponentMaps getModelComponentMaps() {
		return modelComponentMaps;
	}

	@Override
	public int appendAnnotation(String nodeString) {
		XMLNode annotation = sbmlElement.getAnnotation();
		if (annotation == null)
			return sbmlElement.setAnnotation(nodeString);
		else 
			return sbmlElement.appendAnnotation(nodeString);
	}

	@Override
	public int setNotes(String notes) {
		return sbmlElement.setNotes(notes);
	}

	@Override
	public int setId(String id) {
		return sbmlElement.setId(id);
	}

	@Override
	public String getId() {
		return sbmlElement.getId();
	}

	@Override
	public int setName(String name) {
		return sbmlElement.setName(name);
	}

	@Override
	public Reaction createReaction(String reactionId) {
		Reaction reaction = modelComponentMaps.getReaction(reactionId);
		if (reaction != null)
			return reaction;
		org.sbml.libsbml.Reaction sbmlReaction = sbmlElement.createReaction();
		reaction = new LibsbmlReaction(sbmlReaction);
		modelComponentMaps.addReaction(reaction);
		return reaction;
	}

	@Override
	public boolean existsReaction(String reactionId) {
		return modelComponentMaps.existsReaction(reactionId);
	}

	@Override
	public Species createSpecies() {
		org.sbml.libsbml.Species sbmlSpecies = sbmlElement.createSpecies();
		String id = sbmlSpecies.getId();
		Species species = new LibsbmlSpecies(sbmlSpecies);
		modelComponentMaps.addSpecies(species);
		return species;
	}

	@Override
	public Compartment createCompartment(String compartmentId) {
		org.sbml.libsbml.Compartment sbmlCompartment = sbmlElement.createCompartment();
		sbmlCompartment.setId(compartmentId);
		Compartment compartment = new LibsbmlCompartment(sbmlCompartment);
		modelComponentMaps.addCompartment(compartment);
		return compartment;
	}

	@Override
	public Lib createLib() {
		return new LibsbmlLib();
	}

	@Override
	public Writer createWriter() {
		return new LibsbmlWriter();
	}

	@Override
	public Layout createLayout() {
		org.sbml.libsbml.Layout sbmlLayout = sbmlElement.createLayout();
		return new LibsbmlLayout(sbmlLayout);
	}

	@Override
	public ModelHistory getModelHistory() {
		if (modelHistory == null) {
			org.sbml.libsbml.ModelHistory history = new org.sbml.libsbml.ModelHistory();
			modelHistory = new LibsbmlModelHistory(history);
			sbmlElement.setModelHistory(history);
		}
		return modelHistory;
	}

	@Override
	public ModelCreator createModelCreator() {
		return new LibsbmlModelCreator();
	}

	@Override
	public Date createDate(java.util.Date date) {
		return new LibsbmlDate(date);
	}

	@Override
	public void setModelHistory(ModelHistory modelHistory) {
		sbmlElement.setModelHistory(((LibsbmlModelHistory)modelHistory).getSbmlElement());
	}

	@Override
	public boolean autogenerateKinetics() {
		System.err.println("JsbmlModel.autogenerateKinetics: SBMLsqueezer not available under libSBML");
		return false;
	}

	@Override
	public boolean autogenerateKinetics(String autogenerateKineticServletUrl) {
		System.err.println("JsbmlModel.autogenerateKinetics: SBMLsqueezer not available under libSBML");
		return false;
	}
}
