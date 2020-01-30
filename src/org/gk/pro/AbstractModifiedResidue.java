package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.InvalidAttributeException;

public class AbstractModifiedResidue {
    private static int index = 0;
    public AbstractModifiedResidue() {
        index += 1;
    }

    private int getIndex() {
        return index;
    }


    public String export(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        if (modifiedResidue != null)
            return String.valueOf(getIndex());
        if (modifiedResidue == null)
            return null;

        Integer coordinate =  (Integer) modifiedResidue.getAttributeValue(ReactomeJavaConstants.coordinate);

        GKInstance psiMod = null;
        if (modifiedResidue.getSchemClass().isValidAttribute(ReactomeJavaConstants.psiMod))
            psiMod = (GKInstance) modifiedResidue.getAttributeValue(ReactomeJavaConstants.psiMod);

        String identifier = (String) psiMod.getAttributeValue(ReactomeJavaConstants.identifier);

        return "+" + (coordinate == null ? "" : String.valueOf(coordinate)) + "=MOD:" + identifier;
    }
}
