package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;

public class FragmentReplacedModification extends FragmentModification {
    private static int index = 1;
    private final String modificationType = "REPLACED";

    public FragmentReplacedModification() {
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
