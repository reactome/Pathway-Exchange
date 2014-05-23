package org.biopax.model.impl;

import org.biopax.model.Xref;

import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;

/**
 * Generated by Protege-OWL  (http://protege.stanford.edu/plugins/owl).
 * Source OWL Class: http://www.biopax.org/release/biopax-level2.owl#xref
 *
 * @version generated on Mon May 23 15:40:06 EDT 2005
 */
public class Defaultxref extends DefaultexternalReferenceUtilityClass
         implements Xref {

    public Defaultxref(OWLModel owlModel, FrameID id) {
        super(owlModel, id);
    }


    public Defaultxref() {
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#DB

    public String getDB() {
        return (String) getPropertyValue(getDBProperty());
    }


    public RDFProperty getDBProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#DB";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasDB() {
        return getPropertyValueCount(getDBProperty()) > 0;
    }


    public void setDB(String newDB) {
        setPropertyValue(getDBProperty(), newDB);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#DB-VERSION

    public String getDB_VERSION() {
        return (String) getPropertyValue(getDB_VERSIONProperty());
    }


    public RDFProperty getDB_VERSIONProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#DB-VERSION";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasDB_VERSION() {
        return getPropertyValueCount(getDB_VERSIONProperty()) > 0;
    }


    public void setDB_VERSION(String newDB_VERSION) {
        setPropertyValue(getDB_VERSIONProperty(), newDB_VERSION);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#ID

    public String getID() {
        return (String) getPropertyValue(getIDProperty());
    }


    public RDFProperty getIDProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#ID";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasID() {
        return getPropertyValueCount(getIDProperty()) > 0;
    }


    public void setID(String newID) {
        setPropertyValue(getIDProperty(), newID);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#ID-VERSION

    public String getID_VERSION() {
        return (String) getPropertyValue(getID_VERSIONProperty());
    }


    public RDFProperty getID_VERSIONProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#ID-VERSION";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasID_VERSION() {
        return getPropertyValueCount(getID_VERSIONProperty()) > 0;
    }


    public void setID_VERSION(String newID_VERSION) {
        setPropertyValue(getID_VERSIONProperty(), newID_VERSION);
    }
}