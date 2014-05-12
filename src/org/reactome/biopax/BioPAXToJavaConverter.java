/*
 * Created on Jun 17, 2005
 */
package org.reactome.biopax;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.reactome.model.JavaClassDefinition;

import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.model.RDFProperty;
import edu.stanford.smi.protegex.owl.model.RDFResource;
import edu.stanford.smi.protegex.owl.model.RDFSDatatype;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * This source generator uses freemarker template.
 * @author wgm
 */
public class BioPAXToJavaConverter {
    private final String MODEL_PACKAGE_NAME = "org.biopax2.model";
    private final String FTL_DIR = "resources";
    private final String INTERFACE_TEMPLATE_NAME = "OWLToJavaInterface.ftl";
    private final String CLASS_TEMPLATE_NAME = "OWLToJavaClass.ftl";
    private Configuration config = null;
    
    public BioPAXToJavaConverter() throws Exception {
        config = new Configuration();
        config.setDirectoryForTemplateLoading(new File(FTL_DIR));
    }
    
    public void generate(String sourceDir, String bpUri) throws IOException, TemplateException, Exception {
        JenaOWLModel model = ProtegeOWL.createJenaOWLModelFromInputStream(new FileInputStream(bpUri));
        Collection classes = model.getOWLClasses();
        for (Iterator it = classes.iterator(); it.hasNext();) {
            Object cls = it.next();
            if (cls instanceof RDFSNamedClass) {
                RDFSNamedClass namedCls = (RDFSNamedClass) cls;
                if (namedCls.getName().indexOf(":") > -1)
                    continue; // system schema
                outputInterface(namedCls, sourceDir);
                outputClass(namedCls, sourceDir);
            }
        }
    }
    
    private String capFirst(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    
    private void outputClass(RDFSNamedClass cls, String dir) throws IOException, TemplateException {
        JavaClassDefinition clsDef = new JavaClassDefinition();
        String clsName = capFirst(cls.getName());
        clsDef.setName(clsName);
        clsDef.addInterfaceNames(clsName + "I");
        clsDef.setPackageName(MODEL_PACKAGE_NAME);
        Template template = config.getTemplate(CLASS_TEMPLATE_NAME);
        Collection properties = cls.getUnionDomainProperties(true);
        RDFProperty prop = null;
        String variableName = null;
        for (Iterator it = properties.iterator(); it.hasNext();) {
            prop = (RDFProperty) it.next();
            if (prop.getName().indexOf(":") > -1)
                continue;
            variableName = convertPropName(prop.getName());
            clsDef.addVariableName(variableName);
            clsDef.setVariableType(variableName, getType(prop, (OWLNamedClass)cls));
        }
        File file = new File(dir + File.separator + clsName + ".java");
        FileWriter fileWriter = new FileWriter(file);
        template.process(clsDef, fileWriter);
        fileWriter.close();
    }
    
    private String fromXSDTypeToJavaType(String xsdType) {
        String type = null;
        // Built-in types
        if (xsdType.equals("xsd:int"))
            type = "int";
        else if (xsdType.equals("xsd:string"))
            type = "String";
        else if (xsdType.equals("xsd:double"))
            type = "double";
        else if (xsdType.equals("xsd:unsignedLong"))
            type = "long";
        else
            System.out.println("Unhandled Type: " + xsdType);
        return type;
    }
    
    private String getType(RDFProperty prop, OWLNamedClass cls) {
        int max = cls.getMaxCardinality(prop);
        if (max > 1 || max == -1) // -1 means unlimited
            return "java.util.List";
        RDFSDatatype dataType = prop.getRangeDatatype();
        String type = null;
        if (dataType != null) {
            type = dataType.getName();
            type = fromXSDTypeToJavaType(type);
            if (type != null)
                return type;
        }
        Collection ranges = prop.getRanges(true);
        Set types = new HashSet();
        RDFResource range = null;
        for (Iterator it = ranges.iterator(); it.hasNext();) {
            range = (RDFResource) it.next();
            String name = range.getName();
            if (name.indexOf(":") > -1) {
                type = fromXSDTypeToJavaType(name);
            }
            else {
                type = capFirst(name) + "I";
            }
            types.add(type);
        }
        if (types.size() == 0 || types.size() > 1)
            return "Object";
        return (String) types.iterator().next();
    }
    
    private String convertPropName(String propName) {
        StringBuffer rtn = new StringBuffer();
        String[] tokenzier = propName.split("-", 0);
        for (int i = 0; i < tokenzier.length; i++) {
            if (i == 0) {
                rtn.append(tokenzier[i].toLowerCase());
            }
            else {
                rtn.append(capFirst(tokenzier[i].toLowerCase()));
            }
        }
        return rtn.toString();
    }
    
    private void outputInterface(RDFSNamedClass cls, String dir) throws IOException, TemplateException {
        JavaClassDefinition interfaceDef = new JavaClassDefinition();
        String interfaceName = capFirst(cls.getName()) + "I";
        interfaceDef.setName(interfaceName);
        interfaceDef.setPackageName(MODEL_PACKAGE_NAME);
        Collection superClasses = cls.getNamedSuperclasses();
        if (superClasses != null && superClasses.size() > 0) {
            String superClassName = null;
            OWLNamedClass superCls = null;
            for (Iterator it = superClasses.iterator(); it.hasNext();) {
                superCls = (OWLNamedClass) it.next();
                superClassName = superCls.getName();
                if (!(superClassName.indexOf(":") > -1)) {
                    interfaceDef.addSuperClassName(capFirst(superClassName) + "I");
                }
            }
        }
        Template template = config.getTemplate(INTERFACE_TEMPLATE_NAME);
        File file = new File(dir + File.separator + interfaceName + ".java");
        FileWriter fileWriter = new FileWriter(file);
        template.process(interfaceDef, fileWriter);
        fileWriter.close();
    }
    
    public static void main(String[] args) {
        try {
            BioPAXToJavaConverter generator = new BioPAXToJavaConverter();
            generator.generate("src/org/biopax2/model", "resources/biopax-level2.owl");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

}
