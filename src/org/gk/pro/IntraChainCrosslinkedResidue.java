package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;

public class IntraChainCrosslinkedResidue extends CrosslinkedResidue {
    private static int index = 1;
    private final String modificationType = "CROSSLINK";
    public IntraChainCrosslinkedResidue() {
    }

    public static void resetIndex() {
        index = 1;
    }

    public String exportModification(GKInstance modifiedResidue, boolean isSecondResiduePresent) throws InvalidAttributeException, Exception {
        String coordinate = safeString(getCoordinate(modifiedResidue));
        String psiModIdentifier = getIdentifier(modifiedResidue);
        GKInstance modification = (GKInstance) modifiedResidue.getAttributeValue(ReactomeJavaConstants.modification);
        String modIdentifier = safeString(modification.getAttributeValue(ReactomeJavaConstants.identifier));

        String secCoordinate = "";
        if (isSecondResiduePresent)
            secCoordinate = safeString(modifiedResidue.getAttributeValue(ReactomeJavaConstants.secondCoordinate));


        String crosslink = "[" + modificationType + index + "@" + secCoordinate + "]";
        String mod = "+" + coordinate + "=MOD:" + psiModIdentifier + crosslink;
        String chebi = "+" + coordinate + "=CHEBI:" + modIdentifier + crosslink;

        index += 1;
        return mod + chebi;
    }

    public String exportFreeText(GKInstance modifiedResidue) {
        return modificationType + (index++) + "=" + modifiedResidue.getDisplayName();
    }
}
