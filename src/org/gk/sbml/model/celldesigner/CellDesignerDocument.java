/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import org.gk.sbml.model.elements.Document;
import org.gk.sbml.model.elements.Model;
import org.gk.sbml.model.jsbml.JsbmlModel;
import org.gk.sbml.simcd.SimCDDocument;
import org.sbml.jsbml.SBMLDocument;

import jp.sbi.celldesigner.plugin.PluginModel;
import jp.sbi.celldesigner.plugin.PluginModifierSpeciesReference;
import jp.sbi.celldesigner.plugin.PluginReaction;
import jp.sbi.celldesigner.plugin.PluginSpecies;
import jp.sbi.celldesigner.plugin.PluginSpeciesAlias;
import jp.sbi.celldesigner.plugin.PluginSpeciesReference;
import jp.sbi.celldesigner.plugin.util.PluginReactionSymbolType;
import jp.sbi.celldesigner.plugin.util.PluginSpeciesSymbolType;
import jp.sbi.celldesigner.plugin.CellDesignerPlugin;

/**
 * Wraps org.sbml.jsbml.SBMLDocument.
 * 
 * @author David Croft
 *
 */
public class CellDesignerDocument implements Document {
	private SimCDDocument sbmlElement = null;
	private int modelCounter = 0;
	
	public CellDesignerDocument(int level, int version) {
		sbmlElement = new SimCDDocument(level, version);
	}

	public org.gk.sbml.simcd.SimCDDocument getSbmlElement() {
		return sbmlElement;
	}

	public Model createModel() {
		org.sbml.jsbml.Model sbmlModel = sbmlElement.createModel("Model_" + modelCounter);
		modelCounter++;
		
		return new JsbmlModel(sbmlModel);
	}
	
	public void setNotes(String notes) {
		sbmlElement.setNotes(notes);
	}
	
	public void setLevelAndVersion(int level, int version) {
//		sbmlElement.setLevelAndVersion(level, version);
		sbmlElement = new SimCDDocument(level, version); // TODO: potentially dangerous
	}
	
//	private int level;
//	private int version;
//	private String notes;
//	
//	public CellDesignerDocument(int level, int version) {
//		this.level = level;
//		this.version = version;
//	}
//
//	public Model createModel() {
//		PluginModel sbmlModel = new PluginModel(null);
//		
////		return new JsbmlModel(sbmlModel);
//		return null;
//	}
//	
//	public void setNotes(String notes) {
//		this.notes = notes;
//	}
//	
//	public void setLevelAndVersion(int level, int version) {
//		this.level = level;
//		this.version = version;
//	}
}
