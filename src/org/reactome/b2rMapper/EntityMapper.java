/*
 * Created on Jul 21, 2006
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
import org.gk.schema.SchemaClass;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;

/**
 * Properties contained by Entity class, DATA-SOURCE, NAME, SHORT-NAME, SYNONYM, and XREF,
 * are mapped in this class.
 * @author guanming
 *
 */
public class EntityMapper extends AbstractBioPAXToReactomeMapper {

    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor, 
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        GKInstance rInstance = bpToRInstanceMap.get(bpInstance);
        if (rInstance == null)
            return;
        setNames(bpInstance, bpFactory, rInstance);
        setSummations(bpInstance, bpFactory, reactomeAdaptor, rInstance);
        mapXref(bpInstance, bpFactory, reactomeAdaptor, rInstance, bpToRInstanceMap);
        setDataSource(bpInstance, bpFactory, rInstance, reactomeAdaptor, bpToRInstanceMap);
    }
    
    private void setDataSource(OWLIndividual bpInstance,
                               BioPAXFactory bpFactory,
                               GKInstance gkInstance,
                               XMLFileAdaptor reactomeAdaptor,
                               Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        // Want to use a summation for the time being to store this information
        if (!gkInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.summation))
            return;
        // Extract DataSource
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.DATA_SOURCE);
        OWLIndividual bpDataSource = (OWLIndividual) bpInstance.getPropertyValue(prop);
        if (bpDataSource == null)
            return;
        StringBuilder txt = new StringBuilder();
        txt.append("Data source: ");
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.NAME);
        String bpName = (String) bpDataSource.getPropertyValue(prop);
        if (bpName != null)
            txt.append(bpName);
        // See if Xref is specified
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.XREF);
        OWLIndividual xref = (OWLIndividual) bpDataSource.getPropertyValue(prop);
        if (xref != null) {
            prop = bpFactory.getOWLProperty(BioPAXJavaConstants.DB);
            String db = (String) xref.getPropertyValue(prop);
            prop = bpFactory.getOWLProperty(BioPAXJavaConstants.ID);
            String id = (String) xref.getPropertyValue(prop);
            txt.append(", Xref: ");
            txt.append("DB ").append(db);
            txt.append("ID ").append(id);
        }
        // Try to fetch if such a summation existed already
        GKInstance gkSummation = null;
        Collection collection = reactomeAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Summation,
                                                                         ReactomeJavaConstants.text,
                                                                         "=",
                                                                         txt.toString());
        if (collection != null && collection.size() > 0)
            gkSummation = (GKInstance) collection.iterator().next();
        else {
            gkSummation = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.Summation);
            gkSummation.setAttributeValue(ReactomeJavaConstants.text,
                                          txt.toString());
        }
        gkInstance.addAttributeValue(ReactomeJavaConstants.summation,
                                     gkSummation);
     }
    
    private void mapXref(OWLIndividual bpInstance,
                         BioPAXFactory bpFactory,
                         XMLFileAdaptor reactomeAdaptor,
                         GKInstance gkInstance,
                         Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        // Extract XREF
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.XREF);
        Collection xrefList = bpInstance.getPropertyValues(prop);
        if (xrefList != null && xrefList.size() > 0) {
            OWLIndividual bpXref = null;
            GKInstance rXref = null;
            SchemaClass rCls = null;
            for (Iterator it = xrefList.iterator(); it.hasNext();) {
                bpXref = (OWLIndividual) it.next();
                rXref = bpToRInstanceMap.get(bpXref);
                if (rXref == null)
                //    continue; // It might be possible!!!
                    throw new IllegalStateException(bpXref + " cannot be mapped!");
                rCls = rXref.getSchemClass();
                if (rCls.isa(ReactomeJavaConstants.LiteratureReference) &&
                    gkInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.literatureReference)) {
                    gkInstance.addAttributeValue(ReactomeJavaConstants.literatureReference,
                                                 rXref);
                }
                else if (rCls.isa(ReactomeJavaConstants.DatabaseIdentifier) &&
                         gkInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.crossReference)) {
                    gkInstance.addAttributeValue(ReactomeJavaConstants.crossReference,
                                                 rXref);
                }
            }
        }
    }
    
    private void setSummations(OWLIndividual bpInstance,
                               BioPAXFactory bpFactory,
                               XMLFileAdaptor reactomeAdaptor,
                               GKInstance gkInstance) throws Exception {
        if (gkInstance.getSchemClass().isValidAttribute(ReactomeJavaConstants.summation)) {
            OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.COMMENT);
            // Comments might have more than one
            Collection comments = bpInstance.getPropertyValues(prop); 
            if (comments == null || comments.size() == 0)
                return;
            for (Iterator it = comments.iterator(); it.hasNext();) {
                String comment = (String) it.next();
                // Create a new Summation instance
                GKInstance summation = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.Summation);
                summation.setAttributeValue(ReactomeJavaConstants.text,
                                            comment);
                gkInstance.addAttributeValue(ReactomeJavaConstants.summation,
                                             summation);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void setNames(OWLIndividual bpInstance,
                          BioPAXFactory bpFactory,
                          GKInstance gkInstance) throws Exception {
        if (gkInstance.getSchemClass().isValidAttribute("name")) {
            // Need the order for the names. So a List instead of Set should be used.
            List names = new ArrayList();
            OWLProperty prop = bpFactory.getOWLProperty("SHORT-NAME");
            String shortName = (String) bpInstance.getPropertyValue(prop);
            if (shortName != null)
                names.add(shortName);
            prop = bpFactory.getOWLProperty("NAME");
            String name = (String) bpInstance.getPropertyValue(prop);
            if (name != null && !names.contains(name))
                names.add(name);
            prop = bpFactory.getOWLProperty("SYNONYMS");
            Collection synonyms = bpInstance.getPropertyValues(prop);
            if (synonyms != null) {
                for (Iterator it = synonyms.iterator(); it.hasNext();) {
                    String synonym = (String) it.next();
                    // It is recommended to add SHORT-NAME and NAME to SYNONYMS.
                    if (!names.contains(synonym))
                        names.add(synonym);
                }
            }
            // id used in OWL
            String id = bpInstance.getLocalName();
            if (id != null && !names.contains(id))
                names.add(id);
            gkInstance.setAttributeValue("name", names);
        }
    }

}
