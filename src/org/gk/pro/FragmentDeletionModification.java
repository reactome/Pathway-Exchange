package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;

public class FragmentDeletionModification extends FragmentModification {
    private final String modificationType = ProExporterConstants.deletion;

    public FragmentDeletionModification() {
    }

    public String exportModification(GKInstance modifiedResidue, int index) throws InvalidAttributeException, Exception {
        return super.exportModification(modifiedResidue, modificationType, index);
    }

    public String exportFreeText(GKInstance modifiedResidue) {
        return super.exportFreeText(modifiedResidue, modificationType);
    }
}
