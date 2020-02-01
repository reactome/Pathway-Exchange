package org.gk.pro;

import org.gk.model.GKInstance;

public class GroupModifiedResidue extends TranslationalModification {
    public GroupModifiedResidue() {
    }

    /**
     * Return the modification value for a given group modified residue.
     *
     * @param modifiedResidue
     * @return String
     */
    public String exportModification(GKInstance modifiedResidue) throws Exception {
        return super.exportModification(modifiedResidue) + getModIdentifier(modifiedResidue);
    }

}
