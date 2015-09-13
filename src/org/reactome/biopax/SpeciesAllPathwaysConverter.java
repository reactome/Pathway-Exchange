/*
 * Created on Nov 29, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.reactome.biopax;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author vastrik
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SpeciesAllPathwaysConverter  {

	public static void main(String[] args) throws Exception {
        SpeciesAllPathwaysConverter converter = new SpeciesAllPathwaysConverter();
        converter.doDump(args);
//        Collection speci = adaptor.fetchInstanceByAttribute("Species","name","=",args[5]);
//        if (speci.isEmpty()) {
//        		throw(new Exception("Species with name " + args[5] + " not found."));
//        }
//        GKInstance species = (GKInstance) speci.iterator().next();
//        Collection events = adaptor.fetchInstanceByAttribute("Event","species","=",species);
//        adaptor.loadInstanceAttributeValues(events,new String[]{"hasComponent"});
//        events = InstanceUtilities.grepTopLevelEvents(events);
//        try {
//            ReactomeToBioPAXXMLConverter converter = new ReactomeToBioPAXXMLConverter();
//            converter.convert(events);
//            Document biopaxModel = converter.getBioPAXModel();
//            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
//            FileOutputStream fos = new FileOutputStream("homosapiens.owl");
//            outputter.output(biopaxModel, fos);
//        }
//        catch(Exception e) {
//            e.printStackTrace();
//        }
    }
	
	public void doDump(String[] args) throws Exception {
        if (args.length < 6) {
            System.err.println("Please provide these arguments in order: dbHost, dbName, dbUser, dbPwd, dbPort, outputDir, speciesName(optional).");
            return;
        }
        if (!validateDir(args[5])) {
            System.err.println("Cannot create directory: " + args[5]);
            return;
        }
        MySQLAdaptor adaptor = new MySQLAdaptor(args[0],
                                                args[1],
                                                args[2],
                                                args[3],
                                                Integer.parseInt(args[4]));
        if (args.length == 6) // No species name specified. Dump all events for all specieses.
            generateOWLFiles(adaptor, args[5]);
        else
            generateOWLFiles(adaptor, args[6], args[5]);
	}
    
    public void generateOWLFiles(MySQLAdaptor adaptor, String speciesName, String dirName) throws Exception {
        Collection specieses = adaptor.fetchInstanceByAttribute("Species", "_displayName", "=", speciesName);
        if (specieses == null || specieses.size() == 0)
            throw new IllegalArgumentException("Species \"" + speciesName + "\" cannot be found in the database.");
        generateOWLFiles(adaptor, specieses, dirName);
    }
    
    public void generateOWLFiles(MySQLAdaptor adaptor, String dirName) throws Exception {
        Collection specieses = adaptor.fetchInstancesByClass("Species");
        generateOWLFiles(adaptor, specieses, dirName);
    }
    
    protected void generateOWLFiles(MySQLAdaptor adaptor, 
                                    Collection specieses,
                                    String dirName) throws Exception {
        BioPAXOWLIDGenerator idGenerator = new BioPAXOWLIDGenerator();
        for (Iterator it = specieses.iterator(); it.hasNext();) {
            GKInstance species = (GKInstance) it.next();
            idGenerator.setSpecies(species);
            idGenerator.reset();
            Collection events = adaptor.fetchInstanceByAttribute("Event","species","=",species);
            adaptor.loadInstanceAttributeValues(events,
                                                new String[]{ReactomeJavaConstants.hasEvent,
                                                             ReactomeJavaConstants.species});
            filterEventsForSpecies(events, species);
            events = InstanceUtilities.grepTopLevelEvents(events);
            
            if (events == null || events.isEmpty()) {
            	continue;
            }
            
            Document biopaxModel = convert(events, idGenerator);
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            String fileName = species.getDisplayName();
            fileName = fileName.replaceAll("[(|)|/]", "_");
            FileOutputStream fos = new FileOutputStream(dirName + File.separator + fileName + ".owl");
            outputter.output(biopaxModel, fos);
        }
    }
    
    /**
     * There is a model change. Now a pathway can have multiple species. However, for BioPAX purpose,
     * we have to pick up pathways with the first species as the specified one.
     * @param events
     * @param species
     * @return
     * @throws Exception
     */
    private void filterEventsForSpecies(Collection events,
                                        GKInstance species) throws Exception {
        for (Iterator it = events.iterator(); it.hasNext();) {
            GKInstance event = (GKInstance) it.next();
            // Don't show multi-species events 
            List<GKInstance> eventSpecies = (List<GKInstance>) event.getAttributeValuesList(ReactomeJavaConstants.species);
            
            for (GKInstance spc : eventSpecies) {
            	if (spc != species) {
            		it.remove();
            		break;
            	}
            }
        }
    }
    
    private boolean validateDir(String dirName) {
        File dir = new File(dirName);
        if (dir.exists())
            return true;
        // Try to create a dir
        return dir.mkdir();
    }

    protected Document convert(Collection events,
                               BioPAXOWLIDGenerator idGenerator) throws Exception {
        ReactomeToBioPAXXMLConverter converter = new ReactomeToBioPAXXMLConverter();
        converter.setIDGenerator(idGenerator);
        converter.convert(events);
        Document biopaxModel = converter.getBioPAXModel();
        return biopaxModel;
    }
    
                                                                    
}
