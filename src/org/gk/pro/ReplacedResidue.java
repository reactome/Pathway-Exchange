package org.gk.pro;

import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;

public class ReplacedResidue extends GeneticallyModifiedResidue {
    public ReplacedResidue() {
    }

    /**
     * Return the modification value for a given replaced residue.
     *
     * @param modifiedResidue
     * @return String
     */
    public String exportModification(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        if (modifiedResidue == null)
            return null;

        List<Object> psiMods = modifiedResidue.getAttributeValuesList(ReactomeJavaConstants.psiMod);
        String output = "";
        String coordinate = safeString(getCoordinate(modifiedResidue));
        GKInstance psiMod = null;
        String identifier = null;
        for (Object object : psiMods) {
            psiMod = (GKInstance) object;
            identifier = safeString(psiMod.getAttributeValue(ReactomeJavaConstants.identifier));
            output += ProExporterConstants.plus + coordinate + ProExporterConstants.mod + identifier;
        }

        return output;
    }

}
