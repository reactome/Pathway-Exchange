/*
 * Created on Aug 1, 2012
 *
 */
package org.reactome.px.unitTest;

import java.util.ArrayList;
import java.util.List;

import org.gk.persistence.MySQLAdaptor;
import org.gk.sbml.SBMLAndLayoutBuilderFields;
import org.junit.Test;

/**
 * A simple test class for SBML.
 * @author gwu
 *
 */
public class SBMLConvertTest {
    
    public SBMLConvertTest() {
        
    }
    
    @Test
    public void testSBMLConvert() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "gk_current_ver41", 
                                            "root", 
                                            "macmysql01");
        SBMLAndLayoutBuilderFields sbmlAndLayoutBuilder = new SBMLAndLayoutBuilderFields();
        sbmlAndLayoutBuilder.getDatabaseConnectionHandler().setDatabaseAdaptor(dba);
        // Test for a pathway
        Long id = 109581L;
        List<String> values = new ArrayList<String>();
        values.add(id.toString());
        sbmlAndLayoutBuilder.addField("id", values);
        values = new ArrayList<String>();
        String layout = "SBGN";
        values.add(layout);
        sbmlAndLayoutBuilder.addField("layout", values);
        
        sbmlAndLayoutBuilder.convertPathways();
        String result = sbmlAndLayoutBuilder.getSbmlBuilder().sbmlString();
        System.out.println(result);
    }
    
}
