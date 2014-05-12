/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity vertex layout info.
 * 
 * @author David Croft
 *
 */
public class EntityVertex extends Vertex {
	private String sbmlSpeciesId = null;
	private Long entityVertexDbId = null;
	private String type = null;
	private String subType = null;
	private List<String> componentNames = null; // For sets and complexes

	public Long getEntityVertexDbId() {
		return entityVertexDbId;
	}

	public void setEntityVertexDbId(Long entityVertexDbId) {
		this.entityVertexDbId = entityVertexDbId;
	}

	public String getSbmlSpeciesId() {
		return sbmlSpeciesId;
	}

	public void setSbmlSpeciesId(String sbmlSpeciesId) {
		this.sbmlSpeciesId = sbmlSpeciesId;
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

	public static String getGlyphType() {
		return "entityVertex";
	}
}
