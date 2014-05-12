/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity vertex layout info.
 * 
 * @author David Croft
 *
 */
public class EntityVertex extends Vertex {
	private String sbmlSpeciesId;
	private Long entityVertexDbId;
	private String type;
	private String subType = null;
	private List<String> componentNames = null;

	public EntityVertex() {
		super();
	}

	public Long getEntityVertexDbId() {
		return entityVertexDbId;
	}

	public String getSbmlSpeciesId() {
		return sbmlSpeciesId;
	}

	public void setSbmlSpeciesId(String sbmlSpeciesId) {
		this.sbmlSpeciesId = sbmlSpeciesId;
	}

	public void setEntityVertexDbId(Long entityVertexDbId) {
		this.entityVertexDbId = entityVertexDbId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}
	
	public void addComponentName(String componentName) {
		if (componentNames == null)
			componentNames = new ArrayList<String>();
		componentNames.add(componentName);
	}

	public List<String> getComponentNames() {
		return componentNames;
	}
}
