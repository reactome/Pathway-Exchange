package org.biopax.model.impl;

import org.biopax.model.BioSource;
import org.biopax.model.Protein;

import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;

/**
 * Generated by Protege-OWL  (http://protege.stanford.edu/plugins/owl).
 * Source OWL Class: http://www.biopax.org/release/biopax-level2.owl#protein
 *
 * @version generated on Mon May 23 15:40:06 EDT 2005
 */
public class Defaultprotein extends DefaultphysicalEntity
         implements Protein {

    public Defaultprotein(OWLModel owlModel, FrameID id) {
        super(owlModel, id);
    }


    public Defaultprotein() {
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#ORGANISM

    public BioSource getORGANISM() {
        return (BioSource) getPropertyValueAs(getORGANISMProperty(), BioSource.class);
    }


    public RDFProperty getORGANISMProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#ORGANISM";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasORGANISM() {
        return getPropertyValueCount(getORGANISMProperty()) > 0;
    }


    public void setORGANISM(BioSource newORGANISM) {
        setPropertyValue(getORGANISMProperty(), newORGANISM);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#SEQUENCE

    public String getSEQUENCE() {
        return (String) getPropertyValue(getSEQUENCEProperty());
    }


    public RDFProperty getSEQUENCEProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#SEQUENCE";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasSEQUENCE() {
        return getPropertyValueCount(getSEQUENCEProperty()) > 0;
    }


    public void setSEQUENCE(String newSEQUENCE) {
        setPropertyValue(getSEQUENCEProperty(), newSEQUENCE);
    }
}
