package org.biopax.model.impl;

import java.util.Collection;
import java.util.Iterator;

import org.biopax.model.Confidence;
import org.biopax.model.Evidence;
import org.biopax.model.ExperimentalForm;
import org.biopax.model.OpenControlledVocabulary;
import org.biopax.model.Xref;

import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;

/**
 * Generated by Protege-OWL  (http://protege.stanford.edu/plugins/owl).
 * Source OWL Class: http://www.biopax.org/release/biopax-level2.owl#evidence
 *
 * @version generated on Mon May 23 15:40:06 EDT 2005
 */
public class Defaultevidence extends DefaultutilityClass
         implements Evidence {

    public Defaultevidence(OWLModel owlModel, FrameID id) {
        super(owlModel, id);
    }


    public Defaultevidence() {
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#CONFIDENCE

    public Collection getCONFIDENCE() {
        return getPropertyValuesAs(getCONFIDENCEProperty(), Confidence.class);
    }


    public RDFProperty getCONFIDENCEProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#CONFIDENCE";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasCONFIDENCE() {
        return getPropertyValueCount(getCONFIDENCEProperty()) > 0;
    }


    public Iterator listCONFIDENCE() {
        return listPropertyValuesAs(getCONFIDENCEProperty(), Confidence.class);
    }


    public void addCONFIDENCE(Confidence newCONFIDENCE) {
        addPropertyValue(getCONFIDENCEProperty(), newCONFIDENCE);
    }


    public void removeCONFIDENCE(Confidence oldCONFIDENCE) {
        removePropertyValue(getCONFIDENCEProperty(), oldCONFIDENCE);
    }


    public void setCONFIDENCE(Collection newCONFIDENCE) {
        setPropertyValues(getCONFIDENCEProperty(), newCONFIDENCE);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#EVIDENCE-CODE

    public Collection getEVIDENCE_CODE() {
        return getPropertyValuesAs(getEVIDENCE_CODEProperty(), OpenControlledVocabulary.class);
    }


    public RDFProperty getEVIDENCE_CODEProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#EVIDENCE-CODE";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasEVIDENCE_CODE() {
        return getPropertyValueCount(getEVIDENCE_CODEProperty()) > 0;
    }


    public Iterator listEVIDENCE_CODE() {
        return listPropertyValuesAs(getEVIDENCE_CODEProperty(), OpenControlledVocabulary.class);
    }


    public void addEVIDENCE_CODE(OpenControlledVocabulary newEVIDENCE_CODE) {
        addPropertyValue(getEVIDENCE_CODEProperty(), newEVIDENCE_CODE);
    }


    public void removeEVIDENCE_CODE(OpenControlledVocabulary oldEVIDENCE_CODE) {
        removePropertyValue(getEVIDENCE_CODEProperty(), oldEVIDENCE_CODE);
    }


    public void setEVIDENCE_CODE(Collection newEVIDENCE_CODE) {
        setPropertyValues(getEVIDENCE_CODEProperty(), newEVIDENCE_CODE);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#EXPERIMENTAL-FORM

    public Collection getEXPERIMENTAL_FORM() {
        return getPropertyValuesAs(getEXPERIMENTAL_FORMProperty(), ExperimentalForm.class);
    }


    public RDFProperty getEXPERIMENTAL_FORMProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#EXPERIMENTAL-FORM";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasEXPERIMENTAL_FORM() {
        return getPropertyValueCount(getEXPERIMENTAL_FORMProperty()) > 0;
    }


    public Iterator listEXPERIMENTAL_FORM() {
        return listPropertyValuesAs(getEXPERIMENTAL_FORMProperty(), ExperimentalForm.class);
    }


    public void addEXPERIMENTAL_FORM(ExperimentalForm newEXPERIMENTAL_FORM) {
        addPropertyValue(getEXPERIMENTAL_FORMProperty(), newEXPERIMENTAL_FORM);
    }


    public void removeEXPERIMENTAL_FORM(ExperimentalForm oldEXPERIMENTAL_FORM) {
        removePropertyValue(getEXPERIMENTAL_FORMProperty(), oldEXPERIMENTAL_FORM);
    }


    public void setEXPERIMENTAL_FORM(Collection newEXPERIMENTAL_FORM) {
        setPropertyValues(getEXPERIMENTAL_FORMProperty(), newEXPERIMENTAL_FORM);
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
