/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.layout.elements;


/**
 * Generic glyph layout info.
 * 
 * @author David Croft
 *
 */
public abstract class Glyph {
	private String title;

	public Glyph() {
		super();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getShortTitle() {
		return ShortName.generate(title);
	}
}
