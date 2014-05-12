/*
 * Created on Jun 20, 2005
 */
package org.reactome.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Iterator;

import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * This class is used to auto-generate Java classes for the Reactome data schema.
 * @author wgm
 */
public class ReactomeToJavaConverter {
    private final String MODEL_PACKAGE_NAME = "org.reactome.model";
    private final String FTL_DIR = "resources";
    private final String INTERFACE_TEMPLATE_NAME = "OWLToJavaInterface.ftl";
    private final String CLASS_TEMPLATE_NAME = "OWLToJavaClass.ftl";
    private Configuration config = null;
    
    public ReactomeToJavaConverter() throws IOException {
        config = new Configuration();
        config.setDirectoryForTemplateLoading(new File(FTL_DIR));
    }
    
    public void generate(String sourceDir, String schemaFileName) throws IOException, TemplateException, Exception {
        FileInputStream fis = new FileInputStream(schemaFileName);
        ObjectInputStream ois = new ObjectInputStream(fis);
        GKSchema schema = (GKSchema) ois.readObject();
        ois.close();
        fis.close();
        Collection clsNames = schema.getClassNames();
        String clsName = null;
        GKSchemaClass cls = null;
        for (Iterator it = clsNames.iterator(); it.hasNext();) {
            clsName = (String) it.next();
            cls = (GKSchemaClass) schema.getClassByName(clsName);
            //outputInterface(cls, sourceDir);
            outputClass(cls, sourceDir);
        }
    }
    
    private void outputInterface(GKSchemaClass cls, String sourceDir) throws Exception {
        String interfaceName = cls.getName() + "I";
        JavaClassDefinition interfaceDef = new JavaClassDefinition();
        interfaceDef.setPackageName(MODEL_PACKAGE_NAME);
        interfaceDef.setName(interfaceName);
        interfaceDef.setPackageName(MODEL_PACKAGE_NAME);
        Collection superClasses = cls.getSuperClasses();
        if (superClasses != null && superClasses.size() > 0) {
            String superClassName = null;
            for (Iterator it = superClasses.iterator(); it.hasNext();) {
                superClassName = ((SchemaClass)it.next()).getName();
                if (!(superClassName.indexOf(":") > -1)) {
                    interfaceDef.addSuperClassName(superClassName + "I");
                }
            }
        }
        Template template = config.getTemplate(INTERFACE_TEMPLATE_NAME);
        File file = new File(sourceDir + File.separator + interfaceName + ".java");
        FileWriter fileWriter = new FileWriter(file);
        template.process(interfaceDef, fileWriter);
        fileWriter.close();
    }
    
    private void outputClass(GKSchemaClass cls, String sourceDir) throws Exception {
        JavaClassDefinition clsDef = new JavaClassDefinition();
        String clsName = cls.getName();
        clsDef.setName(clsName);
        clsDef.addInterfaceNames(clsName + "I");
        clsDef.setPackageName(MODEL_PACKAGE_NAME);
        Template template = config.getTemplate(CLASS_TEMPLATE_NAME);
        Collection properties = cls.getAttributes();
        GKSchemaAttribute att = null;
        String variableName = null;
        for (Iterator it = properties.iterator(); it.hasNext();) {
            att = (GKSchemaAttribute) it.next();
            variableName = att.getName();
            clsDef.addVariableName(variableName);
            clsDef.setVariableType(variableName, getType(att));
        }
        File file = new File(sourceDir + File.separator + clsName + ".java");
        FileWriter fileWriter = new FileWriter(file);
        template.process(clsDef, fileWriter);
        fileWriter.close();
    }
    
    private String getType(GKSchemaAttribute att) {
        if (att.isMultiple())
            return "java.util.List";
        int type = att.getTypeAsInt();
        switch (type) {
        	case SchemaAttribute.FLOAT_TYPE :
        	    return "float";
        	case SchemaAttribute.INSTANCE_TYPE :
        	    Collection allowedTypes = att.getAllowedClasses();
        		if (allowedTypes.size() > 1)
        		    return "Object"; // Cannot define a way
        		SchemaClass cls = (SchemaClass) allowedTypes.iterator().next();
        		return cls.getName();
        	case SchemaAttribute.INTEGER_TYPE :
        	    return "int";
        	case SchemaAttribute.LONG_TYPE :
        	    return "long";
        	case SchemaAttribute.STRING_TYPE :
        	    return "String";
        	case SchemaAttribute.BOOLEAN_TYPE :
        	    return "boolean";
        	default : // Don't know any type
        	    return "Object";
        }
    }
    
    public static void main(String[] args) {
        try {
            ReactomeToJavaConverter generator = new ReactomeToJavaConverter();
            generator.generate("src/org/reactome/model", "resources/schema");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}
