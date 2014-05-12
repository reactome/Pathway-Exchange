/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.jsbml;

import java.lang.reflect.Method;

import javax.xml.stream.XMLStreamException;

import org.gk.sbml.UrlPostConnector;
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
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.History;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;


/**
 * Wraps org.sbml.jsbml.Model.
 * 
 * @author David Croft
 *
 */
public class JsbmlModel extends JsbmlSBase implements Model {
	private org.sbml.jsbml.Model sbmlElement = null;
	private ModelHistory modelHistory = null;
	public ModelComponentMaps modelComponentMaps; // multiple inheritance avoidance tactic

	public JsbmlModel(org.sbml.jsbml.Model sbmlElement) {
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
		Annotation annotation = sbmlElement.getAnnotation();
		if (annotation == null)
			annotation = new Annotation(nodeString);
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
		org.sbml.jsbml.CVTerm nativeCVTerm = ((JsbmlCVTerm)cVTerm).getSbmlElement();
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
		org.sbml.jsbml.Reaction sbmlReaction = sbmlElement.createReaction();
		reaction = new JsbmlReaction(sbmlReaction);
		modelComponentMaps.addReaction(reaction);
		return reaction;
	}

	@Override
	public boolean existsReaction(String reactionId) {
		return modelComponentMaps.existsReaction(reactionId);
	}

	@Override
	public Species createSpecies() {
		org.sbml.jsbml.Species sbmlSpecies = sbmlElement.createSpecies();
		String id = sbmlSpecies.getId();
		Species species = new JsbmlSpecies(sbmlSpecies);
		modelComponentMaps.addSpecies(species);
		return species;
	}

	@Override
	public Compartment createCompartment(String compartmentId) {
		org.sbml.jsbml.Compartment sbmlCompartment = sbmlElement.createCompartment();
		sbmlCompartment.setId(compartmentId);
		Compartment compartment = new JsbmlCompartment(sbmlCompartment);
		modelComponentMaps.addCompartment(compartment);
		return compartment;
	}

	@Override
	public Lib createLib() {
		return new JsbmlLib();
	}

	@Override
	public Writer createWriter() {
		return new JsbmlWriter();
	}

	@Override
	public Layout createLayout() {
//		org.sbml.jsbml.Layout sbmlLayout = sbmlElement.createLayout();
//		return new JsbmlLayout(sbmlLayout);
		return null;
	}

	@Override
	public ModelHistory getModelHistory() {
		if (modelHistory == null) {
			try {
				History history = new History();
				modelHistory = new JsbmlModelHistory(history);
				sbmlElement.setHistory(history);
			} catch (Exception e) {
				System.err.println("JsbmlModel.getModelHistory: problem setting history");
				e.printStackTrace();
			} catch (NoSuchMethodError e) {
				System.err.println("JsbmlModel.getModelHistory: problem with method sbmlElement.setHistory");
				e.printStackTrace();
			}
		}
		return modelHistory;
	}

	@Override
	public ModelCreator createModelCreator() {
		return new JsbmlModelCreator();
	}

	@Override
	public Date createDate(java.util.Date date) {
		return new JsbmlDate(date);
	}

	@Override
	public void setModelHistory(ModelHistory modelHistory) {
		sbmlElement.setHistory(((JsbmlModelHistory)modelHistory).getSbmlElement());
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
		SBMLWriter sbmlWriter = new SBMLWriter();
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
		SBMLReader sbmlReader = new SBMLReader();
		try {
			SBMLDocument documentWithKinetics = sbmlReader.readSBMLFromString(response);
			org.sbml.jsbml.Model modelWithKinetics = documentWithKinetics.getModel();
			SBMLDocument document = (SBMLDocument) sbmlElement.getParentSBMLObject();
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
