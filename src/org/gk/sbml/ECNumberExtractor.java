/* Copyright (c) 2012 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml;

import java.util.ArrayList;
import java.util.List;

import org.gk.model.GKInstance;

/**
 * Extract the EC numbers from various instance classes.
 * 
 * @author David Croft
 *
 */
public class ECNumberExtractor {
	public static List<String> extractECNumbersFromReactionlikeEvent(GKInstance reactionlikEvent) {
		if (reactionlikEvent == null) {
			System.err.println("ECNumberExtractor.extractECNumbersFromReactionlikeEvent: reactionlikEvent == null");
			return null;
		}
		
		List<String> ecNumbers = new ArrayList<String>();
		try {
			List<GKInstance> catalystActivities = reactionlikEvent.getAttributeValuesList("catalystActivity");
			if (catalystActivities != null)
				for (GKInstance catalystActivity: catalystActivities)
					ecNumbers.addAll(extractECNumbersFromCatalystActivity(catalystActivity));
		} catch (Exception e) {
			System.err.println("Utils.getECNumbersFromReaction: WARNING - problem getting EC numbers");
			e.printStackTrace(System.err);
		}
		
		return ecNumbers;
	}
	
	public static List<String> extractECNumbersFromCatalystActivity(GKInstance catalystActivity) {
		if (catalystActivity == null) {
			System.err.println("ECNumberExtractor.extractECNumbersFromCatalystActivity: catalystActivity == null");
			return null;
		}

		List<String> ecNumbers = new ArrayList<String>();
		try {
			List<GKInstance> activitys = catalystActivity.getAttributeValuesList("activity");
			if (activitys != null)
				for (GKInstance activity: activitys) {
					String ecNumber = (String)activity.getAttributeValue("ecNumber");
					if (ecNumber != null)
						ecNumbers.add(ecNumber);
				}
			if (ecNumbers.size() == 0) {
				List physicalEntitys = catalystActivity.getAttributeValuesList("physicalEntity");
				if (physicalEntitys != null && physicalEntitys.size() == 1) {
					GKInstance physicalEntity = (GKInstance)physicalEntitys.get(0);
					ecNumbers = extractECNumbersFromPhysicalEntity(physicalEntity);
				}
			}
		} catch (Exception e) {
			System.err.println("Utils.getECNumbersFromReaction: WARNING - problem getting EC numbers");
			e.printStackTrace(System.err);
		}

		return ecNumbers;
	}
	
	public static List<String> extractECNumbersFromPhysicalEntity(GKInstance physicalEntity) {
		if (physicalEntity == null) {
			System.err.println("ECNumberExtractor.extractECNumbersFromPhysicalEntity: physicalEntity == null");
			return null;
		}

		List<String> ecNumbers = new ArrayList<String>();
		List<GKInstance> crossReferences;
		try {
			if (physicalEntity.getSchemClass().isValidAttribute("crossReference")) {
				crossReferences = physicalEntity.getAttributeValuesList("crossReference");
				for (GKInstance crossReference: crossReferences) {
					GKInstance referenceDatabase = (GKInstance)crossReference.getAttributeValue("referenceDatabase");
					if (referenceDatabase.getDisplayName().equals("EC")) {
						String ecNumber = (String)crossReference.getAttributeValue("identifier");
						ecNumbers.add(ecNumber);
					}
				}
			}
			if (physicalEntity.getSchemClass().isa("GenomeEncodedEntity")) {
				if (physicalEntity.getSchemClass().isValidAttribute("referenceEntity")) {
					List<GKInstance> referenceEntitys = physicalEntity.getAttributeValuesList("referenceEntity");
					for (GKInstance referenceEntity: referenceEntitys) {
						if (referenceEntity.getSchemClass().isValidAttribute("referenceGene")) {
							List<GKInstance> referenceGenes = referenceEntity.getAttributeValuesList("referenceGene");
							for (GKInstance referenceGene: referenceGenes) {
								crossReferences = referenceGene.getAttributeValuesList("crossReference");
								for (GKInstance crossReference: crossReferences) {
									GKInstance referenceDatabase = (GKInstance)crossReference.getAttributeValue("referenceDatabase");
									if (referenceDatabase.getDisplayName().equals("EC")) {
										String ecNumber = (String)crossReference.getAttributeValue("identifier");
										ecNumbers.add(ecNumber);
									}
								}

							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Utils.getECNumbersFromReaction: WARNING - problem getting EC numbers");
			e.printStackTrace(System.err);
		}

		return ecNumbers;
	}
}
