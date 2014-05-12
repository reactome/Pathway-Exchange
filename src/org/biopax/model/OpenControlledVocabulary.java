package org.biopax.model;

import java.util.Collection;
import java.util.Iterator;

import edu.stanford.smi.protegex.owl.model.RDFProperty;

/**
 * Generated by Protege-OWL  (http://protege.stanford.edu/plugins/owl).
 * Source OWL Class: http://www.biopax.org/release/biopax-level2.owl#openControlledVocabulary
 *
 * @version generated on Mon May 23 15:40:06 EDT 2005
 */
public interface OpenControlledVocabulary extends ExternalReferenceUtilityClass {

    // Property http://www.biopax.org/release/biopax-level2.owl#TERM

    Collection getTERM();

    RDFProperty getTERMProperty();

    boolean hasTERM();

    Iterator listTERM();

    void addTERM(String newTERM);

    void removeTERM(String oldTERM);

    void setTERM(Collection newTERM);


    // Property http://www.biopax.org/release/biopax-level2.owl#XREF

    Collection getXREF();

    RDFProperty getXREFProperty();

    boolean hasXREF();

    Iterator listXREF();

    void addXREF(Xref newXREF);

    void removeXREF(Xref oldXREF);

    void setXREF(Collection newXREF);
}
