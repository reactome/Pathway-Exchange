package org.gk.pro;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
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

/**
 * Export modified residue data for all human EWAS instances in a given database.
 *
 * Reads 'proExport.prop' file for configuration values.
 * Writes exported data to 'exportPath' ('proExport.tsv' by default).
 */
public class ProExporter {
    Map<SchemaClass, AbstractModifiedResidue> residueClassMap;

    public ProExporter() {
        residueClassMap = new HashMap<SchemaClass, AbstractModifiedResidue>();
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
     * Set the modifications (and freeText if applicable) for a given row.
     *
     * @param ewas
     * @param row
     * @throws InvalidAttributeException
     * @throws Exception
     */
    void setModifications(GKInstance ewas, ProExportRow row) throws InvalidAttributeException, Exception {
        // Get all modified residues for the given EWAS.
        List<GKInstance> modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);

        if (modifiedResidues == null || modifiedResidues.size() == 0)
            return;

        AbstractModifiedResidue residueExporter = null;
        Integer index;
        Map<SchemaClass, Integer> indices = new HashMap<SchemaClass, Integer>();
        String modification = null;
        String freeText = null;
        List<String> indexedClasses = Arrays.asList(ReactomeJavaConstants.FragmentInsertionModification,
                                                    ReactomeJavaConstants.FragmentDeletionModification,
                                                    ReactomeJavaConstants.FragmentReplacedModification,
                                                    ReactomeJavaConstants.IntraChainCrosslinkedResidue);
        populateClassMap(ewas);

        for (GKInstance residue : modifiedResidues) {
            residueExporter = residueClassMap.get(residue.getSchemClass());

            modification = residueExporter.exportModification(residue);

            if (!indexedClasses.contains(residue.getSchemClass().getName())) {
                row.addModification(modification);
                continue;
            }

            // Handle indices for the indexed classes.
            index = indices.get(residue.getSchemClass());
            if (index == null)
                index = 1;

            modification = modification.replace(ProExporterConstants.indexPlaceholder, String.valueOf(index));
            row.addModification(modification);

            freeText = residueExporter.exportFreeText(residue);
            freeText = freeText.replace(ProExporterConstants.indexPlaceholder, String.valueOf(index));
            row.addFreeText(freeText);

            indices.put(residue.getSchemClass(), (index + 1));
        }
    }

    /**
     * Return collection of all human EWAS instances.
     *
     * @param dba
     * @return Collection<GKInstance>
     * @throws Exception
     */
    private Collection<GKInstance> getEwasCollection(MySQLAdaptor dba) throws Exception {
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
     * Add the schema classes for all modified residues (of a given EWAS) to the class map for later retrieval.
     *
     * @param ewas
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private void populateClassMap(GKInstance ewas) throws InvalidAttributeException, Exception {
        // Get all modified residues for the given EWAS.
        List<GKInstance> modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);

        Class<? extends AbstractModifiedResidue> cls = null;
        String pkg = this.getClass().getPackage().getName();

        for (GKInstance residue : modifiedResidues) {
            if (!residueClassMap.containsKey(residue.getSchemClass())) {
                cls = (Class<? extends AbstractModifiedResidue>) Class.forName(pkg + "." + residue.getSchemClass().getName());
                residueClassMap.put(residue.getSchemClass(), cls.getConstructor().newInstance());
            }
        }
    }

    /**
     * Retrieve an export row for a given EWAS instance.
     *
     * Order is defined in {@link ProExportRow#getRow}.
     *
     * @param ewas
     * @param modifiedResidues
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    String getRow(GKInstance ewas) throws InvalidAttributeException, Exception {
        ProExportRow row = new ProExportRow();

        // Entity type (Protein, Complex, etc).
        row.setEntityType(ProExporterConstants.ewas);

        // Reactome identifier (R-HSA-).
        String identifier = getIdentifier(ewas);
        row.setIdentifier(identifier);

        // Subcellular location (GO:).
        String location = getLocation(ewas);
        row.setLocation(location == null ? ProExporterConstants.unknown : location);

        // UniProtKB accession (with specific isoform, if indicated).
        String accession = getUniprotAccession(ewas);
        row.setAccession(accession);

        // Start position of the sequence (if unknown, use '?').
        Integer startPosition = getStartPosition(ewas);
        row.setStartPostion(startPosition == null ? ProExporterConstants.unknown : String.valueOf(startPosition));

        // End position of the sequence (if unknown, use '?').
        Integer endPosition = getEndPosition(ewas);
        row.setEndPostion(endPosition == null ? ProExporterConstants.unknown : String.valueOf(endPosition));

        // Modifications and free text (where necessary).
        setModifications(ewas, row);

        // Display Name
        String displayName = ewas.getDisplayName();
        row.setDisplayName(displayName);

        return row.getRow();
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
        if (args.length == 0)
            propPath =  "proExport.prop";
        else
            propPath = args[0];

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
            throw new MissingPropertyException(err + "exportPath");

        // Connect to database.
        MySQLAdaptor dba = new MySQLAdaptor(host,
                                            database,
                                            username,
                                            password,
                                            Integer.valueOf(port));

        // Get the collection of EWAS's from the database.
        Collection<GKInstance> ewasCollection = exporter.getEwasCollection(dba);
        if (ewasCollection == null || ewasCollection.size() == 0)
            return;

        // Open export file.
        PrintWriter writer = new PrintWriter(new File(exportPath));

        // Write column headers.
        ProExportRow row = new ProExportRow();
        List<String> columnHeaders = row.getColumnHeaders();

        writer.print(columnHeaders.get(0));
        for (String column : columnHeaders.subList(1, columnHeaders.size()))
            writer.print(ProExporterConstants.delimiter + column);

        writer.println();

        // Write the export rows for all EWAS instances to file.
        for (GKInstance ewas : ewasCollection)
            writer.println(exporter.getRow(ewas));

        writer.close();
    }

}
