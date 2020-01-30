package org.gk.pro;

public class FragmentModification extends GeneticallyModifiedResidue {
    private static int index = 0;

    public FragmentModification() {
    }

    protected int getIndex() {
        return index;
    }

    protected static void resetIndex() {
        index = 0;
    }

}
