/*
 * Created on Jun 3, 2005
 */
package org.reactome.px.unitTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.reactome.biopax.ReactomeToBioPAXConverter;
import org.reactome.biopax.ReactomeToBioPAXXMLConverter;

import com.hp.hpl.jena.util.FileUtils;

import edu.stanford.smi.protegex.owl.ProtegeOWL;
import edu.stanford.smi.protegex.owl.jena.JenaOWLModel;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.RDFSNamedClass;

/**
 * 
 * @author wgm
 */
public class RToBConverterUnitTest extends TestCase {

    public RToBConverterUnitTest() {
    }
    
    public void testXMLOutput() {
        try {
            String parentDir = "/Users/wgm/Documents/tmp/";
            String convertDir = parentDir + "convertTest/";
            String xmlConvertDir = parentDir + "convertTestXML/";
            File[] files = new File(convertDir).listFiles();
            for (int i = 0; i < files.length; i++) {
                FileInputStream fis = new FileInputStream(files[i]);
                JenaOWLModel model = ProtegeOWL.createJenaOWLModelFromInputStream(fis);
                FileInputStream fis1 = new FileInputStream(new File(xmlConvertDir + files[i].getName()));
                JenaOWLModel model1 = ProtegeOWL.createJenaOWLModelFromInputStream(fis1);
                // Compare the numbers
                Map map = generateCountMap(model);
                Map map1 = generateCountMap(model1);
                for (Iterator it = map.keySet().iterator(); it.hasNext();) {
                    String clsName = (String) it.next();
                    Integer count = (Integer) map.get(clsName);
                    Integer count1 = (Integer) map1.get(clsName);
                    System.out.println(clsName + ": " + count + " ----> " + count1);
                    assertEquals(count, count1);
                }
                System.out.println(files[i].getName() + " passed. " + " --- " + (i + 1));
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private Map generateCountMap(JenaOWLModel model) {
        Collection classes = model.getOWLClasses();
        Map map1 = new HashMap();
        for (Iterator it = classes.iterator(); it.hasNext();) {
            Object cls = it.next();
            if (cls instanceof RDFSNamedClass) {
                RDFSNamedClass namedCls = (RDFSNamedClass) cls;
                if (namedCls.getName().indexOf(":") > -1)
                    continue; // system schema
                map1.put(namedCls.getLocalName(), new Integer(namedCls.getInstanceCount(false)));
            }
        }
        return map1;
    }
    
    public void testXMLConverer() {
        try {
            long time1 = System.currentTimeMillis();
            ReactomeToBioPAXXMLConverter converter = new ReactomeToBioPAXXMLConverter();
            GKInstance topEvent = getTopLevelEvent();
            converter.setReactomeEvent(topEvent);
            converter.convert();
            Document biopaxModel = converter.getBioPAXModel();
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            String file = "/Users/wgm/Documents/tmp/Test.owl";
            FileOutputStream fos = new FileOutputStream(file);
            outputter.output(biopaxModel, fos);
            fos.close();
            long time2 = System.currentTimeMillis();
            System.out.println("Converting time: " + (time2 - time1));
            FileInputStream fis = new FileInputStream(file);
            // Just want to make sure the output file can be opened under protege.
            JenaOWLModel owlModel = ProtegeOWL.createJenaOWLModelFromInputStream(fis);
            fis.close();
            System.out.println("Loading back: " + (System.currentTimeMillis() - time2));
            System.out.println("Test passed!");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }    
    
    public void testLoadConverted() throws Exception {
        long time1 = System.currentTimeMillis();
        //String fileName = "/Users/wgm/Downloads/biopax/Mus musculus.owl";
        String fileName = "/Users/wgm/Desktop/biopax/Homo sapiens.owl";
        FileInputStream fis = new FileInputStream(fileName);
        // Just want to make sure the output file can be opened under protege.
        JenaOWLModel owlModel = ProtegeOWL.createJenaOWLModelFromInputStream(fis);
        fis.close();
        long time2 = System.currentTimeMillis();
        System.out.println("Loading back: " + (time2 - time1));
        System.out.println("Test passed!");
    }
    
    public void testXMLLoad() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        String fileName = "/Users/wgm/Desktop/biopax/Homo sapiens.owl";
        Document document = builder.build(new File(fileName));
        Element root = document.getRootElement();
        System.out.println(root.getName());
    }
    
    public void testReleasedPathwaysForXMLConverter() {
        try {
            List ids = getReleasedEvents();
            GKInstance event = null;
            for (Iterator it = ids.iterator(); it.hasNext();) {
                long time1 = System.currentTimeMillis();
                event = (GKInstance) it.next();
                System.out.println("Exporting " + event.getDisplayName() + "...");
                convertWithXMLConverter(event);
                System.out.println("Done: " + (System.currentTimeMillis() - time1));
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void testReleasedPathways() {
        try {
            List events = getReleasedEvents();
            GKInstance event = null;
            for (Iterator it = events.iterator(); it.hasNext();) {
                long time1 = System.currentTimeMillis();
                event = (GKInstance) it.next();
                System.out.println("Exporting " + event.getDisplayName() + "...");
                convert(event);
                System.out.println("Done: " + (System.currentTimeMillis() - time1));
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private List getReleasedEvents() throws Exception {
        String fileName = "/Users/wgm/Documents/gkteam/gopal/ver15_topics.txt";
        FileReader fileReader = new FileReader(fileName);
        BufferedReader reader = new BufferedReader(fileReader);
        String line = null;
        int index = 0;
        String id = null;
        Set ids = new HashSet();
        while ((line = reader.readLine()) != null) {
            index = line.indexOf("\t");
            id = line.substring(0, index);
            ids.add(new Long(id));
        }        
//        MySQLAdaptor adaptor = new MySQLAdaptor("localhost", 
//                                                "gk_central_080805",
//                                                "root",
//                                                "macmysql01",
//                                                3306);
        MySQLAdaptor adaptor = new MySQLAdaptor("banon.cshl.edu",
                                                "gk_current",
                                                "curator",
                                                "Ixact1y");
        Long idLong = null;
        List events = new ArrayList(ids.size());
        for (Iterator it = ids.iterator(); it.hasNext();) {
            idLong = (Long) it.next();
            GKInstance event = adaptor.fetchInstance(idLong);
            if (event != null)
                events.add(event);
        }
        return events;
    }
    
    private void convertWithXMLConverter(GKInstance topEvent) throws Exception {
        ReactomeToBioPAXXMLConverter converter = new ReactomeToBioPAXXMLConverter();
        converter.setReactomeEvent(topEvent);
        converter.convert();
        Document biopaxModel = converter.getBioPAXModel();
        String displayName = topEvent.getDisplayName();
        String name = displayName.replaceAll("\\W", "_");
        String fileName = "/Users/wgm/Documents/tmp/convertTest/" + name + ".owl";
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(biopaxModel, new FileOutputStream(fileName));
    }
    
    private void convert(GKInstance topEvent) throws Exception {
        ReactomeToBioPAXConverter converter = new ReactomeToBioPAXConverter();
        converter.setReactomeEvent(topEvent);
        converter.convert();
        OWLModel biopaxModel = converter.getBioPAXModel();
        String displayName = topEvent.getDisplayName();
        String name = displayName.replaceAll("\\W", "_");
        String fileName = "/Users/wgm/Documents/tmp/convertTest/" + name + ".owl";
        Collection errors = new ArrayList();
        ((JenaOWLModel)biopaxModel).save(new File(fileName).toURI(), FileUtils.langXMLAbbrev, errors);        
    }
    
    public void testRToBConvert() {
        try {
            GKInstance topEvent = getTopLevelEvent();
            convert(topEvent);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private GKInstance getTopLevelEvent(Long dbID) throws Exception {
        MySQLAdaptor adaptor = new MySQLAdaptor("brie8.cshl.edu", 
                "gk_central",
                "authortool",
                "T001test",
                3306);
        GKInstance event = adaptor.fetchInstance(dbID);
        return event;
    }
    
    private GKInstance getTopLevelEvent() throws Exception {
        //XMLFileAdaptor reactomeAdaptor = new XMLFileAdaptor();
        //reactomeAdaptor.setSource("/home/wgm/gkteam/wgm/Notch.rtpj");        
        //GKInstance notchPathway = reactomeAdaptor.fetchInstance(new Long(157118));
        MySQLAdaptor adaptor = new MySQLAdaptor("localhost", 
                                                "gk_current_ver19",
                                                "root",
                                                "macmysql01",
                                                3306);
//        MySQLAdaptor adaptor = new MySQLAdaptor("brie8",
//                                                "gk_central",
//                                                "authortool",
//                                                "T001test");
        // Notch Pathway
        //GKInstance event = adaptor.fetchInstance(new Long(157118));
        // For Intrinsic Pathway of Apoptosis
        //GKInstance event = adaptor.fetchInstance(new Long(109606));
        // For regulation testing: Ca regulation
        //GKInstance event = adaptor.fetchInstance(new Long(162599));
        // For apoptosis Pathway
        //GKInstance event = adaptor.fetchInstance(new Long(109581));
        // For DNA Repair Pathway
        //GKInstance event = adaptor.fetchInstance(new Long(73894));
        GKInstance event = adaptor.fetchInstance(70216L);
        return event;
    }
    
}
