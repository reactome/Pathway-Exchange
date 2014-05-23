package org.biopax.model;

import java.util.Collection;
import java.util.Iterator;

import edu.stanford.smi.protegex.owl.model.RDFProperty;

/**
 * Generated by Protege-OWL  (http://protege.stanford.edu/plugins/owl).
 * Source OWL Class: http://www.biopax.org/release/biopax-level2.owl#conversion
 *
 * @version generated on Mon May 23 15:40:06 EDT 2005
 */
public interface Conversion extends PhysicalInteraction {

    // Property http://www.biopax.org/release/biopax-level2.owl#LEFT

    Collection getLEFT();

    RDFProperty getLEFTProperty();

    boolean hasLEFT();

    Iterator listLEFT();

    void addLEFT(PhysicalEntityParticipant newLEFT);

    void removeLEFT(PhysicalEntityParticipant oldLEFT);

    void setLEFT(Collection newLEFT);


    // Property http://www.biopax.org/release/biopax-level2.owl#RIGHT

    Collection getRIGHT();

    RDFProperty getRIGHTProperty();

    boolean hasRIGHT();

    Iterator listRIGHT();

    void addRIGHT(PhysicalEntityParticipant newRIGHT);

    void removeRIGHT(PhysicalEntityParticipant oldRIGHT);

    void setRIGHT(Collection newRIGHT);


    // Property http://www.biopax.org/release/biopax-level2.owl#SPONTANEOUS

    Object getSPONTANEOUS();

    RDFProperty getSPONTANEOUSProperty();

    boolean hasSPONTANEOUS();

    void setSPONTANEOUS(Object newSPONTANEOUS);
}