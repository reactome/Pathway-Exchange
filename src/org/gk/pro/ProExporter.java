package org.gk.pro;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private MySQLAdaptor testDBA;

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
        if (referenceEntity.getSchemClass().isa(ReactomeJavaConstants.ReferenceIsoform))
            return (String) referenceEntity.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
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
     * Return the freeText values for a given EWAS.
     *
     * @param modifiedResidues
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private List<String> getExport(List<GKInstance> modifiedResidues, Exporter exporter) throws InvalidAttributeException, Exception {
        if (modifiedResidues == null || modifiedResidues.size() == 0)
            return null;
        
        AbstractModifiedResidue residueExporter = null;
        GKInstance residue = null;
        Integer index;
        Map<SchemaClass, Integer> indices = new HashMap<SchemaClass, Integer>();
        String freeText = null;
        List<String> export = new ArrayList<String>();
        String output = null;

        for (Map.Entry<GKInstance, AbstractModifiedResidue> map : getResidues(modifiedResidues).entrySet()) {
            residueExporter = map.getValue();
            residue = map.getKey();
            freeText = exporter.getString(residueExporter, residue);
            
            if (freeText != null) {
                output = freeText;
                
                // Handle indices for the indexed classes.
                if (ProExporterConstants.indexedClasses.contains(residue.getSchemClass().getName())) {
                    index = indices.get(residue.getSchemClass());
                    if (index == null)
                        index = 1;
                    output = output.replace(ProExporterConstants.indexPlaceholder, String.valueOf(index));
                    indices.put(residue.getSchemClass(), (index + 1));
                }
            }
            
            export.add(output);
        }

        return export;
    }
    
    private interface Exporter {
        public String getString(AbstractModifiedResidue residueExporter, GKInstance residue);
    }
    
    private Map<GKInstance, AbstractModifiedResidue> getResidues(List<GKInstance> modifiedResidues) throws Exception {
        if (modifiedResidues == null || modifiedResidues.size() == 0)
            return null;
        
        String pkg = this.getClass().getPackage().getName();
        Class<? extends AbstractModifiedResidue> cls = null;
        Map<GKInstance, AbstractModifiedResidue> residues = new HashMap<GKInstance, AbstractModifiedResidue>();
        for (GKInstance residue : modifiedResidues) {
            cls = (Class<? extends AbstractModifiedResidue>) Class.forName(pkg + "." + residue.getSchemClass().getName());

            residues.put(residue, cls.getConstructor().newInstance());
        }

        return residues;
    }


    /**
     * Return collection of all human EWAS instances.
     *
     * @return Collection<GKInstance>
     * @throws Exception
     */
    private Collection<GKInstance> getEwasCollection(MySQLAdaptor dba) throws Exception {
        if (dba == null)
            return null;
        // Read data from database.
        String clsName = ReactomeJavaConstants.EntityWithAccessionedSequence;
        String clsAttribute = ReactomeJavaConstants.species;
        GKInstance homeSapiens = dba.fetchInstance(48887L);
        Collection<GKInstance> ewasCollection = dba.fetchInstanceByAttribute(clsName, clsAttribute, "=", homeSapiens);
        if (ewasCollection == null || ewasCollection.size() == 0)
            return null;

        // Required EWAS instance attributes.
        String[] attributes = {ReactomeJavaConstants.compartment,
                               ReactomeJavaConstants.endCoordinate,
                               ReactomeJavaConstants.referenceEntity,
                               ReactomeJavaConstants.stableIdentifier,
                               ReactomeJavaConstants.startCoordinate};
        dba.loadInstanceAttributeValues(ewasCollection, attributes);

        return ewasCollection;
    }

    /**
     * Retrieve a TSV row for a given EWAS instance.
     * 
     * @param ewas
     * @param modifiedResidues
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private String getRow(GKInstance ewas) throws InvalidAttributeException, Exception {
        List<String> row = new ArrayList<String>();
        String output = "";

        // Get all modified residues for the given EWAS.
        List<GKInstance> modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);

        // Entity type (Protein, Complex, etc).
        final String entityType = ProExporterConstants.ewas;;
        row.add(entityType);

        // Reactome identifier (R-HSA-).
        String identifier = getIdentifier(ewas);
        row.add(identifier);

        // Subcellular location (GO:).
        String location = getLocation(ewas);
        row.add(location == null ? ProExporterConstants.unknown : location);

        // UniProtKB accession (with specific isoform, if indicated).
        String accession = getUniprotAccession(ewas);
        row.add(accession);

        // Start position of the sequence (if unknown, use '?').
        Integer startPosition = getStartPosition(ewas);
        row.add(startPosition == null ? ProExporterConstants.unknown : String.valueOf(startPosition));

        // End position of the sequence (if unknown, use '?').
        Integer endPosition = getEndPosition(ewas);
        row.add(endPosition == null ? ProExporterConstants.unknown : String.valueOf(endPosition));

        // Modifications.
        String modifications = getModifications(modifiedResidues);
        row.add(modifications == null ? "" : modifications);

        // Free text (where necessary).
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

    private String getModifications(List<GKInstance> modifiedResidues) throws InvalidAttributeException, Exception {
        List<String> export = getExport(modifiedResidues, (residueExporter, residue) -> residueExporter.exportModification(residue);

        return "";
    }
    
    private String getFreeText(List<GKInstance> modifiedResidues) throws InvalidAttributeException, Exception {
        List<String> export = getExport(modifiedResidues, (residueExporter, residue) -> residueExporter.exportFreeText(residue));
        return "";
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
        Collection<GKInstance> ewasCollection = exporter.getEwasCollection(dba);
        if (ewasCollection == null || ewasCollection.size() == 0)
            throw new SQLException("Could not load EWAS instances from database.");
        
        // Open export file.
        PrintWriter writer = new PrintWriter(new File(exportPath));

        // Write column headers.
        writer.print(ProExporterConstants.columns.get(0));
        for (String column : ProExporterConstants.columns.subList(1, ProExporterConstants.columns.size()))
            writer.println(ProExporterConstants.delimiter + column);

        String row = null;
        for (GKInstance ewas : ewasCollection) {
            // Get the export row for the given EWAS.
            row = exporter.getRow(ewas);

            // Write output to file.
            writer.println(row);
        }

        writer.close();
    }

    @Before
    public void initializeTestDBA() throws SQLException {
        testDBA =  new MySQLAdaptor("localhost",
                                    "reactome",
                                    "liam",
                                    ")8J7m]!%[<");
    }

    private MySQLAdaptor getTestDBA() {
        return testDBA;
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
        GKInstance ewas;
        List<Object> modifiedResidues = new ArrayList<Object>();
        MySQLAdaptor dba = getTestDBA();
        String accession;
        String displayName;
        String freeText;
        String identifier;
        String location;
        String modifications;
        String expected;
        String type = ProExporterConstants.ewas;
        int end;
        int start;

        // FragmentInsertionModification
        ewas = dba.fetchInstance(1839016L);
        identifier = "R-HSA-1839016";
        location = "GO:0005829";
        accession = "Q9UBW7";
        start = 1;
        end = 913;
        modifications = "+914=INSERTION1+766=MOD:00048+=MOD:00048";
        freeText = "INSERTION1=Insertion of residues 429 to 822 at 914 from UniProt:P11362 FGFR1";
        displayName = "ZMYM2-p-2Y-FGFR1 fusion [cytosol]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getRow(ewas));

        // FragmentDeletionModification
        ewas = dba.fetchInstance(5339694L);
        identifier = "R-HSA-5339694";
        location = "GO:0005886";
        accession = "O75197";
        start = 32;
        end = 1615;
        modifications = "+=DELETION1";
        freeText = "DELETION1=Deletion of residues 666 to 809";
        displayName = "LRP5 del666-809 [plasma membrane]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getRow(ewas));

        // FragmentDeletionModification
        ewas = dba.fetchInstance(5604959L);
        identifier = "R-HSA-5604959";
        location = "GO:0005789";
        accession = "P22309";
        start = 26;
        end = 533;
        modifications = "+=DELETION1";
        freeText = "DELETION1=Deletion of residues 294 to 297";
        displayName = "UGT1A1 del294-297 [endoplasmic reticulum membrane]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getRow(ewas));

        // FragmentReplacedModification
        ewas = dba.fetchInstance(5659656L);
        identifier = "R-HSA-5659656";
        location = "GO:0005886";
        accession = "Q9UN76";
        start = 1;
        end = 642;
        modifications = "+=REPLACED1";
        freeText = "REPLACED1=Replacement of residues 20649 to 20469 by T";
        displayName = "SLC6A14 20649C-T [plasma membrane]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getRow(ewas));

        // ReplacedResidue
        ewas = dba.fetchInstance(9606687L);
        identifier = "R-HSA-9606687";
        location = "GO:0005654";
        accession = "Q9UIF7-6";
        start = 1;
        end = 521;
        modifications = "+255=MOD:01643+255=MOD:00029";
        freeText = "";
        displayName = "MUTYH-6 M255V [nucleoplasm]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getRow(ewas));

        // IntraChainCrosslinkedResidue #1
        ewas = dba.fetchInstance(8874904L);
        identifier = "R-HSA-8874904";
        location = "GO:0005758";
        accession = "Q9NRP2";
        start = 1;
        end = 79;
        modifications = "+14=MOD:00798[CROSSLINK1@47]+14=CHEBI:23514[CROSSLINK1@47]+24=MOD:00798[CROSSLINK2@37]+24=CHEBI:23514[CROSSLINK2@37]";
        freeText = "CROSSLINK1=Intra-chain Crosslink via half cystine at 14 and 47^|^CROSSLINK2=Intra-chain Crosslink via half cystine at 24 and 37";
        displayName = "4xHC-CMC2 [mitochondrial intermembrane space]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getRow(ewas));

        // IntraChainCrosslinkedResidue #2
        ewas = dba.fetchInstance(6797422L);
        identifier = "R-HSA-6797422";
        location = "GO:0005576";
        accession = "P09429";
        start = 2;
        end = 215;
        modifications = "+23=MOD:00798[CROSSLINK1@45]+23=CHEBI:30770[CROSSLINK1@45]";
        freeText = "CROSSLINK1=Intra-chain Crosslink via half cystine at 23 and 45";
        displayName = "HC23,45-HMGB1 [extracellular region]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getRow(ewas));

        // IntraChainCrosslinkedResidue #3
        ewas = dba.fetchInstance(8874912L);
        identifier = "R-HSA-8874912";
        location = "GO:0005758";
        accession = "Q9BSY4";
        start = 1;
        end = 110;
        modifications = "+58=MOD:00798[CROSSLINK1@89]+58=CHEBI:23514[CROSSLINK1@89]";
        freeText = "CROSSLINK1=Intra-chain Crosslink via half cystine at 58 and 89";
        displayName = "4xHC-CHCHD5 [mitochondrial intermembrane space]";
        expected = getTestRow(type, identifier, location, accession, start, end, modifications, freeText, displayName);
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getRow(ewas));

    }

    @Test
    public void testGetFreeText() throws Exception {
        MySQLAdaptor dba = getTestDBA();
        GKInstance ewas;
        String expected;
        List<GKInstance> modifiedResidues = new ArrayList<GKInstance>();

        // FragmentInsertionModification
        ewas = dba.fetchInstance(1839016L);
        expected = "INSERTION1=Insertion of residues 429 to 822 at 914 from UniProt:P11362 FGFR1";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getFreeText(modifiedResidues));

        // IntraChainCrosslinkedResidue #1
        ewas = dba.fetchInstance(8874904L);
        expected = "CROSSLINK1=Intra-chain Crosslink via half cystine at 14 and 47^|^" +
                   "CROSSLINK2=Intra-chain Crosslink via half cystine at 24 and 37";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getFreeText(modifiedResidues));

        // IntraChainCrosslinkedResidue #2
        ewas = dba.fetchInstance(6797422L);
        expected = "CROSSLINK1=Intra-chain Crosslink via half cystine at 23 and 45";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getFreeText(modifiedResidues));
    }

    @Test
    public void testGetModification() throws Exception {
        MySQLAdaptor dba = getTestDBA();
        GKInstance ewas;
        String expected;
        List<GKInstance> modifiedResidues = new ArrayList<GKInstance>();

        // General Modifications (unknown positions)
        ewas = dba.fetchInstance(217182L);
        expected = "+=MOD:00046+=MOD:00798";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getModifications(modifiedResidues));

        // General Modification (at position 715)
        ewas = dba.fetchInstance(156901L);
        expected = "+715=MOD:00049";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getModifications(modifiedResidues));

        // FragmentInsertionModification
        ewas = dba.fetchInstance(1839016L);
        expected = "+914=INSERTION1+766=MOD:00048+=MOD:00048";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getModifications(modifiedResidues));

        // InterChainCrosslinkedResidue
        ewas = dba.fetchInstance(4551599L);
        expected = "+2592=MOD:01149+2592=CHEBI:24411+2592=UniProt:P63165[97]+2650=MOD:01149+2650" +
                 "=CHEBI:24411+2650=UniProt:P63165[97]+2723=MOD:01149+2723=CHEBI:24411+2723=UniProt:P63165[97]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getModifications(modifiedResidues));

        // IntraChainCrosslinkedResidue #1
        ewas = dba.fetchInstance(8874904L);
        // TODO Check if last crosslink was mistaken in reactome_SN.how.
        expected = "+14=MOD:00798[CROSSLINK1@47]+14=CHEBI:23514[CROSSLINK1@47]+24=MOD:00798[CROSSLINK2@37]" +
                   "+24=CHEBI:23514[CROSSLINK2@37]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getModifications(modifiedResidues));

        // IntraChainCrosslinkedResidue #2
        ewas = dba.fetchInstance(6797422L);
        expected = "+23=MOD:00798[CROSSLINK1@45]+23=CHEBI:30770[CROSSLINK1@45]";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getModifications(modifiedResidues));

        // GroupModifiedResidue #1
        ewas = dba.fetchInstance(2046248L);
        expected = "+=MOD:00831+=CHEBI:63492";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getModifications(modifiedResidues));

        // GroupModifiedResidue #2
        ewas = dba.fetchInstance(8952387L);
        expected = "+94=MOD:01148+94=Reactome:R-HSA-8939707+148=MOD:01148+148=Reactome:R-HSA-8939707";
        modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        assertEquals(expected, getModifications(modifiedResidues));
    }

}
