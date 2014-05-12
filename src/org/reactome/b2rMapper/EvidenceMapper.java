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
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * Evidence is not supported by Reactome yet. Information in Evidence
 * is wrapped as Summation for Pathway and Reaction in Reactome. 
 * @author guanming
 *
 */
public class EvidenceMapper extends AbstractBioPAXToReactomeMapper {
     
    @Override
    public void mapClass(OWLIndividual bpInstance,
                         BioPAXFactory bpFactory,
                         XMLFileAdaptor reactomeAdaptor,
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass cls = bpInstance.getRDFType();
        if (cls != bpFactory.getevidenceClass())
            return;
        GKInstance gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.Summation);
        bpToRInstancesMap.put(bpInstance, gkInstance);
    }

    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance gkInstance = bpToRInstanceMap.get(bpInstance);
        if (gkInstance == null)
            return;
        // GKInstance should be a Summation class.
        // Extract EvidenceCode value. 
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.EVIDENCE_CODE);
        StringBuilder txtBuilder = new StringBuilder();
        txtBuilder.append("Evidence");
        Collection evCollection = bpInstance.getPropertyValues(prop);
        if (evCollection != null && evCollection.size() > 0) {
            txtBuilder.append(": ");
            for (Iterator it = evCollection.iterator(); it.hasNext();) {
                OWLIndividual ev = (OWLIndividual) it.next();
                prop = bpFactory.getOWLProperty(BioPAXJavaConstants.TERM);
                String term = (String) ev.getPropertyValue(prop);
                txtBuilder.append(term);
                String dbId = B2RMapperUtilities.grepDBAndIDFromXref(ev, bpFactory);
                if (dbId != null) {
                    txtBuilder.append(" (").append(dbId).append(")");
                }
                if (it.hasNext())
                    txtBuilder.append("; ");
            }
        }
        gkInstance.setAttributeValue(ReactomeJavaConstants.text, txtBuilder.toString());
        // Extract xref values. If they are converted to LiteratureReference, attach to Summation.
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.XREF);
        Collection xrefCollection = bpInstance.getPropertyValues(prop);
        if (xrefCollection != null && xrefCollection.size() > 0) {
            for (Iterator it = xrefCollection.iterator(); it.hasNext();) {
                OWLIndividual xref = (OWLIndividual) it.next();
                GKInstance gkXref = (GKInstance) bpToRInstanceMap.get(xref);
                if (gkXref.getSchemClass().isa(ReactomeJavaConstants.LiteratureReference))
                    gkInstance.addAttributeValue(ReactomeJavaConstants.literatureReference,
                                                 gkXref);
            }
        }
    }

}
