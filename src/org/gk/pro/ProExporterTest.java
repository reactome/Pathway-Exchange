package org.gk.pro;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;

public class ProExporterTest {
    private MySQLAdaptor dba;
    private ProExporter proExporter;
    private GKInstance ewas;
    private String expected;
    private ProExportRow row;

    public ProExporterTest() throws SQLException {
        proExporter = new ProExporter();
        dba =  new MySQLAdaptor("localhost",
                                "reactome",
                                "liam",
                                ")8J7m]!%[<");
    }

    private MySQLAdaptor getDBA() {
        return dba;
    }

    /**
     * Convert varargs to a string output with the appropriate delimiter.
     *
     * @param fields
     * @return String
     */
    private String getTestRow(Object... fields) {
        String output = "";
        for (Object field : fields)
            output += field + ProExporterConstants.delimiter;

        // Remove trailing delimiter.
        output = output.substring(0, output.length() - 1);
        return output;
    }

    @Test
    public void testGetRow() throws Exception {
        String accession;
        String displayName;
        String freeText;
        String identifier;
        String location;
        String modifications;
        String type = ProExporterConstants.ewas;
        int end;
        int start;

        // FragmentInsertionModification
        ewas = getDBA().fetchInstance(1839016L);
        identifier = "R-HSA-1839016";
        location = "GO:0005829";
        accession = "Q9UBW7";
        start = 1;
        end = 913;
        modifications = "+914=INSERTION1+766=MOD:00048+=MOD:00048";
        freeText = "INSERTION1=Insertion of residues 429 to 822 at 914 from UniProt:P11362 FGFR1";
        displayName = "ZMYM2-p-2Y-FGFR1 fusion [cytosol]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        assertEquals(expected, proExporter.getRow(ewas));

        // FragmentDeletionModification
        ewas = getDBA().fetchInstance(5339694L);
        identifier = "R-HSA-5339694";
        location = "GO:0005886";
        accession = "O75197";
        start = 32;
        end = 1615;
        modifications = "+=DELETION1";
        freeText = "DELETION1=Deletion of residues 666 to 809";
        displayName = "LRP5 del666-809 [plasma membrane]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        assertEquals(expected, proExporter.getRow(ewas));

        // FragmentDeletionModification
        ewas = getDBA().fetchInstance(5604959L);
        identifier = "R-HSA-5604959";
        location = "GO:0005789";
        accession = "P22309";
        start = 26;
        end = 533;
        modifications = "+=DELETION1";
        freeText = "DELETION1=Deletion of residues 294 to 297";
        displayName = "UGT1A1 del294-297 [endoplasmic reticulum membrane]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        assertEquals(expected, proExporter.getRow(ewas));

        // FragmentReplacedModification
        ewas = getDBA().fetchInstance(5659656L);
        identifier = "R-HSA-5659656";
        location = "GO:0005886";
        accession = "Q9UN76";
        start = 1;
        end = 642;
        modifications = "+=REPLACED1";
        freeText = "REPLACED1=Replacement of residues 20649 to 20469 by T";
        displayName = "SLC6A14 20649C-T [plasma membrane]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        assertEquals(expected, proExporter.getRow(ewas));

        // ReplacedResidue
        ewas = getDBA().fetchInstance(9606687L);
        identifier = "R-HSA-9606687";
        location = "GO:0005654";
        accession = "Q9UIF7-6";
        start = 1;
        end = 521;
        modifications = "+255=MOD:01643+255=MOD:00029";
        freeText = "";
        displayName = "MUTYH-6 M255V [nucleoplasm]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        assertEquals(expected, proExporter.getRow(ewas));

        // IntraChainCrosslinkedResidue #1
        ewas = getDBA().fetchInstance(8874904L);
        identifier = "R-HSA-8874904";
        location = "GO:0005758";
        accession = "Q9NRP2";
        start = 1;
        end = 79;
        modifications = "+14=MOD:00798[CROSSLINK1@47]+14=CHEBI:23514[CROSSLINK1@47]" +
                        "+24=MOD:00798[CROSSLINK2@37]+24=CHEBI:23514[CROSSLINK2@37]";
        freeText = "CROSSLINK1=Intra-chain Crosslink via half cystine at 14 and 47^|^" +
                   "CROSSLINK2=Intra-chain Crosslink via half cystine at 24 and 37";
        displayName = "4xHC-CMC2 [mitochondrial intermembrane space]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        assertEquals(expected, proExporter.getRow(ewas));

        // IntraChainCrosslinkedResidue #2
        ewas = getDBA().fetchInstance(6797422L);
        identifier = "R-HSA-6797422";
        location = "GO:0005576";
        accession = "P09429";
        start = 2;
        end = 215;
        modifications = "+23=MOD:00798[CROSSLINK1@45]+23=CHEBI:30770[CROSSLINK1@45]";
        freeText = "CROSSLINK1=Intra-chain Crosslink via half cystine at 23 and 45";
        displayName = "HC23,45-HMGB1 [extracellular region]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        assertEquals(expected, proExporter.getRow(ewas));

        // IntraChainCrosslinkedResidue #3
        ewas = getDBA().fetchInstance(8874912L);
        identifier = "R-HSA-8874912";
        location = "GO:0005758";
        accession = "Q9BSY4";
        start = 1;
        end = 110;
        modifications = "+58=MOD:00798[CROSSLINK1@89]+58=CHEBI:23514[CROSSLINK1@89]";
        freeText = "CROSSLINK1=Intra-chain Crosslink via half cystine at 58 and 89";
        displayName = "4xHC-CHCHD5 [mitochondrial intermembrane space]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        assertEquals(expected, proExporter.getRow(ewas));

    }

    @Test
    public void testGetFreeText() throws Exception {
        // FragmentInsertionModification
        ewas = getDBA().fetchInstance(1839016L);
        expected = "INSERTION1=Insertion of residues 429 to 822 at 914 from UniProt:P11362 FGFR1";
        row = new ProExportRow();
        proExporter.setModifications(ewas, row);
        assertEquals(expected, row.getFreeText());

        // IntraChainCrosslinkedResidue #1
        ewas = getDBA().fetchInstance(8874904L);
        expected = "CROSSLINK1=Intra-chain Crosslink via half cystine at 14 and 47^|^" +
                   "CROSSLINK2=Intra-chain Crosslink via half cystine at 24 and 37";
        row = new ProExportRow();
        proExporter.setModifications(ewas, row);
        assertEquals(expected, row.getFreeText());

        // IntraChainCrosslinkedResidue #2
        ewas = getDBA().fetchInstance(6797422L);
        expected = "CROSSLINK1=Intra-chain Crosslink via half cystine at 23 and 45";
        row = new ProExportRow();
        proExporter.setModifications(ewas, row);
        assertEquals(expected, row.getFreeText());
    }

    @Test
    public void testGetModification() throws Exception {
        // General Modifications (unknown positions)
        ewas = dba.fetchInstance(217182L);
        expected = "+=MOD:00046+=MOD:00798";
        row = new ProExportRow();
        proExporter.setModifications(ewas, row);
        assertEquals(expected, row.getModifications());

        // General Modification (at position 715)
        ewas = dba.fetchInstance(156901L);
        expected = "+715=MOD:00049";
        row = new ProExportRow();
        proExporter.setModifications(ewas, row);
        assertEquals(expected, row.getModifications());

        // FragmentInsertionModification
        ewas = dba.fetchInstance(1839016L);
        expected = "+914=INSERTION1+766=MOD:00048+=MOD:00048";
        row = new ProExportRow();
        proExporter.setModifications(ewas, row);
        assertEquals(expected, row.getModifications());

        // InterChainCrosslinkedResidue
        ewas = dba.fetchInstance(4551599L);
        expected = "+2592=MOD:01149+2592=CHEBI:24411+2592=UniProt:P63165[97]" +
                   "+2650=MOD:01149+2650=CHEBI:24411+2650=UniProt:P63165[97]" +
                   "+2723=MOD:01149+2723=CHEBI:24411+2723=UniProt:P63165[97]";
        row = new ProExportRow();
        proExporter.setModifications(ewas, row);
        assertEquals(expected, row.getModifications());

        // IntraChainCrosslinkedResidue #1
        ewas = dba.fetchInstance(8874904L);
        expected = "+14=MOD:00798[CROSSLINK1@47]+14=CHEBI:23514[CROSSLINK1@47]" +
                   "+24=MOD:00798[CROSSLINK2@37]+24=CHEBI:23514[CROSSLINK2@37]";
        row = new ProExportRow();
        proExporter.setModifications(ewas, row);
        assertEquals(expected, row.getModifications());

        // IntraChainCrosslinkedResidue #2
        ewas = dba.fetchInstance(6797422L);
        expected = "+23=MOD:00798[CROSSLINK1@45]+23=CHEBI:30770[CROSSLINK1@45]";
        row = new ProExportRow();
        proExporter.setModifications(ewas, row);
        assertEquals(expected, row.getModifications());

        // GroupModifiedResidue #1
        ewas = dba.fetchInstance(2046248L);
        expected = "+=MOD:00831+=CHEBI:63492";
        row = new ProExportRow();
        proExporter.setModifications(ewas, row);
        assertEquals(expected, row.getModifications());

        // GroupModifiedResidue #2
        ewas = dba.fetchInstance(8952387L);
        expected = "+94=MOD:01148+94=Reactome:R-HSA-8939707+148=MOD:01148+148=Reactome:R-HSA-8939707";
        row = new ProExportRow();
        proExporter.setModifications(ewas, row);
        assertEquals(expected, row.getModifications());
    }

}
