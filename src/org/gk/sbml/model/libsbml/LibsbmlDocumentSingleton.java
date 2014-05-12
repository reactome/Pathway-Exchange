/* Copyright (c) 2011 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.libsbml;

import org.gk.sbml.model.elements.Document;
import org.gk.sbml.model.elements.DocumentSingleton;


/**
 * Document creating singleton.  Exists because libSBML needs to use JNI to connect to a C++
 * library.  For other SBML libraries, it may be a bit redundant, but will still
 * exist in skeletal form.
 * 
 * @author David Croft
 *
 */
public class LibsbmlDocumentSingleton extends DocumentSingleton {
	public Document createNewDocument(int level, int version) {
		return new LibsbmlDocument(level, version);
	}

	/**
	 * The following static block is needed in order to load the
	 * the libSBML Java module when the application starts.
	 */
	static
	{
		String varname;
		String shlibname;

		System.err.println("SBMLDocumentSingleton: entered static block");

		if (System.getProperty("mrj.version") != null) {
			varname = "DYLD_LIBRARY_PATH";    // We're on a Mac.
			shlibname = "libsbmlj.jnilib and/or libsbml.dylib";
		} else {
			varname = "LD_LIBRARY_PATH";      // We're not on a Mac.
			shlibname = "libsbmlj.so and/or libsbml.so";
		}

		System.err.println("SBMLDocumentSingleton: varname=" + varname + ", shlibname=" + shlibname);

		try {
			System.loadLibrary("sbmlj");
			// For extra safety, check that the jar file is in the classpath.
			Class.forName("org.sbml.libsbml.libsbml");
		} catch (UnsatisfiedLinkError e) {
			System.err.println("Error encountered while attempting to load libSBML:");
			e.printStackTrace();
			System.err.println("Please check the value of your " + varname +
					" environment variable and/or" +
					" your 'java.library.path' system property" +
					" (depending on which one you are using) to" +
					" make sure it lists all the directories needed to" +
					" find the " + shlibname + " library file and the" +
			" libraries it depends upon (e.g., the XML parser).");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.err.println("Error: unable to load the file libsbmlj.jar." +
					" It is likely your -classpath option and/or" +
					" CLASSPATH environment variable do not" +
			" include the path to the file libsbmlj.jar.");
		} catch (SecurityException e) {
			System.err.println("Error encountered while attempting to load libSBML:");
			e.printStackTrace();
			System.err.println("Could not load the libSBML library files due to a"+
			" security exception.\n");
		}

		System.err.println("SBMLDocumentSingleton: exiting static block");
	}
}
