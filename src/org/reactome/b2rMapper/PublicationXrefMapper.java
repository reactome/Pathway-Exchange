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
import org.gk.database.util.LiteratureReferenceAttributeAutoFiller;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.reactome.biopax.AbstractBioPAXToReactomeMapper;
import org.reactome.biopax.BioPAXJavaConstants;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.OWLProperty;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * PublicationXref is mapped as LiteratureReference in Reactome.
 * @author guanming
 *
 */
public class PublicationXrefMapper extends AbstractBioPAXToReactomeMapper {

    @Override
    public void mapClass(OWLIndividual bpInstance, 
                         BioPAXFactory bpFactory, 
                         XMLFileAdaptor reactomeAdaptor, 
                         Map<OWLIndividual, GKInstance> bpToRInstancesMap) throws Exception {
        RDFSClass type = bpInstance.getRDFType();
        if (type != bpFactory.getpublicationXrefClass())
            return;
        GKInstance gkInstance = null;
        // Need to see if this bpInstance has been converted
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.ID);
        String id = (String) bpInstance.getPropertyValue(prop);
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.DB);
        String dbName = (String) bpInstance.getPropertyValue(prop);
        if (dbName != null && dbName.equalsIgnoreCase("PUBMED")) {
            // ID should be an Integer
            int pubmedid = -1;
            try {
                pubmedid = Integer.parseInt(id);
            }
            catch(NumberFormatException e) {
            }
            // If ID is not correct, -1 will be used.
            Collection list = reactomeAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.LiteratureReference,
                                                                       ReactomeJavaConstants.pubMedIdentifier,
                                                                       "=",
                                                                       id);
            if (list != null && list.size() > 0) {
                gkInstance = (GKInstance) list.iterator().next();
            }
            else {
                gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.LiteratureReference);
                gkInstance.setAttributeValue(ReactomeJavaConstants.pubMedIdentifier,
                                             pubmedid);
            }
        }
        else if (dbName != null && id != null) {
            // Use journal to hold dbName and id
            String journal = dbName + ":" + id;
            Collection list = reactomeAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.LiteratureReference,
                                                                       ReactomeJavaConstants.journal,
                                                                       "=",
                                                                       journal);
            if (list != null && list.size() > 0)
                gkInstance = (GKInstance) list.iterator().next();
            else {
                gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.LiteratureReference);
                gkInstance.setAttributeValue(ReactomeJavaConstants.journal, journal);
            }
        }
        else {
            // Just create a new GKInstance even if it is duplicated
            gkInstance = reactomeAdaptor.createNewInstance(ReactomeJavaConstants.LiteratureReference);
        }
        if (gkInstance != null)
            bpToRInstancesMap.put(bpInstance, gkInstance);
    }

    @Override
    protected void mapClassProperties(OWLIndividual bpInstance,
                                      BioPAXFactory bpFactory,
                                      XMLFileAdaptor reactomeAdaptor,
                                      Map<OWLIndividual, GKInstance> bpToRInstanceMap) throws Exception {
        super.mapClassProperties(bpInstance, 
                                 bpFactory, 
                                 reactomeAdaptor,
                                 bpToRInstanceMap);
        OWLProperty prop = bpFactory.getOWLProperty(BioPAXJavaConstants.ID);
        String id = (String) bpInstance.getPropertyValue(prop);
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.DB);
        String dbName = (String) bpInstance.getPropertyValue(prop);
        if (dbName != null && id != null)
            return;
        // Only under this case we need to create LiteratureReference
        // Create new GKInstance based on any information provided
        // Authors
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.AUTHORS);
        Collection<?> authors = bpInstance.getPropertyValues(prop);
        List<GKInstance> rAuthors = new ArrayList<GKInstance>();
        for (Iterator<?> it = authors.iterator(); it.hasNext();) {
            String author = (String) it.next();
            // Get the first name and initial
            String[] tokens = author.split("(, | )");
            String lastName = tokens[0];
            String initial = tokens.length > 1 ? tokens[1] : null;
            GKInstance rAuthor = new LiteratureReferenceAttributeAutoFiller().queryPerson(reactomeAdaptor, 
                                                                                         lastName,
                                                                                         initial, 
                                                                                         null);
            rAuthors.add(rAuthor);
        }
        GKInstance rInstance = bpToRInstanceMap.get(bpInstance);
        rInstance.setAttributeValue(ReactomeJavaConstants.author, 
                                    rAuthors);
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.TITLE);
        String title = (String) bpInstance.getPropertyValue(prop);
        rInstance.setAttributeValue(ReactomeJavaConstants.title, 
                                    title);
        // This is a temporary way: place all source under Journal
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.SOURCE);
        String source = (String) bpInstance.getPropertyValue(prop);
        rInstance.setAttributeValue(ReactomeJavaConstants.journal,
                                    source);
        prop = bpFactory.getOWLProperty(BioPAXJavaConstants.YEAR);
        Integer year = (Integer) bpInstance.getPropertyValue(prop);
        rInstance.setAttributeValue(ReactomeJavaConstants.year, year);
    }
    
}
