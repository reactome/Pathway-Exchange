/*
 * Created on Jun 24, 2009
 *
 */
package org.reactome.convert.common;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;

/**
 * This helper class is used to do pathway reference entities work.
 * @author wgm
 *
 */
public class PathwayReferenceEntityHelper {
    
    public PathwayReferenceEntityHelper() {
    }
    
    /**
     * Split a passed ReferenenceEntities into two sets: smallMolecules and geneProducts.
     * @param referenceEntities
     * @param smallMolecules
     * @param geneProducts
     * @throws Exception
     */
    public void sortReferenceEntities(Set<GKInstance> referenceEntities,
                                      Set<GKInstance> smallMolecules,
                                      Set<GKInstance> geneProducts) throws Exception {
        for (GKInstance ref : referenceEntities) {
            // For the time being, escape drugs
            if (ref.getSchemClass().isa(ReactomeJavaConstants.ReferenceTherapeutic))
                continue;
            if (ref.getSchemClass().isa(ReactomeJavaConstants.ReferenceGroup) ||
                ref.getSchemClass().isa(ReactomeJavaConstants.ReferenceMolecule) ||
                ref.getSchemClass().isa(ReactomeJavaConstants.ReferenceMoleculeClass))
                smallMolecules.add(ref);
            else
                geneProducts.add(ref);
        }
    }
    
    /**
     * This method is used to grep all ReferenceEntities from a pathway.
     * @param pathway
     * @return
     * @throws Exception
     */
    public Set<GKInstance> grepReferenceEntitiesInPathway(GKInstance pathway) throws Exception {
        Set<GKInstance> referenceEntities = new HashSet<GKInstance>();
        Set<GKInstance> pathwayComponents = InstanceUtilities.grepPathwayEventComponents(pathway);
        Set<GKInstance> pathwayParticipants = new HashSet<GKInstance>();
        for (GKInstance tmp : pathwayComponents) {
            if (tmp.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                Set<GKInstance> rxtPaticipants = InstanceUtilities.getReactionParticipants(tmp);
                pathwayParticipants.addAll(rxtPaticipants);
            }
            // There should be no interactions in the main database.
        }
        // Need to grep all ReferenceEntities from PhysicalEntities
        for (GKInstance pe : pathwayParticipants) {
            Set<GKInstance> peRefEntities = grepRefEntities(pe);
            referenceEntities.addAll(peRefEntities);
        }
        return referenceEntities;
    }
    
    /**
     * Grep all ReferenceEntities from a passed PhysicalEntity.
     * @param pe
     * @return
     * @throws Exception
     */
    private Set<GKInstance> grepRefEntities(GKInstance pe) throws Exception {
        Set<GKInstance> refEntities = new HashSet<GKInstance>();
        if (pe.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
            GKInstance ref = (GKInstance) pe.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (ref != null)
                refEntities.add(ref);
        }
        else if (pe.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
            grepRefEntitiesFromInstanceRecursively(pe,
                                                   refEntities);
        }
        else if (pe.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
            grepRefEntitiesFromInstanceRecursively(pe,
                                                   refEntities);
        }
        else if (pe.getSchemClass().isa(ReactomeJavaConstants.Polymer)) {
            grepRefEntitiesFromInstanceRecursively(pe,
                                                   refEntities);
        }
        return refEntities;
    }
    
    @SuppressWarnings("unchecked")
    private void grepRefEntitiesFromInstanceRecursively(GKInstance pe, 
                                                        Set<GKInstance> refEntities) throws Exception {
        Set<GKInstance> current = new HashSet<GKInstance>();
        current.add(pe);
        Set<GKInstance> next = new HashSet<GKInstance>();
        Set<GKInstance> children = new HashSet<GKInstance>();
        while (current.size() > 0) {
            for (GKInstance inst : current) {
                children.clear();
                // For complex
                if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent)) {
                    List list = inst.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
                    if (list != null)
                        children.addAll(list);
                }
                // For EntitySet
                if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
                    List list = inst.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                    if (list != null)
                        children.addAll(list);
                }
                // For candidate set
                if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
                    List list = inst.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
                    if (list != null)
                        children.addAll(list);
                }
                // For Polymer
                if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.repeatedUnit)) {
                    List<GKInstance> list = inst.getAttributeValuesList(ReactomeJavaConstants.repeatedUnit);
                    if (list != null)
                        children.addAll(list);
                }
                if (children.size() == 0)
                    continue;
                for (Iterator it = children.iterator(); it.hasNext();) {
                    GKInstance tmp = (GKInstance) it.next();
                    if (tmp.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
                        GKInstance ref = (GKInstance) tmp.getAttributeValue(ReactomeJavaConstants.referenceEntity);
                        if (ref != null)
                            refEntities.add(ref);
                    }
                    else if (tmp.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
                        next.add(tmp);
                    else if (tmp.getSchemClass().isa(ReactomeJavaConstants.Complex))
                        next.add(tmp);
                    else if (tmp.getSchemClass().isa(ReactomeJavaConstants.Polymer))
                        next.add(tmp);
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
    }
}
