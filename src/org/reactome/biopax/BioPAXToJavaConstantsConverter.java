/*
 * Created on Jul 6, 2005
 *
 */
package org.reactome.biopax;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.model.OWLDatatypeProperty;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSDatatype;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * This class is used to generate a list of names of classes, properties and types from
 * biopax owl ontology by using free marker template.
 * 
 * @author guanming
 * 
 */
public class BioPAXToJavaConstantsConverter {
    
    public BioPAXToJavaConstantsConverter() {
    }

    public void generate(String javaFileName, 
                         String bpUri) throws IOException, TemplateException, Exception {
        JenaOWLModel model = ProtegeOWL.createJenaOWLModelFromInputStream(new FileInputStream(bpUri));
        Collection classes = model.getOWLClasses();
        Map nameMap = new HashMap();
        nameMap.put("isForBioPAX", Boolean.TRUE);
        Set classNames = new HashSet();
        for (Iterator it = classes.iterator(); it.hasNext();) {
            Object cls = it.next();
            if (cls instanceof RDFSNamedClass) {
                RDFSNamedClass namedCls = (RDFSNamedClass) cls;
                if (namedCls.getName().indexOf(":") > -1)
                    continue; // system schema
                classNames.add(namedCls.getLocalName());
            }
        }
        nameMap.put("classNames", classNames);
        Collection properties = model.getRDFProperties();
        Set propertyNames = new HashSet();
        Set dataTypeNames = new HashSet();
        for (Iterator it = properties.iterator(); it.hasNext();) {
            RDFProperty property = (RDFProperty) it.next();
            if (property.getName().indexOf(":") > -1)
                continue; // Escape system properties
            propertyNames.add(property.getLocalName());
            if (property instanceof OWLDatatypeProperty) {
                extractType(property, dataTypeNames);
            }
        }
        nameMap.put("propertyNames", propertyNames);
        nameMap.put("dataTypeNames", dataTypeNames);
        Configuration config = new Configuration();
        config.setDirectoryForTemplateLoading(new File("resources"));
        Template template = config.getTemplate("BioPAX3JavaConstants.ftl");
        File file = new File(javaFileName);
        FileWriter fileWriter = new FileWriter(file);
        template.process(nameMap, fileWriter);
        fileWriter.close();
    }
    
    private void extractType(RDFProperty prop, Set dataTypeNames) {
        RDFSDatatype dataType = prop.getRangeDatatype();
        if (dataType != null) {
            dataTypeNames.add(getType(dataType.getName()));
            return;
        }
        Collection ranges = prop.getRanges(true);
        RDFResource range = null;
        for (Iterator it = ranges.iterator(); it.hasNext();) {
            range = (RDFResource) it.next();
            String name = range.getName();
            if (name.indexOf(":") > -1) {
                dataTypeNames.add(getType(name));
            }
        }
    }
    
    private String getType(String typeName) {
        int index = typeName.indexOf(":");
        return typeName.substring(index + 1);
    }
    
    public static void main(String[] args) {
        try {
            BioPAXToJavaConstantsConverter generator = new BioPAXToJavaConstantsConverter();
            generator.generate("src/org/reactome/biopax/BioPAX3JavaConstants.java", 
                               "resources/biopax-level3.owl");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}
