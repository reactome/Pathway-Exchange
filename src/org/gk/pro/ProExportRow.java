package org.gk.pro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProExportRow {
    private String entityType;
    private String identifier;
    private String location;
    private String accession;
    private String startPosition;
    private String endPosition;
    private List<String> modifications;
    private List<String> freeTexts;
    private String displayName;

    public ProExportRow() {
        modifications = new ArrayList<String>();
        freeTexts = new ArrayList<String>();
    }

    void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    private String getEntityType() {
        return entityType;
    }

    void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    private String getIdentifier() {
        return identifier;
    }

    void setLocation(String location) {
        this.location = location;
    }

    private String getLocation() {
        return location;
    }

    void setAccession(String accession) {
        this.accession = accession;
    }

    private String getAccession() {
        return accession;
    }

    void setStartPostion(String startPosition) {
        this.startPosition = startPosition;
    }

    private String getStartPostion() {
        return startPosition;
    }

    void setEndPostion(String endPosition) {
        this.endPosition = endPosition;
    }

    private String getEndPostion() {
        return endPosition;
    }

    void addModification(String modification) {
        modifications.add(modification);
    }

    String getModifications() {
        StringBuilder mods = new StringBuilder();

        for (String modification : modifications)
            mods.append(modification);

        return mods.toString();
    }

    void addFreeText(String freeText) {
        freeTexts.add(freeText);
    }

    String getFreeText() {
        StringBuilder free = new StringBuilder();

        if (freeTexts.size() > 0) {
            free.append(freeTexts.get(0));
            if (freeTexts.size() > 1) {
                for (String freeText : freeTexts.subList(1, freeTexts.size()))
                    free.append(ProExporterConstants.freeTextDelimiter.concat(freeText));
            }
        }

        return free.toString();
    }

    void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    private String getDisplayName() {
        return displayName;
    }

    List<String> getColumnHeaders() {
        return Arrays.asList("Type",
                             "Identifier",
                             "Location",
                             "Accession",
                             "Start",
                             "End",
                             "Modifications",
                             "Free Text",
                             "Display Name");
    }

    /**
     * Return the current row data as a String.
     *
     * Order is as follows:
     * <ul>
     *   <li>Entity type (Protein, Complex, etc).</li>
     *   <li>Reactome identifier (R-HSA-).</li>
     *   <li>Subcellular location (GO:).</li>
     *   <li>UniProtKB accession (with specific isoform, if indicated).</li>
     *   <li>Start position of the sequence (if unknown, use '?').</li>
     *   <li>End position of the sequence (if unknown, use '?').</li>
     *   <li>Modifications</li>
     *   <li>Free text (where necessary).</li>
     *   <li>Display Name</li>
     * </ul>
     *
     * @return String
     */
    String getRow() {
        StringBuilder row = new StringBuilder();
        List<String> cells = new ArrayList<String>();

        cells.add(getEntityType());
        cells.add(getIdentifier());
        cells.add(getLocation());
        cells.add(getAccession());
        cells.add(getStartPostion());
        cells.add(getEndPostion());
        cells.add(getModifications());
        cells.add(getFreeText());
        cells.add(getDisplayName());

        row.append(cells.get(0));
        for (String cell : cells.subList(1, cells.size()))
            row.append(ProExporterConstants.delimiter.concat(cell));

        return row.toString();
    }
}
