/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.sbml.model.elements.Compartment;
import org.gk.sbml.model.elements.Document;
import org.gk.sbml.model.elements.DocumentSingleton;
import org.gk.sbml.model.elements.Lib;
import org.gk.sbml.model.elements.Model;
import org.gk.sbml.model.elements.ModifierSpeciesReference;
import org.gk.sbml.model.elements.Reaction;
import org.gk.sbml.model.elements.SBO;
import org.gk.sbml.model.elements.Species;
import org.gk.sbml.model.elements.SpeciesReference;
import org.gk.sbml.model.elements.Writer;

/**
 * Build SBML from Reactome.  This is a kind of base class, it uses a bunch of interfaces
 * and abstract classes that act as placeholders for elements with the functionality of
 * libSBML or similar.  It allows you to wrap these kinds of SBML construction packages,
 * so that you can use more than one of them, e.g. you might want to use JSBML on
 * Wednesdays and libSBML on alternating Tuesdays and Thursdays.
 * 
 * @author David Croft
 *
 */
public abstract class SBMLBuilder {
	private MySQLAdaptor dbAdaptor;
	private DocumentSingleton documentSingleton;
	private Document document;
	private Model model = null;
	private int currentMetaId;
	private int level = 2;
	private int version = 4;	

	public SBMLBuilder(DocumentSingleton documentSingleton) {
		super();
		this.documentSingleton = documentSingleton;
		currentMetaId = 0;
	}
	
	public void setDatabaseAdaptor(MySQLAdaptor dbAdaptor) {
		this.dbAdaptor = dbAdaptor;
	}

	public MySQLAdaptor getDatabaseAdaptor() {
		return this.dbAdaptor;
	}

	public void setLevel(int level) {
		this.level = level;
		document = documentSingleton.getDocument(level, version);
	}

	public void setVersion(int version) {
		this.version = version;
		document = documentSingleton.getDocument(level, version);
	}

	public Document getDocument() {
		if (document == null)
			document = documentSingleton.createNewDocument(level, version);
		
		return document;
	}

	public Model getModel() {
		if (model == null)
			model = getDocument().createModel();

		return model;
	}

	public String getCurrentMetaIdString() {
		String currentMetaIdString = "metaid_" + this.currentMetaId;
		this.currentMetaId++;
		return currentMetaIdString;
	}

	public void buildFromPathwayReactionHash(Map<Long,List<Long>> pathwayReactionHash) {
		Model model = getModel();
		if (model == null) {
			System.err.println("SBMLBuilder.convertReactions: WARNING - model is null, aborting!");
			return;
		}
		model.setMetaId(getCurrentMetaIdString());

		CVTermBuilder cvTermBuilder = new CVTermBuilder(model, model);

		GKInstance pathway;
		String id = "pathway";
		String title = "";
		HashMap<Long,GKInstance> reactionHash = new HashMap<Long,GKInstance>();
		try {
			int pathwayCounter = 0;
			Set<Long> pathwayDbIDs = pathwayReactionHash.keySet();
			int totalPathwayCount = pathwayDbIDs.size();
			
//			System.err.println("SBMLBuilder.convertReactions: totalPathwayCount=" + totalPathwayCount);
			
			List<GKInstance> pathways = new ArrayList<GKInstance>();
			
			for (Long pathwayDbID: pathwayDbIDs) {
				if (pathwayDbID == null) {
					System.err.println("SBMLBuilder.convertReactions: WARNING - pathwayDbID is null, skipping!");
					continue;
				}
//				if (pathwayCounter % 10 == 0)
//					System.err.println("SBMLBuilder.convertReactions: pathway DB_ID=" + pathwayDbID + "(" + (100.0*pathwayCounter)/totalPathwayCount + "%)");
				pathwayCounter++;
				pathway = dbAdaptor.fetchInstance(pathwayDbID);
				if (pathway == null) {
					System.err.println("SBMLBuilder.convertReactions: WARNING - no pathway found for DB_ID=" + pathwayDbID);
					continue;
				}
				if (pathwayCounter < 3) {
					id += "_" + pathwayDbID;
					if (!title.isEmpty())
						title += ", ";
					title += pathway.getDisplayName();
				} else if (pathwayCounter == 3)
					title += ", etc.";
				
				pathways.add(pathway);
				
				for (Long reactionDbId: pathwayReactionHash.get(pathwayDbID)) {
					GKInstance reaction = dbAdaptor.fetchInstance(reactionDbId);
					if (reaction == null) {
						System.err.println("SBMLBuilder.convertReactions: WARNING - no reaction found for DB_ID=" + reactionDbId);
						continue;
					}
					if (!reaction.getSchemClass().isa("ReactionlikeEvent")) {
						System.err.println("SBMLBuilder.convertReactions: WARNING - DB_ID is not a reaction!");
						continue;
					}
					reactionHash.put(reactionDbId, reaction);
				}
				
				List<GKInstance> literatureReferences = pathway.getAttributeValuesList("literatureReference");
				if (literatureReferences != null)
					for (GKInstance literatureReference: literatureReferences) {
						if (literatureReference.getSchemClass().isValidAttribute("pubMedIdentifier")) {
							String resource = "urn:miriam:pubmed:" + literatureReference.getAttributeValue("pubMedIdentifier");
							cvTermBuilder.addResourcesBqbIsDescribedBy(resource);
						}
						if (literatureReference.getSchemClass().isValidAttribute("ISBN")) {
							String resource = "urn:miriam:isbn:" + literatureReference.getAttributeValue("ISBN");
							cvTermBuilder.addResourcesBqbIsDescribedBy(resource);
						}
					}
				GKInstance stableIdentifier = (GKInstance)pathway.getAttributeValue("stableIdentifier");
				if (stableIdentifier != null) {
					String resource = "urn:miriam:reactome:" + stableIdentifier.getAttributeValue("identifier");
					cvTermBuilder.addResourcesBqbIs(resource);
				}
			}
			
			NotesBuilder.appendInstanceSummationsToSBase(model, pathways);
			HistoryBuilder.appendHistoryToModel(model, pathways);
			String sbmlEngineName = getClass().getSimpleName().replaceAll("SBMLBuilder$", "");
			model.appendAnnotation("<p xmlns=\"http://www.w3.org/1999/xhtml\">SBML engine: " + sbmlEngineName + "</p>");
			
			model.setId(id);
			model.setName(title);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		
		Collection<GKInstance> reactionlikEvents = reactionHash.values();
		int totalReactionCount = reactionlikEvents.size();
		int reactionCounter = 0;
		double percentDone = 0.0;
		for (GKInstance reactionlikEvent: reactionlikEvents) {
			if (reactionCounter%10 == 0) {
				percentDone = (100.0*reactionCounter)/totalReactionCount;
//				System.err.println("SBMLBuilder.convertReactions: reaction DB_ID=" + reactionlikEvent.getDBID() + "(" + percentDone + "%)");
			}

			String reactionId = Utils.getReactionIdFromReaction(reactionlikEvent);
			if (model.getModelComponentMaps().existsReaction(reactionId))
				continue;

			if (!Utils.hasValidInputsOrOutputs(reactionlikEvent))
				continue;

			Reaction sbmlReaction = buildSbmlReaction(reactionlikEvent);
			reactionCounter++;
		}
		
		cvTermBuilder.commit();
	}
	
	private Reaction buildSbmlReaction(GKInstance reactionlikEvent) {
		Model model = getModel();
		String reactionId = Utils.getReactionIdFromReaction(reactionlikEvent);
		if (model.existsReaction(reactionId))
			return model.createReaction(reactionId);
		Reaction reaction = model.createReaction(reactionId);
		String displayName = reactionlikEvent.getDisplayName();
		reaction.setId(reactionId);
		reaction.setMetaId(getCurrentMetaIdString());
		reaction.setName(displayName);
		reaction.setReversible(false);
		
		CVTermBuilder cvTermBuilder = new CVTermBuilder(model, reaction);

		try {
			NotesBuilder.appendInstanceSummationsToSBase(reaction, reactionlikEvent);

			// Add inputs
			List<GKInstance> inputs = reactionlikEvent.getAttributeValuesList("input");
			if (inputs != null) {
				HashMap<Long,SpeciesReference> speciesReferenceHash = new HashMap<Long,SpeciesReference>();
				for (GKInstance entity: inputs) {
					handleSpeciesReference(speciesReferenceHash, reactionlikEvent, reaction, entity, true);
				}
			}

			// Add outputs
			List<GKInstance> outputs = reactionlikEvent.getAttributeValuesList("output");
			if (outputs != null) {
				HashMap<Long,SpeciesReference> speciesReferenceHash = new HashMap<Long,SpeciesReference>();
				for (GKInstance entity: outputs) {
					handleSpeciesReference(speciesReferenceHash, reactionlikEvent, reaction, entity, false);
				}
			}

			// Catalysts
			List<GKInstance> catalystActivities = reactionlikEvent.getAttributeValuesList("catalystActivity");
			if (catalystActivities != null) {
				HashMap<Long,ModifierSpeciesReference> modifierSpeciesReferenceHash = new HashMap<Long,ModifierSpeciesReference>();
				for (GKInstance catalystActivity: catalystActivities) {
					GKInstance entity = (GKInstance)catalystActivity.getAttributeValue("physicalEntity");
					handleModifierSpeciesReference(modifierSpeciesReferenceHash, reactionlikEvent, reaction, entity);
				}
				
				// Get GO term (if available) from catalyst,
				// if a single catalyst is present.
				if (catalystActivities.size() == 1) {
					GKInstance catalystActivity = catalystActivities.get(0);
					List activitys = catalystActivity.getAttributeValuesList("activity");
					if (activitys != null  && activitys.size() == 1) {
						GKInstance activity = (GKInstance)activitys.get(0);
						String accession = (String)activity.getAttributeValue("accession");
						if (accession != null) {
							String resource = "urn:miriam:obo.go:GO%3A" + accession;
							cvTermBuilder.addResourcesBqbIs(resource);
						}
					}
				}
			}
			
			List<String> ecNumbers = ECNumberExtractor.extractECNumbersFromReactionlikeEvent(reactionlikEvent);
			for (String ecNumber: ecNumbers) {
				String resource = "urn:miriam:ec:" + ecNumber;
				cvTermBuilder.addResourcesBqbIs(resource);
			}

			// Add literature references
			List<GKInstance> literatureReferences = reactionlikEvent.getAttributeValuesList("literatureReference");
			if (literatureReferences != null)
				for (GKInstance literatureReference: literatureReferences) {
					if (literatureReference.getSchemClass().isValidAttribute("pubMedIdentifier")) {
						String resource = "urn:miriam:pubmed:" + literatureReference.getAttributeValue("pubMedIdentifier");
						cvTermBuilder.addResourcesBqbIsDescribedBy(resource);
					}
					if (literatureReference.getSchemClass().isValidAttribute("ISBN")) {
						String resource = "urn:miriam:isbn:" + literatureReference.getAttributeValue("ISBN");
						cvTermBuilder.addResourcesBqbIsDescribedBy(resource);
					}
				}

			// Add GO biological process
			List<GKInstance> goBiologicalProcesses = reactionlikEvent.getAttributeValuesList("goBiologicalProcess");
			if (goBiologicalProcesses != null)
				for (GKInstance goBiologicalProcess: goBiologicalProcesses) {
					String accession = (String)goBiologicalProcess.getAttributeValue("accession");
					if (accession != null) {
						String resource = "urn:miriam:obo.go:GO%3A" + accession;
						cvTermBuilder.addResourcesBqbIs(resource);
					}
				}

			GKInstance stableIdentifier = (GKInstance)reactionlikEvent.getAttributeValue("stableIdentifier");
			if (stableIdentifier != null) {
				String resource = "urn:miriam:reactome:" + stableIdentifier.getAttributeValue("identifier");
				cvTermBuilder.addResourcesBqbIs(resource);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		cvTermBuilder.commit();

		if (displayName.matches("^.*[tT][tT][aA][nN][sS][pP][oO][rR][tT].*$") || displayName.matches("^.*[tT][tT][aA][nN][sS][lL][oO][cC][aA][tT].*$"))
			reaction.setSBOTerm(SBO.getTransport());

		return reaction;
	}

	private Species getSpecies(GKInstance entity, GKInstance reactionlikEvent) {
		if (entity == null)
			return null;
		if (!entity.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
			return null;
		String speciesId = Utils.getSpeciesIdFromEntity(entity);
		Model model = getModel();
		Species species = model.getModelComponentMaps().getSpecies(speciesId);
		
		if (species != null)
			return species;
		try {
			GKInstance stableIdentifier;
			Compartment compartment = getCompartment(entity);
			if (compartment == null) {
				// A species without a compartment is invalid SBML
				System.err.println("SBMLBuilder.getSpecies: WARNING - compartment == null for entity DB_ID=" + entity.getDBID() + ", reaction DB_ID=" + reactionlikEvent.getDBID());
				return null;
			}
			species = model.createSpecies();
			species.setId(speciesId);
			species.setMetaId(getCurrentMetaIdString());
			species.setName(entity.getDisplayName());
			species.setCompartment(compartment.getId());
			species.setSBOTerm(Utils.getEntityTypeAsSBOTerm(entity));
			model.getModelComponentMaps().addSpecies(species);
//			NotesBuilder.appendInstanceSummationsToSBase(species, entity);
			NotesBuilder.appendEntityInformationToSpecies(species, entity);

			CVTermBuilder cvTermBuilder = new CVTermBuilder(model, species);

			List<String> resources = getResources(entity);
			if (entity.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence) || entity.getSchemClass().isa(ReactomeJavaConstants.SimpleEntity)) {
				if (resources != null)
					for (String resource: resources)
						cvTermBuilder.addResourcesBqbIs(resource);
				
				// TODO: Recent data model changes may have rendered this code block outdated.
				if (entity.getSchemClass().isValidAttribute("hasModifiedResidue")) {
					List<GKInstance> hasModifiedResidues = entity.getAttributeValuesList("hasModifiedResidue");
					for (GKInstance hasModifiedResidue: hasModifiedResidues) {
						if (hasModifiedResidue.getSchemClass().isValidAttribute("psiMod")) {
							GKInstance psiMod = (GKInstance)hasModifiedResidue.getAttributeValue("psiMod");
							String resource = "urn:miriam:obo.psi-mod:MOD%3A" + psiMod.getAttributeValue("identifier");
							cvTermBuilder.addResourcesBqbHasVersion(resource);
						}
					}
				}
				List<GKInstance> inferredFroms = entity.getAttributeValuesList("inferredFrom");
				for (GKInstance inferredFrom: inferredFroms) {
					stableIdentifier = (GKInstance)inferredFrom.getAttributeValue("stableIdentifier");
					if (stableIdentifier != null) {
						String resource = "urn:miriam:reactome:" + stableIdentifier.getAttributeValue("identifier");
						cvTermBuilder.addResourcesBqbIsHomologTo(resource);
					}
				}
				List<GKInstance> inferredTos = entity.getAttributeValuesList("inferredTo");
				for (GKInstance inferredTo: inferredTos) {
					stableIdentifier = (GKInstance)inferredTo.getAttributeValue("stableIdentifier");
					if (stableIdentifier != null) {
						String resource = "urn:miriam:reactome:" + stableIdentifier.getAttributeValue("identifier");
						cvTermBuilder.addResourcesBqbIsHomologTo(resource);
					}
				}
			} else {
				if (resources != null)
					for (String resource: resources)
						cvTermBuilder.addResourcesBqbHasPart(resource);
			}
			
			stableIdentifier = (GKInstance)entity.getAttributeValue("stableIdentifier");
			if (stableIdentifier != null) {
				String resource = "urn:miriam:reactome:" + stableIdentifier.getAttributeValue("identifier");
				cvTermBuilder.addResourcesBqbIs(resource);
			}
			
			cvTermBuilder.commit();

			return species;
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		return null;
	}

	private List<String> getResources(GKInstance entity) {
		List<String> resources = new ArrayList<String>();

		try {
			if (entity.getSchemClass().isValidAttribute("hasComponent")) {
				List<GKInstance> hasComponents = entity.getAttributeValuesList("hasComponent");
				for (GKInstance hasComponent: hasComponents) {
					List<String> subResources = getResources(hasComponent);
					for (String resource: subResources)
						resources.add(resource);
				}

				return resources;
			}
			if (entity.getSchemClass().isValidAttribute("hasMember")) {
				List<GKInstance> hasComponents = entity.getAttributeValuesList("hasMember");
				for (GKInstance hasComponent: hasComponents) {
					List<String> subResources = getResources(hasComponent);
					for (String resource: subResources)
						resources.add(resource);
				}

				return resources;
			}
			if (entity.getSchemClass().isValidAttribute("hasCandidate")) {
				List<GKInstance> hasComponents = entity.getAttributeValuesList("hasCandidate");
				for (GKInstance hasComponent: hasComponents) {
					List<String> subResources = getResources(hasComponent);
					for (String resource: subResources)
						resources.add(resource);
				}

				return resources;
			}
			if (entity.getSchemClass().isValidAttribute("repeatedUnit")) {
				List<GKInstance> hasComponents = entity.getAttributeValuesList("repeatedUnit");
				for (GKInstance hasComponent: hasComponents) {
					List<String> subResources = getResources(hasComponent);
					for (String resource: subResources)
						resources.add(resource);
				}

				return resources;
			}

			if (entity.getSchemClass().isValidAttribute("referenceEntity")) {
				GKInstance referenceEntity = (GKInstance)entity.getAttributeValue("referenceEntity");
				if (referenceEntity != null) {
					if (entity.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence))
						resources.add("urn:miriam:uniprot:" + referenceEntity.getAttributeValue("identifier"));
					if (entity.getSchemClass().isa(ReactomeJavaConstants.SimpleEntity))
						resources.add("urn:miriam:obo.chebi:CHEBI%3A" + referenceEntity.getAttributeValue("identifier"));
				}
			}
			if (entity.getSchemClass().isValidAttribute("crossReference")) {
				List<GKInstance> crossReferences = entity.getAttributeValuesList("crossReference");
				for (GKInstance crossReference: crossReferences) {
					if (crossReference.getAttributeValue("referenceDatabase") != null &&
						((GKInstance)(crossReference.getAttributeValue("referenceDatabase"))).getAttributeValue("name").equals("COMPOUND")) {
						resources.add("urn:miriam:kegg.compound:" + crossReference.getAttributeValue("identifier"));
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}

		return resources;
	}

	private void handleSpeciesReference(HashMap<Long,SpeciesReference> speciesReferenceHash, GKInstance reactionlikEvent, Reaction sbmlReaction, GKInstance entity, boolean isInput) {
		Long entityDbId = entity.getDBID();
		Long reactionDbId = reactionlikEvent.getDBID();
		SpeciesReference speciesReference;
		if (speciesReferenceHash.containsKey(entityDbId)) {
			speciesReference = speciesReferenceHash.get(entityDbId);
			double stoichiometry = speciesReference.getStoichiometry();
			stoichiometry += 1.0;
			speciesReference.setStoichiometry(stoichiometry);
		} else {
			try {
				Species species = getSpecies(entity, reactionlikEvent);
				if (species == null)
					return;
				String role = "output";
				if (isInput) {
					role = "input";
					speciesReference = sbmlReaction.createReactant();
					speciesReference.setSBOTerm(SBO.getReactant()); // substrate
				} else {
					speciesReference = sbmlReaction.createProduct();
					speciesReference.setSBOTerm(SBO.getProduct()); // product
				}
				speciesReference.setSpecies(species.getId());
				speciesReference.setId(Utils.getSpeciesReferenceId(reactionDbId.toString(), role, entityDbId.toString()));
				speciesReference.setMetaId(getCurrentMetaIdString());
				speciesReferenceHash.put(entityDbId, speciesReference);
			} catch (Exception e) {
				System.err.println("SBMLBuilder.handleSpeciesReference: WARNING - problem setting up new species reference");
				e.printStackTrace(System.err);
			}
		}
	}

	private Compartment getCompartment(GKInstance entity) {
		Compartment compartment = null;
		if (entity == null) {
			System.err.println("SBMLBuilder.getCompartment: WARNING - entity == null!!");
			return null;
		}
		try {
			GKInstance reactomeCompartment = (GKInstance)entity.getAttributeValue("compartment");
			if (reactomeCompartment == null) {
				System.err.println("SBMLBuilder.getCompartment: WARNING - reactomeCompartment == null for entity DB_ID=" + entity.getDBID());
				return null;
			}
			
			String compartmentId = Utils.getCompartmentIdFromCompartment(reactomeCompartment);
			Model model = getModel();
			compartment = model.getModelComponentMaps().getCompartment(compartmentId);
			if (compartment == null) {
				compartment = model.createCompartment(compartmentId);
				
				CVTermBuilder cvTermBuilder = new CVTermBuilder(model, compartment);

				try {
					String compartmentName = reactomeCompartment.getDisplayName();
					compartment.setId(compartmentId);
					compartment.setName(compartmentName);
					compartment.setMetaId(getCurrentMetaIdString());

					String accession = (String)reactomeCompartment.getAttributeValue("accession");
					if (accession != null) {
						String resource = "urn:miriam:obo.go:GO%3A" + accession;
						cvTermBuilder.addResourcesBqbIs(resource);
					}
					
					GKInstance stableIdentifier = (GKInstance)reactomeCompartment.getAttributeValue("stableIdentifier");
					if (stableIdentifier != null) {
						String resource = "urn:miriam:reactome:" + stableIdentifier.getAttributeValue("identifier");
						cvTermBuilder.addResourcesBqbIs(resource);
					}
					
					model.getModelComponentMaps().addCompartment(compartment);
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
				
				cvTermBuilder.commit();
				
				NotesBuilder.appendInstanceSummationsToSBase(compartment, reactomeCompartment);
				compartment.setSBOTerm(SBO.getPhysicalCompartment());
			}
		} catch (Exception e) {
			System.err.println("SBMLBuilder.getCompartment: WARNING - problem creating new compartment");
			e.printStackTrace(System.err);
		}

		return compartment;
	}

	private void handleModifierSpeciesReference(HashMap<Long,ModifierSpeciesReference> modifierSpeciesReferenceHash, GKInstance reactionlikEvent, Reaction sbmlReaction, GKInstance entity) {
		if (entity == null)
			return;
		Long entityDbId = entity.getDBID();
		ModifierSpeciesReference modifierSpeciesReference = modifierSpeciesReferenceHash.get(entityDbId);
		if (modifierSpeciesReference == null) {
			Species species = getSpecies(entity, reactionlikEvent);
			if (species == null)
				return;
			modifierSpeciesReference = sbmlReaction.createModifier();
			modifierSpeciesReference.setSpecies(species.getId());
			modifierSpeciesReference.setId("modifierspeciesreference_" + reactionlikEvent.getDBID() + "_catalyst_" + entityDbId);
			modifierSpeciesReference.setMetaId(getCurrentMetaIdString());
			modifierSpeciesReference.setSBOTerm(SBO.getCatalysis());
			modifierSpeciesReferenceHash.put(entityDbId, modifierSpeciesReference);
		}
	}
	
	public String sbmlString() {
		Lib lib = model.createLib();
		return lib.writeSBMLToString(document);
	}

	public void printDocument(String filename) {
		if (model == null) {
			System.err.println("SBMLBuilder.printDocument: WARNING - model == null");
			return;
		}
		try {
			if (filename == null) {
				Writer writer = model.createWriter();
				if (writer == null) {
					System.err.println("SBMLBuilder.printDocument: WARNING - could not create an SBML writer");
					return;
				}
				writer.writeSBML(document);
			} else {
				Lib lib = model.createLib();
				if (lib == null) {
					System.err.println("SBMLBuilder.printDocument: WARNING - could not create a lib");
					return;
				}
				lib.writeSBMLToFile(document, filename);
			}
		} catch (Exception e) {
			System.err.println("SBMLBuilder.printDocument: WARNING - IOException while trying to write to: " + filename);
			e.printStackTrace(System.err);
		}
	}
	
	public boolean autogenerateKinetics(String autogenerateKineticServletUrl) {
		return model.autogenerateKinetics(autogenerateKineticServletUrl);
	}
}
