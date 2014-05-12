/*
 * Created on Aug 17, 2005
 *
 */
package org.reactome.model;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * A utility class uses FreeMarker template to generate a list of constants based
 * on the Reactome schema so that these constants to be used by other classes to
 * avoid mistyping.
 * @author guanming
 *
 */
@SuppressWarnings("unchecked")
public class ReactomeJavaConstantsGenerator {
    
    public ReactomeJavaConstantsGenerator() {
        
    }

    public void generate(String javaFileName, MySQLAdaptor dba) throws TemplateException, Exception {
        Schema schema = dba.getSchema();
        Map nameMap = new HashMap();
        nameMap.put("isForBioPAX", Boolean.FALSE);
        Set classNames = new HashSet();
        for (Iterator it = schema.getClassNames().iterator(); it.hasNext();) {
            classNames.add(it.next());
        }
        nameMap.put("classNames", classNames);
        Set propertyNames = new HashSet();
        SchemaClass cls = null;
        SchemaAttribute att = null;
        for (Iterator it = schema.getClasses().iterator(); it.hasNext();) {
            cls = (SchemaClass) it.next();
            for (Iterator it1 = cls.getAttributes().iterator(); it1.hasNext();) {
                att = (SchemaAttribute) it1.next();
                propertyNames.add(att.getName());
            }
        }
        nameMap.put("propertyNames", propertyNames);
        Configuration config = new Configuration();
        config.setDirectoryForTemplateLoading(new File("resources"));
        Template template = config.getTemplate("BioPAXJavaConstants.ftl");
        File file = new File(javaFileName);
        FileWriter fileWriter = new FileWriter(file);
        template.process(nameMap, fileWriter);
        fileWriter.close();
    }
    
    public static void main(String[] args) {
        try {
            ReactomeJavaConstantsGenerator generator = new ReactomeJavaConstantsGenerator();
            MySQLAdaptor dba = new MySQLAdaptor("brie8.cshl.edu",
                                                "gk_central",
                                                "authortool",
                                                "T001test",
                                                3306);
            generator.generate("src/org/reactome/biopax/ReactomeJavaConstants.java", dba);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}
