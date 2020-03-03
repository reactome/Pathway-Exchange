package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;

public class FragmentReplacedModification extends FragmentModification {
    private final String modificationType = ProExporterConstants.replaced;

    public FragmentReplacedModification() {
    }

    public String exportModification(GKInstance modifiedResidue, int index) throws InvalidAttributeException, Exception {
        return super.exportModification(modifiedResidue, modificationType, index);
    }

    public String exportFreeText(GKInstance modifiedResidue, int index) {
        return super.exportFreeText(modifiedResidue, modificationType, index);
    }

}
