/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

import org.gk.sbml.model.elements.ModelHistory;

/**
 * Wraps org.sbml.libsbml.Model and its analogs in other packages.
 * 
 * @author David Croft
 *
 */
public interface Model extends SBase {
	public ModelComponentMaps getModelComponentMaps();
	public int setId(String id);
	public String getId();
	public int setName(String name);
	public int appendAnnotation(String nodeString); 
	public boolean existsReaction(String reactionId);
	public Reaction createReaction(String reactionId);
	public Species createSpecies();
	public Compartment createCompartment(String compartmentId);
	public Lib createLib();
	public Writer createWriter();
	public Layout createLayout(); // null return means no layout extension available
	public ModelHistory getModelHistory();
	public ModelCreator createModelCreator();
	public Date createDate(java.util.Date date);
	public void setModelHistory(ModelHistory modelHistory);
	public boolean autogenerateKinetics();
	public boolean autogenerateKinetics(String autogenerateKineticServletUrl);
}
