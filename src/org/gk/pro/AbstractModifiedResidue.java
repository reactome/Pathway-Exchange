package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;

public class AbstractModifiedResidue {
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
    public String exportModification(GKInstance modifiedResidue) {
        if (modifiedResidue == null)
            return null;
        String identifier = null;
        String coordinate = null;
        GKInstance psiMod = null;

        if (modifiedResidue.getSchemClass().isValidAttribute(ReactomeJavaConstants.psiMod)) {
            try {
                psiMod = (GKInstance) modifiedResidue.getAttributeValue(ReactomeJavaConstants.psiMod);
                coordinate = safeString(getCoordinate(modifiedResidue));
                identifier = safeString(psiMod.getAttributeValue(ReactomeJavaConstants.identifier));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return ProExporterConstants.plus + coordinate + ProExporterConstants.mod + identifier;
        }

        return null;
    }

    public String exportFreeText(GKInstance residue) {
        // TODO Auto-generated method stub
        return null;
    }

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
