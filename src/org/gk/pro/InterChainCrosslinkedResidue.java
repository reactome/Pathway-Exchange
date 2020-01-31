package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;

public class InterChainCrosslinkedResidue extends CrosslinkedResidue {
    public InterChainCrosslinkedResidue() {
    }

    public String exportModification(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        if (modifiedResidue == null)
            return null;
        String coordinate = safeString(getCoordinate(modifiedResidue));
        String psiModIdentifier = getIdentifier(modifiedResidue);
        GKInstance modification = (GKInstance) modifiedResidue.getAttributeValue(ReactomeJavaConstants.modification);
        Object modIdentifier = modification.getAttributeValue(ReactomeJavaConstants.identifier);
        GKInstance secRefSeq = (GKInstance) modifiedResidue.getAttributeValue(ReactomeJavaConstants.secondReferenceSequence);
        Object secRefSeqIdentifier = secRefSeq.getAttributeValue(ReactomeJavaConstants.identifier);
        String secCoordinate = safeString(modifiedResidue.getAttributeValue(ReactomeJavaConstants.secondCoordinate));

        String output = "";

        // MOD (coordinate, psiMod.identifier)
        output += "+" + coordinate + "=MOD:" + psiModIdentifier;

        // CHEBI (coordinate, modification.identifier)
        output += "+" + coordinate + "=CHEBI:" + String.valueOf(modIdentifier);

        // UniProt (coordinate, secondReferenceSequence.identifier, secondCoordinate)
        output += "+" + coordinate + "=UniProt:" + String.valueOf(secRefSeqIdentifier) + "[" + secCoordinate + "]";

        return output;
    }

}
