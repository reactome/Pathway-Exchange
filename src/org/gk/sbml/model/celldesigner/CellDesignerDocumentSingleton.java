/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

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
public class CellDesignerDocumentSingleton extends DocumentSingleton {
	public Document createNewDocument(int level, int version) {
		return new CellDesignerDocument(level, version);
	}
}
