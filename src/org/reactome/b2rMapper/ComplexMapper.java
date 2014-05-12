/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSLiteral;

/**
 * PhysicalEntityParticipant for Complex and Complex are both mapped to the same Complex. So
 * properties in these two classes are merged into one.
 * @author guanming
 *
 */
public class ComplexMapper extends AbstractBioPAXToReactomeMapper {

    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance gkInstance = bpToRInstanceMap.get(bpInstance);
        if (gkInstance == null) {
            System.out.println("Complex " + bpInstance.getBrowserText() + " cannot be mapped!");
            // In this case, the mapped complex is not used as physicalEntityParticipant anywhere. Just ignore.
            return; 
            //gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.Complex);
            // Complex and its PhysicalEntityParticipant will be merged as one in the Reactome
            // data model.
            //bpToRInstanceMap.put(bpInstance, gkInstance);
        }
        // Should NOT get null exception
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.COMPONENTS);
        Collection componentCollection = bpInstance.getPropertyValues(prop);
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.STOICHIOMETRIC_COEFFICIENT);
        if (componentCollection != null && componentCollection.size() > 0) {
            OWLIndividual bpComp = null;
            GKInstance gkComp = null;
            for (Iterator it = componentCollection.iterator(); it.hasNext();) {
                bpComp = (OWLIndividual) it.next();
                gkComp = bpToRInstanceMap.get(bpComp);
                if (gkComp == null)
                    throw new IllegalStateException(bpComp +" in " + bpInstance + 
                                                    " cannot be mapped!");
                // Check stoichiometry
                RDFSLiteral stoi = (RDFSLiteral) bpComp.getPropertyValue(prop);
                if (stoi == null)
                    gkInstance.addAttributeValue(ReactomeJavaConstants.hasComponent,
                                                 gkComp);
                else {
//                  Get the wrapped double value
                    double stoiValue = stoi.getDouble();
                    for (int i = 0; i < (int)stoiValue; i++)
                        gkInstance.addAttributeValue(ReactomeJavaConstants.hasComponent,
                                                     gkComp);
                }
            }
        }
    }

}
