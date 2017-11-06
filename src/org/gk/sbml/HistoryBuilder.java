/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.sbml.model.elements.Model;
import org.gk.sbml.model.elements.ModelCreator;
import org.gk.sbml.model.elements.ModelHistory;

/**
 * Methods for extracting history information from Reactome instances and inserting
 * them into SBML SBase objects.
 * 
 * @author David Croft
 *
 */
public class HistoryBuilder {
	public static void appendHistoryToModel(Model model, GKInstance instance) {
	}
	
	public static void appendHistoryToModel(Model model, List<GKInstance> instances) {
		ModelHistory modelHistory = model.getModelHistory();
		Map<Long,Long> authorDbidHash = new HashMap<Long,Long>();
		Map<String,String> modifiedDateTimeHash = new HashMap<String,String>();
		java.util.Date earliestCreatedDate = null;
		List<java.util.Date> modifiedDates = new ArrayList<java.util.Date>();
		for (GKInstance instance: instances) {
			try {
				String dateTime;
				GKInstance created = (GKInstance)instance.getAttributeValue("created");
				if (created != null) {
					GKInstance author = (GKInstance)created.getAttributeValue("author");
					Long authorDbid = author.getDBID();
					if (!authorDbidHash.containsKey(authorDbid)) {
						GKInstance affiliation = (GKInstance)author.getAttributeValue("affiliation");
						String organization = affiliation == null ? "": (String)affiliation.getAttributeValue("name");
//						String eMailAddress = (String)author.getAttributeValue("eMailAddress");
						String eMailAddress = null;
						if (author.getSchemClass().isValidAttribute("eMailAddress")) {
						    eMailAddress = (String) author.getAttributeValue("eMailAddress");
						}
						String firstname = (String)author.getAttributeValue("firstname");
						String surname = (String)author.getAttributeValue("surname");
						
						ModelCreator modelCreator = model.createModelCreator();
						modelCreator.setEmail(eMailAddress == null ? "": eMailAddress);
						modelCreator.setFamilyName(surname == null ? "": surname);
						modelCreator.setGivenName(firstname == null ? "": firstname);
						modelCreator.setOrganisation(organization == null ? "": organization);
						modelHistory.addCreator(modelCreator);
						
						authorDbidHash.put(authorDbid, authorDbid);
					}
					
					String createdDateTime = (String)created.getAttributeValue("dateTime");
					if (createdDateTime != null) {
						java.util.Date createdDate = Utils.reactomeDateTimeStringToDate(createdDateTime);
						if (earliestCreatedDate == null)
							earliestCreatedDate = createdDate;
						if (createdDate.compareTo(earliestCreatedDate) < 0)
							earliestCreatedDate = createdDate;
					}
				}
				
				List<GKInstance> modifieds = instance.getAttributeValuesList("modified");
				for (GKInstance modified: modifieds) {
					dateTime = (String)modified.getAttributeValue("dateTime");
					
					if (dateTime != null && !modifiedDateTimeHash.containsKey(dateTime)) {
						modifiedDateTimeHash.put(dateTime, dateTime);
						modifiedDates.add(Utils.reactomeDateTimeStringToDate(dateTime));
					}
				}
			} catch (Exception e) {
				System.err.println("");
				e.printStackTrace(System.err);
			}
		}
		
		if (earliestCreatedDate != null)
			modelHistory.setCreatedDate(model.createDate(earliestCreatedDate));
		
		Collections.sort(modifiedDates);
		for (java.util.Date modifiedDate: modifiedDates)
			modelHistory.setModifiedDate(model.createDate(modifiedDate));

		
		model.setModelHistory(modelHistory);
	}
}
