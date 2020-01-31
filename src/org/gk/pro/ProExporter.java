package org.gk.pro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaClass;
import org.junit.Test;

/**
 * Export modified residue data for all EWAS instances in a given database.
 */
public class ProExporter {

    public ProExporter() {
    }

    private String getIdentifier(GKInstance ewas) throws InvalidAttributeException, Exception {
        GKInstance stableIdentifier = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
        return (String) stableIdentifier.getAttributeValue(ReactomeJavaConstants.identifier);
    }

    private String getLocation(GKInstance ewas) throws InvalidAttributeException, Exception {
        GKInstance compartment = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.compartment);
        // TODO Confirm if we should filter out non-GO databases.
        if (compartment == null)
            return null;
        GKInstance referenceDatabase = (GKInstance) compartment.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
        String prefix = referenceDatabase.getDisplayName();
        String accession = (String) compartment.getAttributeValue(ReactomeJavaConstants.accession);
        return prefix + ":" + accession;
    }

    private String getUniprotAccession(GKInstance ewas) throws Exception {
        // TODO Get isoform data.
        GKInstance referenceEntity = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.referenceEntity);
        return (String) referenceEntity.getAttributeValue(ReactomeJavaConstants.identifier);
    }

    private Integer getStartPosition(GKInstance ewas) throws Exception {
        Object startPosition = ewas.getAttributeValue(ReactomeJavaConstants.startCoordinate);
        if (startPosition == null)
            return null;
        return (Integer) startPosition;
    }

    private Integer getEndPosition(GKInstance ewas) throws Exception {
        Object endPosition = ewas.getAttributeValue(ReactomeJavaConstants.endCoordinate);
        if (endPosition == null)
            return null;
        return (Integer) endPosition;
    }

    interface StringExporter {
        public String export(AbstractModifiedResidue residueExporter, GKInstance modifiedResidue);
    }

    /**
     * Return the modifications for a given EWAS (as referenced by the "hasModifiedResidue" attribute value).
     *
     * @param ewas
     * @param modifiedResidues
     * @return String
     * @throws Exception
     */
    private String getModifications(GKInstance ewas, List<Object> modifiedResidues) throws Exception {
        String output = "";
        AbstractModifiedResidue residueExporter = null;
        GKInstance modifiedResidue = null;
        // TODO Fix classpath and remove this.
        String pkg = this.getClass().getPackage().getName();
        String cls = null;
        for (Object object : modifiedResidues) {
            modifiedResidue = (GKInstance) object;
            cls = modifiedResidue.getSchemClass().getName();
            residueExporter = (AbstractModifiedResidue) Class.forName(pkg + "." + cls).getConstructor().newInstance();
            if (modifiedResidue.getSchemClass().isa(ReactomeJavaConstants.IntraChainCrosslinkedResidue)) {
                boolean isSecondResiduePresent = modifiedResidues.size() == 2;
                output += residueExporter.exportModification(modifiedResidues, isSecondResiduePresent);
            }
            else
                output += residueExporter.exportModification(modifiedResidue);
        }
        resetIndices();

        return output;
    }

    /**
     * Return the freeText values for a given EWAS.
     *
     * @param ewas
     * @param modifiedResidues
     * @return String
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private String getFreeText(GKInstance ewas, List<Object> modifiedResidues) throws InvalidAttributeException, Exception {
        String output = "";
        FragmentModification residueExporter = null;
        GKInstance modifiedResidue = null;
        String pkg = this.getClass().getPackage().getName();
        String cls = null;
        for (Object object : modifiedResidues) {
            modifiedResidue = (GKInstance) object;
            if (!modifiedResidue.getSchemClass().isa(ReactomeJavaConstants.FragmentModification))
                continue;
            if (output.length() > 0)
                output += ProExporterConstants.freeTextDelimiter;

            cls = modifiedResidue.getSchemClass().getName();
            residueExporter = (FragmentModification) Class.forName(pkg + "." + cls).getConstructor().newInstance();
            output += residueExporter.exportFreeText(modifiedResidue);
        }
        resetIndices();

        return output;
    }

    private void resetIndices() {
        FragmentInsertionModification.resetIndex();
        FragmentDeletionModification.resetIndex();
        FragmentReplacedModification.resetIndex();
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
        int i = 0;
        for (GKInstance ewas : rawEwasCollection) {
            GKInstance species = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.species);
            if (species == null)
                continue;
            // Home Sapien (DBID: 48887)
            if (species.getDBID().equals(48887L)) {
                humanEwasCollection.add(ewas);
            }
            // TODO Remove this break statement after testing.
            if (i++ > 100)
                break;
        }
        // TODO Pare down loaded attributes to only those that are needed.
        dba.loadInstanceAttributeValues(humanEwasCollection);

        return humanEwasCollection;
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
            throw new Exception(err + "host");
        String database = properties.getProperty("database");
        if (database == null || database.length() == 0)
            throw new Exception(err + "database");
        String username = properties.getProperty("username");
        if (username == null || username.length() == 0)
            throw new Exception(err + "username");
        String password = properties.getProperty("password");
        if (password == null || password.length() == 0)
            throw new Exception(err + "password");
        String port = properties.getProperty("port");
        if (port == null || port.length() == 0)
            throw new Exception(err + "port");
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
            throw new Exception("Could not load EWAS instances from database.");

        // Open export file.
        PrintWriter writer = new PrintWriter(new File(exportPath));

        List<Object> modifiedResidues = null;
        for (GKInstance ewas : ewasCollection) {
            modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
            if (modifiedResidues == null || modifiedResidues.size() == 0)
                continue;

            // Entity type (Protein, Complex, etc)
            // TODO Confirm if anything other than "EWAS" will be Entity Type.
            String entityType = "EWAS";
            writer.print(entityType + ProExporterConstants.delimiter);

            // Reactome identifier (R-HSA-)
            String identifier = exporter.getIdentifier(ewas);
            writer.print(identifier + ProExporterConstants.delimiter);

            // Subcellular location (GO:)
            String location = exporter.getLocation(ewas);
            writer.print(location + ProExporterConstants.delimiter);

            // UniProtKB accession (with specific isoform, if indicated)
            String accession = exporter.getUniprotAccession(ewas);
            writer.print(accession + ProExporterConstants.delimiter);

            // Start position of the sequence (if unknown, use '?')
            Integer startPosition = exporter.getStartPosition(ewas);
            writer.print(startPosition == null ? "?" : startPosition);
            writer.print(ProExporterConstants.delimiter);

            // End position of the sequence (if unknown, use '?')
            Integer endPosition = exporter.getEndPosition(ewas);
            writer.print(endPosition == null ? "?" : endPosition);
            writer.print(ProExporterConstants.delimiter);

            // Modifications (see general and specific instructions below)
            String modifications = exporter.getModifications(ewas, modifiedResidues);
            writer.print(modifications);

            // Free text (where necessary; see specific instructions below)
            String freeText = exporter.getFreeText(ewas, modifiedResidues);
            if (freeText != null && freeText.length() > 0) {
                writer.print(ProExporterConstants.delimiter);
                writer.print(freeText);
            }

            // ID used for debugging.
            writer.print(ProExporterConstants.delimiter);
            writer.print(String.valueOf(ewas.getDBID()));

            writer.println();
        }

        writer.close();
    }

    @Test
    public void testGetModification() {

    }

}
