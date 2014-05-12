/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.simcd;

import java.util.List;
import java.util.Map;

import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;

/**
 * Extend JSBML to simulate some aspects of CellDesigner SBML.
 * 
 * @author David Croft
 *
 */
public class SimCDAnnotation extends Annotation {
	public SimCDAnnotation() {
		super();
	}

	public SimCDAnnotation(Annotation annotation) {
		super(annotation);
	}

	public SimCDAnnotation(List<CVTerm> cvTerms) {
		super(cvTerms);
	}

	public SimCDAnnotation(Map<String, String> annotations) {
		super(annotations);
	}

	public SimCDAnnotation(String annotation, List<CVTerm> cvTerms) {
		super(annotation, cvTerms);
	}

	public SimCDAnnotation(String annotation) {
		super(annotation);
	}
}
