/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.util.Map;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.SchemaAttribute;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * Control is mapped to Regulation in Reactome. Dependent on CONTROL-TYPE,
 * PositiveRegulation or NegativeRegulation is used.
 * @author guanming
 *
 */
public class ControlMapper extends AbstractBioPAXToReactomeMapper {

    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory, 
                         XMLFileAdaptor reactomeAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass type = bpInstance.getRDFType();
        RDFSClass controlCls = bpFactory.getcontrolClass();
        // This mapper should cover control and its sub classes.
        if (type != controlCls &&
            !type.isSubclassOf(controlCls))
            return;
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.CONTROL_TYPE);
        String controlType = (String) bpInstance.getPropertyValue(prop);
        GKInstance gkInstance = null;
        if (controlType == null) 
            gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.Regulation);
        else if (controlType.startsWith("INHIBITION"))
            gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.NegativeRegulation);
        else if (controlType.startsWith("ACTIVATION"))
            gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.PositiveRegulation);
        else
            gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.Regulation);
        bpToRInstancesMap.put(bpInstance, gkInstance);
    }
    
    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance gkInstance = bpToRInstanceMap.get(bpInstance);
        if (gkInstance == null)
            throw new IllegalStateException(bpInstance.toString() + " has not been mapped.");
//        mapControlType(bpInstance, bpFactory, gkInstance, reactomeAdaptor, bpToRInstanceMap);
        mapControlled(bpInstance, bpFactory, gkInstance, bpToRInstanceMap);
        mapController(bpInstance, bpFactory, bpToRInstanceMap, gkInstance);
    }
    
    /**
     * RegulationType class in Reactome has been deleted. So there is no need to do what is
     * implemented in this method.
     * @param bpInstance
     * @param bpFactory
     * @param gkInstance
     * @param bpToRInstanceMap
     * @throws Exception
     */
//    private void mapControlType(OWLIndividual bpInstance,
//                                BioPAXFactory bpFactory,
//                                GKInstance gkInstance, 
//                                XMLFileAdaptor reactomeAdaptor,
//                                Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
//        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.CONTROL_TYPE);
//        String type = (String) bpInstance.getPropertyValue(prop);
//        if (type == null)
//            return;
//        GKInstance  gkType = null;
////        Collection collection = reactomeAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.RegulationType,
////                                                                         ReactomeJavaConstants.name,
////                                                                         "=",
////                                                                         type);
////        if (collection != null && collection.size() > 0) 
////            gkType = (GKInstance) collection.iterator().next();
////        else {
////            gkType = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.RegulationType);
////            gkType.setAttributeValue(ReactomeJavaConstants.name, type);
////        }
//    }
    
    private void mapControlled(OWLIndividual bpInstance, 
                               BioPAXFactory bpFactory,
                               GKInstance gkInstance,
                               Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        OWLProperty prop;
        // Get the controlled.
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.CONTROLLED);
        OWLIndividual controlled = (OWLIndividual) bpInstance.getPropertyValue(prop);
        if (controlled != null) {
            GKInstance gkControlled = bpToRInstanceMap.get(controlled);
            if (gkControlled == null)
                return;
            // In 2018's Reactome data model, Regulation has been migrated to RLE
            if (gkControlled.getSchemClass().isValidAttribute(ReactomeJavaConstants.regulatedBy)) {
                SchemaAttribute att = gkControlled.getSchemClass().getAttribute(ReactomeJavaConstants.regulatedBy);
                if (att.isValidValue(gkInstance))
                    gkControlled.addAttributeValue(att, gkInstance);
            }
//            SchemaAttribute att = gkInstance.getSchemClass().getAttribute(ReactomeJavaConstants.regulatedEntity);
//            if (att.isValidValue(gkControlled))
//                gkInstance.addAttributeValue(ReactomeJavaConstants.regulatedEntity, gkControlled);
        }
    }

    private void mapController(OWLIndividual bpInstance, 
                               BioPAXFactory bpFactory, 
                               Map<OWLIndividual, GKInstance> bpToRInstanceMap, 
                               GKInstance gkInstance) throws InvalidAttributeException, InvalidAttributeValueException {
        // Get the controller
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.CONTROLLER);
        OWLIndividual controller = (OWLIndividual) bpInstance.getPropertyValue(prop);
        if (controller != null) {
            GKInstance gkRegulator = bpToRInstanceMap.get(controller);
            if (gkRegulator != null)
                gkInstance.addAttributeValue(ReactomeJavaConstants.regulator,
                                             gkRegulator);
        }
    }
}
