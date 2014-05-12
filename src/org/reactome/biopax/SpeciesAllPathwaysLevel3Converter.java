/*
 * Created on Oct 9, 2008
 *
 */
package org.reactome.biopax;

import java.util.Collection;

import org.jdom.Document;


/**
 * This class is used to create BioPAX level 3 export in one file for one species.
 * @author wgm
 *
 */
public class SpeciesAllPathwaysLevel3Converter extends SpeciesAllPathwaysConverter {
    
    public SpeciesAllPathwaysLevel3Converter() {
    }

    /**
     * Override the super class method to dump the Reactome pathways in BioPAX level 3.
     */
    @Override
    protected Document convert(Collection events,
                               BioPAXOWLIDGenerator idGenerator) throws Exception {
        ReactomeToBioPAX3XMLConverter converter = new ReactomeToBioPAX3XMLConverter();
        converter.setIDGenerator(idGenerator);
        converter.convert(events);
        Document biopaxModel = converter.getBioPAXModel();
        return biopaxModel;
    }
    
    public static void main(String[] args) throws Exception {
        SpeciesAllPathwaysLevel3Converter converter = new SpeciesAllPathwaysLevel3Converter();
        converter.doDump(args);
    }
    
}
