/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Model layout info.
 * 
 * @author David Croft
 *
 */
public class Diagram extends Glyph {
	private double width;
	private double height;
	private List<Reaction> reactionLayouts = new ArrayList<Reaction>();
	private List<CompartmentVertex> compartmentVertexLayouts = new ArrayList<CompartmentVertex>();

	public Diagram() {
		super();
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public List<Reaction> getReactionLayouts() {
		return reactionLayouts;
	}

	public List<CompartmentVertex> getCompartmentVertexLayouts() {
		return compartmentVertexLayouts;
	}

	public Reaction createReactionLayout() {
		Reaction reactionLayout = new Reaction();
		reactionLayouts.add(reactionLayout);
		return reactionLayout;
	}
	
	public CompartmentVertex createCompartmentVertexLayout() {
		CompartmentVertex compartmentVertexLayout = new CompartmentVertex();
		compartmentVertexLayouts.add(compartmentVertexLayout);
		return compartmentVertexLayout;
	}
	
	public void prepare() {
		HashMap<Long,EntityVertex> entityVertexLayoutHash = new HashMap<Long,EntityVertex>();
		for (Reaction reactionLayout: reactionLayouts)
			for (ReactionVertex reactionVertexLayout: reactionLayout.getReactionVertexLayouts())
				for (Edge edgeLayout: reactionVertexLayout.getEdgeLayouts()) {
					EntityVertex entityVertexLayout = edgeLayout.getEntityVertexLayout();
					Long entityVertexDbId = entityVertexLayout.getEntityVertexDbId();
					EntityVertex previousEntityVertexLayout = entityVertexLayoutHash.get(entityVertexDbId);
					if (previousEntityVertexLayout == null)
						entityVertexLayoutHash.put(entityVertexDbId, entityVertexLayout);
					else
						edgeLayout.setEntityVertexLayout(previousEntityVertexLayout);
				}
	}
}
