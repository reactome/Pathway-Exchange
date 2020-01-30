package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;

public class FragmentDeletionModification extends FragmentModification {
    private static int index = 0;
    private final String modificationType = "DELETION";

    public FragmentDeletionModification() {
    }

    public static void resetIndex() {
        index = 0;
    }

    public String exportModification(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        return modificationType + (index++) + super.exportModification(modifiedResidue);
    }

    public String exportFreeText(GKInstance modifiedResidue) {
        return modificationType + (index++) + modifiedResidue.getDisplayName();
    }
}
