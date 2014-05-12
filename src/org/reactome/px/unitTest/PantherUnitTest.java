/*
 * Created on Apr 6, 2006
 *
 */
package org.reactome.px.unitTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class PantherUnitTest extends TestCase {

    public PantherUnitTest() {
        
    }
    
    public void testEntrezIDs() throws Exception {
        String fileName = "/Users/wgm/Documents/EclipseWorkspace/caBigR3/entrez.txt";
        FileReader fileReader = new FileReader(fileName);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        Set<String> ids = new HashSet<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            ids.add(line);
        }
        StringBuilder builder = new StringBuilder();
        int total = 0;
        int size = ids.size();
        int cycle = 1;
        List<String> idList = new ArrayList<String>(ids);
        while (total < size) {
            File file = new File("ids" + cycle + ".txt");
            FileOutputStream fos = new FileOutputStream(file);
            PrintStream ps = new PrintStream(fos);
            for (int i = 0; i < 1000; i++) {
                ps.println(idList.get(total));
                total ++;
                if (total == size)
                    break;
            }
            ps.close();
            fos.close();
            cycle ++;
        }
    }
    
}
