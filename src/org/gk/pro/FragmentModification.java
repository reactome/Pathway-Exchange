package org.gk.pro;

import org.gk.model.GKInstance;

public abstract class FragmentModification extends GeneticallyModifiedResidue {
    public FragmentModification() {
    }

    public abstract String exportFreeText(GKInstance modifiedResidue);
}
