package org.gk.pro;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaClass;
import org.junit.Before;
import org.junit.Test;

/**
 * Export modified residue data for all human EWAS instances in a given database.
 */
public class ProExporter {
    private MySQLAdaptor dba;

    public ProExporter() {
    }

    /**
     * Return the Reactome identifier (R-HSA-) for a given EWAS instance.
     *
     * @param ewas
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private String getIdentifier(GKInstance ewas) throws InvalidAttributeException, Exception {
        GKInstance stableIdentifier = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
        return (String) stableIdentifier.getAttributeValue(ReactomeJavaConstants.identifier);
    }

    /**
     * Return the  Subcellular location (GO:) of a given EWAS instance.
     *
     * @param ewas
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private String getLocation(GKInstance ewas) throws InvalidAttributeException, Exception {
        GKInstance compartment = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.compartment);
        // TODO Confirm if we should filter out non-GO databases.
        if (compartment == null)
            return null;
        GKInstance referenceDatabase = (GKInstance) compartment.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
        String prefix = referenceDatabase.getDisplayName();
        String accession = (String) compartment.getAttributeValue(ReactomeJavaConstants.accession);
        return prefix + ProExporterConstants.colon + accession;
    }

    /**
     * Return the UniProtKB accession (with specific isoform, if indicated) for a given EWAS instance.
     *
     * @param ewas
     * @return String
     * @throws Exception
     */
    private String getUniprotAccession(GKInstance ewas) throws Exception {
        GKInstance referenceEntity = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.referenceEntity);
        if (referenceEntity == null)
            return ProExporterConstants.unknown;
        return (String) referenceEntity.getAttributeValue(ReactomeJavaConstants.identifier);
    }

    /**
     * Return the start position of a given EWAS instance.
     *
     * @param ewas
     * @return String
     * @throws Exception
     */
    private Integer getStartPosition(GKInstance ewas) throws Exception {
        Object startPosition = ewas.getAttributeValue(ReactomeJavaConstants.startCoordinate);
        if (startPosition == null)
            return null;
        return (Integer) startPosition;
    }

    /**
     * Return the end position of a given EWAS instance.
     *
     * @param ewas
     * @return Integer
     * @throws Exception
     */
    private Integer getEndPosition(GKInstance ewas) throws Exception {
        Object endPosition = ewas.getAttributeValue(ReactomeJavaConstants.endCoordinate);
        if (endPosition == null)
            return null;
        return (Integer) endPosition;
    }

    /**
     * Return the modifications for a given EWAS (as referenced by the "hasModifiedResidue" attribute value).
     *
     * @param modifiedResidues
     * @return String
     * @throws Exception
     */
    private String getModifications(List<Object> modifiedResidues) throws Exception {
        if (modifiedResidues == null || modifiedResidues.size() == 0)
            return null;
        String output = "";
        AbstractModifiedResidue residueExporter = null;
        List<Object> intraChainModifiedResidues = new ArrayList<Object>();
        List<Object> stdModifiedResidues = new ArrayList<Object>();
        String modifications = null;
        String pkg = this.getClass().getPackage().getName();
        String cls = null;

        for (Object modifiedResidue : modifiedResidues) {
            if (((GKInstance) modifiedResidue).getSchemClass().isa(ReactomeJavaConstants.IntraChainCrosslinkedResidue))
                intraChainModifiedResidues.add(modifiedResidue);
            else
                stdModifiedResidues.add(modifiedResidue);
        }

        if (intraChainModifiedResidues.size() > 0)
            output += getIntraChainModifications(intraChainModifiedResidues);
        resetIndices();

        for (Object modifiedResidue : stdModifiedResidues) {
            cls = ((GKInstance) modifiedResidue).getSchemClass().getName();
            residueExporter = (AbstractModifiedResidue) Class.forName(pkg + "." + cls).getConstructor().newInstance();
            modifications = residueExporter.exportModification((GKInstance) modifiedResidue);
            if (modifications != null)
                output += modifications;
        }
        resetIndices();

        return output;
    }

    /**
     * Return the modifications for a given IntraChainCrosslinkedResidue.
     *
     * Special case of {@link #getModifications(GKInstance, List)}
     *
     * @param modifiedResidue
     * @return String
     * @throws Exception
     */
    private String getIntraChainModifications(List<Object> modifiedResidues) throws Exception {
        String output = "";
        String modification = null;
        IntraChainCrosslinkedResidue residueExporter = new IntraChainCrosslinkedResidue();
        for (Object modifiedResidue : modifiedResidues) {
            modification = residueExporter.exportPsiModIdentifier((GKInstance) modifiedResidue);
            if (modification != null)
                output += modification;
        }
        resetIndices();

        for (Object modifiedResidue : modifiedResidues) {
            modification = residueExporter.exportModificationIdentifier((GKInstance) modifiedResidue);
            if (modification != null)
                output += modification;
        }
        resetIndices();

        return output;
    }

    /**
     * Return the freeText values for a given EWAS.
     *
     * @param modifiedResidues
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private String getFreeText(List<Object> modifiedResidues) throws InvalidAttributeException, Exception {
        if (modifiedResidues == null || modifiedResidues.size() == 0)
            return null;
        String output = "";
        AbstractModifiedResidue residueExporter = null;
        String pkg = this.getClass().getPackage().getName();
        String cls = null;
        // Modification classes that have free text columns.
        List<String> allowedClasses = Arrays.asList(ReactomeJavaConstants.FragmentInsertionModification,
                                                    ReactomeJavaConstants.FragmentDeletionModification,
                                                    ReactomeJavaConstants.FragmentReplacedModification,
                                                    ReactomeJavaConstants.IntraChainCrosslinkedResidue);
        for (Object modifiedResidue : modifiedResidues) {
            if (allowedClasses.stream().noneMatch(((GKInstance) modifiedResidue).getSchemClass()::isa))
                continue;

            if (output.length() > 0)
                output += ProExporterConstants.freeTextDelimiter;

            cls = ((GKInstance) modifiedResidue).getSchemClass().getName();
            residueExporter = (AbstractModifiedResidue) Class.forName(pkg + "." + cls).getConstructor().newInstance();
            output += residueExporter.exportFreeText((GKInstance) modifiedResidue);
        }
        resetIndices();

        return output;
    }

    /**
     * Helper function to reset indices for FragmentModification residues.
     */
    private void resetIndices() {
        FragmentInsertionModification.resetIndex();
        FragmentDeletionModification.resetIndex();
        FragmentReplacedModification.resetIndex();
        IntraChainCrosslinkedResidue.resetIndex();
    }

    /**
     * Return collection of all human EWAS instances.
     *
     * @return List<GKInstance>
     * @throws Exception
     */
    private List<GKInstance> getEwasCollection(MySQLAdaptor dba) throws Exception {
        if (dba == null)
            return null;
        // Read data from database.
        SchemaClass cls = dba.getSchema().getClassByName(ReactomeJavaConstants.EntityWithAccessionedSequence);
        Collection<GKInstance> rawEwasCollection = dba.fetchInstancesByClass(cls);
        if (rawEwasCollection == null || rawEwasCollection.size() == 0)
            return null;
        List<GKInstance> humanEwasCollection = new ArrayList<GKInstance>();

        for (GKInstance ewas : rawEwasCollection) {
            GKInstance species = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.species);
            if (species == null)
                continue;
            // Home Sapien (DBID: 48887)
            if (species.getDBID().equals(48887L))
                humanEwasCollection.add(ewas);
        }
        // Required EWAS instance attributes.
        String[] attributes = {ReactomeJavaConstants.compartment,
                               ReactomeJavaConstants.endCoordinate,
                               ReactomeJavaConstants.referenceEntity,
                               ReactomeJavaConstants.stableIdentifier,
                               ReactomeJavaConstants.startCoordinate};
        dba.loadInstanceAttributeValues(humanEwasCollection, attributes);

        return humanEwasCollection;
    }

    private String getRow(GKInstance ewas, List<Object> modifiedResidues) throws InvalidAttributeException, Exception {
        List<String> row = new ArrayList<String>();
        String output = "";

        // Entity type (Protein, Complex, etc)
        // TODO Confirm if anything other than "EWAS" will be Entity Type.
        final String entityType = ProExporterConstants.ewas;;
        row.add(entityType);

        // Reactome identifier (R-HSA-)
        String identifier = getIdentifier(ewas);
        row.add(identifier);

        // Subcellular location (GO:)
        String location = getLocation(ewas);
        row.add(location == null ? ProExporterConstants.unknown : location);

        // UniProtKB accession (with specific isoform, if indicated)
        String accession = getUniprotAccession(ewas);
        row.add(accession);

        // Start position of the sequence (if unknown, use '?')
        Integer startPosition = getStartPosition(ewas);
        row.add(startPosition == null ? ProExporterConstants.unknown : String.valueOf(startPosition));

        // End position of the sequence (if unknown, use '?')
        Integer endPosition = getEndPosition(ewas);
        row.add(endPosition == null ? ProExporterConstants.unknown : String.valueOf(endPosition));

        // Modifications (see general and specific instructions below)
        String modifications = getModifications(modifiedResidues);
        row.add(modifications == null ? "" : modifications);

        // Free text (where necessary; see specific instructions below)
        String freeText = getFreeText(modifiedResidues);
        row.add(freeText == null ? "" : freeText);

        // Display Name
        String displayName = ewas.getDisplayName();
        row.add(displayName);

        output += row.get(0);
        for (String cell : row.subList(1, row.size()))
            output += ProExporterConstants.delimiter + cell;

        return output;
   }

    @SuppressWarnings("serial")
    private static class MissingPropertyException extends IllegalStateException {
        public MissingPropertyException(String errorMsg) {
            super(errorMsg);
        }
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ProExporter exporter = new ProExporter();
        // Get property file.
        String propPath = null;
        if (args.length == 0) {
            // Default property file.
            final String propertyFile = "proExport.prop";
            URL pathURL = ProExporter.class.getResource(propertyFile);
            if (pathURL == null)
                throw new FileNotFoundException("Could not find file: " + propertyFile);
            propPath = pathURL.getFile();
        }
        else {
            // User passes in property file as argument.
            propPath = args[0];
        }

        // Get database information from property file.
        Properties properties = new Properties();
        properties.load(new FileInputStream(propPath));

        String err = "Property is null or empty: ";
        String host = properties.getProperty("host");
        if (host == null || host.length() == 0)
            throw new MissingPropertyException(err + "host");
        String database = properties.getProperty("database");
        if (database == null || database.length() == 0)
            throw new MissingPropertyException(err + "database");
        String username = properties.getProperty("username");
        if (username == null || username.length() == 0)
            throw new MissingPropertyException(err + "username");
        String password = properties.getProperty("password");
        if (password == null || password.length() == 0)
            throw new MissingPropertyException(err + "password");
        String port = properties.getProperty("port");
        if (port == null || port.length() == 0)
            throw new MissingPropertyException(err + "port");
        // Get export filename/path.
        String exportPath = properties.getProperty("exportPath");
        if (exportPath == null || exportPath.length() == 0)
            throw new Exception(err + "exportPath");

        // Connect to database.
        MySQLAdaptor dba = new MySQLAdaptor(host,
                                            database,
                                            username,
                                            password,
                                            Integer.valueOf(port));

        // Get the collection of EWAS's from the database.
        List<GKInstance> ewasCollection = exporter.getEwasCollection(dba);
        if (ewasCollection == null || ewasCollection.size() == 0)
            throw new SQLException("Could not load EWAS instances from database.");

        // Open export file.
        PrintWriter writer = new PrintWriter(new File(exportPath));

        // Write column headers.
        writer.print(ProExporterConstants.COLUMNS.get(0));
        for (String column : ProExporterConstants.COLUMNS.subList(1, ProExporterConstants.COLUMNS.size()))
            writer.write(ProExporterConstants.delimiter + column);
        writer.println();

        List<Object> modifiedResidues = null;
        String row = null;
        for (GKInstance ewas : ewasCollection) {
            // Get all modified residues for the given EWAS.
            modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);

            // Get the export row for the given EWAS.
            row = exporter.getRow(ewas, modifiedResidues);

            // Write output to file.
            writer.print(row);
            writer.println();
        }

        writer.close();
    }

    @Before
    public void initializeTestDBA() throws SQLException {
        dba =  new MySQLAdaptor("localhost",
                                "reactome",
                                "liam",
                                ")8J7m]!%[<");
    }

    private MySQLAdaptor getTestDBA() {
        return dba;
    }

    @Test
    public void testGetRow() throws Exception {
        MySQLAdaptor dba = getTestDBA();
        GKInstance ewas;
        String output;
        List<Object> modifiedResidues = new ArrayList<Object>();

        // FragmentInsertionModification
        ewas = dba.fetchInstance(1839016L);
        output = "EWAS  R-HSA-1839016   GO:0005829  Q9UBW7  1   913     +914=INSERTION1+766=MOD:00048+=MOD:00048    INSERTION1=Insertion of residues 429 to 822 at 914 from UniProt:P11362 FGFR1    ZMYM2-p-2Y-FGFR1 fusion [cytosol]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getRow(ewas, modifiedResidues));

        // FragmentDeletionModification
        ewas = dba.fetchInstance(5339694L);
        output = "EWAS  R-HSA-5339694   GO:0005886  O75197  32  1615    +=DELETION1     DELETION1=Deletion of residues 666 to 809   LRP5 del666-809 [plasma membrane]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getRow(ewas, modifiedResidues));

        // FragmentDeletionModification
        ewas = dba.fetchInstance(5604959L);
        output = "EWAS  R-HSA-5604959   GO:0005789  P22309  26  533     +=DELETION1     DELETION1=Deletion of residues 294 to 297   UGT1A1 del294-297 [endoplasmic reticulum membrane]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getRow(ewas, modifiedResidues));

        // FragmentReplacedModification
        ewas = dba.fetchInstance(5659656L);
        output = "EWAS  R-HSA-5659656   GO:0005886  Q9UN76  1   642     +=REPLACED1     REPLACED1=Replacement of residues 20649 to 20469 by T   SLC6A14 20649C-T [plasma membrane]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getRow(ewas, modifiedResidues));

        // IntraChainCrosslinkedResidue #1
        ewas = dba.fetchInstance(8874904L);
        output = "EWAS  R-HSA-8874904   GO:0005758  Q9NRP2  1   79  +14=MOD:00798[CROSSLINK1@47]+24=MOD:00798[CROSSLINK2@37]+14=CHEBI:23514[CROSSLINK1@47]+24=CHEBI:23514[CROSSLINK2@37]    CROSSLINK1=Intra-chain Crosslink via half cystine at 14 and 47^|^CROSSLINK2=Intra-chain Crosslink via half cystine at 24 and 37     4xHC-CMC2 [mitochondrial intermembrane space]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getRow(ewas, modifiedResidues));

        // IntraChainCrosslinkedResidue #2
        ewas = dba.fetchInstance(6797422L);
        output = "EWAS  R-HSA-6797422   GO:0005576  P09429  2   215     +23=MOD:00798[CROSSLINK1@45]+23=CHEBI:30770[CROSSLINK1@45]  CROSSLINK1=Intra-chain Crosslink via half cystine at 23 and 45  HC23,45-HMGB1 [extracellular region]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getRow(ewas, modifiedResidues));

        // IntraChainCrosslinkedResidue #3
        ewas = dba.fetchInstance(8874912L);
        output = "EWAS  R-HSA-8874912   GO:0005758  Q9BSY4  1   110     +58=MOD:00798[CROSSLINK1@89]+58=CHEBI:23514[CROSSLINK1@89]  CROSSLINK1=Intra-chain Crosslink via half cystine at 58 and 89  4xHC-CHCHD5 [mitochondrial intermembrane space]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getRow(ewas, modifiedResidues));

    }

    @Test
    public void testGetFreeText() throws Exception {
        MySQLAdaptor dba = getTestDBA();
        GKInstance ewas;
        String output;
        List<Object> modifiedResidues = new ArrayList<Object>();

        // FragmentInsertionModification
        ewas = dba.fetchInstance(1839016L);
        output = "INSERTION1=Insertion of residues 429 to 822 at 914 from UniProt:P11362 FGFR1";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getFreeText(modifiedResidues));

        // IntraChainCrosslinkedResidue #1
        ewas = dba.fetchInstance(8874904L);
        output = "CROSSLINK1=Intra-chain Crosslink via half cystine at 14 and 47^|^CROSSLINK2=Intra-chain Crosslink via half cystine at 24 and 37";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getFreeText(modifiedResidues));

        // IntraChainCrosslinkedResidue #2
        ewas = dba.fetchInstance(6797422L);
        output = "CROSSLINK1=Intra-chain Crosslink via half cystine at 23 and 45";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getFreeText(modifiedResidues));
    }

    @Test
    public void testGetModification() throws Exception {
        MySQLAdaptor dba = getTestDBA();
        GKInstance ewas;
        String output;
        List<Object> modifiedResidues = new ArrayList<Object>();

        // General Modifications (unknown positions)
        ewas = dba.fetchInstance(217182L);
        output = "+=MOD:00046+=MOD:00798";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getModifications(modifiedResidues));

        // General Modification (at position 715)
        ewas = dba.fetchInstance(156901L);
        output = "+715=MOD:00049";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getModifications(modifiedResidues));

        // FragmentInsertionModification
        ewas = dba.fetchInstance(1839016L);
        output = "+914=INSERTION1+766=MOD:00048+=MOD:00048";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getModifications(modifiedResidues));

        // InterChainCrosslinkedResidue
        ewas = dba.fetchInstance(4551599L);
        output = "+2592=MOD:01149+2592=CHEBI:24411+2592=UniProt:P63165[97]+2650=MOD:01149+2650=CHEBI:24411+2650=UniProt:P63165[97]+2723=MOD:01149+2723=CHEBI:24411+2723=UniProt:P63165[97]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getModifications(modifiedResidues));

        // IntraChainCrosslinkedResidue #1
        ewas = dba.fetchInstance(8874904L);
        // TODO Check if last crosslink was mistaken in reactome_SN.how.
        output = "+14=MOD:00798[CROSSLINK1@47]+24=MOD:00798[CROSSLINK2@37]+14=CHEBI:23514[CROSSLINK1@47]+24=CHEBI:23514[CROSSLINK2@37]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getModifications(modifiedResidues));

        // IntraChainCrosslinkedResidue #2
        ewas = dba.fetchInstance(6797422L);
        output = "+23=MOD:00798[CROSSLINK1@45]+23=CHEBI:30770[CROSSLINK1@45]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getModifications(modifiedResidues));

        // GroupModifiedResidue #1
        ewas = dba.fetchInstance(2046248L);
        output = "+=MOD:00831+=CHEBI:63492";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getModifications(modifiedResidues));

        // GroupModifiedResidue #2
        ewas = dba.fetchInstance(8952387L);
        output = "+94=MOD:01148+94=Reactome:R-HSA-8939707+148=MOD:01148+148=Reactome:R-HSA-8939707";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(output, getModifications(modifiedResidues));
    }

}
