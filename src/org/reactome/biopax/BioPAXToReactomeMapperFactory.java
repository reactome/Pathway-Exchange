/*
 * Created on Jul 24, 2006
 *
 */
package org.reactome.biopax;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import edu.stanford.smi.protegex.owl.model.OWLIndividual;
import edu.stanford.smi.protegex.owl.model.RDFSClass;

/**
 * This factory class is used to create BioPAXToReactomeMapper objects. An XML configuration
 * file is used to configure BioPAXToReactomeMappers to be used.
 * @author guanming
 *
 */
public class BioPAXToReactomeMapperFactory {
    private final String CONFIG_FILE_NAME = "resources/BioPAXToReactomeMappers.xml";
    // This map is used to ensure mapped BioPAXToReactomeMappers are singletons.
    private Map<String, AbstractBioPAXToReactomeMapper> nameToMapper;
    private Map<String, BioPAXToReactomeMapper> bpNameToMapper;
    private Logger logger = Logger.getLogger(BioPAXToReactomeMapperFactory.class);
    private BioPAXToReactomePostProcessor postProcessor;
    // This class should be used as a Singleton
    private static BioPAXToReactomeMapperFactory factory;
    
    private BioPAXToReactomeMapperFactory() {
        init();
    }
    
    public static BioPAXToReactomeMapperFactory getFactory() {
        if (factory == null)
            factory = new BioPAXToReactomeMapperFactory();
        return factory;
    }
    
    private void init() {
        nameToMapper = new HashMap<String, AbstractBioPAXToReactomeMapper>();
        bpNameToMapper = new HashMap<String, BioPAXToReactomeMapper>();
        try {
            // Load the XML configuation file
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new File(CONFIG_FILE_NAME));
            Element root = doc.getRootElement();
            String packageName = root.getAttributeValue("package");
            List mappers = root.getChildren("class");
            AbstractBioPAXToReactomeMapper mapper;
            for (Iterator it = mappers.iterator(); it.hasNext();) {
                Element elm = (Element) it.next();
                String bpClsName = elm.getAttributeValue("biopax");
                List children = elm.getChildren();
                mapper = null;
                for (Iterator it1 = children.iterator(); it1.hasNext();) {
                    Element elm1 = (Element) it1.next();
                    if (elm1.getName().equals("mapper")) {
                        String clsName = elm1.getAttributeValue("name");
                        mapper = getMapper(clsName, packageName);
                    }
                    else if (elm1.getName().equals("super")) {
                        String clsName = elm1.getAttributeValue("name");
                        AbstractBioPAXToReactomeMapper superMapper = getMapper(clsName, packageName);
                        mapper.addSuperMapper(superMapper);
                    }
                }
                bpNameToMapper.put(bpClsName, mapper);
            }
            // Check if postProcessor is specified
            Element postProcessorElm = root.getChild("postProcessor");
            if (postProcessorElm != null) {
                String clsName = postProcessorElm.getAttributeValue("class");
                // Make sure full name is used
                if (clsName.indexOf(".") < 0) 
                    clsName = packageName + "." + clsName;
                // This is a full name
                postProcessor = (BioPAXToReactomePostProcessor) Class.forName(clsName).newInstance();
            }
        }
        catch(Exception e) {
            logger.error("Error in parsing xml", e);
        }
    }
    
    private AbstractBioPAXToReactomeMapper getMapper(String clsName, String packageName) throws Exception {
        AbstractBioPAXToReactomeMapper mapper = nameToMapper.get(clsName);
        if (mapper == null) {
            Class cls = Class.forName(packageName + "." + clsName);
            mapper = (AbstractBioPAXToReactomeMapper) cls.newInstance();
            nameToMapper.put(clsName, mapper);
        }
        return mapper;
    }
    
    public BioPAXToReactomeMapper getMapper(OWLIndividual bpInstance) { 
        RDFSClass cls = bpInstance.getRDFType();
        String clsName = cls.getLocalName();
        BioPAXToReactomeMapper mapper = bpNameToMapper.get(clsName);
        if (mapper != null)
            return mapper;
        // Try to walk up the class hierarchy
        Collection superCls = cls.getSuperclasses(false);
        Set<RDFSClass> next = new HashSet<RDFSClass>();
        Set<RDFSClass> current = new HashSet<RDFSClass>();
        // Push superCls into current
        for (Iterator it = superCls.iterator(); it.hasNext();) {
            RDFSClass tmp = (RDFSClass) it.next();
            current.add(tmp);
        }
        while (current.size() > 0) {
            for (RDFSClass tmp : current) {
                mapper = bpNameToMapper.get(tmp.getLocalName());
                if (mapper != null) // Take the first one. The configuration file should
                    return mapper;  // make the mapping correct if multiple inheritance is
                                    // used.
                superCls = tmp.getSuperclasses(false);
                if (superCls != null)
                    for (Iterator it = superCls.iterator(); it.hasNext();)
                        next.add((RDFSClass) it.next());
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
        return null;
    }
    
    public BioPAXToReactomePostProcessor getPostProcessor() {
        return this.postProcessor;
    }
    
    public static void main(String[] args) {
        new BioPAXToReactomeMapperFactory();
    }
    
}
