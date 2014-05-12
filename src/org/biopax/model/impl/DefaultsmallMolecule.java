package org.biopax.model.impl;

import java.util.Collection;
import java.util.Iterator;

import org.biopax.model.ChemicalStructure;
import org.biopax.model.SmallMolecule;

import edu.stanford.smi.protege.model.FrameID;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFSLiteral;

/**
 * Generated by Protege-OWL  (http://protege.stanford.edu/plugins/owl).
 * Source OWL Class: http://www.biopax.org/release/biopax-level2.owl#smallMolecule
 *
 * @version generated on Mon May 23 15:40:06 EDT 2005
 */
public class DefaultsmallMolecule extends DefaultphysicalEntity
         implements SmallMolecule {

    public DefaultsmallMolecule(OWLModel owlModel, FrameID id) {
        super(owlModel, id);
    }


    public DefaultsmallMolecule() {
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#CHEMICAL-FORMULA

    public String getCHEMICAL_FORMULA() {
        return (String) getPropertyValue(getCHEMICAL_FORMULAProperty());
    }


    public RDFProperty getCHEMICAL_FORMULAProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#CHEMICAL-FORMULA";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasCHEMICAL_FORMULA() {
        return getPropertyValueCount(getCHEMICAL_FORMULAProperty()) > 0;
    }


    public void setCHEMICAL_FORMULA(String newCHEMICAL_FORMULA) {
        setPropertyValue(getCHEMICAL_FORMULAProperty(), newCHEMICAL_FORMULA);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#MOLECULAR-WEIGHT

    public RDFSLiteral getMOLECULAR_WEIGHT() {
        return (RDFSLiteral) getPropertyValue(getMOLECULAR_WEIGHTProperty());
    }


    public RDFProperty getMOLECULAR_WEIGHTProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#MOLECULAR-WEIGHT";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasMOLECULAR_WEIGHT() {
        return getPropertyValueCount(getMOLECULAR_WEIGHTProperty()) > 0;
    }


    public void setMOLECULAR_WEIGHT(RDFSLiteral newMOLECULAR_WEIGHT) {
        setPropertyValue(getMOLECULAR_WEIGHTProperty(), newMOLECULAR_WEIGHT);
    }



    // Property http://www.biopax.org/release/biopax-level2.owl#STRUCTURE

    public Collection getSTRUCTURE() {
        return getPropertyValuesAs(getSTRUCTUREProperty(), ChemicalStructure.class);
    }


    public RDFProperty getSTRUCTUREProperty() {
        final String uri = "http://www.biopax.org/release/biopax-level2.owl#STRUCTURE";
        final String name = getOWLModel().getResourceNameForURI(uri);
        return getOWLModel().getRDFProperty(name);
    }


    public boolean hasSTRUCTURE() {
        return getPropertyValueCount(getSTRUCTUREProperty()) > 0;
    }


    public Iterator listSTRUCTURE() {
        return listPropertyValuesAs(getSTRUCTUREProperty(), ChemicalStructure.class);
    }


    public void addSTRUCTURE(ChemicalStructure newSTRUCTURE) {
        addPropertyValue(getSTRUCTUREProperty(), newSTRUCTURE);
    }


    public void removeSTRUCTURE(ChemicalStructure oldSTRUCTURE) {
        removePropertyValue(getSTRUCTUREProperty(), oldSTRUCTURE);
    }


    public void setSTRUCTURE(Collection newSTRUCTURE) {
        setPropertyValues(getSTRUCTUREProperty(), newSTRUCTURE);
    }
}
