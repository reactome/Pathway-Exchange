/*
 * Created on May 24, 2005
 */
package org.reactome.px.unitTest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.PropertyConfigurator;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.reactome.biopax.BioPAXToReactomeConverter;

/**
 * Unit test for converting.
 * @author wgm
 */
public class BToRConverterUnitTest extends TestCase {
    //private final String dirName = "/Users/wgm/Documents/caBIG_R3/datasets/cellmap_may_2006/";
    //private final String dirName = "/Users/wgm/Documents/caBIG_R3/datasets/INOH/AllBioPAX_level2_060802/";
    //private final String dirName = "datasets/NCI-Pathways/121808/";
    //private final String dirName = "/Users/wgm/Documents/caBIG_R3/datasets/HPRD/";
    //private final String dirName = "datasets/NCI-Pathways/031709/";
    //private final String dirName = "/Users/wgm/Documents/gkteam/Liya/";
    private final String dirName = "/home/preecej/Documents/projects/plant_reactome/aracyc_to_reactome_conversion/aracyc_data/aracyc_v11/";
    //private final String dirName = "/home/preecej/Documents/projects/reactome/aracyc_to_reactome_conversion/chorismate_sample/";
    
    public BToRConverterUnitTest() {      
    }
    
    protected void setUp() throws Exception {
        //BasicConfigurator.configure();
        PropertyConfigurator.configure("resources/log4j.properties");
        super.setUp();
    }
    
    private MySQLAdaptor getDBA() throws Exception {
//        MySQLAdaptor dba = new MySQLAdaptor("localhost",
//                                            "gk_central_101606",
//                                            "root",
//                                            "macmysql01",
//                                            3306);
//        MySQLAdaptor dba = new MySQLAdaptor("localhost",
//                                            "gk_central_121108",
//                                            "root",
//                                            "macmysql01",
//                                            3306);
//        MySQLAdaptor dba = new MySQLAdaptor("localhost",
//                                            "rice_reactome_v3_1",
//                                            "root",
//                                            "macmysql01",
//                                            3306);
        MySQLAdaptor dba = new MySQLAdaptor("floret.cgrb.oregonstate.edu",
                "gk_central_121613",
                "reactome_aracyc",
                "r3actom3_aracyc",
                3306);
        return dba;
    }
    
    public void testBatchConvert() throws Exception {
        BioPAXToReactomeConverter converter = new BioPAXToReactomeConverter();
//        MySQLAdaptor dba = new MySQLAdaptor("localhost",
//                                            "gk_central_101606",
//                                            "root",
//                                            "macmysql01",
//                                            3306);
        MySQLAdaptor dba = getDBA();
        converter.setReactomeDB(dba);
        File dir = new File(dirName);
        String[] files = dir.list();
        List<String> fileNames = new ArrayList<String>();
        for (String tmp : files) {
            if (!tmp.endsWith(".owl"))
                continue;
            // Take only the curated pathway
            if (tmp.contains("Curated"))
                fileNames.add(dirName + tmp);
        }
        converter.convert(fileNames);
        XMLFileAdaptor fileAdaptor = converter.getReactomeModel();
        //String rtpjName = dirName + "CellMap.rtpj";
        //String rtpjName = dirName + "INHO.rtpj";
        String rtpjName = dirName + "NCI-Nature_Curated.rtpj";
        fileAdaptor.save(rtpjName);
    }
//    
//    public void testPaxTool() throws Exception {
//        JenaIOHandler janaHandler = new JenaIOHandler();
//        //String fileName = "/Users/wgm/Downloads/biopax-1/Homo sapiens.owl";
//        String fileName = "/Users/wgm/Downloads/Reactome_188383-2.owl";
//        FileInputStream fis = new FileInputStream(fileName);
//        Model model = janaHandler.convertFromOWL(fis);
//        Set<BioPAXElement> objects = model.getObjects();
//        System.out.println("Total objects: " + objects.size());
//    }
        
    /**
     * Convert a single BioPAX level 2 file into the Reactome format.
     */
    /**
     * @throws Exception
     */
    public void testBToRConvert() throws Exception {
        BioPAXToReactomeConverter converter = new BioPAXToReactomeConverter();
        //String fileName = dirName + "200010.owl";
        //String fileName = dirName + "FASSignalling.owl";
        //String fileName = dirName + "Hedgehog.owl";
        //String fileName = dirName + "EGF.owl";
        //String fileName = dirName + "NCI-Nature_Curated.owl";
        //String fileName = dirName + "BioCarta.owl";
        //String fileName = "/Users/wgm/Downloads/biopax-1/Homo sapiens.owl";
        //String fileName = "/Users/wgm/Documents/gkteam/Liya/chorismate_biosynthesis_040510.owl";
        //String fileName = "/Users/wgm/Documents/gkteam/Liya/ricecyc_v3_0_biopax2.owl";
        //String fileName = "/Documents/projects/reactome/AraCyc to Reactome Conversion/aracyc_v7_0_biopax-level2.owl";
        //String fileName = dirName + "aracyc_v7_0_biopax-level2_STOIdouble_AUTHORSYEAR.owl";
        //String fileName = dirName + "aracyc_v8_0_biopax-level2_STOIdouble_AUTHORSYEAR_STEPINT.owl";
        //String fileName = dirName + "aracyc_v10_biopax-level2_STOIdouble_noCANCELchar.owl";
        String fileName = dirName + "aracyc_v11_biopax-level2_STOIdouble.owl";
        //String fileName = dirName + "tryptophan_biosynthesis_pathway-biopax-lvl2.STOIdouble.owl";
        //String fileName = dirName + "ara8test.owl";
        //String fileName = dirName + "allantoin_test.owl";
        //String fileName = dirName + "chorismate_biosynthesis_040610_STOIdouble.owl";
        //String fileName = dirName + "aracyc_chorismate_test.owl";
        //NciPIDBToRPostProcessor postProcessor = (NciPIDBToRPostProcessor) converter.getMapperFactory().getPostProcessor();
        //postProcessor.setDataSourceName("BioCarta - Imported by PID");
        
        MySQLAdaptor dba = getDBA();
        converter.setReactomeDB(dba);
        long time1 = System.currentTimeMillis();
        converter.convert(fileName);
        long time2 = System.currentTimeMillis();
        System.out.println("Time for converting: " + (time2 - time1));
        XMLFileAdaptor reactomeAdaptor = converter.getReactomeModel();
//        reactomeAdaptor.save(dirName + "ricecyc_v3_0_biopax2_before_dump_120910_test_only.rtpj");
//        reactomeAdaptor.save(dirName + "ricecyc_v3_0_biopax2_0709.rtpj");
//        reactomeAdaptor.save(dirName + "Chorismate_pathway_040610.rtpj");
//        reactomeAdaptor.save(dirName + "ricecyc_v3_0_biopax2_justin_preece_test.rtpj");
//        reactomeAdaptor.save(dirName + "chorismate_justin_preece_test_2.rtpj");
//        reactomeAdaptor.save(dirName + "aracyc_v7_0_biopax-level2.rtpj");
//        reactomeAdaptor.save(dirName + "tryptophan_biosynthesis_pathway-biopax-lvl2.2.rtpj");
        reactomeAdaptor.save(dirName + "aracyc_v11_biopax-level2.rtpj.5.part_1");
        //reactomeAdaptor.save(dirName + "aracyc_chorismate_test.rtpj");
        //reactomeAdaptor.save(dirName + "NCI-Nature_Curated.rtpj");
        //reactomeAdaptor.save(dirName + "BioCarta.rtpj");
    }

    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configure("resources/log4j.properties");
        BToRConverterUnitTest test = new BToRConverterUnitTest();
        test.testBToRConvert();
    }
    
}
