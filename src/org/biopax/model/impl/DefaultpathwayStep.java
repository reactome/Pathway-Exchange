package org.biopax.model.impl;

import java.util.Collection;
import java.util.Iterator;

import org.biopax.model.PathwayStep;

import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;

/**
 * Generated by Protege-OWL  (http://protege.stanford.edu/plugins/owl).
 * Source OWL Class: http://www.biopax.org/release/biopax-level2.owl#pathwayStep
 *
 * @version generated on Mon May 23 15:40:06 EDT 2005
 */
public class DefaultpathwayStep extends DefaultutilityClass
         implements PathwayStep {

    public DefaultpathwayStep(OWLModel owlModel, FrameID id) {
        super(owlModel, id);
    }


    public DefaultpathwayStep() {
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#NEXT-STEP

    public Collection getNEXT_STEP() {
        return getPropertyValuesAs(getNEXT_STEPProperty(), PathwayStep.class);
    }


    public RDFProperty getNEXT_STEPProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#NEXT-STEP";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasNEXT_STEP() {
        return getPropertyValueCount(getNEXT_STEPProperty()) > 0;
    }


    public Iterator listNEXT_STEP() {
        return listPropertyValuesAs(getNEXT_STEPProperty(), PathwayStep.class);
    }


    public void addNEXT_STEP(PathwayStep newNEXT_STEP) {
        addPropertyValue(getNEXT_STEPProperty(), newNEXT_STEP);
    }


    public void removeNEXT_STEP(PathwayStep oldNEXT_STEP) {
        removePropertyValue(getNEXT_STEPProperty(), oldNEXT_STEP);
    }


    public void setNEXT_STEP(Collection newNEXT_STEP) {
        setPropertyValues(getNEXT_STEPProperty(), newNEXT_STEP);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#STEP-INTERACTIONS

    public Collection getSTEP_INTERACTIONS() {
        return getPropertyValues(getSTEP_INTERACTIONSProperty());
    }


    public RDFProperty getSTEP_INTERACTIONSProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#STEP-INTERACTIONS";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasSTEP_INTERACTIONS() {
        return getPropertyValueCount(getSTEP_INTERACTIONSProperty()) > 0;
    }


    public Iterator listSTEP_INTERACTIONS() {
        return listPropertyValues(getSTEP_INTERACTIONSProperty());
    }


    public void addSTEP_INTERACTIONS(Object newSTEP_INTERACTIONS) {
        addPropertyValue(getSTEP_INTERACTIONSProperty(), newSTEP_INTERACTIONS);
    }


    public void removeSTEP_INTERACTIONS(Object oldSTEP_INTERACTIONS) {
        removePropertyValue(getSTEP_INTERACTIONSProperty(), oldSTEP_INTERACTIONS);
    }


    public void setSTEP_INTERACTIONS(Collection newSTEP_INTERACTIONS) {
        setPropertyValues(getSTEP_INTERACTIONSProperty(), newSTEP_INTERACTIONS);
    }
}
