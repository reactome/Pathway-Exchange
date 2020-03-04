package org.gk.pro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaClass;

/**
 * Export modified residue data for all human EWAS instances in a given database.
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
     * @param ewas
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    String getModifications(GKInstance ewas) throws InvalidAttributeException, Exception {
        // Get all modified residues for the given EWAS.
        List<GKInstance> modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);

        if (modifiedResidues == null || modifiedResidues.size() == 0)
            return null;

        AbstractModifiedResidue residueExporter = null;
        Integer index;
        Map<SchemaClass, Integer> indices = new HashMap<SchemaClass, Integer>();
        StringBuilder modifications = new StringBuilder();
        String tmp = null;

        if (residueClassMap.size() < 8)
            populateClassMap(ewas);

        for (GKInstance residue : modifiedResidues) {
            residueExporter = residueClassMap.get(residue.getSchemClass());

            tmp = residueExporter.exportModification(residue);

            if (!ProExporterConstants.indexedClasses.contains(residue.getSchemClass().getName())) {
                modifications.append(tmp);
                continue;
            }

            // Handle indices for the indexed classes.
            index = indices.get(residue.getSchemClass());
            if (index == null)
                index = 1;

            modifications.append(tmp.replace(ProExporterConstants.indexPlaceholder, String.valueOf(index)));

            indices.put(residue.getSchemClass(), (index + 1));
        }

        return modifications.toString();
    }

    /**
     * Return the freeText values for a given EWAS.
     *
     * @param ewas
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    String getFreeText(GKInstance ewas) throws InvalidAttributeException, Exception {
        // Get all modified residues for the given EWAS.
        List<GKInstance> modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);

        if (modifiedResidues == null || modifiedResidues.size() == 0)
            return null;

        AbstractModifiedResidue residueExporter = null;
        Integer index;
        Map<SchemaClass, Integer> indices = new HashMap<SchemaClass, Integer>();
        StringBuilder freeText = new StringBuilder();
        String tmp = null;

        if (residueClassMap.size() < 8)
            populateClassMap(ewas);

        for (GKInstance residue : modifiedResidues) {
            residueExporter = residueClassMap.get(residue.getSchemClass());

            if (!ProExporterConstants.indexedClasses.contains(residue.getSchemClass().getName()))
                continue;

            // Handle indices for the indexed classes.
            index = indices.get(residue.getSchemClass());
            if (index == null)
                index = 1;

            tmp = residueExporter.exportFreeText(residue).replace(ProExporterConstants.indexPlaceholder, String.valueOf(index));
            if (freeText.length() == 0)
                freeText.append(tmp);
            else
                freeText.append(ProExporterConstants.freeTextDelimiter + tmp);

            indices.put(residue.getSchemClass(), (index + 1));
        }

        return freeText.toString();
    }

    /**
     * Return collection of all human EWAS instances.
     *
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
     * Retrieve a TSV row for a given EWAS instance.
     *
     * @param ewas
     * @param modifiedResidues
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    String getRow(GKInstance ewas) throws InvalidAttributeException, Exception {
        List<String> row = new ArrayList<String>();
        StringBuilder output = new StringBuilder();

        // Entity type (Protein, Complex, etc).
        row.add(ProExporterConstants.ewas);

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
        String modifications = getModifications(ewas);
        row.add(modifications == null ? "" : modifications);

        // Free text (where necessary).
        String freeText = getFreeText(ewas);
        row.add(freeText == null ? "" : freeText);

        // Display Name
        String displayName = ewas.getDisplayName();
        row.add(displayName);

        output.append(row.get(0));
        for (String cell : row.subList(1, row.size()))
            output.append(ProExporterConstants.delimiter + cell);

        return output.toString();
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
        List<String> columns = Arrays.asList("Type",
                                             "Identifier",
                                             "Location",
                                             "Accession",
                                             "Start",
                                             "End",
                                             "Modifications",
                                             "Free Text",
                                             "Display Name");
        writer.print(columns.get(0));
        for (String column : columns.subList(1, columns.size()))
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

}
