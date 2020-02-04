package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;

public class IntraChainCrosslinkedResidue extends CrosslinkedResidue {
    private static int index = 1;
    private final String modificationType = ProExporterConstants.crosslink;

    public IntraChainCrosslinkedResidue() {
    }

    public static void resetIndex() {
        index = 1;
    }

    /**
     * Return the PsiMod identifiers for a given modified residue.
     *
     * E.g. modifiedResidue with dbID 8874875 (Intra-chain Crosslink via half cystine at 14 and 47)
     * would return "+14=MOD:00798[CROSSLINK1@47]".
     *
     * @param modifiedResidue
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    public String exportPsiModIdentifier(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        String modification = super.exportModification(modifiedResidue);
        if (modification == null || modification.length() == 0)
            return null;
        return modification + getCrosslink(modifiedResidue);
    }

    /**
     * Return the ChEBI or Reactome identifiers for a given modified residue.
     *
     * E.g. modifiedResidue with dbID 8874875 (Intra-chain Crosslink via half cystine at 14 and 47)
     * would return "+14=CHEBI:23514[CROSSLINK1@47]".
     *
     * @param modifiedResidue
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    public String exportModificationIdentifier(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        String modIdentifier = getModIdentifier(modifiedResidue);
        if (modIdentifier == null || modIdentifier.length() == 0)
            return null;
        String crosslink = getCrosslink(modifiedResidue);
        return modIdentifier + crosslink;
    }

    /**
     * Return the crosslink string for a given residue.
     *
     * E.g. modifiedResidue with dbID 8874875 (Intra-chain Crosslink via half cystine at 14 and 47)
     * would return "[CROSSLINK1@47]".
     *
     * @param modifiedResidue
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private String getCrosslink(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        String secCoordinate = "";
        boolean isSecondResiduePresent = (modifiedResidue.getAttributeValue(ReactomeJavaConstants.secondCoordinate) != null);
        if (isSecondResiduePresent)
            secCoordinate = safeString(modifiedResidue.getAttributeValue(ReactomeJavaConstants.secondCoordinate));

        String crosslink = ProExporterConstants.leftBracket + modificationType +
                (index++) + ProExporterConstants.at + secCoordinate + ProExporterConstants.rightBracket;

        return crosslink;
    }

    /**
     * Return the free text value for a given modifiedResidue.
     *
     * @param modifiedResidue
     * @param isSecondResiduePresent
     * @return String
     */
    public String exportFreeText(GKInstance modifiedResidue) {
        return modificationType + (index++) + ProExporterConstants.equals + modifiedResidue.getDisplayName();
    }
}
