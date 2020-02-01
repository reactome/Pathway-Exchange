package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;

public class FragmentDeletionModification extends FragmentModification {
    private static int index = 1;
    private final String modificationType = ProExporterConstants.deletion;

    public FragmentDeletionModification() {
    }

    public static void resetIndex() {
        index = 1;
    }

    public String exportModification(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        return super.exportModification(modifiedResidue, modificationType, index++);
    }

    public String exportFreeText(GKInstance modifiedResidue) {
        return super.exportFreeText(modifiedResidue, modificationType, index++);
    }
}
