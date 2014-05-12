/*
 * Created on Jul 22, 2006
 *
 */
package org.reactome.biopax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.persistence.XMLFileAdaptor;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;

/**
 * This is the default implmentation of BioPAXToReactomeMapper. All concrete implementation
 * should extend this class to reduce some duplicated work.
 * @author guanming
 *
 */
public abstract class AbstractBioPAXToReactomeMapper implements BioPAXToReactomeMapper{
    // Use association instead of inheritance since some BioPAX classes might
    // have multiple super classes. 
    private List<BioPAXToReactomeMapper> superMappers;
    
    /**
     * Do nothing in this method.
     * @param bpFactory TODO
     */
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory,
                         XMLFileAdaptor reactomeAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
    }

    /**
     * This is a template method. All concrete class should implement another method called
     * mapClassProperties(OWLIndividual, Map<OWLIndividual, GKInstance>).
     * @param bpFactory
     */
    public void mapProperties(OWLIndividual bpInstance, 
                              BioPAXFactory bpFactory,
                              XMLFileAdaptor reactomeAdaptor, 
                              Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        if (superMappers != null) {
            for (BioPAXToReactomeMapper mapper : superMappers)
                mapper.mapProperties(bpInstance, 
                                     bpFactory,
                                     reactomeAdaptor, 
                                     bpToRInstancesMap);
        }
        mapClassProperties(bpInstance, bpFactory, reactomeAdaptor, bpToRInstancesMap);
    }
    
    /**
     * Any concrete sub class of BioPAXToReactomeMapper should implement this method to map properties
     * defined in the OWL class to Reactome class.
     * @param bpInstance
     * @param bpFactory
     * @param bpToRInstanceMap
     * @throws Exception
     */
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        
    }
    
    /**
     * Other map should be done after properties have been mapped in other places.
     * @param bpInstance
     * @param bpFactor
     * @param reactomeAdaptor
     * @param bpToRInstanceMap
     * @throws Exception
     */
    public void postMap(OWLIndividual bpInstance,
                           BioPAXFactory bpFactor,
                           XMLFileAdaptor reactomeAdaptor,
                           Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        
    }
    
    public void addSuperMapper(BioPAXToReactomeMapper mapper) {
        if (superMappers == null)
            superMappers = new ArrayList<BioPAXToReactomeMapper>();
        superMappers.add(mapper);
    }
    
}
