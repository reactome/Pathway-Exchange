package org.gk.pro;

import org.gk.model.GKInstance;

public class FragmentInsertionModification extends FragmentModification {
    private int index = 0;
    public FragmentInsertionModification() {
        index += 1;
    }

    private int getIndex() {
        return index;
    }

    public String export(GKInstance modifiedResidue) {
        // TODO add indexing number to output (e.g. INSERTION1, INSERTION2).
        return "INSERTION";
    }
}
