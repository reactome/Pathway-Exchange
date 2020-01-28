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
import java.util.stream.Collectors;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaClass;

/**
 * Export modified residue data for all EWAS instances in a given database.
 */
public class ProExporter {
    private final String delimiter = "\t";
    
    public ProExporter() {
    }
    
    public String getDelimiter() {
        return delimiter;
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

        for (GKInstance ewas : ewasCollection) {
            // Format data to specifications and write export data to file.
            // TODO Confirm if anything other than "EWAS" will be Entity Type.
            writer.print("EWAS" + exporter.getDelimiter());

            writer.print(exporter.getIdentifier(ewas) + exporter.getDelimiter());

            writer.print(exporter.getLocation(ewas) + exporter.getDelimiter());

            writer.print(exporter.getUniprotAccession(ewas) + exporter.getDelimiter());

            Integer startPosition = exporter.getStartPosition(ewas);
            writer.print(startPosition == null ? "?" : startPosition);
            writer.print(exporter.getDelimiter());

            Integer endPosition = exporter.getEndPosition(ewas);
            writer.print(endPosition == null ? "?" : endPosition);
            writer.print(exporter.getDelimiter());

            writer.print(exporter.getModifications(ewas)+ exporter.getDelimiter());

            // Reactome ID used for debugging.
            writer.print(String.valueOf(ewas.getDBID()));
            writer.println();
        }

        writer.close();
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

    /**
     * 
     * @param ewas
     * @param dba
     * @return
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private String getModifications(GKInstance ewas) throws InvalidAttributeException, Exception {
        // TODO Confirm that identifier means Reactome ID, and not ChEBI ID.
        List<Object> modifiedResidues = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        if (modifiedResidues == null || modifiedResidues.size() == 0)
            return "";

        SchemaClass cls = null;
        String output = null;
        for (Object object : modifiedResidues) {
            GKInstance modifiedResidue = (GKInstance) object;
            AbstractModifiedResidue mod = null;
            cls = modifiedResidue.getSchemClass();
        
            if (cls.isa(ReactomeJavaConstants.FragmentInsertionModification))
                mod = new FragmentInsertionModification();

            else if (cls.isa(ReactomeJavaConstants.FragmentDeletionModification))
                mod = new FragmentDeletionModification();

            else if (cls.isa(ReactomeJavaConstants.FragmentReplacedModification))
                mod = new FragmentReplacedModification();

            else if (cls.isa(ReactomeJavaConstants.ReplacedResidue))
                mod = new ReplacedResidue();

            else if (cls.isa(ReactomeJavaConstants.InterChainCrosslinkedResidue))
                mod = new InterChainCrosslinkedResidue();

            else if (cls.isa(ReactomeJavaConstants.IntraChainCrosslinkedResidue))
                mod = new IntraChainCrosslinkedResidue();

            else if (cls.isa(ReactomeJavaConstants.GroupModifiedResidue))
                mod = new GroupModifiedResidue();

            else if (cls.isa(ReactomeJavaConstants.ModifiedResidue))
                mod = new ModifiedResidue();

            else
                throw new Exception("Unknown schema class: " + cls);

            output += mod.export(modifiedResidue);
        }

        return output;
    }
    
    /**
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
     * 
     * @param modifiedResidue
     * @return
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private String getCoordinate(GKInstance modifiedResidue) throws InvalidAttributeException, Exception {
        if (modifiedResidue == null ||
            !modifiedResidue.getSchemClass().isValidAttribute(ReactomeJavaConstants.coordinate))
            return null;
        Integer coordinate = (Integer) modifiedResidue.getAttributeValue(ReactomeJavaConstants.coordinate);
        String modPosition = "";
        if (coordinate != null)
            modPosition = String.valueOf(coordinate);
        return modPosition;
    }
}
