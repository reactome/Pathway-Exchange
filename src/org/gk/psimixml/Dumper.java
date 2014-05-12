/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.psimixml;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;

import psidev.psi.mi.xml.PsimiXmlVersion;
import psidev.psi.mi.xml.PsimiXmlWriter;
import psidev.psi.mi.xml.model.EntrySet;

/**
 * Dump EntrySets to strings, streams or files.
 * 
 * @author David Croft
 *
 */
public class Dumper {
	public static void dumpToFile(EntrySet entrySet) {
		dumpToFile(entrySet, "psimi.xml");
	}
	
	/**
	 * Dump to the named file.  If filename is null, dump to STDOUT.
	 * 
	 * @param filename
	 */
	public static void dumpToFile(EntrySet entrySet, String filename) {
		if (filename == null)
			System.out.print(dumpToString(entrySet));
		else
			dumpToFile(entrySet, new File(filename));
	}
	
	public static void dumpToFile(EntrySet entrySet, File file) {
		if (entrySet != null) {
	        try {
				PsimiXmlWriter psimiXmlWriter = new PsimiXmlWriter(PsimiXmlVersion.VERSION_254);
				Writer fileWriter = new FileWriter(file);
				psimiXmlWriter.write(entrySet, fileWriter);
				fileWriter.close();
			} catch (Exception e) {
				System.err.println("Dumper.dumpToFile: problem writing to file " + file.getName());
				e.printStackTrace(System.err);
			}
		}
	}

	public static String dumpToString(EntrySet entrySet) {
		if (entrySet != null) {
	        try {
				PsimiXmlWriter psimiXmlWriter = new PsimiXmlWriter(PsimiXmlVersion.VERSION_254);
				StringWriter stringWriter = new StringWriter();
				psimiXmlWriter.write(entrySet, stringWriter);
				stringWriter.close();
				
				return stringWriter.toString();
			} catch (Exception e) {
				System.err.println("Dumper.dumpToString: problem writing to string");
				e.printStackTrace(System.err);
			}
		}
		
		return null;
	}
}
