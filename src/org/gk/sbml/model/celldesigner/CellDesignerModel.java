/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import javax.xml.stream.XMLStreamException;

import org.gk.sbml.model.elements.CVTerm;
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
import org.gk.sbml.simcd.SimCDAnnotation;
import org.gk.sbml.simcd.SimCDCompartment;
import org.gk.sbml.simcd.SimCDDocument;
import org.gk.sbml.simcd.SimCDModel;
import org.gk.sbml.simcd.SimCDReaction;
import org.gk.sbml.simcd.SimCDReader;
import org.gk.sbml.simcd.SimCDSBase;
import org.gk.sbml.simcd.SimCDSpecies;
import org.gk.sbml.simcd.SimCDWriter;
import org.gk.sbml.UrlPostConnector;
//import org.sbml.squeezer.KineticLawGenerator;


/**
 * Wraps org.sbml.jsbml.Model.
 * 
 * @author David Croft
 *
 */
public class CellDesignerModel extends CellDesignerSBase implements Model {
	private org.gk.sbml.simcd.SimCDModel sbmlElement = null;
	private ModelHistory modelHistory = null;
	public ModelComponentMaps modelComponentMaps; // multiple inheritance avoidance tactic

	public CellDesignerModel(org.gk.sbml.simcd.SimCDModel sbmlElement) {
		super((SimCDSBase) sbmlElement);
		this.sbmlElement = sbmlElement;
		modelComponentMaps = new ModelComponentMaps();
	}

	@Override
	public ModelComponentMaps getModelComponentMaps() {
		return modelComponentMaps;
	}

	@Override
	public int appendAnnotation(String nodeString) {
		org.gk.sbml.simcd.SimCDAnnotation annotation = (SimCDAnnotation) sbmlElement.getAnnotation();
		if (annotation == null)
			annotation = new org.gk.sbml.simcd.SimCDAnnotation(nodeString);
		else
			annotation.appendNoRDFAnnotation(nodeString);
		sbmlElement.setAnnotation(annotation);
		return 0;
	}


	@Override
	public int setMetaId(String metaId) {
		sbmlElement.setMetaId(metaId);
		return 0;
	}

	@Override
	public int addCVTerm(CVTerm cVTerm) {
		org.sbml.jsbml.CVTerm nativeCVTerm = ((CellDesignerCVTerm)cVTerm).getSbmlElement();
		sbmlElement.addCVTerm(nativeCVTerm);
		return 0;
	}

	@Override
	public int setNotes(String notes) {
		sbmlElement.setNotes(notes);
		return 0;
	}

	@Override
	public int setId(String id) {
		sbmlElement.setId(id);
		return 0;
	}

	@Override
	public String getId() {
		return sbmlElement.getId();
	}

	@Override
	public int setName(String name) {
		sbmlElement.setName(name);
		return 0;
	}

	@Override
	public Reaction createReaction(String reactionId) {
		Reaction reaction = modelComponentMaps.getReaction(reactionId);
		if (reaction != null)
			return reaction;
		org.gk.sbml.simcd.SimCDReaction sbmlReaction = (SimCDReaction) sbmlElement.createReaction();
		reaction = new CellDesignerReaction(sbmlReaction);
		modelComponentMaps.addReaction(reaction);
		return reaction;
	}

	@Override
	public boolean existsReaction(String reactionId) {
		return modelComponentMaps.existsReaction(reactionId);
	}

	@Override
	public Species createSpecies() {
		org.gk.sbml.simcd.SimCDSpecies sbmlSpecies = (SimCDSpecies) sbmlElement.createSpecies();
		String id = sbmlSpecies.getId();
		Species species = new CellDesignerSpecies(sbmlSpecies);
		modelComponentMaps.addSpecies(species);
		return species;
	}

	@Override
	public Compartment createCompartment(String compartmentId) {
		org.gk.sbml.simcd.SimCDCompartment sbmlCompartment = (SimCDCompartment) sbmlElement.createCompartment();
		sbmlCompartment.setId(compartmentId);
		Compartment compartment = new CellDesignerCompartment(sbmlCompartment);
		modelComponentMaps.addCompartment(compartment);
		return compartment;
	}

	@Override
	public Lib createLib() {
		return new CellDesignerLib();
	}

	@Override
	public Writer createWriter() {
		return new CellDesignerWriter();
	}

	@Override
	public Layout createLayout() {
//		org.gk.sbml.simcd.SimCDLayout sbmlLayout = sbmlElement.createLayout();
//		return new JsbmlLayout(sbmlLayout);
		return null;
	}

	@Override
	public ModelHistory getModelHistory() {
		if (modelHistory == null) {
			org.gk.sbml.simcd.SimCDHistory history = new org.gk.sbml.simcd.SimCDHistory();
			modelHistory = new CellDesignerModelHistory(history);
			sbmlElement.setHistory(history);
		}
		return modelHistory;
	}

	@Override
	public ModelCreator createModelCreator() {
		return new CellDesignerModelCreator();
	}

	@Override
	public Date createDate(java.util.Date date) {
		return new CellDesignerDate(date);
	}

	@Override
	public void setModelHistory(ModelHistory modelHistory) {
		sbmlElement.setHistory(((CellDesignerModelHistory)modelHistory).getSbmlElement());
	}

	@Override
	public boolean autogenerateKinetics() {
		return autogenerateKinetics(null);
	}

	@Override
	public boolean autogenerateKinetics(String autogenerateKineticServletUrl) {
// TODO: It would be cleaner if this could be done from within the running application, but SBMLsqueezer 1.3 uses an outdated and incompatible JSBML
//		try {
//			// run SBMLsqueezer to generate kinetics
//			KineticLawGenerator kineticLawGenerator = new KineticLawGenerator(sbmlElement, null);
//			sbmlElement = kineticLawGenerator.getMiniModel();
//			return true;
//		} catch (Throwable e) {
//			System.err.println("JsbmlModel.autogenerateKinetics: problem generating kinetic laws");
//			e.printStackTrace(System.err);
//		}
		
		// Serialize the current model into a string.
		SimCDWriter sbmlWriter = new SimCDWriter();
		String modelString = null;
		try {
			modelString = sbmlWriter.writeSBMLToString(sbmlElement.getSBMLDocument());
		} catch (Exception e) {
			System.err.println("JsbmlModel.autogenerateKinetics: WARNING - Could not write SBML to a string");
			e.printStackTrace(System.err);
			return false;
		}
		if (modelString == null) {
			System.err.println("JsbmlModel.autogenerateKinetics: WARNING - modelString == null!!");
			return false;
		}
		
		// Use a servlet that encapsulates SBMLsqueezer 1.3 to convert the current
		// model string into a new model string that contains kinetics
		if (autogenerateKineticServletUrl == null)
			autogenerateKineticServletUrl = "http://www.reactome.org/SBMLsqueezer/squeezer/SBMLSqueezerServlet";
		UrlPostConnector uploadUrlPostConnector = new UrlPostConnector(autogenerateKineticServletUrl);
		uploadUrlPostConnector.addParameter("model", modelString);
		String response = uploadUrlPostConnector.connect();
		if (response == null || response.isEmpty()) {
			System.err.println("JsbmlModel.autogenerateKinetics: WARNING - response is null or empty!!");
			return false;
		}
		response = response.replaceAll("<!--.*Created by SBMLsqueezer.*-->", "");
		
		// Convert the string containing the new model with kinetics into
		// a valid Model object, and store it.
		SimCDReader sbmlReader = new SimCDReader();
		try {
			SimCDDocument documentWithKinetics = (SimCDDocument) sbmlReader.readSBMLFromString(response);
			org.gk.sbml.simcd.SimCDModel modelWithKinetics = (SimCDModel) documentWithKinetics.getModel();
			SimCDDocument document = (SimCDDocument) sbmlElement.getParentSBMLObject();
			document.setModel(modelWithKinetics);
			sbmlElement = modelWithKinetics;
			appendAnnotation("<p xmlns=\"http://www.w3.org/1999/xhtml\">Kinetics generated by SBMLsqueezer 1.3</p>");
		} catch (XMLStreamException e) {
			System.err.println("JsbmlModel.autogenerateKinetics: WARNING - Could not get SBML from response");
			e.printStackTrace(System.err);
			return false;
		}
		
		return true;
	}
}
