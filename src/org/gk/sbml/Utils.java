/* Copyright (c) 2010 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.model.ClassAttributeFollowingInstruction;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.sbml.model.elements.SBO;
import org.gk.schema.SchemaClass;

/**
 * Utility methods
 * 
 * @author David Croft
 *
 */
public class Utils {
	public static String getSpeciesIdFromEntity(GKInstance entity) {
		return "species_" + entity.getDBID().toString();
	}

	public static int getEntityTypeAsSBOTerm(GKInstance entity) {
		int sboTerm = SBO.getMaterialEntity();
		
		if (entity.getSchemClass().isa("Complex"))
			sboTerm =  SBO.getComplex();
		if (entity.getSchemClass().isa("GenomeEncodedEntity"))
			sboTerm =  SBO.getProtein();
		if (entity.getSchemClass().isa("SimpleEntity"))
			sboTerm =  SBO.getSimpleMolecule();
		
		return sboTerm;
	}

	public static String getReactionIdFromReaction(GKInstance reaction) {
		return "reaction_" + reaction.getDBID().toString();
	}

	public static String getCompartmentIdFromCompartment(GKInstance compartment) {
		if (compartment == null) {
			System.err.println("Utils.getCompartmentIdFromCompartment: compartment == null");
			return null;
		}
		return getCompartmentIdFromCompartmentDbID(compartment.getDBID());
	}

	public static String getCompartmentIdFromCompartmentDbID(Long compartmentDbId) {
		return "compartment_" + compartmentDbId.toString();
	}

	public static String getSpeciesReferenceId(String reactionDbId, String role, String entityDbId) {
		return "speciesreference_" + reactionDbId + "_" + role + "_" + entityDbId;
	}
	
	public static boolean hasValidInputsOrOutputs(GKInstance reactionlikEvent) {
		SchemaClass schemaClass = reactionlikEvent.getSchemClass();
		if (!schemaClass.isValidAttribute("input") && !schemaClass.isValidAttribute("output"))
			return false;
		
		try {
			List<GKInstance> inputs = reactionlikEvent.getAttributeValuesList("input");
			for (GKInstance input: inputs)
				if (input.getAttributeValue("compartment") != null)
					return true;
		} catch (Exception e) {
			System.err.println("SBMLBuilder.hasInputsAndOutputs: WARNING - problem getting inputs for reaction");
			e.printStackTrace(System.err);
		} 
		
		try {
			List<GKInstance> outputs = reactionlikEvent.getAttributeValuesList("output");
			for (GKInstance output: outputs)
				if (output.getAttributeValue("compartment") != null)
					return true;
		} catch (Exception e) {
			System.err.println("SBMLBuilder.hasInputsAndOutputs: WARNING - problem getting outputs for reaction");
			e.printStackTrace(System.err);
		} 
		
		return false;
	}
	
	public static java.util.Date reactomeDateTimeStringToDate(String dateTime) {
		String year = dateTime.substring(0, 4);
		String month = dateTime.substring(5, 7);
		String day = dateTime.substring(8, 10);
		String hour = dateTime.substring(11, 13);
		String minute = dateTime.substring(14, 16);
		String second = dateTime.substring(17, 19);
		
		java.util.Date date = new java.util.Date();
		date.setYear(Integer.parseInt(year) - 1900);
		date.setMonth(Integer.parseInt(month));
		date.setDate(Integer.parseInt(day));
		date.setHours(Integer.parseInt(hour));
		date.setMinutes(Integer.parseInt(minute));
		date.setSeconds(Integer.parseInt(second));
		
		return date;
	}
}
