/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.elements;

/**
 * Wraps org.sbml.jsbml.SBO and its analogs in other packages.
 * 
 * Actually, since libSBML doesn't seem to provide an equivalent for this
 * simply wrap the JSBML class, and expose just the methods needed by
 * Reactome.
 * 
 * @author David Croft
 *
 */
public class SBO {
	public static int getCatalysis() {
		return org.sbml.jsbml.SBO.getCatalysis();
	}
	
	public static int getComplex() {
		return org.sbml.jsbml.SBO.getComplex();
	}
	
	public static int getMaterialEntity() {
		return org.sbml.jsbml.SBO.getMaterialEntity();
	}
	
	public static int getPhysicalCompartment() {
		return org.sbml.jsbml.SBO.getPhysicalCompartment();
	}
	
	public static int getProduct() {
		return org.sbml.jsbml.SBO.getProduct();
	}
	
	public static int getProtein() {
		return org.sbml.jsbml.SBO.getProtein();
	}

	public static int getReactant() {
		return org.sbml.jsbml.SBO.getReactant();
	}
	
	public static int getSimpleMolecule() {
		return org.sbml.jsbml.SBO.getSimpleMolecule();
	}
	
	public static int getTransport() {
		return org.sbml.jsbml.SBO.getTransport();
	}
}
