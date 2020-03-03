package org.gk.pro;

import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;

public class FragmentModification extends GeneticallyModifiedResidue {
    public FragmentModification() {
    }

    /**
     * Return the modification value for a given FragmentModification residue.
     *
     * @param modifiedResidue
     * @param type
     * @param index
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    public String exportModification(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        if (modifiedResidue == null)
            return null;
        String coordinate = safeString(getCoordinate(modifiedResidue));

        return ProExporterConstants.plus + coordinate + ProExporterConstants.equals;
    }

    public String exportFreeText(GKInstance modifiedResidue) {
        return ProExporterConstants.indexPlaceholder + ProExporterConstants.equals + modifiedResidue.getDisplayName();
    }
}
