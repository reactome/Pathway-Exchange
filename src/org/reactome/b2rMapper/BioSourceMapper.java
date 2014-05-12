/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.util.Collection;
import java.util.Map;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * BioSource in BioPAX is mapped to Species in Reactome. 
 * @author guanming
 *
 */
public class BioSourceMapper extends AbstractBioPAXToReactomeMapper {

    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory,
                         XMLFileAdaptor fileAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass cls = bpInstance.getRDFType();
        if (cls != bpFactory.getbioSourceClass())
            return; // this method will work for pathway only
        // In some BioPAX files, species are duplicated
        OWLProperty nameProp = bpFactory.getOWLProperty(BioPAXJavaConstants.NAME);
        String name = (String) bpInstance.getPropertyValue(nameProp);
        GKInstance instance = null;
        if (name != null) {
            Collection list = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species,
                                                            ReactomeJavaConstants.name,
                                                            "=",
                                                            name);
            if (list != null && list.size() > 0) {
                instance = (GKInstance) list.iterator().next();
            }
            else {
                instance = fileAdaptor.createNewInstance(ReactomeJavaConstants.Species);
                instance.setAttributeValue(ReactomeJavaConstants.name, name);
            }
        }
        else
            instance = fileAdaptor.createNewInstance(ReactomeJavaConstants.Species);
        bpToRInstancesMap.put(bpInstance, instance);
    }
    
    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
            BioPAXFactory bpFactory,
            XMLFileAdaptor reactomeAdaptor, 
            Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance rInstance = bpToRInstanceMap.get(bpInstance);
        if (rInstance == null)
            return; // Just in case. It should not occur.
        // Name should be handled by mapClass() method.
    }

}
