package org.biopax.model;

import java.util.Collection;
import java.util.Iterator;

import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFSLiteral;

/**
 * Generated by Protege-OWL  (http://protege.stanford.edu/plugins/owl).
 * Source OWL Class: http://www.biopax.org/release/biopax-level2.owl#publicationXref
 *
 * @version generated on Mon May 23 15:40:06 EDT 2005
 */
public interface PublicationXref extends Xref {

    // Property http://www.biopax.org/release/biopax-level2.owl#AUTHORS

    Collection getAUTHORS();

    RDFProperty getAUTHORSProperty();

    boolean hasAUTHORS();

    Iterator listAUTHORS();

    void addAUTHORS(String newAUTHORS);

    void removeAUTHORS(String oldAUTHORS);

    void setAUTHORS(Collection newAUTHORS);


    // Property http://www.biopax.org/release/biopax-level2.owl#SOURCE

    Collection getSOURCE();

    RDFProperty getSOURCEProperty();

    boolean hasSOURCE();

    Iterator listSOURCE();

    void addSOURCE(String newSOURCE);

    void removeSOURCE(String oldSOURCE);

    void setSOURCE(Collection newSOURCE);


    // Property http://www.biopax.org/release/biopax-level2.owl#TITLE

    String getTITLE();

    RDFProperty getTITLEProperty();

    boolean hasTITLE();

    void setTITLE(String newTITLE);


    // Property http://www.biopax.org/release/biopax-level2.owl#URL

    Collection getURL();

    RDFProperty getURLProperty();

    boolean hasURL();

    Iterator listURL();

    void addURL(String newURL);

    void removeURL(String oldURL);

    void setURL(Collection newURL);


    // Property http://www.biopax.org/release/biopax-level2.owl#YEAR

    RDFSLiteral getYEAR();

    RDFProperty getYEARProperty();

    boolean hasYEAR();

    void setYEAR(RDFSLiteral newYEAR);
}
