package org.biopax.model.impl;

import java.util.Collection;
import java.util.Iterator;

import org.biopax.model.OpenControlledVocabulary;
import org.biopax.model.SequenceFeature;
import org.biopax.model.SequenceLocation;
import org.biopax.model.Xref;

import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;

/**
 * Generated by Protege-OWL  (http://protege.stanford.edu/plugins/owl).
 * Source OWL Class: http://www.biopax.org/release/biopax-level2.owl#sequenceFeature
 *
 * @version generated on Mon May 23 15:40:06 EDT 2005
 */
public class DefaultsequenceFeature extends DefaultutilityClass
         implements SequenceFeature {

    public DefaultsequenceFeature(OWLModel owlModel, FrameID id) {
        super(owlModel, id);
    }


    public DefaultsequenceFeature() {
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#FEATURE-LOCATION

    public Collection getFEATURE_LOCATION() {
        return getPropertyValuesAs(getFEATURE_LOCATIONProperty(), SequenceLocation.class);
    }


    public RDFProperty getFEATURE_LOCATIONProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#FEATURE-LOCATION";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasFEATURE_LOCATION() {
        return getPropertyValueCount(getFEATURE_LOCATIONProperty()) > 0;
    }


    public Iterator listFEATURE_LOCATION() {
        return listPropertyValuesAs(getFEATURE_LOCATIONProperty(), SequenceLocation.class);
    }


    public void addFEATURE_LOCATION(SequenceLocation newFEATURE_LOCATION) {
        addPropertyValue(getFEATURE_LOCATIONProperty(), newFEATURE_LOCATION);
    }


    public void removeFEATURE_LOCATION(SequenceLocation oldFEATURE_LOCATION) {
        removePropertyValue(getFEATURE_LOCATIONProperty(), oldFEATURE_LOCATION);
    }


    public void setFEATURE_LOCATION(Collection newFEATURE_LOCATION) {
        setPropertyValues(getFEATURE_LOCATIONProperty(), newFEATURE_LOCATION);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#FEATURE-TYPE

    public OpenControlledVocabulary getFEATURE_TYPE() {
        return (OpenControlledVocabulary) getPropertyValueAs(getFEATURE_TYPEProperty(), OpenControlledVocabulary.class);
    }


    public RDFProperty getFEATURE_TYPEProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#FEATURE-TYPE";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasFEATURE_TYPE() {
        return getPropertyValueCount(getFEATURE_TYPEProperty()) > 0;
    }


    public void setFEATURE_TYPE(OpenControlledVocabulary newFEATURE_TYPE) {
        setPropertyValue(getFEATURE_TYPEProperty(), newFEATURE_TYPE);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#NAME

    public String getNAME() {
        return (String) getPropertyValue(getNAMEProperty());
    }


    public RDFProperty getNAMEProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#NAME";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasNAME() {
        return getPropertyValueCount(getNAMEProperty()) > 0;
    }


    public void setNAME(String newNAME) {
        setPropertyValue(getNAMEProperty(), newNAME);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#SHORT-NAME

    public String getSHORT_NAME() {
        return (String) getPropertyValue(getSHORT_NAMEProperty());
    }


    public RDFProperty getSHORT_NAMEProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#SHORT-NAME";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasSHORT_NAME() {
        return getPropertyValueCount(getSHORT_NAMEProperty()) > 0;
    }


    public void setSHORT_NAME(String newSHORT_NAME) {
        setPropertyValue(getSHORT_NAMEProperty(), newSHORT_NAME);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#SYNONYMS

    public Collection getSYNONYMS() {
        return getPropertyValues(getSYNONYMSProperty());
    }


    public RDFProperty getSYNONYMSProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#SYNONYMS";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasSYNONYMS() {
        return getPropertyValueCount(getSYNONYMSProperty()) > 0;
    }


    public Iterator listSYNONYMS() {
        return listPropertyValues(getSYNONYMSProperty());
    }


    public void addSYNONYMS(String newSYNONYMS) {
        addPropertyValue(getSYNONYMSProperty(), newSYNONYMS);
    }


    public void removeSYNONYMS(String oldSYNONYMS) {
        removePropertyValue(getSYNONYMSProperty(), oldSYNONYMS);
    }


    public void setSYNONYMS(Collection newSYNONYMS) {
        setPropertyValues(getSYNONYMSProperty(), newSYNONYMS);
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
