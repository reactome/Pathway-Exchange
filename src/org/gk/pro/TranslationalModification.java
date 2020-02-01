package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;

public class TranslationalModification extends AbstractModifiedResidue {
    public TranslationalModification() {
    }

    /**
     * Return the ChEBI identifier for a given modifiedResidue if the class of the modification
     * attribute is "ReferenceGroup".
     *
     * Otherwise, the class of the modification attribute will be either "EntitySet" or "Polymer",
     * in which case return the Reactome stableIdentifier value.
     *
     * @param modifiedResidue
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    protected String getModIdentifier(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        String identifier = null;
        String coordinate = safeString(getCoordinate(modifiedResidue));
        GKInstance modification = (GKInstance) modifiedResidue.getAttributeValue(ReactomeJavaConstants.modification);
        if (modification == null)
            return "";
        if (modification.getSchemClass().isa(ReactomeJavaConstants.ReferenceGroup)) {
            identifier = safeString(modification.getAttributeValue(ReactomeJavaConstants.identifier));
            return ProExporterConstants.plus + coordinate + ProExporterConstants.chebi + identifier;
        }
        else {
            identifier = String.valueOf(modification.getDBID());
            return ProExporterConstants.plus + coordinate + ProExporterConstants.reactome +
                   ProExporterConstants.stableIdPrefix + identifier;
        }
    }

}
