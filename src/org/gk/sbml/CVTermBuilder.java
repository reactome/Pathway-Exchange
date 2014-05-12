/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.util.HashMap;
import java.util.Map;

import org.gk.sbml.model.elements.CVTerm;
import org.gk.sbml.model.elements.Model;
import org.gk.sbml.model.elements.SBase;

/**
 * Builds up CVTerms and adds them to appropriate objects.
 * 
 * @author David Croft
 *
 */
public class CVTermBuilder {
	private Model model = null;
	private SBase sbase = null;
	private Map<String,String> resourcesBqbIs = new HashMap<String,String>();
	private Map<String,String> resourcesBqbHasVersion = new HashMap<String,String>();
	private Map<String,String> resourcesBqbIsHomologTo = new HashMap<String,String>();
	private Map<String,String> resourcesBqbHasPart = new HashMap<String,String>();
	private Map<String,String> resourcesBqbIsDescribedBy = new HashMap<String,String>();

	public CVTermBuilder(Model model, SBase sbase) {
		this.model = model;
		this.sbase = sbase;
	}

	public void addResourcesBqbIs(String resource) {
		resourcesBqbIs.put(resource, resource);
	}

	public void addResourcesBqbHasVersion(String resource) {
		resourcesBqbHasVersion.put(resource, resource);
	}

	public void addResourcesBqbIsHomologTo(String resource) {
		resourcesBqbIsHomologTo.put(resource, resource);
	}

	public void addResourcesBqbHasPart(String resource) {
		resourcesBqbHasPart.put(resource, resource);
	}

	public void addResourcesBqbIsDescribedBy(String resource) {
		resourcesBqbIsDescribedBy.put(resource, resource);
	}
	
	public void commit() {
		commitBqbIs();
		commitBqbHasVersion();
		commitBqbIsHomologTo();
		commitBqbHasPart();
		commitBqbIsDescribedBy();
	}
	
	private void commitBqbIs() {
		if (resourcesBqbIs.size() > 0) {
			CVTerm cvTerm = createBiologicalCVTerm();
			cvTerm.setBiologicalQualifierTypeBqbIs();
			for (String resource: resourcesBqbIs.keySet())
				cvTerm.addResource(resource);
			sbase.addCVTerm(cvTerm);
		}
	}
	
	private void commitBqbHasVersion() {
		if (resourcesBqbHasVersion.size() > 0) {
			CVTerm cvTerm = createBiologicalCVTerm();
			cvTerm.setBiologicalQualifierTypeBqbHasVersion();
			for (String resource: resourcesBqbHasVersion.keySet())
				cvTerm.addResource(resource);
			sbase.addCVTerm(cvTerm);
		}
	}
	
	private void commitBqbIsHomologTo() {
		if (resourcesBqbIsHomologTo.size() > 0) {
			CVTerm cvTerm = createBiologicalCVTerm();
			cvTerm.setBiologicalQualifierTypeBqbIsHomologTo();
			for (String resource: resourcesBqbIsHomologTo.keySet())
				cvTerm.addResource(resource);
			sbase.addCVTerm(cvTerm);
		}
	}
	
	private void commitBqbHasPart() {
		if (resourcesBqbHasPart.size() > 0) {
			CVTerm cvTerm = createBiologicalCVTerm();
			cvTerm.setBiologicalQualifierTypeBqbHasPart();
			for (String resource: resourcesBqbHasPart.keySet())
				cvTerm.addResource(resource);
			sbase.addCVTerm(cvTerm);
		}
	}
	
	private void commitBqbIsDescribedBy() {
		if (resourcesBqbIsDescribedBy.size() > 0) {
			CVTerm cvTerm = createBiologicalCVTerm();
			cvTerm.setBiologicalQualifierTypeBqbIsDescribedBy();
			for (String resource: resourcesBqbIsDescribedBy.keySet())
				cvTerm.addResource(resource);
			sbase.addCVTerm(cvTerm);
		}
	}
	
	private CVTerm createBiologicalCVTerm() {
		CVTerm cvTerm = model.createCVTerm();
		cvTerm.setQualifierTypeBiological();
		
		return cvTerm;
	}
}
