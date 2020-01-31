package org.gk.pro;

import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;

public class AbstractModifiedResidue {
    public AbstractModifiedResidue() {
    }

    public String exportModification(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        if (modifiedResidue == null)
            return null;
        Integer coordinate = getCoordinate(modifiedResidue);
        String identifier = getIdentifier(modifiedResidue);

        return "+" + (coordinate == null ? "" : String.valueOf(coordinate)) + "=MOD:" + identifier;
    }

    public Integer getCoordinate(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        Integer coordinate = null;
        if (modifiedResidue.getSchemClass().isValidAttribute(ReactomeJavaConstants.coordinate))
            coordinate = (Integer) modifiedResidue.getAttributeValue(ReactomeJavaConstants.coordinate);

        return coordinate;
    }

    public String getIdentifier(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        GKInstance psiMod = null;
        if (modifiedResidue.getSchemClass().isValidAttribute(ReactomeJavaConstants.psiMod)) {
            psiMod = (GKInstance) modifiedResidue.getAttributeValue(ReactomeJavaConstants.psiMod);
            String identifier = safeString(psiMod.getAttributeValue(ReactomeJavaConstants.identifier));
            return identifier;
        }

        return "";
    }

    protected String safeString(Object input) {
        return (input == null ? "" : String.valueOf(input));
    }

    public String exportFreeText(GKInstance residue) {
        return null;
    }

    public String exportModification(List<Object> modifiedResidues, boolean isSecondResiduePresent) {
        return null;
    }
}
