/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.convert.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

/**
 * Filter instances according to their properties
 * 
 * @author David Croft
 *
 */
public class InstanceFilters {
	MySQLAdaptor databaseAdaptor = null;
	
	// The key in the outer Map (the first String parameter) corresponds to
	// a Reactome instance class, e.g. "Pathway".  The key in the inner
	// Map (the second String parameter) corresponds to an attribute of the
	// instance class.  The List contains instance names or DB_IDs that
	// should match what is found in the attribute.
	private Map<String, Map<String, List<String>>> exclusionFilters = new HashMap<String, Map<String, List<String>>>();
	private Map<String, Map<String, List<String>>> inclusionFilters = new HashMap<String, Map<String, List<String>>>();
	
	public void setDatabaseAdaptor(MySQLAdaptor databaseAdaptor) {
		this.databaseAdaptor = databaseAdaptor;
	}

	public void addExclusionFilter(String instanceClassName, String attribute, String value) {
		addFilter(exclusionFilters, instanceClassName, attribute, value);
	}
	
	public void addInclusionFilter(String instanceClassName, String attribute, String value) {
		addFilter(inclusionFilters, instanceClassName, attribute, value);
	}
	
	private void addFilter(Map<String, Map<String, List<String>>> filters, String instanceClassName, String attribute, String value) {
		Map<String, List<String>> instanceClassFilters = filters.get(instanceClassName);
		if (instanceClassFilters == null)
			instanceClassFilters = new HashMap<String, List<String>>();
		List<String> filter = instanceClassFilters.get(attribute);
		if (filter == null)
			filter = new ArrayList<String>();
		filter.add(value);
		instanceClassFilters.put(attribute, filter);
		filters.put(instanceClassName, instanceClassFilters);
	}
	
	/**
	 * Returns true if the supplied instance passes through all filters,
	 * false otherwise.
	 * 
	 * @param instance
	 * @return
	 */
	public boolean isPassingThroughFilters(GKInstance instance) {
		try {
			for (String instanceClassName: exclusionFilters.keySet()) {
				if (!instance.getSchemClass().isa(instanceClassName))
					continue;
				Map<String, List<String>> instanceClassFilters = exclusionFilters.get(instanceClassName);
				for (String attribute: instanceClassFilters.keySet()) {
					if (!instance.getSchemClass().isValidAttribute(attribute))
						continue;

					List<GKInstance> attributeInstances = instance.getAttributeValuesList(attribute);
					if (attributeInstances == null)
						continue;
					for (GKInstance attributeInstance: attributeInstances)
						for (String value: instanceClassFilters.get(attribute))
							if (isMatchingInstance(attributeInstance, value))
								return false;
				}
			}
			
			boolean isPassingThroughInclusionFilter = true;
			for (String instanceClassName: inclusionFilters.keySet()) {
				if (!instance.getSchemClass().isa(instanceClassName))
					continue;

				Map<String, List<String>> instanceClassFilters = inclusionFilters.get(instanceClassName);
				for (String attribute: instanceClassFilters.keySet()) {
					if (!instance.getSchemClass().isValidAttribute(attribute))
						continue;

					isPassingThroughInclusionFilter = false;
					List<GKInstance> attributeInstances = instance.getAttributeValuesList(attribute);
					if (attributeInstances == null)
						continue;
					for (GKInstance attributeInstance: attributeInstances) {
						for (String value: instanceClassFilters.get(attribute))
							if (isMatchingInstance(attributeInstance, value)) {
								isPassingThroughInclusionFilter = true;
								break;
							}
						if (isPassingThroughInclusionFilter)
							break;
					}
					if (!isPassingThroughInclusionFilter)
						break;
				}
			}
			
			return isPassingThroughInclusionFilter;
		} catch (Exception e) {
			System.err.println("InstanceFilters.isPassingThroughFilters: WARNING - problem getting value for attribute");
			e.printStackTrace(System.err);
		}
		
		return false;
	}
	
	/**
	 * Returns true if the supplied instance corresponds to the value.  "Corresponds
	 * to" means that:
	 * 
	 * 1. The instance's _displayName is the same as the value, or
	 * 2. The instance's DB_ID is the same as the value, or
	 * 3. The instance's name is the same as the value.
	 * 
	 * @param instance
	 * @param value
	 * @return
	 */
	private boolean isMatchingInstance(GKInstance instance, String value) {
		if (instance.getDisplayName().equals(value))
			return true;
		if (instance.getDBID().toString().equals(value))
			return true;
		try {
			List<String> names = instance.getAttributeValuesList("name");
			for (String name: names)
				if (name.equals(value))
					return true;
		} catch (Exception e) {
			System.err.println("InstanceFilters.isMatchingInstance: WARNING - problem getting value for name");
			e.printStackTrace(System.err);
		}
		return false;
	}
	
	public Map<Long,List<Long>> filterPathwayReactionHash(Map<Long,List<Long>> pathwayReactionHash) {
		Set<Long> pathwayDbIDs = pathwayReactionHash.keySet();
		Map<Long,List<Long>> newPathwayReactionHash = new HashMap<Long,List<Long>>();
		try {
			GKInstance pathway;
			for (Long pathwayDbID: pathwayDbIDs) {
				if (pathwayDbID == null) {
					System.err.println("InstanceFilters.filterPathwayReactionHash: WARNING - pathwayDbID is null, skipping!");
					continue;
				}
				pathway = databaseAdaptor.fetchInstance(pathwayDbID);
				if (pathway == null) {
					System.err.println("InstanceFilters.filterPathwayReactionHash: WARNING - no pathway found for DB_ID=" + pathwayDbID);
					continue;
				}
				
				if (!isPassingThroughFilters(pathway))
					continue;
				
				List<Long> reactionDbIdList = new ArrayList<Long>();

				for (Long reactionDbId: pathwayReactionHash.get(pathwayDbID)) {
					GKInstance reaction = databaseAdaptor.fetchInstance(reactionDbId);
					if (reaction == null) {
						System.err.println("InstanceFilters.convertReactions: WARNING - no reaction found for DB_ID=" + reactionDbId);
						continue;
					}
					if (!reaction.getSchemClass().isa("ReactionlikeEvent")) {
						System.err.println("InstanceFilters.convertReactions: WARNING - DB_ID is not a reaction!");
						continue;
					}
					if (!isPassingThroughFilters(reaction))
						continue;
					reactionDbIdList.add(reactionDbId);
				}
				newPathwayReactionHash.put(pathwayDbID, reactionDbIdList);
			}
		} catch (Exception e) {
			System.err.println("InstanceFilters.filterPathwayReactionHash: DBAdaptor problem");
			e.printStackTrace(System.err);
		}
		
		return newPathwayReactionHash;
	}
}
