package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;

public abstract class AbstractModifiedResidue {
    public String modificationType;
    
    public String getModType() {
        return modificationType;
    }

    public AbstractModifiedResidue() {
    }

    /**
     * Return the modification value for a given modifiedResidue.
     *
     * E.g. ModifiedResidue with dbID 217001 (O-phospho-L-serine at unknown position)
     * would return "+=MOD:00046"
     *
     * @param modifiedResidue
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    public String exportModification(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        if (modifiedResidue == null)
            return null;

        if (modifiedResidue.getSchemClass().isValidAttribute(ReactomeJavaConstants.psiMod)) {
            GKInstance psiMod = (GKInstance) modifiedResidue.getAttributeValue(ReactomeJavaConstants.psiMod);
            String coordinate = safeString(getCoordinate(modifiedResidue));
            String identifier = safeString(psiMod.getAttributeValue(ReactomeJavaConstants.identifier));

            return ProExporterConstants.plus + coordinate + ProExporterConstants.mod + identifier;
        }

        return null;
    }

    /**
     * Dummy method used for reflection purposes in {@link ProExporter#getFreeText(List)}. 
     * 
     * @param residue
     * @return String
     */
    public abstract String exportFreeText(GKInstance residue);

    /**
     * Return the coordinate value for a given modifiedResidue.
     *
     * @param modifiedResidue
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    public Integer getCoordinate(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        Integer coordinate = null;
        if (modifiedResidue.getSchemClass().isValidAttribute(ReactomeJavaConstants.coordinate))
            coordinate = (Integer) modifiedResidue.getAttributeValue(ReactomeJavaConstants.coordinate);

        return coordinate;
    }

    /**
     * Utility method to return "" if input is null, otherwise return the String value of input.
     *
     * @param input
     * @return String
     */
    protected String safeString(Object input) {
        return (input == null ? "" : String.valueOf(input));
    }

}
