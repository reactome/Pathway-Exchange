package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;

public class InterChainCrosslinkedResidue extends CrosslinkedResidue {
    public InterChainCrosslinkedResidue() {
    }

    /**
     * Return the modification value for a given modifiedResidue, consisting of three parts:
     * (1) MOD id
     * (2) ChEBI id
     * (3) UniProt id
     *
     * @param modifiedResidue
     * @return String
     */
    public String exportModification(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        String coordinate = safeString(getCoordinate(modifiedResidue));
        GKInstance secRefSeq = (GKInstance) modifiedResidue.getAttributeValue(ReactomeJavaConstants.secondReferenceSequence);
        String secRefSeqIdentifier = safeString(secRefSeq.getAttributeValue(ReactomeJavaConstants.identifier));
        String secCoordinate = safeString(modifiedResidue.getAttributeValue(ReactomeJavaConstants.secondCoordinate));
        String output = "";

        // MOD (coordinate, psiMod.identifier)
        output += super.exportModification(modifiedResidue);

        // CHEBI (coordinate, modification.identifier)
        output += getModIdentifier(modifiedResidue);

        // UniProt (coordinate, secondReferenceSequence.identifier, secondCoordinate)
        output += ProExporterConstants.plus + coordinate
                + ProExporterConstants.uniprot + secRefSeqIdentifier
                + ProExporterConstants.leftBracket + secCoordinate + ProExporterConstants.rightBracket;

        return output;
    }

}
