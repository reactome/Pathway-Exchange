package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;

public class FragmentInsertionModification extends FragmentModification {
    private static int index = 1;
    private final String modificationType = "INSERTION";

    public FragmentInsertionModification() {
    }

    public static void resetIndex() {
        index = 1;
    }
    public String exportModification(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        return modificationType + (index++) + super.exportModification(modifiedResidue);
    }

    public String exportFreeText(GKInstance modifiedResidue) {
        return modificationType + (index++) + modifiedResidue.getDisplayName();
    }
}
