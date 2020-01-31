package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaClass;

public class GroupModifiedResidue extends TranslationalModification {
    public GroupModifiedResidue() {
    }

    public String exportModification(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        String coordinate = safeString(getCoordinate(modifiedResidue));
        String psiModIdentifier = getIdentifier(modifiedResidue);
        GKInstance modification = (GKInstance) modifiedResidue.getAttributeValue(ReactomeJavaConstants.modification);
        String modIdentifier = "";
        SchemaClass cls = modification.getSchemClass();
        if (cls.isa(ReactomeJavaConstants.ReferenceGroup)) {
            String identifier = safeString(modification.getAttributeValue(ReactomeJavaConstants.identifier));
            modIdentifier = "+" + coordinate + "=CHEBI:" + identifier;
        }
        else {
            String identifier = String.valueOf(modification.getDBID());
            modIdentifier = "+" + coordinate + "=Reactome:R-HSA-" + identifier;
        }

        String mod = "+" + coordinate + "=MOD:" + psiModIdentifier;

        return mod + modIdentifier;
    }

}
