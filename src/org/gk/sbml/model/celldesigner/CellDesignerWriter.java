/* Copyright (c) 2013 European Bioinformatics Institute and Cold Spring Harbor Laboratory. */

package org.gk.sbml.model.celldesigner;

import java.io.PrintStream;

import javax.xml.stream.XMLStreamException;

import org.gk.sbml.model.elements.Document;
import org.gk.sbml.model.elements.Writer;
import org.gk.sbml.simcd.SimCDException;
import org.gk.sbml.simcd.SimCDWriter;

/**
 * Wraps org.sbml.jsbml.Writer.
 * 
 * @author David Croft
 *
 */
public class CellDesignerWriter implements Writer {
	@Override
	public boolean writeSBML(Document document) {
		SimCDWriter sbmlWriter = new SimCDWriter();
		
		try {
			PrintStream outputStream = System.out;
			sbmlWriter.write(((org.gk.sbml.model.jsbml.JsbmlDocument)document).getSbmlElement(), outputStream);
			outputStream.println("");
		} catch (SimCDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
}
