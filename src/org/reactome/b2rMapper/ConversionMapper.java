/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.b2rMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * All BioPAX Conversion instances will be converted to Reactome Reactions.
 * @author guanming
 *
 */
public class ConversionMapper extends AbstractBioPAXToReactomeMapper {

    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory,
                         XMLFileAdaptor fileAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass conversionCls = bpFactory.getconversionClass();
        RDFSClass bpCls = bpInstance.getRDFType();
        // SubclassOf check can work for only one layer. This is not enough!
        Collection<?> subclasses = conversionCls.getSubclasses(true);
        if ((subclasses != null && subclasses.contains(bpCls)) ||
            (bpCls == conversionCls)) {
            GKInstance rInstance = fileAdaptor.createNewInstance(ReactomeJavaConstants.Reaction);
            bpToRInstancesMap.put(bpInstance, rInstance);
        }
    }
    
    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance rReaction = bpToRInstanceMap.get(bpInstance);
        if (rReaction == null)
            return;
        // ORGANISM is not defined for the conversion class in BioPAX.
        mapParticipants(bpInstance, 
                        bpFactory, 
                        bpToRInstanceMap, 
                        rReaction);
        mapECNumber(bpInstance,
                    bpFactory,
                    bpToRInstanceMap,
                    rReaction,
                    reactomeAdaptor);
    }
    
    private void mapECNumber(OWLIndividual bpInstance,
                             BioPAXFactory bpFactory,
                             Map<OWLIndividual, GKInstance> bpToRInstanceMap, 
                             GKInstance rReaction,
                             XMLFileAdaptor reactomeAdaptor) throws Exception {
        if (bpInstance.getRDFType() == bpFactory.getbiochemicalReactionClass()) {
            // Check if EC number has been assigned
            OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.EC_NUMBER);
            String ecNumber = (String) bpInstance.getPropertyValue(prop);
            if (ecNumber == null)
                return;
            GKInstance dbId = getDBIdentifierForEC(ecNumber,
                                                   reactomeAdaptor);
            rReaction.addAttributeValue(ReactomeJavaConstants.crossReference,
                                        dbId);
        }
    }
    
    private GKInstance getDBIdentifierForEC(String ecNumber,
                                            XMLFileAdaptor reactomeAdaptor) throws Exception {
        // Check if there is an GKInstance available
        Collection c = reactomeAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseIdentifier,
                                                                ReactomeJavaConstants._displayName,
                                                                "=",
                                                                "EC-NUMBER:" + ecNumber);
        if (c != null && c.size() > 0)
            return (GKInstance) c.iterator().next();
        // Need to create one
        GKInstance ecNumberDb = B2RMapperUtilities.getReferenceDB("EC-NUMBER", 
                                                                  reactomeAdaptor);
        GKInstance dbID = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.DatabaseIdentifier);
        dbID.setAttributeValue(ReactomeJavaConstants.referenceDatabase,
                               ecNumberDb);
        dbID.setAttributeValue(ReactomeJavaConstants.identifier,
                               ecNumber);
        return dbID;
    }

    private void mapParticipants(OWLIndividual bpInstance,
                                 BioPAXFactory bpFactory,
                                 Map<OWLIndividual, GKInstance> bpToRInstanceMap, 
                                 GKInstance rReaction) throws InvalidAttributeException, InvalidAttributeValueException {
        // Have to check SPONTONEOUS first
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.SPONTANEOUS);
        String spontaneous = (String) bpInstance.getPropertyValue(prop);
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.LEFT);
        Collection left = bpInstance.getPropertyValues(prop);
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.RIGHT);
        Collection right = bpInstance.getPropertyValues(prop);
        Collection bpInputs, bpOutputs;
        if (spontaneous != null && spontaneous.equals("R-L")) {
            bpInputs = right;
            bpOutputs = left;
        }
        else { // In all other cases, L is assumed as Input
            bpInputs = left;
            bpOutputs = right;
        }
        List inputs = mapParticipants(bpInputs, bpToRInstanceMap);
        if (inputs != null)
            rReaction.setAttributeValue(ReactomeJavaConstants.input,
                                        inputs);
        List outputs = mapParticipants(bpOutputs, bpToRInstanceMap);
        if (outputs != null)
            rReaction.setAttributeValue(ReactomeJavaConstants.output,
                                        outputs);
    }
    
    private List mapParticipants(Collection collection,
                                 Map<OWLIndividual, GKInstance> bpToRInstanceMap) {
        if (collection == null || collection.size() == 0)
            return null;
        List<GKInstance> rtn = new ArrayList<GKInstance>(collection.size());
        for (Iterator it = collection.iterator(); it.hasNext();) {
            OWLIndividual bpInstance = (OWLIndividual) it.next();
            GKInstance gkInstance = bpToRInstanceMap.get(bpInstance);
            if (gkInstance != null)
                rtn.add(gkInstance);
        }
        return rtn;
    }

}
