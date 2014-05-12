/*
 * Created on Jul 21, 2006
 *
 */
package org.reactome.biopax;

import java.util.Map;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.persistence.XMLFileAdaptor;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;

/**
 * This interface defines two methods to convert any BioPAX Individual to Reactome GKInstance.
 * There should be two steps in any concrete implementation: the first to create an empty
 * Reactome GKInstance, and the second to fill up properties from BioPAX OWLIndividual to
 * Reactome GKInstance after all OWLIndividuals have been converted since objects references
 * are needed for object properties.
 * @author guanming
 *
 */
public interface BioPAXToReactomeMapper {

    public void mapClass(OWLIndividual bpInstance,
                         BioPAXFactory bpFactory,
                         XMLFileAdaptor reactomeAdaptor,
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception;
    
    public void mapProperties(OWLIndividual bpInstance,
                              BioPAXFactory bpFactory,
                              XMLFileAdaptor reactomeAdaptor,
                              Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception;
    
    public void postMap(OWLIndividual bpInstance,
                           BioPAXFactory bpFactor,
                           XMLFileAdaptor reactomeAdaptor,
                           Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception;
    
}
