package org.gk.pro;

import org.gk.model.GKInstance;

public class FragmentInsertionModification extends FragmentModification {
    private static int index = 0;

    public FragmentInsertionModification() {
        index += 1;
    }

    public String export(GKInstance modifiedResidue) {
        // TODO add indexing number to output (e.g. INSERTION1, INSERTION2).
        return "INSERTION";
    }
}
