/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.layout;


/**
 * Generic glyph layout info.
 * 
 * @author David Croft
 *
 */
public abstract class Glyph {
	private String title;
	private String id;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getShortTitle() {
		return ShortName.generate(title);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	// abstract static methods are not allowed, but by exiting, I imagine I will
	// make programmers sit up and take notice.
	public static String getGlyphType() {
		System.err.println("Glyph.getGlyphType: ERROR - you must implement this in a subclass");
		System.exit(1);
		return null;
	}
}
