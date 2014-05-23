package org.biopax.model.impl;

import java.util.Collection;
import java.util.Iterator;

import org.biopax.model.DataSource;
import org.biopax.model.Xref;

import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;

/**
 * Generated by Protege-OWL  (http://protege.stanford.edu/plugins/owl).
 * Source OWL Class: http://www.biopax.org/release/biopax-level2.owl#dataSource
 *
 * @version generated on Mon May 23 15:40:06 EDT 2005
 */
public class DefaultdataSource extends DefaultexternalReferenceUtilityClass
         implements DataSource {

    public DefaultdataSource(OWLModel owlModel, FrameID id) {
        super(owlModel, id);
    }


    public DefaultdataSource() {
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#NAME

    public Collection getNAME() {
        return getPropertyValues(getNAMEProperty());
    }


    public RDFProperty getNAMEProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#NAME";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasNAME() {
        return getPropertyValueCount(getNAMEProperty()) > 0;
    }


    public Iterator listNAME() {
        return listPropertyValues(getNAMEProperty());
    }


    public void addNAME(String newNAME) {
        addPropertyValue(getNAMEProperty(), newNAME);
    }


    public void removeNAME(String oldNAME) {
        removePropertyValue(getNAMEProperty(), oldNAME);
    }


    public void setNAME(Collection newNAME) {
        setPropertyValues(getNAMEProperty(), newNAME);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#XREF

    public Collection getXREF() {
        return getPropertyValuesAs(getXREFProperty(), Xref.class);
    }


    public RDFProperty getXREFProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#XREF";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasXREF() {
        return getPropertyValueCount(getXREFProperty()) > 0;
    }


    public Iterator listXREF() {
        return listPropertyValuesAs(getXREFProperty(), Xref.class);
    }


    public void addXREF(Xref newXREF) {
        addPropertyValue(getXREFProperty(), newXREF);
    }


    public void removeXREF(Xref oldXREF) {
        removePropertyValue(getXREFProperty(), oldXREF);
    }


    public void setXREF(Collection newXREF) {
        setPropertyValues(getXREFProperty(), newXREF);
    }
}