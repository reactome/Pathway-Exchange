/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import java.io.FileNotFoundException;

import javax.xml.stream.XMLStreamException;

import org.gk.sbml.model.elements.Document;
import org.gk.sbml.model.elements.Lib;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.SBMLException;

/**
 * Wraps org.sbml.jsbml.Lib.
 * 
 * @author David Croft
 *
 */
public class CellDesignerLib extends Lib {
	@Override
	public String writeSBMLToString(Document document) {
		try {
			return JSBML.writeSBMLToString(((CellDesignerDocument)document).getSbmlElement()) + "\n";
		} catch (SBMLException e) {
			System.err.println("JsbmlLib.writeSBMLToString: WARNING");
			e.printStackTrace(System.err);
		} catch (XMLStreamException e) {
			System.err.println("JsbmlLib.writeSBMLToString: WARNING");
			e.printStackTrace(System.err);
		}
		return null;
	}

	@Override
	public int writeSBMLToFile(Document document, String filename) {
		try {
			JSBML.writeSBML(((CellDesignerDocument)document).getSbmlElement(), filename);
		} catch (SBMLException e) {
			System.err.println("JsbmlLib.writeSBMLToFile: WARNING");
			e.printStackTrace(System.err);
		} catch (FileNotFoundException e) {
			System.err.println("JsbmlLib.writeSBMLToFile: WARNING");
			e.printStackTrace(System.err);
		} catch (XMLStreamException e) {
			System.err.println("JsbmlLib.writeSBMLToFile: WARNING");
			e.printStackTrace(System.err);
		}
		return 0;
	}
}
