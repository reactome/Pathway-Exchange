package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;

public class FragmentInsertionModification extends FragmentModification {
    private String modificationType = ProExporterConstants.insertion;

    public FragmentInsertionModification() {
    }

    public String exportModification(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        return super.exportModification(modifiedResidue) + modificationType + ProExporterConstants.indexPlaceholder;
    }

    public String exportFreeText(GKInstance modifiedResidue) {
        return modificationType + super.exportFreeText(modifiedResidue);
    }
}
