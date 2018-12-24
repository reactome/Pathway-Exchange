/*
 * Created on May 24, 2005
 */
package org.reactome.biopax;

import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.biopax.model.BioPAXFactory;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;

import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.model.OWLIndividual;

/**
 * This class is used to convert a BioPAX Model to Reactome Model.
 * @author wgm
 */
@SuppressWarnings("unchecked")
public class BioPAXToReactomeConverter {
    // Map from bioPax instances to Reactome instances
    private Map<OWLIndividual, GKInstance> bpToRInstances;
    private BioPAXFactory biopaxFactory;
    private XMLFileAdaptor reactomeAdaptor;
    // Used to query the reactome database to get the controlled vocabular
    private MySQLAdaptor reactomeDB;
    // Used to log
    private Logger logger = Logger.getLogger(BioPAXToReactomeConverter.class);
    // Used to generate BioPAXToReactomeMapper objects
    private BioPAXToReactomeMapperFactory mapperFactory;
    
    public BioPAXToReactomeConverter() throws Exception {
        reactomeAdaptor = new XMLFileAdaptor();
        mapperFactory = BioPAXToReactomeMapperFactory.getFactory();
        bpToRInstances = new HashMap<OWLIndividual, GKInstance>();
    }
    
    public void setReactomeDB(MySQLAdaptor dba) {
        this.reactomeDB = dba;
    }
    
    /**
     * A batch converting should call this method to make a one-short postprocessing.
     * @param bpSourceFileNames
     * @throws Exception
     */
    public void convert(List<String> bpSourceFileNames) throws Exception {
        logger.info("Starting a batch converting...");
        for (String fileName : bpSourceFileNames) {
            logger.info("Converting " + fileName + "...");
            convertBeforePost(fileName);
        }
        logger.info("Post procecssing...");
        // Check if any post processing is needed
        if (mapperFactory.getPostProcessor() != null)
            mapperFactory.getPostProcessor().postProcess(reactomeDB, 
                                                         reactomeAdaptor);
    }
    
    /**
     * The actual converting process.
     */
    public void convert(String bpSourceFileName) throws Exception {
        logger.info("Starting process " + bpSourceFileName + "...");
        convertBeforePost(bpSourceFileName);
        logger.info("Starting postProcess...");
        // Check if any post processing is needed
        if (mapperFactory.getPostProcessor() != null)
            mapperFactory.getPostProcessor().postProcess(reactomeDB, 
                                                         reactomeAdaptor);
    }
    
    public BioPAXToReactomeMapperFactory getMapperFactory() {
        return this.mapperFactory;
    }
    
    private void convertBeforePost(String bpSourceFileName) throws Exception {
        logger.info("Loading by Jena " + bpSourceFileName + "...");
        JenaOWLModel model = ProtegeOWL.createJenaOWLModelFromInputStream(new FileInputStream(bpSourceFileName));
        biopaxFactory = new BioPAXFactory(model);
        Collection instances = biopaxFactory.getOWLModel().getOWLIndividuals();
        logger.info("Total loaded instances: " + instances.size());
        int index = 0;
        for (Iterator it = instances.iterator(); it.hasNext();) {
            OWLIndividual bpInstance = (OWLIndividual) it.next();
            BioPAXToReactomeMapper mapper = mapperFactory.getMapper(bpInstance);
            if (mapper == null) {
                logger.info(bpInstance.toString() + " cannot be mapped!");
                continue;
            }
            mapper.mapClass(bpInstance, 
                            biopaxFactory, 
                            reactomeAdaptor, 
                            bpToRInstances);
            index ++;
            if (index % 1000 == 0)
                logger.info("Done class mapping: " + index);
        }
        logger.info("Handling properties...");
        handleProperties();
        logger.info("Post mapping...");
        postMap();
        logger.info("Set display names...");
        setDisplayNames();
    }
    
    private void postMap() throws Exception {
        Collection instances = biopaxFactory.getOWLModel().getOWLIndividuals();
        for (Iterator it = instances.iterator(); it.hasNext();) {
            OWLIndividual bioInstance = (OWLIndividual) it.next();
            BioPAXToReactomeMapper mapper = mapperFactory.getMapper(bioInstance);
            if (mapper != null)
                mapper.postMap(bioInstance,
                               biopaxFactory,
                               reactomeAdaptor, 
                               bpToRInstances);
        }
    }
    
    private void handleProperties() throws Exception {
        Collection instances = biopaxFactory.getOWLModel().getOWLIndividuals();
        for (Iterator it = instances.iterator(); it.hasNext();) {
            OWLIndividual bioInstance = (OWLIndividual) it.next();
            BioPAXToReactomeMapper mapper = mapperFactory.getMapper(bioInstance);
            if (mapper != null)
                mapper.mapProperties(bioInstance,
                                     biopaxFactory,
                                     reactomeAdaptor, 
                                     bpToRInstances);
        }
    }
    
    private void setDisplayNames() throws Exception {
        // Call this first sequence displayNames are used by others
        setDisplayNames(ReactomeJavaConstants.Compartment);
        setDisplayNames(ReactomeJavaConstants.ReferenceDatabase);
        Collection rInstances = reactomeAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        GKInstance rInstance;
        for (Object obj : rInstances) {
            rInstance = (GKInstance) obj;
            if (rInstance.getSchemClass().isa(ReactomeJavaConstants.ModifiedResidue))
                continue;
            InstanceDisplayNameGenerator.setDisplayName(rInstance);
        }
    }
    
    private void setDisplayNames(String clsName) throws Exception {
        Collection rInstances = reactomeAdaptor.fetchInstancesByClass(clsName);
        GKInstance rInstance = null;
        if (rInstances != null) {
            for (Object obj : rInstances) {
                rInstance = (GKInstance) obj;
                InstanceDisplayNameGenerator.setDisplayName(rInstance);
            }
        }
    }
    
    public XMLFileAdaptor getReactomeModel() {
        return this.reactomeAdaptor;
    }
}
